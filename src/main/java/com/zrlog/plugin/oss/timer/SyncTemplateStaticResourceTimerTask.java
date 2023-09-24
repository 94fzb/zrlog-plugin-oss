package com.zrlog.plugin.oss.timer;

import com.google.gson.Gson;
import com.hibegin.common.util.FileUtils;
import com.zrlog.plugin.IMsgPacketCallBack;
import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.common.IdUtil;
import com.zrlog.plugin.common.modle.BlogRunTime;
import com.zrlog.plugin.common.modle.TemplatePath;
import com.zrlog.plugin.oss.entry.UploadFile;
import com.zrlog.plugin.oss.service.UploadService;
import com.zrlog.plugin.data.codec.ContentType;
import com.zrlog.plugin.data.codec.MsgPacket;
import com.zrlog.plugin.data.codec.MsgPacketStatus;
import com.zrlog.plugin.type.ActionType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class SyncTemplateStaticResourceTimerTask extends TimerTask {

    private final IOSession session;

    private Map<String, Long> fileWatcherMap = new HashMap<>();

    public SyncTemplateStaticResourceTimerTask(IOSession session) {
        this.session = session;
    }

    @Override
    public void run() {
        Map<String, Object> map = new HashMap<>();
        map.put("key", "syncTemplate");
        session.sendJsonMsg(map, ActionType.GET_WEBSITE.name(), IdUtil.getInt(), MsgPacketStatus.SEND_REQUEST, new IMsgPacketCallBack() {
            @Override
            public void handler(MsgPacket msgPacket) {
                Map<String, String> responseMap = new Gson().fromJson(msgPacket.getDataStr(), Map.class);
                if ("on".equals(responseMap.get("syncTemplate"))) {
                    TemplatePath templatePath = session.getResponseSync(ContentType.JSON, new HashMap(), ActionType.CURRENT_TEMPLATE, TemplatePath.class);
                    BlogRunTime blogRunTime = session.getResponseSync(ContentType.JSON, new HashMap(), ActionType.BLOG_RUN_TIME, BlogRunTime.class);
                    File templateFilePath = new File(blogRunTime.getPath() + templatePath.getValue());
                    if (templateFilePath.isDirectory()) {
                        File propertiesFile = new File(templateFilePath + "/template.properties");
                        if (propertiesFile.exists()) {
                            Properties prop = new Properties();
                            try {
                                prop.load(new FileInputStream(propertiesFile));
                                String staticResource = (String) prop.get("staticResource");
                                List<File> fileList = new ArrayList<>();
                                if (staticResource != null && !"".equals(staticResource)) {
                                    String[] staticFileArr = staticResource.split(",");
                                    for (String sFile : staticFileArr) {
                                        fileList.add(new File(templateFilePath + "/" + sFile));
                                    }
                                }
                                new UploadService().upload(session, convertToUploadFiles(fileList, blogRunTime.getPath()));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        });

    }

    private List<UploadFile> convertToUploadFiles(List<File> files, String blogRootPath) {
        List<UploadFile> uploadFiles = new ArrayList<>();
        List<File> fullFileList = new ArrayList<>();
        for (File file : files) {
            FileUtils.getAllFiles(file.toString(), fullFileList);
        }
        if (!blogRootPath.endsWith("/")) {
            blogRootPath = blogRootPath + "/";
        }
        for (File file : fullFileList) {
            if (!file.isDirectory() && file.exists()) {
                if (fileWatcherMap.get(file.toString()) == null || fileWatcherMap.get(file.toString()) != file.lastModified()) {
                    UploadFile uploadFile = new UploadFile();
                    uploadFile.setFile(file);

                    String key = file.toString().substring(blogRootPath.length());
                    uploadFile.setFileKey(key);
                    uploadFiles.add(uploadFile);
                    fileWatcherMap.put(file.toString(), file.lastModified());
                }
            }
        }
        return uploadFiles;
    }

}
