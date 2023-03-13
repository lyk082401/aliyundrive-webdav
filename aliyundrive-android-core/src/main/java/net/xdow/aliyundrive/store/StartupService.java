package net.xdow.aliyundrive.store;

import com.github.zxbu.webdavteambition.manager.AliyunDriveSessionManager;
import com.github.zxbu.webdavteambition.store.AliyunDriveClientService;
import net.xdow.aliyundrive.IAliyunDrive;
import net.xdow.aliyundrive.config.AliyunDriveCronTask;
import net.xdow.aliyundrive.util.AliyunDriveClientServiceHolder;
import net.xdow.aliyundrive.webapi.impl.AliyunDriveWebApiImplV1;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

public class StartupService extends GenericServlet {

    private AliyunDriveCronTask mAliyunDriveCronTask;
    private AliyunDriveSessionManager mAliyunDriveSessionManager;


    @Override
    public void init() throws ServletException {
        super.init();
        startAliyunDriveCronTask();
        startAliYunSessionManager();
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {

    }

    @Override
    public void destroy() {
        super.destroy();
        stopAliYunDriverCronTask();
        stopAliyunDriveSessionManager();
        AliyunDriveClientServiceHolder.onShutdown();
    }

    private void startAliyunDriveCronTask(){
        AliyunDriveCronTask task = mAliyunDriveCronTask;
        if (task != null) {
            task.stop();
        }
        task = new AliyunDriveCronTask(AliyunDriveClientServiceHolder.getInstance());
        mAliyunDriveCronTask = task;
        task.start();
    }

    private void stopAliYunDriverCronTask(){
        AliyunDriveCronTask task = mAliyunDriveCronTask;
        if (task != null) {
            task.stop();
            mAliyunDriveCronTask = null;
        }
    }

    private void startAliYunSessionManager(){
        AliyunDriveClientService service = AliyunDriveClientServiceHolder.getInstance();
        IAliyunDrive aliyunDrive = service.getAliyunDrive();
        if (aliyunDrive instanceof AliyunDriveWebApiImplV1) {
        } else {
            return;
        }
        AliyunDriveSessionManager mgr = mAliyunDriveSessionManager;
        if (mgr != null) {
            mgr.stop();
        }
        mgr = new AliyunDriveSessionManager((AliyunDriveWebApiImplV1) service.getAliyunDrive(), service.getProperties());
        mAliyunDriveSessionManager = mgr;
        mgr.start();
    }

    private void stopAliyunDriveSessionManager(){
        AliyunDriveSessionManager mgr = mAliyunDriveSessionManager;
        if (mgr != null) {
            mgr.stop();
            mAliyunDriveSessionManager = null;
        }
    }
}
