package com.reactlibrary;

import android.net.Uri;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;

import org.json.JSONException;
import org.matrix.androidsdk.HomeServerConnectionConfig;
import org.matrix.androidsdk.MXDataHandler;
import org.matrix.androidsdk.MXSession;
import org.matrix.androidsdk.data.Room;
import org.matrix.androidsdk.data.store.MXMemoryStore;
import org.matrix.androidsdk.rest.client.LoginRestClient;
import org.matrix.androidsdk.rest.model.CreateRoomParams;
import org.matrix.androidsdk.rest.model.CreateRoomResponse;
import org.matrix.androidsdk.rest.model.CreatedEvent;
import org.matrix.androidsdk.rest.model.login.Credentials;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.reactlibrary.MatrixData.convertRoomToMap;

public class MatrixSdkModule extends ReactContextBaseJavaModule {
    public static final String TAG = MatrixSdkModule.class.getSimpleName();
    private final ReactApplicationContext reactContext;

    private HomeServerConnectionConfig hsConfig;
    private MXSession mxSession;

    public static final String E_MATRIX_ERROR = "E_MATRIX_ERROR";
    public static final String E_NETWORK_ERROR = "E_NETWORK_ERROR";
    public static final String E_UNEXCPECTED_ERROR = "E_UNKNOWN_ERROR";


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
        new LoginRestClient(hsConfig).loginWithUser(username, password, new RejectingOnErrorApiCallback<Credentials>(promise) {
            @Override
            public void onSuccess(Credentials info) {
                try {
                    hsConfig.setCredentials(info);
                    promise.resolve(info.toJson().toString());
                } catch (JSONException e) {
                    promise.reject(E_UNEXCPECTED_ERROR, "{\"error\": \"Couldn't parse JSON response\"}");
                }
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

        List<String> participants = new ArrayList<>();
        participants.add(userId);

        CreateRoomParams params = new CreateRoomParams();
        params.invitedUserIds = participants;
        params.isDirect = true;
        params.visibility = "private";

        mxSession.getRoomsApiClient().createRoom(params, new RejectingOnErrorApiCallback<CreateRoomResponse>(promise) {
            @Override
            public void onSuccess(CreateRoomResponse info) {
                promise.resolve(
                        convertRoomToMap(mxSession.getDataHandler().getRoom(info.roomId))
                );
            }
        });
    }

    // TODO: make it worky worky
    @ReactMethod
    public void getJoinedRooms(Promise promise) {
        if(mxSession == null) {
            promise.reject(E_MATRIX_ERROR, "client is not connected yet");
            return;
        }

//        List<WritableMap> rooms = new ArrayList<>();
//        Log.d(TAG, "init complete? Answer: " + mxSession.getDataHandler().isInitialSyncComplete());
//        Collection<RoomSummary> roomSummaries = mxSession.getDataHandler().getStore().getSummaries();
//        Log.d(TAG, "roomSummeriesCount: " + roomSummaries.size());
//        for(RoomSummary summary : roomSummaries) {
//            rooms.add(
//                    convertRoomToMap(
//                            mxSession.getDataHandler().getRoom(summary.getRoomId())
//                    )
//            );
//        }

        List<WritableMap> rooms = new ArrayList<>();
        for (Room room : mxSession.getDataHandler().getStore().getRooms()) {
            rooms.add(
                    convertRoomToMap(room)
            );
        }

        promise.resolve(Arguments.fromList(rooms));
    }

    @ReactMethod
    public void sendMessageToRoom(String roomId, String messageType, ReadableMap data, Promise promise) {
        mxSession.getRoomsApiClient().sendMessage(
                UUID.randomUUID().toString(),
                roomId,
                RNJson.convertMapToJson(data),
                new RejectingOnErrorApiCallback<CreatedEvent>(promise) {
                    @Override
                    public void onSuccess(CreatedEvent info) {
                        WritableMap map = Arguments.createMap();
                        map.putString("success", info.eventId);
                        promise.resolve(map);
                    }
                });
    }
}
