package com.zrlog.plugin.oss.handler;

import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.client.ClientActionHandler;
import com.zrlog.plugin.data.codec.MsgPacket;
import com.zrlog.plugin.oss.timer.SyncTemplateStaticResourceRunnable;

import java.util.Objects;

public class OssActionHandler extends ClientActionHandler {

    private final ConnectHandler connectHandler;

    public OssActionHandler(ConnectHandler connectHandler) {
        this.connectHandler = connectHandler;
    }

    @Override
    public void refreshCache(IOSession session, MsgPacket msgPacket) {
        SyncTemplateStaticResourceRunnable runnable = connectHandler.getSyncTemplateStaticResourceRunnable();
        if (Objects.isNull(runnable)) {
            return;
        }
        runnable.run();
    }
}
