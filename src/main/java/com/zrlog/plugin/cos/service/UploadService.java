package com.zrlog.plugin.cos.service;

import com.fzb.io.api.BucketManageAPI;
import com.fzb.io.yunstore.QCloudBucketManageImpl;
import com.fzb.io.yunstore.QCloudBucketVO;
import com.google.gson.Gson;
import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.api.IPluginService;
import com.zrlog.plugin.api.Service;
import com.zrlog.plugin.common.IdUtil;
import com.zrlog.plugin.common.response.UploadFileResponse;
import com.zrlog.plugin.common.response.UploadFileResponseEntry;
import com.zrlog.plugin.cos.entry.UploadFile;
import com.zrlog.plugin.data.codec.ContentType;
import com.zrlog.plugin.data.codec.MsgPacket;
import com.zrlog.plugin.data.codec.MsgPacketStatus;
import com.zrlog.plugin.type.ActionType;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("uploadService")
public class UploadService implements IPluginService {

    private static Logger LOGGER = Logger.getLogger(UploadService.class);

    @Override
    public void handle(final IOSession ioSession, final MsgPacket requestPacket) {
        Map<String, Object> request = new Gson().fromJson(requestPacket.getDataStr(), Map.class);
        List<String> fileInfoList = (List<String>) request.get("fileInfo");
        List<UploadFile> uploadFileList = new ArrayList<>();
        for (String fileInfo : fileInfoList) {
            UploadFile uploadFile = new UploadFile();
            uploadFile.setFile(new File(fileInfo.split(",")[0]));
            String fileKey = fileInfo.split(",")[1];
            if (fileKey.startsWith("/")) {
                uploadFile.setFileKey(fileKey.substring(1, fileKey.length()));
            } else {
                uploadFile.setFileKey(fileKey);
            }
            uploadFileList.add(uploadFile);
        }
        UploadFileResponse uploadFileResponse = upload(ioSession, uploadFileList);
        List<Map<String, Object>> responseList = new ArrayList<>();
        for (UploadFileResponseEntry entry : uploadFileResponse) {
            Map<String, Object> map = new HashMap<>();
            map.put("url", entry.getUrl());
            responseList.add(map);
        }
        ioSession.sendMsg(ContentType.JSON, responseList, requestPacket.getMethodStr(), requestPacket.getMsgId(), MsgPacketStatus.RESPONSE_SUCCESS);
    }

    public UploadFileResponse upload(IOSession session, final List<UploadFile> uploadFileList) {
        final UploadFileResponse response = new UploadFileResponse();
        if (uploadFileList != null && !uploadFileList.isEmpty()) {
            final Map<String, Object> keyMap = new HashMap<>();
            keyMap.put("key", "bucket,access_key,secret_key,host,appId,region,supportHttps");
            int msgId = IdUtil.getInt();
            session.sendJsonMsg(keyMap, ActionType.GET_WEBSITE.name(), msgId, MsgPacketStatus.SEND_REQUEST, null);
            MsgPacket packet = session.getResponseMsgPacketByMsgId(msgId);
            Map<String, String> responseMap = new Gson().fromJson(packet.getDataStr(), Map.class);
            QCloudBucketVO bucket = new QCloudBucketVO(responseMap.get("bucket"), responseMap.get("access_key"),
                    responseMap.get("secret_key"), responseMap.get("host"), Long.valueOf(responseMap.get("appId")), responseMap.get("region"));
            BucketManageAPI man = new QCloudBucketManageImpl(bucket);
            for (UploadFile uploadFile : uploadFileList) {
                LOGGER.info("upload file " + uploadFile.getFile());
                UploadFileResponseEntry entry = new UploadFileResponseEntry();
                try {
                    boolean supportHttps = responseMap.get("supportHttps") != null && "on".equalsIgnoreCase(responseMap.get("supportHttps"));
                    entry.setUrl(man.create(uploadFile.getFile(), uploadFile.getFileKey(), true, supportHttps));
                } catch (Exception e) {
                    LOGGER.error("upload error", e);
                    entry.setUrl(uploadFile.getFileKey());
                }
                response.add(entry);
            }
            LOGGER.info("upload file finish");
        }
        return response;
    }
}
