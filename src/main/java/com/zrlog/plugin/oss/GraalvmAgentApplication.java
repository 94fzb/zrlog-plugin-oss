package com.zrlog.plugin.oss;

import com.zrlog.plugin.common.PluginNativeImageUtils;
import com.zrlog.plugin.oss.controller.OssController;
import com.zrlog.plugin.oss.service.UploadService;
import com.zrlog.plugin.oss.service.UploadToPrivateService;
import org.apache.commons.logging.impl.LogFactoryImpl;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

public class GraalvmAgentApplication {


    public static void main(String[] args) throws IOException, InstantiationException, IllegalAccessException {
        LogFactoryImpl.getLog(GraalvmAgentApplication.class).info("Common logging print");
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