package com.fzb.io.yunstore;

public class AliyunBucketVO extends BucketVO {

    private String region;

    public AliyunBucketVO(String bucketName, String accessKey, String secretKey,
                          String host, String region) {
        super(bucketName, accessKey, secretKey, host);
        this.region = region;
    }


    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }
}
