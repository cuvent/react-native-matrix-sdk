package de.hannojg;

import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import org.matrix.androidsdk.data.Room;
import org.matrix.androidsdk.data.RoomSummary;
import org.matrix.androidsdk.rest.model.Event;
import org.matrix.androidsdk.rest.model.RoomMember;

import java.util.Collection;
import java.util.List;

import static de.hannojg.MatrixSdkModule.TAG;

public class MatrixData {
    public static WritableMap convertEventToMap(Event matrixEvent) {
        WritableMap map = Arguments.createMap();
        map.putString("event_type", matrixEvent.type);
        map.putString("event_id", matrixEvent.eventId);
        map.putString("room_id", matrixEvent.roomId);
        map.putString("sender_id", matrixEvent.sender);
        map.putDouble("age", matrixEvent.age != null ? matrixEvent.age : Long.MAX_VALUE - 1);
        map.putDouble("ts", matrixEvent.originServerTs);
        map.putMap("content", RNJson.convertJsonToMap(matrixEvent.getContentAsJsonObject()));
        return map;
    }

    public static WritableMap convertMemberToMap(RoomMember member) {
        WritableMap map = Arguments.createMap();
        map.putString("membership", member.getMembership());
        map.putString("userId", member.getUserId());
        map.putString("name", member.getName());
        map.putString("avatarUrl", member.getAvatarUrl());
        return map;
    }

    public static WritableArray convertMembersToArray(Collection<RoomMember> members) {
        WritableArray array = Arguments.createArray();
        for (RoomMember member : members) {
            array.pushMap(
                    convertMemberToMap(member)
            );
        }
        return array;
    }

    public static WritableMap convertRoomToMap(Room room) {
        WritableMap map = Arguments.createMap();
        map.putString("room_id", room.getRoomId());
        map.putString("name", room.getState().name);
        map.putInt("notification_count", room.getNotificationCount());
        map.putInt("highlight_count", room.getHighlightCount());
        map.putBoolean("is_direct", room.isDirect());
        map.putBoolean("isLeft", room.isLeft());

        // room member
        List<RoomMember> members = room.getState().getDisplayableLoadedMembers();
        if (members.isEmpty()) {
            Log.d(TAG, "No members for room found, run query first");
            map.putNull("members");
        } else {
            map.putArray("members", convertMembersToArray(members));
        }

        // Room summary -> last event
        RoomSummary summary = room.getRoomSummary();
        if (summary == null) {
            summary = room.getStore().getSummary(room.getRoomId());
        }

        if (summary != null) {
            map.putMap("last_message", convertEventToMap(summary.getLatestReceivedEvent()));
        } else {
            Log.d(TAG, "Room summary was empty, thus we couldn't fetch latest event");
            map.putNull("last_message");
        }

        return map;
    }
}
