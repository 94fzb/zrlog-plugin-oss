package com.zrlog.plugin.oss.timer;

import com.google.gson.Gson;
import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.common.HexaConversionUtil;
import com.zrlog.plugin.common.IOUtil;
import com.zrlog.plugin.common.IdUtil;
import com.zrlog.plugin.common.LoggerUtil;
import com.zrlog.plugin.common.modle.BlogRunTime;
import com.zrlog.plugin.common.modle.TemplatePath;
import com.zrlog.plugin.data.codec.ContentType;
import com.zrlog.plugin.data.codec.MsgPacket;
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
import java.util.stream.Collectors;

public class SyncTemplateStaticResourceTimerTask extends TimerTask {

    private static final Logger LOGGER = LoggerUtil.getLogger(SyncTemplateStaticResourceTimerTask.class);

    private final IOSession session;

    private final Map<String, Object> fileWatcherMap = new HashMap<>();

    public SyncTemplateStaticResourceTimerTask(IOSession session) {
        this.session = session;
    }

    private List<UploadFile> cacheFiles(BlogRunTime blogRunTime) {
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

    @Override
    public void run() {
        Map<String, Object> map = new HashMap<>();
        String cacheKey = "cacheInfo_hex";
        map.put("key", "syncTemplate,access_key,secret_key,host,region,supportHttps,bucket," + cacheKey);
        session.sendJsonMsg(map, ActionType.GET_WEBSITE.name(), IdUtil.getInt(), MsgPacketStatus.SEND_REQUEST, msgPacket -> {
            Map<String, String> responseMap = new Gson().fromJson(msgPacket.getDataStr(), Map.class);
            LOGGER.info(new Gson().toJson(responseMap));
            String cacheStr = responseMap.get(cacheKey);
            if (Objects.nonNull(cacheStr) && !cacheStr.trim().isEmpty()) {
                fileWatcherMap.putAll(new Gson().fromJson(new String(HexaConversionUtil.HexString2Bytes(cacheStr)), Map.class));
            }
            TemplatePath templatePath = session.getResponseSync(ContentType.JSON, new HashMap<>(), ActionType.CURRENT_TEMPLATE, TemplatePath.class);
            BlogRunTime blogRunTime = session.getResponseSync(ContentType.JSON, new HashMap<>(), ActionType.BLOG_RUN_TIME, BlogRunTime.class);
            List<UploadFile> uploadFiles = new ArrayList<>();
            uploadFiles.addAll(templateUploadFiles(blogRunTime, responseMap, templatePath));
            uploadFiles.addAll(cacheFiles(blogRunTime));
            if (uploadFiles.isEmpty()) {
                return;
            }
            new UploadService().upload(session, uploadFiles);
            Map<String, String> hashMap = new HashMap<>();
            hashMap.put(cacheKey, HexaConversionUtil.bytesToHexString(new Gson().toJson(fileWatcherMap).getBytes()));
            session.sendMsg(new MsgPacket(hashMap, ContentType.JSON, MsgPacketStatus.SEND_REQUEST, IdUtil.getInt(), ActionType.SET_WEBSITE.name()));
            new RefreshCdnWorker(responseMap.get("access_key"), responseMap.get("secret_key"), responseMap.get("region"), responseMap.get("host"), Objects.equals("on", responseMap.get("supportHttps")), uploadFiles.stream().map(UploadFile::getFileKey).collect(Collectors.toList())).run();
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
                    if (fileWatcherMap.get(file.toString()) == null || Objects.equals(fileWatcherMap.get(file.toString()), md5)) {
                        UploadFile uploadFile = new UploadFile();
                        uploadFile.setFile(file);
                        String key = file.toString().substring(startPath.length());
                        uploadFile.setFileKey(key);
                        uploadFiles.add(uploadFile);
                        fileWatcherMap.put(file.toString(), md5);
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
