package com.fzb.io.api;

import java.io.File;
import java.io.InputStream;
import java.util.Map;

public interface BucketManageAPI {


    /**
     * 删除单个文件
     *
     * @param key
     * @return
     */
    boolean delFile(String key);

    String create(File file, String key, boolean deleteRepeat) throws Exception;

    /**
     * 添加/创建文件
     *
     * @param file
     * @return
     */

    String create(File file, String key) throws Exception;

    void resetClient();

}