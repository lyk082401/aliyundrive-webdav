package com.github.zxbu.webdavteambition.store;

import com.github.zxbu.webdavteambition.config.AliYunDriverCronTask;
import com.github.zxbu.webdavteambition.manager.AliYunSessionManager;

import java.io.IOException;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class StartupService extends GenericServlet {

    private AliYunDriverCronTask mAliYunDriverCronTask;
    private AliYunSessionManager mAliYunSessionManager;

    @Override
    public void init() throws ServletException {
        super.init();
        startAliYunDriverCronTask();
        startAliYunSessionManager();
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {

    }

    @Override
    public void destroy() {
        super.destroy();
        stopAliYunDriverCronTask();
        stopAliYunSessionManager();
    }

    private void startAliYunDriverCronTask(){
        AliYunDriverCronTask task = mAliYunDriverCronTask;
        if (task != null) {
            task.stop();
        }
        task = new AliYunDriverCronTask(AliYunDriverClientService.getInstance());
        mAliYunDriverCronTask = task;
        task.start();
    }

    private void stopAliYunDriverCronTask(){
        AliYunDriverCronTask task = mAliYunDriverCronTask;
        if (task != null) {
            task.stop();
            mAliYunDriverCronTask = null;
        }
    }

    private void startAliYunSessionManager(){
        AliYunSessionManager mgr = mAliYunSessionManager;
        if (mgr != null) {
            mgr.stop();
        }
        mgr = new AliYunSessionManager(AliYunDriverClientService.getInstance().client);
        mAliYunSessionManager = mgr;
        mgr.start();
    }

    private void stopAliYunSessionManager(){
        AliYunSessionManager mgr = mAliYunSessionManager;
        if (mgr != null) {
            mgr.stop();
            mAliYunSessionManager = null;
        }
    }
}
