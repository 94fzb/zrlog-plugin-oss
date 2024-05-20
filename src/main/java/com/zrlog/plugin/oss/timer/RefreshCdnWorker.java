package com.zrlog.plugin.oss.timer;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.cdn.model.v20180510.RefreshObjectCachesRequest;
import com.aliyuncs.http.HttpResponse;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.zrlog.plugin.common.LoggerUtil;

import java.util.List;
import java.util.StringJoiner;
import java.util.logging.Logger;

public class RefreshCdnWorker {

    private final Logger LOGGER = LoggerUtil.getLogger(RefreshCdnWorker.class);
    private final DefaultAcsClient client;

    public RefreshCdnWorker(String accessKeyId, String accessKeySecret, String region) {
        IClientProfile profile = DefaultProfile.getProfile(region.replace("oss-", "").replace(".aliyuncs.com", ""), accessKeyId, accessKeySecret);
        this.client = new DefaultAcsClient(profile);
    }

    public void start(List<String> urls) {
        if (urls.isEmpty()) {
            return;
        }
        long start = System.currentTimeMillis();
        refreshObjectCaches(urls);
        LOGGER.info("Refresh cdn used time " + (System.currentTimeMillis() - start) + "ms");
    }


    private void refreshObjectCaches(List<String> urls) {
        RefreshObjectCachesRequest request = new RefreshObjectCachesRequest();
        //要刷新的URI
        StringJoiner stringJoiner = new StringJoiner("\n");
        urls.forEach(stringJoiner::add);
        request.setObjectPath(stringJoiner.toString());
        try {
            HttpResponse httpResponse = client.doAction(request);
            //System.out.println("Refresh " + url + " --> response " + new String(httpResponse.getHttpContent()));
        } catch (Exception e) {
            LOGGER.warning("Refresh failed: " + e.getMessage());
        }
    }
}
