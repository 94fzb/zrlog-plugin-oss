package com.fzb.io.api;

import java.io.File;

public interface BucketManageAPI {

    String create(File file, String key, boolean deleteRepeat, boolean supportHttps) throws Exception;

}