package com.zrlog.plugin.cos;


import com.zrlog.plugin.client.NioClient;
import com.zrlog.plugin.cos.controller.CosController;
import com.zrlog.plugin.cos.handler.ConnectHandler;
import com.zrlog.plugin.cos.service.UploadService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Start {
    /**
     * @param args
     */
    public static void main(String[] args) throws IOException {
        List<Class> classList = new ArrayList<>();
        classList.add(CosController.class);
        new NioClient(new ConnectHandler(), null).connectServer(args, classList, CosPluginAction.class, UploadService.class);
    }
}

