package com.reactlibrary;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
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
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.json.JSONException;
import org.matrix.androidsdk.HomeServerConnectionConfig;
import org.matrix.androidsdk.MXDataHandler;
import org.matrix.androidsdk.MXSession;
import org.matrix.androidsdk.data.Pusher;
import org.matrix.androidsdk.data.Room;
import org.matrix.androidsdk.data.RoomState;
import org.matrix.androidsdk.data.store.MXFileStore;
import org.matrix.androidsdk.listeners.MXEventListener;
import org.matrix.androidsdk.rest.client.LoginRestClient;
import org.matrix.androidsdk.rest.model.CreateRoomParams;
import org.matrix.androidsdk.rest.model.CreateRoomResponse;
import org.matrix.androidsdk.rest.model.CreatedEvent;
import org.matrix.androidsdk.rest.model.Event;
import org.matrix.androidsdk.rest.model.PushersResponse;
import org.matrix.androidsdk.rest.model.TokensChunkEvents;
import org.matrix.androidsdk.rest.model.User;
import org.matrix.androidsdk.rest.model.login.Credentials;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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

    private HashMap<String, MXEventListener> roomEventListener = new HashMap<>();

    /**
     * Used when loading old messages
     */
    private HashMap<String, String> roomPaginationTokens = new HashMap<>();
    private MXEventListener globalListener;


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
                    JsonElement json = JsonParser.parseString(info.toJson().toString());
                    WritableMap map = RNJson.convertJsonToMap(json.getAsJsonObject());
                    promise.resolve(map);
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
                        new MXFileStore(
                                hsConfig,
                                false,
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

        WritableMap map = Arguments.createMap();

        map.putString("user_id", mxSession.getMyUser().user_id);
        map.putString("display_name", mxSession.getMyUser().displayname);
        map.putString("avatar", mxSession.getMyUser().avatar_url);
        Long lastActive = mxSession.getMyUser().lastActiveAgo != null ? mxSession.getMyUser().lastActiveAgo : 0L;
        map.putDouble("last_active", lastActive);
        map.putString("last_active", mxSession.getMyUser().statusMsg);


        if (!mxSession.getDataHandler().isInitialSyncComplete()) {
            mxSession.getDataHandler().addListener(new MXEventListener() {
                @Override
                public void onInitialSyncComplete(String toToken) {
                    super.onInitialSyncComplete(toToken);
                    promise.resolve(map);
                    mxSession.getDataHandler().removeListener(this);
                }
            });
            mxSession.getDataHandler().getStore().open();
            mxSession.startEventStream(null);
        } else {
            promise.resolve(map);
        }
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
    public void joinRoom(String roomId, Promise promise) {
        if (mxSession == null) {
            promise.reject(E_MATRIX_ERROR, "client is not connected yet");
            return;
        }

        mxSession.joinRoom(roomId, new RejectingOnErrorApiCallback<String>(promise) {
            @Override
            public void onSuccess(String info) {
                promise.resolve(
                        convertRoomToMap(mxSession.getDataHandler().getRoom(roomId))
                );
            }
        });
    }

    @ReactMethod
    public void getInvitedRooms(Promise promise) {
        if (mxSession == null) {
            promise.reject(E_MATRIX_ERROR, "client is not connected yet");
            return;
        }

        WritableArray rooms = Arguments.createArray();
        for(Room room : mxSession.getDataHandler().getStore().getRooms()) {
            if(room.isInvited()) {
                rooms.pushMap(
                        convertRoomToMap(room)
                );
            }
        }

        promise.resolve(rooms);
    }

//    TODO: getPublicRooms(url: string): Promise<PublicRooms>;


    @ReactMethod
    public void getLastEventsForAllRooms(Promise promise) {
        if (mxSession == null) {
            promise.reject(E_MATRIX_ERROR, "client is not connected yet");
            return;
        }

        if (!mxSession.getDataHandler().isInitialSyncComplete()) {
            promise.reject(E_MATRIX_ERROR, "client is setup, but not synced yet. Please start a session first.");
            return;
        }

        Collection<Room> rooms = mxSession.getDataHandler().getStore().getRooms();
        WritableArray roomSummaries = Arguments.createArray();
        for(Room room : rooms) {
            roomSummaries.pushMap(
                    convertRoomToMap(
                            room
                    )
            );
        }
        promise.resolve(roomSummaries);
    }

    @ReactMethod
    public void getJoinedRooms(Promise promise) {
        if (mxSession == null) {
            promise.reject(E_MATRIX_ERROR, "client is not connected yet");
            return;
        }


        if (mxSession.getDataHandler().isInitialSyncComplete()) {
            WritableArray rooms = Arguments.createArray();
            for (Room room : mxSession.getDataHandler().getStore().getRooms()) {
                rooms.pushMap(
                        convertRoomToMap(room)
                );
            }

            promise.resolve(rooms);
        } else {
            promise.reject(E_MATRIX_ERROR, "Initial sync ain't completed yet, please start session first");
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

        MXEventListener eventListener = new MXEventListener() {
            @Override
            public void onLiveEvent(Event event, RoomState roomState) {
                sendEvent(
                        "matrix.room.forwards",
                        convertEventToMap(event)
                );
                Log.d(TAG, event.toString());
            }
        };

        roomEventListener.put(roomId, eventListener);
        room.addEventListener(eventListener);

        promise.resolve(null);
    }

    @ReactMethod
    public void unlistenToRoom(String roomId, Promise promise) {
        if (mxSession == null) {
            promise.reject(E_MATRIX_ERROR, "client is not connected yet");
            return;
        }

        Room room = mxSession.getDataHandler().getRoom(roomId);
        if (room == null) {
            promise.reject(E_MATRIX_ERROR, "Room not found");
            return;
        }

        room.removeEventListener(roomEventListener.get(roomId));
        promise.resolve(null);
    }

    @ReactMethod
    public void listen(Promise promise) {
        if (mxSession == null) {
            promise.reject(E_MATRIX_ERROR, "client is not connected yet");
            return;
        }

        if (!mxSession.getDataHandler().isInitialSyncComplete()) {
            promise.reject(E_MATRIX_ERROR, "client is setup, but not synced yet. Please start a session first.");
            return;
        }

        if(globalListener == null) {
            globalListener = new MXEventListener() {
                @Override
                public void onLiveEvent(Event event, RoomState roomState) {
                    sendEvent(event.getType(), convertEventToMap(event));
                }

                @Override
                public void onPresenceUpdate(Event event, User user) {
                    sendEvent(event.getType(), convertEventToMap(event));
                }

                @Override
                public void onEventSent(Event event, String prevEventId) {
                    sendEvent(event.getType(), convertEventToMap(event));
                }
            };

            mxSession.getDataHandler().addListener(globalListener);
        } else {
            promise.reject(E_MATRIX_ERROR, "You already started listening, only one global listener is supported. You maybe forget to call `unlisten()`");
            return;
        }

        WritableMap successMap = Arguments.createMap();
        successMap.putBoolean("success", true);
        promise.resolve(successMap);
    }

    @ReactMethod
    public void unlisten() {
        if(globalListener != null && mxSession != null) {
            mxSession.getDataHandler().removeListener(globalListener);
            globalListener = null;
        }
    }

    @ReactMethod
    public void loadMessagesInRoom(String roomId, int perPage, boolean initialLoad, Promise promise) {
        if (mxSession == null) {
            promise.reject(E_MATRIX_ERROR, "client is not connected yet");
            return;
        }

        Room room = mxSession.getDataHandler().getRoom(roomId);
        if (room == null) {
            promise.reject(E_MATRIX_ERROR, "Room not found");
            return;
        }

        String fromToken = null;
        if(!initialLoad && roomPaginationTokens.get(roomId) != null) {
            fromToken = roomPaginationTokens.get(roomId);
        }

        room.requestServerRoomHistory(fromToken, perPage, new RejectingOnErrorApiCallback<TokensChunkEvents>(promise) {
            @Override
            public void onSuccess(TokensChunkEvents info) {
                roomPaginationTokens.put(roomId, info.end);
                WritableArray msgs = Arguments.createArray();
                for (Event event : info.chunk) {
                    msgs.pushMap(convertEventToMap(event));
                }
                promise.resolve(msgs);
            }
        });
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

    @ReactMethod
    public void registerPushNotifications(String displayName, String pushServiceUrl, String token, Promise promise) {
        if (mxSession == null) {
            promise.reject(E_MATRIX_ERROR, "client is not connected yet");
            return;
        }

        // First check whether a pusher is already registered
        mxSession.getPushersRestClient()
                .getPushers(new RejectingOnErrorApiCallback<PushersResponse>(promise) {
                    @Override
                    public void onSuccess(PushersResponse info) {
                        List<Pusher> pushers = info.pushers;
                        if(pushers.isEmpty()) {
                            // register push notifications
                            addHttpPusher(displayName, pushServiceUrl, token, promise);
                        } else {
                            // check pushers:
                            boolean needToRegister = true;
                            for(Pusher pusher : pushers) {
                                Log.d(TAG, pusher.toString());
                                if(pusher.pushkey.equals(token)) {
                                    needToRegister = false;
                                    break;
                                }
                            }
                            if(needToRegister) {
                                addHttpPusher(displayName, pushServiceUrl, token, promise);
                            } else {
                                // pusher already setup :)
                                promise.resolve(null);
                            }
                        }
                    }
                });
    }

    /**
     * internal, assumes that you have checked that session is active.
     * Implementation from {https://github.com/vector-im/riot-android/blob/develop/vector/src/main/java/im/vector/push/PushManager.java}
     */
    private void addHttpPusher(String displayName, String pushServiceUrl, String token, Promise promise) {
        Context mContext = reactContext.getApplicationContext();
        PackageInfo pInfo = null;
        try {
            pInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            promise.reject(e);
            return;
        }
        String mPusherAppName = pInfo.packageName;
        String mPusherLang = mContext.getResources().getConfiguration().locale.getLanguage();
        String mBasePusherDeviceName = Build.MODEL.trim();

        mxSession.getPushersRestClient()
                .addHttpPusher(
                        token,
                        mPusherAppName,
                        computePushTag(mxSession),
                        mPusherLang,
                        displayName,
                        mBasePusherDeviceName,
                        pushServiceUrl,
                        false,
                        false,
                        new RejectingOnErrorApiCallback<Void>(promise) {
                            @Override
                            public void onSuccess(Void info) {
                                promise.resolve(info);
                            }
                        }
                );
    }

    @Override
    public void onHostResume() {

    }

    @Override
    public void onHostPause() {

    }

    @Override
    public void onHostDestroy() {
        unlisten();
        mxSession.stopEventStream();
    }


    private void sendEvent(String eventName,
                           @Nullable WritableMap params) {
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }


    /**
     * Compute the profileTag for a session
     *
     * @param session the session
     * @return the profile tag
     */
    private static String computePushTag(final MXSession session) {
        String tag = "mobile_" + Math.abs(session.getMyUserId().hashCode());

        // tag max length : 32 bytes
        if (tag.length() > 32) {
            tag = Math.abs(tag.hashCode()) + "";
        }

        return tag;
    }
}
