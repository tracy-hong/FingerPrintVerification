package com.hong.fingerprintverification;

import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import javax.crypto.Cipher;

import static android.content.Context.FINGERPRINT_SERVICE;

/**
 * Created by root on 2018/9/20.
 */

public class FingerPrintDialog extends DialogFragment {

    private FingerprintManager manager;
    private TextView message;
    private CancellationSignal cancel;
    // 是否主动取消
    private boolean isCancel;
    private Cipher cipher;
    private Handler handler = new Handler();
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        manager = (FingerprintManager) getContext().getSystemService(FINGERPRINT_SERVICE);
        cancel = new CancellationSignal();
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_fingerprint, container, false);
        message = view.findViewById(R.id.error_msg);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        startListening();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopListening();
    }

    public void setCipher(Cipher cipher){
        this.cipher = cipher;
    }

    private void startListening() {
        isCancel = false;
        manager.authenticate(new FingerprintManager.CryptoObject(cipher), cancel, 0, new FingerprintManager.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                if (!isCancel) {
                    message.setText(errString);
                    // 超过5次错误，自动锁住
                    if (errorCode == FingerprintManager.FINGERPRINT_ERROR_LOCKOUT) {
                        Toast.makeText(getContext(), errString, Toast.LENGTH_SHORT).show();
                        dismiss();
                    }
                }
            }

            @Override
            public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
                message.setText(helpString);
            }

            @Override
            public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
                Toast.makeText(getContext(), "指纹识别成功！", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getActivity(), SuccessPageActivity.class);
                startActivity(intent);
            }

            @Override
            public void onAuthenticationFailed() {
                message.setText("指纹验证失败，请重试！");
                // 一秒钟后去掉提示
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        message.setText(" ");
                    }
                }, 1000);
            }
        }, null);
    }

    private void stopListening() {
        isCancel = true;
    }
}
