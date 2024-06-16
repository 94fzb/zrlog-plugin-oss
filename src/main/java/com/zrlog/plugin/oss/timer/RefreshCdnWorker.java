package com.zrlog.plugin.oss.timer;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.cdn.model.v20180510fix.RefreshObjectCachesRequest;
import com.aliyuncs.http.HttpResponse;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.zrlog.plugin.RunConstants;
import com.zrlog.plugin.common.LoggerUtil;
import com.zrlog.plugin.type.RunType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.logging.Logger;

public class RefreshCdnWorker implements AutoCloseable {

    private static final Logger LOGGER = LoggerUtil.getLogger(RefreshCdnWorker.class);
    private final DefaultAcsClient client;

    public RefreshCdnWorker(String accessKeyId, String accessKeySecret, String region) {
        IClientProfile profile = DefaultProfile.getProfile(region.replace("oss-", "").replace(".aliyuncs.com", ""), accessKeyId, accessKeySecret);
        this.client = new DefaultAcsClient(profile);
    }

    /**
     * 每次请求最多支持提交 1000 条 URL 刷新或者 100 个目录刷新或者 1 个正则刷新。
     *
     * @param urls
     */
    public void start(List<String> urls) {
        if (urls.isEmpty()) {
            return;
        }
        int maxSize = 800;
        List<String> spitsUrls = new ArrayList<>(maxSize);
        urls.forEach(e -> {
            //添加到批量更新的 list
            spitsUrls.add(e);
            if (spitsUrls.size() == maxSize) {
                refreshObjectCaches(spitsUrls);
                spitsUrls.clear();
            }
        });
        //刷新剩余的
        refreshObjectCaches(spitsUrls);
    }

    private void refreshObjectCaches(List<String> urls) {
        long start = System.currentTimeMillis();
        RefreshObjectCachesRequest request = new RefreshObjectCachesRequest();
        //要刷新的URI
        StringJoiner stringJoiner = new StringJoiner("\n");
        urls.forEach(stringJoiner::add);
        request.setObjectPath(stringJoiner.toString());
        try {
            HttpResponse httpResponse = client.doAction(request);
            if (RunConstants.runType == RunType.DEV) {
                LOGGER.info(("Refresh --> response " + new String(httpResponse.getHttpContent())));
            }
        } catch (Exception e) {
            LOGGER.warning("Refresh failed: " + e.getMessage());
        } finally {
            LOGGER.info("Refresh cdn used time " + (System.currentTimeMillis() - start) + "ms");
        }
    }

    @Override
    public void close() throws Exception {
        if (Objects.isNull(client)) {
            return;
        }
        client.shutdown();
    }
}
