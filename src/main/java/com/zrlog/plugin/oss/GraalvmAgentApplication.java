package com.zrlog.plugin.oss;

import com.google.gson.Gson;
import com.zrlog.plugin.common.PluginNativeImageUtils;
import com.zrlog.plugin.common.response.UploadFileResponse;
import com.zrlog.plugin.common.response.UploadFileResponseEntry;
import com.zrlog.plugin.data.codec.HttpRequestInfo;
import com.zrlog.plugin.message.Plugin;
import com.zrlog.plugin.oss.controller.OssController;
import com.zrlog.plugin.oss.service.UploadService;
import com.zrlog.plugin.oss.service.UploadToPrivateService;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;

public class GraalvmAgentApplication {


    public static void main(String[] args) throws IOException, InstantiationException, IllegalAccessException {
        PluginNativeImageUtils.usedGsonObject();
        UploadService.class.newInstance();
        UploadToPrivateService.class.newInstance();
        String basePath = System.getProperty("user.dir").replace("/target", "");
        //PathKit.setRootPath(basePath);
        File file = new File(basePath + "/src/main/resources");
        PluginNativeImageUtils.doLoopResourceLoad(file.listFiles(), file.getPath() + "/", "/");
        //Application.nativeAgent = true;
        PluginNativeImageUtils.exposeController(Collections.singletonList(OssController.class));
        Application.main(args);

    }
}