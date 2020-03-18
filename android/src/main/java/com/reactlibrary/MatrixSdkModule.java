package com.reactlibrary;

import android.net.Uri;
import android.util.Log;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import org.matrix.androidsdk.HomeServerConnectionConfig;
import org.matrix.androidsdk.MXDataHandler;
import org.matrix.androidsdk.MXSession;
import org.matrix.androidsdk.core.callback.SimpleApiCallback;
import org.matrix.androidsdk.data.store.MXMemoryStore;
import org.matrix.androidsdk.rest.client.LoginRestClient;
import org.matrix.androidsdk.rest.model.login.Credentials;

public class MatrixSdkModule extends ReactContextBaseJavaModule {
    private final String TAG = MatrixSdkModule.class.getSimpleName();
    private final ReactApplicationContext reactContext;

    private HomeServerConnectionConfig hsConfig;
    private MXSession mxSession;


    public MatrixSdkModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "MatrixSdk";
    }

    @ReactMethod
    public void configure(String matrixServerUrl) {
        hsConfig = new HomeServerConnectionConfig.Builder()
                .withHomeServerUri(Uri.parse(matrixServerUrl))
                .build();
        Log.i(TAG, "HomeServer config created for matrix server " + matrixServerUrl);
    }


    @ReactMethod
    public void login(String username, String password, final Callback callback) {
        new LoginRestClient(hsConfig).loginWithUser(username, password, new SimpleApiCallback<Credentials>() {
            @Override
            public void onSuccess(Credentials info) {
                callback.invoke(info.accessToken);
            }
        });
    }

    @ReactMethod
    public void startSession(Credentials credentials) {
        mxSession = new MXSession.Builder(hsConfig,
                new MXDataHandler(new MXMemoryStore(), credentials),
                getReactApplicationContext())
                .build();
    }
}
