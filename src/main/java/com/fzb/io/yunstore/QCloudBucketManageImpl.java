package com.fzb.io.yunstore;

import com.fzb.io.api.BucketManageAPI;
import com.zrlog.plugin.common.LoggerUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.request.DelFileRequest;
import com.qcloud.cos.request.UploadFileRequest;
import com.qcloud.cos.sign.Credentials;

import java.io.File;
import java.util.logging.Level;

public class QCloudBucketManageImpl implements BucketManageAPI {

    private QCloudBucketVO qCloudBucketVO;
    private COSClient cosClient;

    public QCloudBucketManageImpl(QCloudBucketVO qCloudBucketVO) {
        this.qCloudBucketVO = qCloudBucketVO;
        this.cosClient = getCosClient();
    }

    @Override
    public boolean delFile(String key) {
        DelFileRequest delFileRequest = new DelFileRequest(qCloudBucketVO.getBucketName(), "/" + key);
        cosClient.delFile(delFileRequest);
        return true;
    }

    @Override
    public String create(File file, String key) {
        return create(file, key, false);
    }

    @Override
    public void resetClient() {
        this.cosClient.shutdown();
        this.cosClient = getCosClient();
    }

    private COSClient getCosClient() {
        Credentials credentials = new Credentials(qCloudBucketVO.getAppId(), qCloudBucketVO.getAccessKey(), qCloudBucketVO.getSecretKey());
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setRegion(qCloudBucketVO.getRegion());
        return new COSClient(clientConfig, credentials);
    }

    @Override
    public String create(File file, String key, boolean deleteRepeat) {
        try {
            delFile(key);
        } catch (Exception e) {
            LoggerUtil.getLogger(QCloudBucketManageImpl.class).log(Level.SEVERE, "", e);
        }
        UploadFileRequest uploadFileRequest = new UploadFileRequest(qCloudBucketVO.getBucketName(), "/" + key, file.toString());
        cosClient.uploadFile(uploadFileRequest);
        return "http://" + qCloudBucketVO.getHost() + "/" + key;
    }

}
