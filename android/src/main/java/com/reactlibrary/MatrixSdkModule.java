package com.reactlibrary;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import org.json.JSONException;
import org.matrix.androidsdk.HomeServerConnectionConfig;
import org.matrix.androidsdk.MXDataHandler;
import org.matrix.androidsdk.MXSession;
import org.matrix.androidsdk.data.Room;
import org.matrix.androidsdk.data.RoomState;
import org.matrix.androidsdk.data.store.MXMemoryStore;
import org.matrix.androidsdk.listeners.MXEventListener;
import org.matrix.androidsdk.rest.client.LoginRestClient;
import org.matrix.androidsdk.rest.model.CreateRoomParams;
import org.matrix.androidsdk.rest.model.CreateRoomResponse;
import org.matrix.androidsdk.rest.model.CreatedEvent;
import org.matrix.androidsdk.rest.model.Event;
import org.matrix.androidsdk.rest.model.login.Credentials;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.reactlibrary.MatrixData.convertEventToMap;
import static com.reactlibrary.MatrixData.convertRoomToMap;

public class MatrixSdkModule extends ReactContextBaseJavaModule implements LifecycleEventListener {
    public static final String TAG = MatrixSdkModule.class.getSimpleName();
    private final ReactApplicationContext reactContext;

    private HomeServerConnectionConfig hsConfig;
    private MXSession mxSession;

    public static final String E_MATRIX_ERROR = "E_MATRIX_ERROR";
    public static final String E_NETWORK_ERROR = "E_NETWORK_ERROR";
    public static final String E_UNEXCPECTED_ERROR = "E_UNKNOWN_ERROR";


    /**
     * In android we can only retrieve rooms, once the inital sync is done
     * and the initial sync can take some seconds.
     * Therefor we have a list of pending promises that are interested in
     * getting rooms list.
     * Once the initial sync is completed we call {@link #getJoinedRooms(Promise)}
     * with the promises from the list, which results in returning the rooms.
     */
    private List<Promise> pendingRoomsPromises = new ArrayList<>();


    public MatrixSdkModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "RN_MatrixSdk";
    }

    /**
     * Event Listener is used to fulfil pending promises that needed
     * to wait for the initial sync to be happened.
     */
    private MXEventListener initialSyncCompletedListener = new MXEventListener() {
        @Override
        public void onInitialSyncComplete(String toToken) {
            super.onInitialSyncComplete(toToken);
            Log.d(TAG, "Initial sync completed!");

            for(Promise pendingProise :  pendingRoomsPromises) {
                getJoinedRooms(pendingProise);
                pendingRoomsPromises.remove(pendingProise);
            }

        }
    };

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
        if (!mxSession.isAlive()) {
            promise.reject(E_MATRIX_ERROR, "Couldn't start session for unknown reason.");
            return;
        }

        if(!mxSession.getDataHandler().isInitialSyncComplete()) {
            mxSession.getDataHandler().addListener(initialSyncCompletedListener);
            mxSession.getDataHandler().getStore().open();
            mxSession.startEventStream(null);
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
        if (mxSession == null) {
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

    @ReactMethod
    public void getJoinedRooms(Promise promise) {
        if (mxSession == null) {
            promise.reject(E_MATRIX_ERROR, "client is not connected yet");
            return;
        }


        if(mxSession.getDataHandler().isInitialSyncComplete()) {
            WritableArray rooms = Arguments.createArray();
            for (Room room : mxSession.getDataHandler().getStore().getRooms()) {
                rooms.pushMap(
                        convertRoomToMap(room)
                );
            }

            promise.resolve(rooms);
        } else {
            pendingRoomsPromises.add(promise);
        }
    }

    @ReactMethod
    public void listenToRoom(String roomId, Promise promise) {
        if (mxSession == null) {
            promise.reject(E_MATRIX_ERROR, "client is not connected yet");
            return;
        }

        Room room = mxSession.getDataHandler().getRoom(roomId);
        if (room == null) {
            promise.reject(E_MATRIX_ERROR, "Room not found");
            return;
        }

//        room.getTimeline().addEventTimelineListener((event, direction, roomState) -> {
//            // doesn't give typing indications
//        });

        room.addEventListener(new MXEventListener() {
            @Override
            public void onLiveEvent(Event event, RoomState roomState) {
                super.onLiveEvent(event, roomState);
                sendEvent(
                        "matrix.room.forwards",
                        convertEventToMap(event)
                );
                Log.d(TAG, event.toString());
            }
        });

        promise.resolve(null);
    }

    @ReactMethod
    public void sendMessageToRoom(String roomId, String messageType, ReadableMap data, Promise promise) {
        if (mxSession == null) {
            promise.reject(E_MATRIX_ERROR, "client is not connected yet");
            return;
        }

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

    @Override
    public void onHostResume() {

    }

    @Override
    public void onHostPause() {

    }

    @Override
    public void onHostDestroy() {
        mxSession.stopEventStream();
        mxSession.getDataHandler().removeListener(initialSyncCompletedListener);
    }


    private void sendEvent(String eventName,
                           @Nullable WritableMap params) {
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }
}
