package de.hannojg;

import android.content.Context;
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
import com.facebook.react.bridge.ReadableArray;
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
import org.matrix.androidsdk.data.timeline.EventTimeline;
import org.matrix.androidsdk.listeners.IMXMediaUploadListener;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

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
        if (hsConfig != null) {
            if (hsConfig.getCredentials() != null) {
                try {
                    JsonElement json = JsonParser.parseString(hsConfig.getCredentials().toJson().toString());
                    WritableMap map = RNJson.convertJsonToMap(json.getAsJsonObject());
                    promise.resolve(map);
                    return;
                } catch (JSONException ignore) {
                    // swallow
                }
            }
        }

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
        if (mxSession != null && mxSession.getDataHandler().isInitialSyncComplete()) {
            // TODO: refactor, duplicated code
            WritableMap map = Arguments.createMap();

            map.putString("user_id", mxSession.getMyUser().user_id);
            map.putString("display_name", mxSession.getMyUser().displayname);
            map.putString("avatar", mxSession.getMyUser().avatar_url);
            Long lastActive = mxSession.getMyUser().lastActiveAgo != null ? mxSession.getMyUser().lastActiveAgo : 0L;
            map.putDouble("last_active", lastActive);
            map.putString("last_active", mxSession.getMyUser().statusMsg);
            promise.resolve(map);
            return;
        }

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
    public void createRoom(ReadableArray userIds, boolean isDirect, boolean isTrustedPrivateChat, String name, Promise promise) {
        if (mxSession == null) {
            promise.reject(E_MATRIX_ERROR, "client is not connected yet");
            return;
        }

        // Convert readableArray to List<String>
        List<String> userIdsList = new ArrayList<>();
        if (userIds != null) {
            int size = userIds.size();
            for (int j = 0; j < size; j++) {
                if (!userIds.isNull(j)) {
                    userIdsList.add(userIds.getString(j));
                }
            }
        } else {
            promise.reject(E_MATRIX_ERROR, "no userIds provided");
        }

        CreateRoomParams params = new CreateRoomParams();
        params.invitedUserIds = userIdsList;
        params.visibility = "private";
        params.isDirect = isDirect;
        if (isTrustedPrivateChat) {
            params.preset = "trusted_private_chat";
        }
        params.name = name;

        if (!isDirect) {
            mxSession.getRoomsApiClient().createRoom(params, new RejectingOnErrorApiCallback<CreateRoomResponse>(promise) {
                @Override
                public void onSuccess(CreateRoomResponse info) {
                    Room room = mxSession.getDataHandler().getRoom(info.roomId);
                    room.updateJoinRules(RoomState.JOIN_RULE_PUBLIC, new RejectingOnErrorApiCallback<Void>(promise) {
                        @Override
                        public void onSuccess(Void info) {
                            promise.resolve(
                                    MatrixData.convertRoomToMap(room)
                            );
                        }
                    });
                }
            });
        } else {
            mxSession.createDirectMessageRoom(userIdsList.get(0), new RejectingOnErrorApiCallback<String>(promise) {
                @Override
                public void onSuccess(String info) {
                    Room room = mxSession.getDataHandler().getRoom(info);
                    room.updateJoinRules(RoomState.JOIN_RULE_PUBLIC, new RejectingOnErrorApiCallback<Void>(promise) {
                        @Override
                        public void onSuccess(Void info) {
                            promise.resolve(
                                    MatrixData.convertRoomToMap(room)
                            );
                        }
                    });
                }
            });
        }
    }

    @ReactMethod
    public void updateRoomName(String roomId, String newName, Promise promise) {
        if (mxSession == null) {
            promise.reject(E_MATRIX_ERROR, "client is not connected yet");
            return;
        }

        Room room = mxSession.getDataHandler().getRoom(roomId, false);
        if (room == null) {
            promise.reject(E_MATRIX_ERROR, "RoomID " + roomId + " not found. Can't remove user");
            return;
        }

        room.updateName(newName, new RejectingOnErrorApiCallback<Void>(promise) {
            @Override
            public void onSuccess(Void info) {
                promise.resolve(null);
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
                        MatrixData.convertRoomToMap(mxSession.getDataHandler().getRoom(roomId))
                );
            }
        });
    }

    @ReactMethod
    public void leaveRoom(String roomId, Promise promise) {
        if (mxSession == null) {
            promise.reject(E_MATRIX_ERROR, "client is not connected yet");
            return;
        }

        Room room = mxSession.getDataHandler().getRoom(roomId, false);
        if (room == null) {
            promise.reject(E_MATRIX_ERROR, "RoomID ' + roomId + ' not found. Can't leave");
            return;
        }

        room.leave(new RejectingOnErrorApiCallback<Void>(promise) {
            @Override
            public void onSuccess(Void info) {
                promise.resolve(null);
            }
        });
    }

    @ReactMethod
    public void removeUserFromRoom(String roomId, String userId, Promise promise) {
        if (mxSession == null) {
            promise.reject(E_MATRIX_ERROR, "client is not connected yet");
            return;
        }

        Room room = mxSession.getDataHandler().getRoom(roomId, false);
        if (room == null) {
            promise.reject(E_MATRIX_ERROR, "RoomID " + roomId + " not found. Can't remove user");
            return;
        }
        room.kick(userId, null, new RejectingOnErrorApiCallback<Void>(promise) {
            @Override
            public void onSuccess(Void info) {
                promise.resolve(null);
            }
        });
    }

    @ReactMethod
    public void changeUserPermission(String roomId, String userId, boolean setAdmin, Promise promise) {
        if (mxSession == null) {
            promise.reject(E_MATRIX_ERROR, "client is not connected yet");
            return;
        }

        Room room = mxSession.getDataHandler().getRoom(roomId, false);
        if (room == null) {
            promise.reject(E_MATRIX_ERROR, "RoomID " + roomId + " not found. Can't remove user");
            return;
        }
        int power = setAdmin ? 100 : 0;
        room.updateUserPowerLevels(userId, power, new RejectingOnErrorApiCallback<Void>(promise) {
            @Override
            public void onSuccess(Void info) {
                promise.resolve(null);
            }
        });
    }

    @ReactMethod
    public void inviteUserToRoom(String roomId, String userId, Promise promise) {
        if (mxSession == null) {
            promise.reject(E_MATRIX_ERROR, "client is not connected yet");
            return;
        }

        Room room = mxSession.getDataHandler().getRoom(roomId, false);
        if (room == null) {
            promise.reject(E_MATRIX_ERROR, "RoomID ' + roomId + ' not found. Can't remove user");
            return;
        }
        room.invite(mxSession, userId, new RejectingOnErrorApiCallback<Void>(promise) {
            @Override
            public void onSuccess(Void info) {
                promise.resolve(null);
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
        for (Room room : mxSession.getDataHandler().getStore().getRooms()) {
            if (room.isInvited()) {
                rooms.pushMap(
                        MatrixData.convertRoomToMap(room)
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
        for (Room room : rooms) {
            roomSummaries.pushMap(
                    MatrixData.convertRoomToMap(
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
                        MatrixData.convertRoomToMap(room)
                );
            }

            promise.resolve(rooms);
        } else {
            promise.reject(E_MATRIX_ERROR, "Initial sync ain't completed yet, please start session first");
        }
    }

    @ReactMethod
    public void getLeftRooms(Promise promise) {
        if (mxSession == null) {
            promise.reject(E_MATRIX_ERROR, "client is not connected yet");
            return;
        }


        if (mxSession.getDataHandler().isInitialSyncComplete()) {
            WritableArray rooms = Arguments.createArray();
            if (!mxSession.getDataHandler().areLeftRoomsSynced()) {
                Log.d(TAG, "Syncing left rooms first");
                // we need to get left rooms first
                mxSession.getDataHandler().retrieveLeftRooms(new RejectingOnErrorApiCallback<Void>(promise) {
                    @Override
                    public void onSuccess(Void info) {
                        getLeftRooms(promise);
                    }
                });
                return;
            }

            for (Room room : mxSession.getDataHandler().getLeftRooms()) {
                rooms.pushMap(
                        MatrixData.convertRoomToMap(room)
                );
            }

            promise.resolve(rooms);
        } else {
            promise.reject(E_MATRIX_ERROR, "Initial sync ain't completed yet, please start session first");
        }
    }

    @ReactMethod
    public void forgetRoom(String roomId, Promise promise) {
        if (mxSession == null) {
            promise.reject(E_MATRIX_ERROR, "client is not connected yet");
            return;
        }

        mxSession.getDataHandler().getRoom(roomId).forget(new RejectingOnErrorApiCallback<Void>(promise) {
            @Override
            public void onSuccess(Void info) {
                promise.resolve(null);
            }
        });
    }


    EventTimeline.Listener backwardsListener = (event, direction, roomState) -> {
        if (direction == EventTimeline.Direction.BACKWARDS) {
            sendEvent(
                    "matrix.room.backwards",
                    MatrixData.convertEventToMap(event)
            );
            Log.d(TAG, event.toString());
        }
    };
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
                        MatrixData.convertEventToMap(event)
                );
                Log.d(TAG, event.toString());
            }
        };
        room.getTimeline().addEventTimelineListener(backwardsListener);

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
        room.getTimeline().removeEventTimelineListener(backwardsListener);
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

        if (globalListener == null) {
            globalListener = new MXEventListener() {
                @Override
                public void onLiveEvent(Event event, RoomState roomState) {
                    sendEvent(event.getType(), MatrixData.convertEventToMap(event));
                }

                @Override
                public void onPresenceUpdate(Event event, User user) {
                    sendEvent(event.getType(), MatrixData.convertEventToMap(event));
                }

                @Override
                public void onEventSent(Event event, String prevEventId) {
                    sendEvent(event.getType(), MatrixData.convertEventToMap(event));
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
        if (globalListener != null && mxSession != null) {
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
        if (!initialLoad && roomPaginationTokens.get(roomId) != null) {
            fromToken = roomPaginationTokens.get(roomId);
        }

        room.requestServerRoomHistory(fromToken, perPage, new RejectingOnErrorApiCallback<TokensChunkEvents>(promise) {
            @Override
            public void onSuccess(TokensChunkEvents info) {
                roomPaginationTokens.put(roomId, info.end);
                WritableArray msgs = Arguments.createArray();
                for (Event event : info.chunk) {
                    msgs.pushMap(MatrixData.convertEventToMap(event));
                }
                promise.resolve(msgs);
            }
        });
    }

    @ReactMethod
    public void backPaginate(String roomId, int perPage, boolean initHistory, Promise promise) {
        if (mxSession == null) {
            promise.reject(E_MATRIX_ERROR, "client is not connected yet");
            return;
        }

        Room room = mxSession.getDataHandler().getRoom(roomId);
        if (room == null) {
            promise.reject(E_MATRIX_ERROR, "Room not found");
            return;
        }

        if (initHistory) {
            room.getTimeline().initHistory();
        }

        room.getTimeline().backPaginate(perPage, new RejectingOnErrorApiCallback<Integer>(promise) {
            @Override
            public void onSuccess(Integer info) {
                promise.resolve(info);
            }
        });
    }

    @ReactMethod
    private void canBackPaginate(String roomId, Promise promise) {
        if (mxSession == null) {
            promise.reject(E_MATRIX_ERROR, "client is not connected yet");
            return;
        }

        Room room = mxSession.getDataHandler().getRoom(roomId);
        if (room == null) {
            promise.reject(E_MATRIX_ERROR, "Room not found");
            return;
        }

        promise.resolve(room.getTimeline().canBackPaginate());
    }

    @ReactMethod
    public void sendReadReceipt(String roomId, String eventId, Promise promise) {
        if (mxSession == null) {
            promise.reject(E_MATRIX_ERROR, "client is not connected yet");
            return;
        }

        Room room = mxSession.getDataHandler().getRoom(roomId, false);

        if (room == null) {
            promise.reject(E_MATRIX_ERROR, "Room not found");
            return;
        }

        Event event = mxSession.getDataHandler().getStore().getEvent(eventId, roomId);
        if (event == null) {
            promise.reject(E_MATRIX_ERROR, "Event not found");
            return;
        }

        room.sendReadReceipt(event, new RejectingOnErrorApiCallback<Void>(promise) {
            @Override
            public void onSuccess(Void info) {
                promise.resolve(null);
            }
        });
    }

    @ReactMethod
    public void markRoomAsRead(String roomId, Promise promise) {
        if (mxSession == null) {
            promise.reject(E_MATRIX_ERROR, "client is not connected yet");
            return;
        }

        Room room = mxSession.getDataHandler().getRoom(roomId, false);

        if (room == null) {
            promise.reject(E_MATRIX_ERROR, "Room not found");
            return;
        }

        room.markAllAsRead(new RejectingOnErrorApiCallback<Void>(promise) {
            @Override
            public void onSuccess(Void info) {
                promise.resolve(null);
            }
        });
    }

    //* ******************************************
    //*  SENDING EVENTS
    //* ******************************************
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
    public void sendEventToRoom(String roomId, String eventType, ReadableMap data, Promise promise) {
        if (mxSession == null) {
            promise.reject(E_MATRIX_ERROR, "client is not connected yet");
            return;
        }

        mxSession.getRoomsApiClient().sendEventToRoom(
                UUID.randomUUID().toString(),
                roomId,
                eventType,
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

    //* ******************************************
    //*  PROFILE
    //* ******************************************

    @ReactMethod
    public void setUserDisplayName(String displayName, Promise promise) {
        if (mxSession == null) {
            promise.reject(E_MATRIX_ERROR, "client is not connected yet");
            return;
        }

        mxSession.getProfileApiClient().updateDisplayname(
                mxSession.getMyUserId(),
                displayName,
                new RejectingOnErrorApiCallback<Void>(promise) {
                    @Override
                    public void onSuccess(Void info) {
                        promise.resolve(null);
                    }
                }
        );
    }

    //* ******************************************
    //*  TYPING
    //* ******************************************

    @ReactMethod
    public void sendTyping(String roomId, boolean isTyping, int timeout, Promise promise) {
        if (mxSession == null) {
            promise.reject(E_MATRIX_ERROR, "client is not connected yet");
            return;
        }

        Room room = mxSession.getDataHandler().getRoom(roomId);
        room.sendTypingNotification(isTyping, timeout, new RejectingOnErrorApiCallback<Void>(promise) {
            @Override
            public void onSuccess(Void info) {
                promise.resolve(null);
            }
        });
    }

    //* ******************************************
    //*  MEDIA
    //* ******************************************
    @ReactMethod
    public void uploadContent(String fileUrl, String fileName, String mimeType, String uploadId, Promise promise) {
        if (mxSession == null) {
            promise.reject(E_MATRIX_ERROR, "client is not connected yet");
            return;
        }

        try {
            File file = new File(fileUrl);
            FileInputStream fileInputStream = new FileInputStream(file);
            mxSession.getMediaCache().uploadContent(fileInputStream, fileName, mimeType, uploadId, new IMXMediaUploadListener() {
                @Override
                public void onUploadStart(String uploadId) {

                }

                @Override
                public void onUploadProgress(String uploadId, UploadStats uploadStats) {

                }

                @Override
                public void onUploadCancel(String uploadId) {
                    promise.reject(uploadId);
                }

                @Override
                public void onUploadError(String uploadId, int serverResponseCode, String serverErrorMessage) {
                    promise.reject(uploadId);
                }

                @Override
                public void onUploadComplete(String uploadId, String contentUri) {
                    WritableMap result = Arguments.createMap();
                    result.putString("uploadId", contentUri);
                    promise.resolve(result);
                }
            });
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            promise.reject("File not found");
        }
    }

    @ReactMethod
    public void contentGetDownloadableUrl(String matrixContentUri, Promise promise) {
        if (mxSession == null) {
            promise.reject(E_MATRIX_ERROR, "client is not connected yet");
            return;
        }

        String url = mxSession.getContentManager().getDownloadableUrl(matrixContentUri, false);
        if (url != null) {
            promise.resolve(url);
        } else {
            promise.reject(E_MATRIX_ERROR, "Failed to resolve content url");
        }
    }

    //* ******************************************
    //*  PUSH NOTIFICATIONS
    //* ******************************************

    @ReactMethod
    public void registerPushNotifications(String displayName, String appId, String pushServiceUrl, String token, Promise promise) {
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
                        if (pushers.isEmpty()) {
                            // register push notifications
                            addHttpPusher(displayName, appId, pushServiceUrl, token, promise);
                        } else {
                            // check pushers:
                            boolean needToRegister = true;
                            for (Pusher pusher : pushers) {
                                Log.d(TAG, pusher.toString());
                                if (pusher.pushkey.equals(token)) {
                                    needToRegister = false;
                                    break;
                                }
                            }
                            if (needToRegister) {
                                addHttpPusher(displayName, appId, pushServiceUrl, token, promise);
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
    private void addHttpPusher(String displayName, String appId, String pushServiceUrl, String token, Promise promise) {
        Context mContext = reactContext.getApplicationContext();
        String mPusherLang = mContext.getResources().getConfiguration().locale.getLanguage();
        String mBasePusherDeviceName = Build.MODEL.trim();

        mxSession.getPushersRestClient()
                .addHttpPusher(
                        token,
                        appId,
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

    //* ******************************************
    //*  LIFE CYCLE
    //* ******************************************

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

    //* ******************************************
    //*  UTILITIES
    //* ******************************************

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
