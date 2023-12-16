package com.fzb.io.yunstore;

public class AliyunBucketVO extends BucketVO {

    private Long appId;
    private String region;

    public AliyunBucketVO(String bucketName, String accessKey, String secretKey,
                          String host, Long appId, String region) {
        super(bucketName, accessKey, secretKey, host);
        this.appId = appId;
        this.region = region;
    }

    public Long getAppId() {
        return appId;
    }

    public void setAppId(Long appId) {
        this.appId = appId;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }
}
