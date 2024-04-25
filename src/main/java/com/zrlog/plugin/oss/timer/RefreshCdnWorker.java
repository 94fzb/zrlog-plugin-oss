package com.zrlog.plugin.oss.timer;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.cdn.model.v20180510.RefreshObjectCachesRequest;
import com.aliyuncs.http.HttpResponse;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.zrlog.plugin.common.LoggerUtil;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class RefreshCdnWorker {

    private final Logger LOGGER = LoggerUtil.getLogger(RefreshCdnWorker.class);
    private final DefaultAcsClient client;
    private final ForkJoinPool forkJoinPool = new ForkJoinPool(5);

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
        urls.stream().map(url -> CompletableFuture.runAsync(() -> {
            RefreshObjectCachesRequest request = new RefreshObjectCachesRequest();
            //要刷新的URI
            request.setObjectPath(url);
            try {
                HttpResponse httpResponse = client.doAction(request);
                System.out.println("Refresh " + url + " --> response " + new String(httpResponse.getHttpContent()));
            } catch (Exception e) {
                LOGGER.warning("Refresh " + url + " failed: " + e.getMessage());
            }
        }, forkJoinPool)).collect(Collectors.toList()).forEach(e -> {
            try {
                e.get();
            } catch (InterruptedException | ExecutionException ex) {
                LOGGER.warning("Refresh error " + ex.getMessage());
            }
        });
    }
}
