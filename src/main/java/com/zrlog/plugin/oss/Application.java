package com.zrlog.plugin.oss;


import com.zrlog.plugin.client.NioClient;
import com.zrlog.plugin.render.SimpleTemplateRender;
import com.zrlog.plugin.oss.controller.OssController;
import com.zrlog.plugin.oss.handler.ConnectHandler;
import com.zrlog.plugin.oss.service.UploadService;
import com.zrlog.plugin.oss.service.UploadToPrivateService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Application {
    /**
     * @param args
     */
    public static void main(String[] args) throws IOException {
        List<Class> classList = new ArrayList<>();
        classList.add(OssController.class);
        new NioClient(new ConnectHandler(), new SimpleTemplateRender())
                .connectServer(args, classList, OssPluginAction.class, Arrays.asList(UploadService.class, UploadToPrivateService.class));
    }
}

