package com.zrlog.plugin.oss.service;

import com.fzb.io.api.BucketManageAPI;
import com.fzb.io.yunstore.AliyunBucketManageImpl;
import com.fzb.io.yunstore.AliyunBucketVO;
import com.google.gson.Gson;
import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.api.IPluginService;
import com.zrlog.plugin.api.Service;
import com.zrlog.plugin.common.IdUtil;
import com.zrlog.plugin.common.LoggerUtil;
import com.zrlog.plugin.common.response.UploadFileResponse;
import com.zrlog.plugin.common.response.UploadFileResponseEntry;
import com.zrlog.plugin.oss.entry.UploadFile;
import com.zrlog.plugin.data.codec.ContentType;
import com.zrlog.plugin.data.codec.MsgPacket;
import com.zrlog.plugin.data.codec.MsgPacketStatus;
import com.zrlog.plugin.type.ActionType;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.File;

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
        if (uploadFileList != null && !uploadFileList.isEmpty()) {
            final Map<String, Object> keyMap = new HashMap<>();
            keyMap.put("key", "access_key,secret_key,host,region,supportHttps");
            int msgId = IdUtil.getInt();
            session.sendJsonMsg(keyMap, ActionType.GET_WEBSITE.name(), msgId, MsgPacketStatus.SEND_REQUEST, null);
            MsgPacket packet = session.getResponseMsgPacketByMsgId(msgId);
            Map<String, String> responseMap = new Gson().fromJson(packet.getDataStr(), Map.class);
            AliyunBucketVO bucket = new AliyunBucketVO(getBucketName(session), responseMap.get("access_key"),
                    responseMap.get("secret_key"), responseMap.get("host"),
                    responseMap.get("region"));
            if (Objects.isNull(bucket.getBucketName()) || bucket.getBucketName().isEmpty()) {
                LOGGER.warning("missing config " + getBucketName(session));
                for (UploadFile uploadFile : uploadFileList) {
                    UploadFileResponseEntry entry = new UploadFileResponseEntry();
                    entry.setUrl(uploadFile.getFileKey());
                    response.add(entry);
                }
            } else {
                BucketManageAPI man = new AliyunBucketManageImpl(bucket);
                for (UploadFile uploadFile : uploadFileList) {
                    LOGGER.info("upload file " + uploadFile.getFile());
                    UploadFileResponseEntry entry = new UploadFileResponseEntry();
                    try {
                        boolean supportHttps = responseMap.get("supportHttps") != null && "on".equalsIgnoreCase(responseMap.get("supportHttps"));
                        entry.setUrl(man.create(uploadFile.getFile(), uploadFile.getFileKey(), true, supportHttps));
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "upload error", e);
                        entry.setUrl(uploadFile.getFileKey());
                    }
                    response.add(entry);
                }
                LOGGER.info("upload file finish");
            }
        }
        return response;
    }

    private String getBucketName(IOSession session) {
        final Map<String, Object> keyMap = new HashMap<>();
        keyMap.put("key", getBucketKeyName());
        int msgId = IdUtil.getInt();
        session.sendJsonMsg(keyMap, ActionType.GET_WEBSITE.name(), msgId, MsgPacketStatus.SEND_REQUEST, null);
        MsgPacket packet = session.getResponseMsgPacketByMsgId(msgId);
        Map<String, String> responseMap = new Gson().fromJson(packet.getDataStr(), Map.class);
        return responseMap.get(getBucketKeyName());
    }

    public String getBucketKeyName() {
        return "bucket";
    }
}
