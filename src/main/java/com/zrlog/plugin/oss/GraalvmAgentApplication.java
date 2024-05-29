package com.zrlog.plugin.oss;

import com.aliyun.oss.internal.Mimetypes;
import com.aliyuncs.cdn.model.v20180510.RefreshObjectCachesResponse;
import com.aliyuncs.cdn.model.v20180510fix.RefreshObjectCachesRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zrlog.plugin.common.PluginNativeImageUtils;
import com.zrlog.plugin.oss.controller.OssController;
import com.zrlog.plugin.oss.service.UploadService;
import com.zrlog.plugin.oss.service.UploadToPrivateService;
import com.zrlog.plugin.oss.timer.RefreshCdnWorker;
import org.apache.commons.logging.impl.LogFactoryImpl;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

public class GraalvmAgentApplication {


    public static void main(String[] args) throws IOException, InstantiationException, IllegalAccessException {
        //upload need set content-type
        Mimetypes.getInstance();
        LogFactoryImpl.getLog(GraalvmAgentApplication.class).info("Common logging print");
        PluginNativeImageUtils.usedGsonObject();
        RefreshObjectCachesResponse refreshObjectCachesResponse = new RefreshObjectCachesResponse();
        refreshObjectCachesResponse.setRefreshTaskId("");
        refreshObjectCachesResponse.setRequestId("");
        new Gson().toJson(refreshObjectCachesResponse);
        GsonBuilder builder = new GsonBuilder();
        RefreshObjectCachesRequest refreshObjectCachesRequest = new RefreshObjectCachesRequest();
        refreshObjectCachesRequest.setObjectPath("Test");
        builder.create().toJson(refreshObjectCachesRequest);
        new RefreshCdnWorker("test", "test", "oss-cn-chengdu.aliyuncs.com").start(Arrays.asList("https://blog.zrlog.com/?"));
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