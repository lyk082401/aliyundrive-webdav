package com.github.zxbu.webdavteambition.manager;

import com.github.zxbu.webdavteambition.client.AliYunDriverClient;
import com.github.zxbu.webdavteambition.config.AliYunDriveProperties;

import net.sf.webdav.exceptions.WebdavException;
import net.xdow.aliyundriver.BuildConfig;

import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AliYunSessionManager {

    public static final int SIGN_EXPIRED_TIME_SEC = 60 * 5; //5min

    private static final Logger LOGGER = LoggerFactory.getLogger(AliYunSessionManager.class);

    private final AliYunDriverClient aliYunDriverClient;
    private ScheduledExecutorService mTaskPool = Executors.newScheduledThreadPool(1);

    public AliYunSessionManager(AliYunDriverClient client) {
        this.aliYunDriverClient = client;
        makeKeyPairIfNeeded();
    }

    public void makeKeyPairIfNeeded() {
        AliYunDriveProperties.Session session = this.aliYunDriverClient.aliYunDriveProperties.session;
        if (!session.isEmpty()) {
            return;
        }
        makeKeyPair();
    }

    public void makeKeyPair() {
        AliYunDriveProperties.Session session = this.aliYunDriverClient.aliYunDriveProperties.session;
        BigInteger privateKeyInt = new BigInteger(256, new SecureRandom());
        BigInteger publicKeyInt = Sign.publicKeyFromPrivate(privateKeyInt);
        session.privateKey = privateKeyInt.toString(16);
        session.publicKey = publicKeyInt.toString(16);
        makeSignature(0);
        this.aliYunDriverClient.aliYunDriveProperties.save();
    }

    private void makeSignature(int nonce) {
        AliYunDriveProperties aliYunDriveProperties = this.aliYunDriverClient.aliYunDriveProperties;
        AliYunDriveProperties.Session session = aliYunDriveProperties.session;
        byte[] dataBytes = (aliYunDriveProperties.appId + ":" + aliYunDriveProperties.deviceId + ":"
                + aliYunDriveProperties.userId + ":" + nonce).getBytes(StandardCharsets.UTF_8);
        byte[] dataHash = Hash.sha256(dataBytes);
        ECKeyPair keyPair = new ECKeyPair(new BigInteger(session.privateKey, 16), new BigInteger(session.publicKey, 16));
        Sign.SignatureData signatureInfo = Sign.signMessage(dataHash, keyPair, false);
        session.signature = Hex.toHexString(signatureInfo.getR()) + Hex.toHexString(signatureInfo.getS());
        session.nonce = nonce;
        session.expireTimeSec = System.currentTimeMillis() / 1000 + SIGN_EXPIRED_TIME_SEC;
    }

    public void updateSession() {
        AliYunDriveProperties.Session session = this.aliYunDriverClient.aliYunDriveProperties.session;
        Map<String, String> args = new HashMap<>();
        args.put("deviceName", "Edge浏览器");
        args.put("modelName", "Windows网页版");
        args.put("pubKey", "04" + session.publicKey);

        String createSessionResult = "";
        if (session.nonce >= 1073741823) {
            session.nonce = 0;
        }
        makeSignature(session.nonce);
        try {
            if (session.nonce == 0) {
                createSessionResult = this.aliYunDriverClient.post("https://api.aliyundrive.com/users/v1/users/device/create_session", args);
            } else {
                createSessionResult = this.aliYunDriverClient.post("https://api.aliyundrive.com/users/v1/users/device/renew_session", args);
            }
        } catch (WebdavException e) {
            if (e.responseMessage != null) {
                if (e.responseMessage.contains("DeviceSessionSignatureInvalid")
                    ||e.responseMessage.contains("UserDeviceOffline")){
                    makeKeyPair();
                    createSessionResult = this.aliYunDriverClient.post("https://api.aliyundrive.com/users/v1/users/device/create_session", args);
                }
            }
        }
        if (createSessionResult.contains("\"result\":false")) {
            LOGGER.error("登录设备过多, 请进入\"登录设备管理\", 退出一些设备。");
            session.nonce = 0;
        }
        if (createSessionResult.contains("\"result\":true")) {
            session.nonce++;
        }
        this.aliYunDriverClient.aliYunDriveProperties.save();
    }

    public void start() {
        updateSession();
        mTaskPool.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                AliYunDriveProperties.Session session = AliYunSessionManager.this.aliYunDriverClient.aliYunDriveProperties.session;
                if (!session.isExpired()) {
                    return;
                }
                updateSession();
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    public void stop() {
        mTaskPool.shutdownNow();
    }
}
