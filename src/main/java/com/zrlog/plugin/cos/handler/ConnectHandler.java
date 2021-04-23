package com.zrlog.plugin.cos.handler;

import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.api.IConnectHandler;
import com.zrlog.plugin.cos.timer.SyncTemplateStaticResourceTimerTask;
import com.zrlog.plugin.data.codec.MsgPacket;

import java.util.Date;
import java.util.Timer;

public class ConnectHandler implements IConnectHandler {

    private static final Timer timer = new Timer();

    @Override
    public void handler(IOSession ioSession, MsgPacket msgPacket) {
        timer.scheduleAtFixedRate(new SyncTemplateStaticResourceTimerTask(ioSession), new Date(), 6000);
    }
}
