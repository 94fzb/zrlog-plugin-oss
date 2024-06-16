package com.fzb.io.yunstore;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.DeleteObjectsRequest;
import com.aliyun.oss.model.PutObjectRequest;
import com.fzb.io.api.BucketManageAPI;
import com.zrlog.plugin.common.LoggerUtil;

import java.io.File;
import java.util.Objects;
import java.util.logging.Level;

public class AliyunBucketManageImpl implements BucketManageAPI {

    private final AliyunBucketVO aliyunBucketVO;
    private final OSS ossClient;

    public AliyunBucketManageImpl(AliyunBucketVO aliyunBucketVO) {
        this.aliyunBucketVO = aliyunBucketVO;
        this.ossClient = getOssClient();
    }

    private void deleteFile(String key) {
        try {
            DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(aliyunBucketVO.getBucketName());
            deleteObjectsRequest.setKey(key);
            ossClient.deleteObject(deleteObjectsRequest);
        } catch (Exception e) {
            LoggerUtil.getLogger(AliyunBucketManageImpl.class).log(Level.SEVERE, "", e);
        }
    }

    private OSS getOssClient() {
        return new OSSClientBuilder().build(aliyunBucketVO.getRegion(), aliyunBucketVO.getAccessKey(), aliyunBucketVO.getSecretKey());
    }

    @Override
    public String create(File file, String key, boolean deleteRepeat, boolean supportHttps) {
        if (deleteRepeat) {
            deleteFile(key);
        }
        PutObjectRequest putObjectRequest = new PutObjectRequest(aliyunBucketVO.getBucketName(), key, file);
        ossClient.putObject(putObjectRequest);
        return (supportHttps ? "https" : "http") + "://" + aliyunBucketVO.getHost() + "/" + key;
    }

    @Override
    public void close() throws Exception {
        if (Objects.isNull(ossClient)) {
            return;
        }
        ossClient.shutdown();
    }
}