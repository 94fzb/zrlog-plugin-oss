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
import com.zrlog.plugin.oss.entry.UploadFile;
import com.zrlog.plugin.oss.service.UploadService;
import com.zrlog.plugin.type.ActionType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;

public class SyncTemplateStaticResourceTimerTask extends TimerTask {

    private static final Logger LOGGER = LoggerUtil.getLogger(SyncTemplateStaticResourceTimerTask.class);

    private final IOSession session;

    private final Map<String, Long> fileWatcherMap = new HashMap<>();

    public SyncTemplateStaticResourceTimerTask(IOSession session) {
        this.session = session;
    }

    @Override
    public void run() {
        Map<String, Object> map = new HashMap<>();
        map.put("key", "syncTemplate");
        session.sendJsonMsg(map, ActionType.GET_WEBSITE.name(), IdUtil.getInt(), MsgPacketStatus.SEND_REQUEST, msgPacket -> {
            Map<String, String> responseMap = new Gson().fromJson(msgPacket.getDataStr(), Map.class);
            if (!"on".equals(responseMap.get("syncTemplate"))) {
                return;
            }
            TemplatePath templatePath = session.getResponseSync(ContentType.JSON, new HashMap<>(), ActionType.CURRENT_TEMPLATE, TemplatePath.class);
            BlogRunTime blogRunTime = session.getResponseSync(ContentType.JSON, new HashMap<>(), ActionType.BLOG_RUN_TIME, BlogRunTime.class);
            File templateFilePath = new File(blogRunTime.getPath() + templatePath.getValue());
            if (!templateFilePath.isDirectory()) {
                LOGGER.log(Level.INFO, "Template path not directory " + templateFilePath);
            }
            File propertiesFile = new File(templateFilePath + "/template.properties");
            if (!propertiesFile.exists()) {
                LOGGER.log(Level.SEVERE, "Template properties error " + templateFilePath);

                return;
            }
            Properties prop = new Properties();
            try (FileInputStream fileInputStream = new FileInputStream(propertiesFile)) {
                prop.load(fileInputStream);
                String staticResource = (String) prop.get("staticResource");
                List<File> fileList = new ArrayList<>();
                if (staticResource != null && !staticResource.isEmpty()) {
                    String[] staticFileArr = staticResource.split(",");
                    for (String sFile : staticFileArr) {
                        fileList.add(new File(templateFilePath + "/" + sFile));
                    }
                }
                List<UploadFile> uploadFiles = convertToUploadFiles(fileList, blogRunTime.getPath());
                String cacheFolder = new File(blogRunTime.getPath()).getParent() + "/cache/zh_CN";
                File cacheFile = new File(cacheFolder);
                if (cacheFile.exists()) {
                    File[] fs = cacheFile.listFiles();
                    uploadFiles.addAll(convertToUploadFiles(Arrays.asList(fs), cacheFolder));
                }
                new UploadService().upload(session, uploadFiles);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "", e);
            }
        });

    }

    public static long crc32(File file) throws IOException {
        CRC32 crc32 = new CRC32();
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            crc32.update(IOUtil.getByteByInputStream(fileInputStream));
            return Math.abs(crc32.getValue());
        }
    }

    private List<UploadFile> convertToUploadFiles(List<File> files, String startPath) {
        List<UploadFile> uploadFiles = new ArrayList<>();
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
                try {
                    long crc32 = crc32(file);
                    if (fileWatcherMap.get(file.toString()) == null || fileWatcherMap.get(file.toString()) != crc32) {
                        UploadFile uploadFile = new UploadFile();
                        uploadFile.setFile(file);
                        String key = file.toString().substring(startPath.length());
                        uploadFile.setFileKey(key);
                        uploadFiles.add(uploadFile);
                        fileWatcherMap.put(file.toString(), crc32);
                    }
                } catch (IOException e) {
                    LOGGER.warning("Crc32 error " + file.getAbsolutePath());
                }

            } else if (file.isDirectory()) {
                File[] fs = file.listFiles();
                if (fs.length == 0) {
                    continue;
                }
                convertToUploadFiles(Arrays.asList(fs), startPath);
            }
        }
        return uploadFiles;
    }

}
