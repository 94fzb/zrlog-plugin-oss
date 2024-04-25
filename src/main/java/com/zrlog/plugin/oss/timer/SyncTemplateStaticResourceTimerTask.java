package com.zrlog.plugin.oss.timer;

import com.google.gson.Gson;
import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.common.IOUtil;
import com.zrlog.plugin.common.IdUtil;
import com.zrlog.plugin.common.LoggerUtil;
import com.zrlog.plugin.common.modle.BlogRunTime;
import com.zrlog.plugin.common.modle.TemplatePath;
import com.zrlog.plugin.data.codec.ContentType;
import com.zrlog.plugin.data.codec.MsgPacketStatus;
import com.zrlog.plugin.oss.FileUtils;
import com.zrlog.plugin.oss.Md5Utils;
import com.zrlog.plugin.oss.entry.UploadFile;
import com.zrlog.plugin.oss.service.UploadService;
import com.zrlog.plugin.type.ActionType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SyncTemplateStaticResourceTimerTask extends TimerTask {

    private static final Logger LOGGER = LoggerUtil.getLogger(SyncTemplateStaticResourceTimerTask.class);

    private final IOSession session;

    private final Map<String, Object> fileInfoCacheMap = new TreeMap<>();
    private final String cacheKeyMapKey = "cacheMap";

    public SyncTemplateStaticResourceTimerTask(IOSession session) {
        this.session = session;
    }

    private List<UploadFile> cacheFiles(BlogRunTime blogRunTime, Map<String, String> responseMap) {
        if (!"on".equals(responseMap.get("syncHtml"))) {
            return new ArrayList<>();
        }
        String cacheFolder = new File(blogRunTime.getPath()).getParent() + "/cache/zh_CN";
        File cacheFile = new File(cacheFolder);
        List<UploadFile> uploadFiles = new ArrayList<>();
        if (cacheFile.exists()) {
            File[] fs = cacheFile.listFiles();
            fillToUploadFiles(Arrays.asList(fs), cacheFolder, uploadFiles);
        }
        return uploadFiles;
    }

    private List<UploadFile> templateUploadFiles(BlogRunTime blogRunTime, Map<String, String> responseMap, TemplatePath templatePath) {
        if (!"on".equals(responseMap.get("syncTemplate"))) {
            return new ArrayList<>();
        }
        File templateFilePath = new File(blogRunTime.getPath() + templatePath.getValue());
        if (!templateFilePath.isDirectory()) {
            LOGGER.log(Level.INFO, "Template path not directory " + templateFilePath);
            return new ArrayList<>();
        }
        File propertiesFile = new File(templateFilePath + "/template.properties");
        if (!propertiesFile.exists()) {
            LOGGER.log(Level.SEVERE, "Template properties error " + templateFilePath);
            return new ArrayList<>();
        }
        List<UploadFile> uploadFiles = new ArrayList<>();
        Properties prop = new Properties();

        try (FileInputStream fileInputStream = new FileInputStream(propertiesFile)) {
            prop.load(fileInputStream);
            String staticResource = (String) prop.get("staticResource");
            List<File> fileList = new ArrayList<>(getStaticFolderFiles(staticResource, templateFilePath, blogRunTime));
            fillToUploadFiles(fileList, blogRunTime.getPath(), uploadFiles);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "", e);
        }
        return uploadFiles;
    }

    private void preloadCache(Map<String, String> responseMap) {
        String cacheMapStr = responseMap.get(cacheKeyMapKey);
        if (Objects.nonNull(cacheMapStr) && !cacheMapStr.isEmpty()) {
            fileInfoCacheMap.putAll(new Gson().fromJson(cacheMapStr, Map.class));
        }
    }

    private void saveCacheToDb() {
        Map<String, String> newCacheMap = new TreeMap<>();
        newCacheMap.put(cacheKeyMapKey, new Gson().toJson(fileInfoCacheMap));
        session.sendJsonMsg(newCacheMap, ActionType.SET_WEBSITE.name(), IdUtil.getInt(), MsgPacketStatus.SEND_REQUEST);
    }

    @Override
    public void run() {
        Map<String, Object> map = new HashMap<>();
        map.put("key", "syncTemplate,syncHtml," + cacheKeyMapKey);
        session.sendJsonMsg(map, ActionType.GET_WEBSITE.name(), IdUtil.getInt(), MsgPacketStatus.SEND_REQUEST, msgPacket -> {
            try {
                Map<String, String> responseMap = new Gson().fromJson(msgPacket.getDataStr(), Map.class);
                preloadCache(responseMap);
                TemplatePath templatePath = session.getResponseSync(ContentType.JSON, new HashMap<>(), ActionType.CURRENT_TEMPLATE, TemplatePath.class);
                BlogRunTime blogRunTime = session.getResponseSync(ContentType.JSON, new HashMap<>(), ActionType.BLOG_RUN_TIME, BlogRunTime.class);
                List<UploadFile> uploadFiles = new ArrayList<>();
                uploadFiles.addAll(templateUploadFiles(blogRunTime, responseMap, templatePath));
                uploadFiles.addAll(cacheFiles(blogRunTime, responseMap));
                if (uploadFiles.isEmpty()) {
                    return;
                }
                new UploadService().upload(session, uploadFiles);
                saveCacheToDb();
            } catch (Exception e) {
                LOGGER.warning("Sync error " + e.getMessage());
            }
        });
    }

    private static List<File> getStaticFolderFiles(String staticResource, File templateFilePath, BlogRunTime blogRunTime) {
        List<File> fileList = new ArrayList<>();
        if (staticResource != null && !staticResource.isEmpty()) {
            String[] staticFileArr = staticResource.split(",");
            for (String sFile : staticFileArr) {
                fileList.add(new File(templateFilePath + "/" + sFile));
            }
        }
        File faviconIco = new File(blogRunTime.getPath() + "/favicon.ico");
        if (faviconIco.exists()) {
            fileList.add(faviconIco);
        }
        return fileList;
    }


    private void fillToUploadFiles(List<File> files, String startPath, List<UploadFile> uploadFiles) {
        List<File> fullFileList = new ArrayList<>();
        for (File file : files) {
            FileUtils.getAllFiles(file.toString(), fullFileList);
        }
        if (!startPath.endsWith("/")) {
            startPath = startPath + "/";
        }
        for (File file : fullFileList) {
            if (!file.exists()) {
                continue;
            }
            if (file.isFile()) {
                try (FileInputStream inputStream = new FileInputStream(file)) {
                    String md5 = Md5Utils.md5(IOUtil.getByteByInputStream(inputStream));
                    if (fileInfoCacheMap.get(file.toString()) == null || !Objects.equals(fileInfoCacheMap.get(file.toString()), md5)) {
                        UploadFile uploadFile = new UploadFile();
                        uploadFile.setFile(file);
                        uploadFile.setRefresh(true);
                        String key = file.toString().substring(startPath.length());
                        uploadFile.setFileKey(key);
                        uploadFiles.add(uploadFile);
                        fileInfoCacheMap.put(file.toString(), md5);
                    }
                } catch (IOException e) {
                    LOGGER.warning("md5 error " + file.getAbsolutePath());
                }

            } else if (file.isDirectory()) {
                File[] fs = file.listFiles();
                if (fs.length == 0) {
                    continue;
                }
                fillToUploadFiles(Arrays.asList(fs), startPath, uploadFiles);
            }
        }
    }

}
