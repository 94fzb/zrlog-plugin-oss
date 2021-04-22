package com.zrlog.plugin.cos.service;

import com.zrlog.plugin.api.Service;

@Service("uploadToPrivateService")
public class UploadToPrivateService extends UploadService {

    @Override
    public String getBucketKeyName() {
        return "private_bucket";
    }
}
