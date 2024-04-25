package com.zrlog.plugin.oss.service;

import com.fzb.io.api.BucketManageAPI;
import com.fzb.io.yunstore.AliyunBucketManageImpl;
import com.fzb.io.yunstore.AliyunBucketVO;
import com.google.gson.Gson;
import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.api.IPluginService;
import com.zrlog.plugin.api.Service;
import com.zrlog.plugin.common.LoggerUtil;
import com.zrlog.plugin.common.response.UploadFileResponse;
import com.zrlog.plugin.common.response.UploadFileResponseEntry;
import com.zrlog.plugin.data.codec.ContentType;
import com.zrlog.plugin.data.codec.MsgPacket;
import com.zrlog.plugin.data.codec.MsgPacketStatus;
import com.zrlog.plugin.oss.entry.UploadFile;
import com.zrlog.plugin.oss.timer.RefreshCdnWorker;
import com.zrlog.plugin.type.ActionType;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service("uploadService")
public class UploadService implements IPluginService {

    private static final Logger LOGGER = LoggerUtil.getLogger(UploadService.class);

    @Override
    public void handle(final IOSession ioSession, final MsgPacket requestPacket) {
        Map<String, Object> request = new Gson().fromJson(requestPacket.getDataStr(), Map.class);
        List<String> fileInfoList = (List<String>) request.get("fileInfo");
        List<UploadFile> uploadFileList = getUploadFiles(fileInfoList);
        UploadFileResponse uploadFileResponse = upload(ioSession, uploadFileList);
        List<Map<String, Object>> responseList = new ArrayList<>();
        for (UploadFileResponseEntry entry : uploadFileResponse) {
            Map<String, Object> map = new HashMap<>();
            map.put("url", entry.getUrl());
            responseList.add(map);
        }
        ioSession.sendMsg(ContentType.JSON, responseList, requestPacket.getMethodStr(), requestPacket.getMsgId(), MsgPacketStatus.RESPONSE_SUCCESS);
    }

    private static List<UploadFile> getUploadFiles(List<String> fileInfoList) {
        List<UploadFile> uploadFileList = new ArrayList<>();
        for (String fileInfo : fileInfoList) {
            UploadFile uploadFile = new UploadFile();
            uploadFile.setFile(new File(fileInfo.split(",")[0]));
            String fileKey = fileInfo.split(",")[1];
            if (fileKey.startsWith("/")) {
                uploadFile.setFileKey(fileKey.substring(1));
            } else {
                uploadFile.setFileKey(fileKey);
            }
            uploadFileList.add(uploadFile);
        }
        return uploadFileList;
    }

    public UploadFileResponse upload(IOSession session, final List<UploadFile> uploadFileList) {
        final UploadFileResponse response = new UploadFileResponse();
        long startTime = System.currentTimeMillis();
        if (uploadFileList == null || uploadFileList.isEmpty()) {
            return response;
        }
        final Map<String, Object> keyMap = new HashMap<>();
        String bucketKeyName = getBucketKeyName();
        keyMap.put("key", "access_key,secret_key,host,region,supportHttps," + bucketKeyName);
        Map<String, String> responseMap = session.getResponseSync(ContentType.JSON, keyMap, ActionType.GET_WEBSITE, Map.class);
        AliyunBucketVO bucket = new AliyunBucketVO(responseMap.get(bucketKeyName), responseMap.get("access_key"),
                responseMap.get("secret_key"), responseMap.get("host"),
                responseMap.get("region"));
        if (Objects.isNull(bucket.getBucketName()) || bucket.getBucketName().isEmpty()) {
            LOGGER.warning("missing config " + bucketKeyName);
            for (UploadFile uploadFile : uploadFileList) {
                UploadFileResponseEntry entry = new UploadFileResponseEntry();
                entry.setUrl(uploadFile.getFileKey());
                response.add(entry);
            }
            return response;
        }
        BucketManageAPI man = new AliyunBucketManageImpl(bucket);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (UploadFile uploadFile : uploadFileList) {
            futures.add(CompletableFuture.runAsync(() -> {
                LOGGER.info("upload file " + uploadFile.getFile());
                UploadFileResponseEntry entry = new UploadFileResponseEntry();
                try {
                    boolean supportHttps = responseMap.get("supportHttps") != null && "on".equalsIgnoreCase(responseMap.get("supportHttps"));
                    entry.setUrl(man.create(uploadFile.getFile(), uploadFile.getFileKey(), true, supportHttps));
                    if (Objects.equals(uploadFile.getRefresh(), true)) {
                        List<String> urls = new ArrayList<>();
                        urls.add(entry.getUrl());
                        //首页的情况，需要额外，更新下不带目录的
                        if (entry.getUrl().endsWith("/index.html")) {
                            urls.add(entry.getUrl().substring(0, entry.getUrl().lastIndexOf("/") + 1));
                        }
                        new RefreshCdnWorker(responseMap.get("access_key"), responseMap.get("secret_key"), responseMap.get("region")).start(urls);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "upload error " + e.getMessage());
                    entry.setUrl(uploadFile.getFileKey());
                }
                response.add(entry);
            }));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        LOGGER.info("upload file finish use time " + (System.currentTimeMillis() - startTime) + "ms");
        return response;
    }

    public String getBucketKeyName() {
        return "bucket";
    }
}
