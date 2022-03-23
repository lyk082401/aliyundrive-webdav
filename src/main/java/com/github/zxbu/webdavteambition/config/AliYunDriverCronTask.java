package com.github.zxbu.webdavteambition.config;

import com.github.zxbu.webdavteambition.model.result.TFile;
import com.github.zxbu.webdavteambition.store.AliYunDriverClientService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AliYunDriverCronTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(AliYunDriverCronTask.class);

    private final AliYunDriverClientService mAliYunDriverClientService;

    private ScheduledExecutorService mTaskPool = Executors.newScheduledThreadPool(1);


    public AliYunDriverCronTask(AliYunDriverClientService service) {
        mAliYunDriverClientService = service;
    }

    /**
     * 每隔30-60分钟请求一下接口，保证token不过期
     */
    public void refreshToken() {
        try {
            LOGGER.info("定时刷新 Refresh Token 任务开始");
            TFile root = mAliYunDriverClientService.getTFileByPath("/");
            mAliYunDriverClientService.getTFiles(root.getFile_id(), null, null);
        } catch (Throwable e) {
            LOGGER.error("", e);
        } finally {
            LOGGER.info("定时刷新 Refresh Token 任务结束");
        }
    }

    public void start() {
        mTaskPool.schedule(new Runnable() {
            @Override
            public void run() {
                refreshToken();
                mTaskPool.schedule(this, getRandomNumber(30, 60), TimeUnit.MINUTES);
            }
        }, 10, TimeUnit.SECONDS);
    }

    public void stop() {
        mTaskPool.shutdownNow();
    }

    private int getRandomNumber(int min, int max) {
        return (int) ((Math.random() * (max - min)) + min);
    }
}
