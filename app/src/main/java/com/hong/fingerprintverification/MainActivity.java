package com.hong.fingerprintverification;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Handler;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class MainActivity extends AppCompatActivity {

    private FingerprintManager manager;
    private KeyStore keyStore;
    private static final String DEFAULT_KEY_NAME = "default_key";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
    }

    private void init() {
        manager = (FingerprintManager) getSystemService(FINGERPRINT_SERVICE);
        // 判断是否支持指纹
        if (isSupportFingerPrint()) {
            initKey();
            initCipher();
        }
    }

    private boolean isSupportFingerPrint() {
        if (Build.VERSION.SDK_INT >= 23) {
            // 权限判断
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "没有指纹识别权限", Toast.LENGTH_SHORT).show();
                return false;
            }

            // 硬件判断
            if (!manager.isHardwareDetected()) {
                Toast.makeText(this, "没有指纹识别模块", Toast.LENGTH_SHORT).show();
                return false;
            }

            if (!manager.hasEnrolledFingerprints()) {
                Toast.makeText(this, "您至少需要在系统设置中添加一个指纹", Toast.LENGTH_SHORT).show();
                return false;
            }
        } else {
            Toast.makeText(this, "您的手机暂不支持指纹识别", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void initKey() {
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(DEFAULT_KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT |
                            KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7);
            keyGenerator.init(builder.build());
            keyGenerator.generateKey();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void initCipher() {
        try {
            SecretKey key = (SecretKey) keyStore.getKey(DEFAULT_KEY_NAME, null);
            Cipher cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_CBC + "/"
                    + KeyProperties.ENCRYPTION_PADDING_PKCS7);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            showFingerPrintDialog(cipher);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void showFingerPrintDialog(Cipher cipher) {
        final FingerPrintDialog fingerPrintDialog = new FingerPrintDialog();
        fingerPrintDialog.setCipher(cipher);
        fingerPrintDialog.setCancelable(false);
        fingerPrintDialog.show(getSupportFragmentManager(), "fingerprint");
    }
}
