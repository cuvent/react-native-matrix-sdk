package com.reactlibrary;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

import org.matrix.androidsdk.data.Room;
import org.matrix.androidsdk.rest.model.Event;

public class MatrixData {
    public static WritableMap convertEventToMap(Event matrixEvent) {
        WritableMap map = Arguments.createMap();
        map.putString("event_type", matrixEvent.type);
        map.putString("event_id", matrixEvent.eventId);
        map.putString("room_id", matrixEvent.roomId);
        map.putString("sender_id", matrixEvent.sender);
        map.putDouble("age", matrixEvent.age);
        map.putString("content", matrixEvent.contentJson.getAsString());
        return map;
    }

    public static WritableMap convertRoomToMap(Room room) {
        WritableMap map = Arguments.createMap();
        map.putString("room_id", room.getRoomId());
        map.putString("name", room.getState().name);
        map.putInt("notification_count", room.getNotificationCount());
        map.putInt("highlight_count", room.getHighlightCount());
        map.putBoolean("is_direct", room.isDirect());
        map.putMap("last_message", convertEventToMap(
                room.getRoomSummary().getLatestReceivedEvent()
        ));

        return map;
    }
}
