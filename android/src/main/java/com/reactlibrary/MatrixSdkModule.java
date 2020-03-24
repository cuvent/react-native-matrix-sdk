package com.reactlibrary;

import android.net.Uri;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;

import org.json.JSONException;
import org.matrix.androidsdk.HomeServerConnectionConfig;
import org.matrix.androidsdk.MXDataHandler;
import org.matrix.androidsdk.MXSession;
import org.matrix.androidsdk.core.callback.ApiCallback;
import org.matrix.androidsdk.core.callback.SimpleApiCallback;
import org.matrix.androidsdk.core.model.MatrixError;
import org.matrix.androidsdk.data.store.MXMemoryStore;
import org.matrix.androidsdk.rest.client.LoginRestClient;
import org.matrix.androidsdk.rest.model.login.Credentials;

import static com.reactlibrary.MatrixData.convertRoomToMap;

public class MatrixSdkModule extends ReactContextBaseJavaModule {
    private final String TAG = MatrixSdkModule.class.getSimpleName();
    private final ReactApplicationContext reactContext;

    private HomeServerConnectionConfig hsConfig;
    private MXSession mxSession;

    private static final String E_MATRIX_ERROR = "E_MATRIX_ERROR";
    private static final String E_NETWORK_ERROR = "E_NETWORK_ERROR";
    private static final String E_UNEXCPECTED_ERROR = "E_UNKNOWN_ERROR";


    public MatrixSdkModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "RN_MatrixSdk";
    }

    @ReactMethod
    public void configure(String matrixServerUrl) {
        hsConfig = new HomeServerConnectionConfig.Builder()
                .withHomeServerUri(Uri.parse(matrixServerUrl))
                .build();
    }


    @ReactMethod
    public void login(String username, String password, Promise promise) {
        new LoginRestClient(hsConfig).loginWithUser(username, password, new SimpleApiCallback<Credentials>() {
            @Override
            public void onSuccess(Credentials info) {
                try {
                    hsConfig.setCredentials(info);
                    promise.resolve(info.toJson().toString());
                } catch (JSONException e) {
                    promise.reject(E_UNEXCPECTED_ERROR, "{\"error\": \"Couldn't parse JSON response\"}");
                }
            }

            @Override
            public void onMatrixError(MatrixError e) {
                super.onMatrixError(e);
                promise.reject(E_MATRIX_ERROR, "{\"error\": \""+e.getMessage()+"\"}");
            }

            @Override
            public void onNetworkError(Exception e) {
                super.onNetworkError(e);
                promise.reject(E_NETWORK_ERROR,"{\"error\": \""+e.getMessage()+"\"}");
            }

            @Override
            public void onUnexpectedError(Exception e) {
                super.onUnexpectedError(e);
                promise.reject(E_UNEXCPECTED_ERROR, "{\"error\": \""+e.getMessage()+"\"}");
            }
        });
    }

    @ReactMethod
    public void startSession(Promise promise) {
        mxSession = new MXSession.Builder(
                hsConfig,
                new MXDataHandler(
                        // TODO: uses MemoryStore, FileStore maybe better for persistence? (caused issues when trying to use)
                        new MXMemoryStore(
                                hsConfig.getCredentials(),
                                reactContext.getApplicationContext()
                        ),
                        hsConfig.getCredentials()),
                reactContext.getApplicationContext())
                .build();

        // The session should be alive after we created it
        if(!mxSession.isAlive()) {
            promise.reject(E_MATRIX_ERROR, "Couldn't start session for unknown reason.");
            return;
        }

        WritableMap map = Arguments.createMap();

        map.putString("user_id", mxSession.getMyUser().user_id);
        map.putString("display_name", mxSession.getMyUser().displayname);
        map.putString("avatar", mxSession.getMyUser().avatar_url);
        Long lastActive = mxSession.getMyUser().lastActiveAgo != null ? mxSession.getMyUser().lastActiveAgo : 0L;
        map.putDouble("last_active", lastActive);
        map.putString("last_active", mxSession.getMyUser().statusMsg);

        promise.resolve(map);
    }

    @ReactMethod
    public void createRoom(String userId, Promise promise) {
        if(mxSession == null) {
            promise.reject(E_MATRIX_ERROR, "client is not connected yet");
            return;
        }

        mxSession.createDirectMessageRoom(userId, new ApiCallback<String>() {
            @Override
            public void onNetworkError(Exception e) {
                promise.reject(E_NETWORK_ERROR, e.getMessage());
            }

            @Override
            public void onMatrixError(MatrixError e) {
                promise.reject(E_MATRIX_ERROR, e.getMessage());
            }

            @Override
            public void onUnexpectedError(Exception e) {
                promise.reject(E_UNEXCPECTED_ERROR, e.getMessage());
            }

            @Override
            public void onSuccess(String roomId) {
                promise.resolve(
                        convertRoomToMap(mxSession.getDataHandler().getRoom(roomId))
                );
            }
        });
    }
}
