package com.zrlog.plugin.oss.render;


import com.zrlog.plugin.common.IOUtil;
import com.zrlog.plugin.message.Plugin;
import com.zrlog.plugin.render.IRenderHandler;

import java.io.InputStream;
import java.util.Map;

public class SimpleTemplateRender implements IRenderHandler {

    @Override
    public String render(String s, Plugin plugin, Map<String, Object> map) {
        return render(SimpleTemplateRender.class.getResourceAsStream(s), plugin, map);
    }

    @Override
    public String render(InputStream inputStream, Plugin plugin, Map<String, Object> map) {
        String renderResult = IOUtil.getStringInputStream(inputStream);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() != null) {
                renderResult = renderResult.replace("${" + entry.getKey() + "}", entry.getValue().toString());
            } else {
                renderResult = renderResult.replace("${" + entry.getKey() + "}", "");
            }
        }
        return renderResult;
    }
}
