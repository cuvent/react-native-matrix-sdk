declare interface MXCredentials {
  user_id: string;
  home_server: string;
  access_token: string;
  refresh_token: string | undefined;
  device_id: string;
}

declare interface MXSessionAttributes {
  user_id: string;
  display_name: string;
  avatar: string;
  last_active: number;
  status: string;
}

declare enum MessageEventType {
  text = "text",
  image = "image",
  video = "video",
  file = "file",
  audio = "audio",
  emote = "emote",
  location = "location",
}


declare interface MXMessageEvent {
  event_type: MessageEventType;
  event_id: string;
  room_id: string;
  sender_id: string;
  age: number;
  content: string;
}

declare interface MXRoomAttributes {
  room_id: string;
  name: string;
  notificationcount: number;
  highlight_count: number;
  is_direct: boolean;
  last_message: MXMessageEvent;
}

declare interface PublicRooms {
  id: string;
  aliases: string;
  name: string;
  guestCanJoin: boolean;
  numJoinedMembers: number;
}

declare interface MessagesFromRoom {
  start: string;
  end: string;
  results: [string];
}

declare interface SuccessResponse {
  success: string;
}

declare module 'react-native-matrix-sdk' {
  import {EventSubscriptionVendor} from "react-native";

  export interface MatrixSDKStatic extends EventSubscriptionVendor {
    configure(host: string): void;
    // TODO: actually credentials are returned as string, and not as Credentials Type
    login(username: string, password: string): Promise<MXCredentials>;
    startSession(): Promise<MXSessionAttributes>;
    createRoom(userId: string): Promise<MXRoomAttributes>;
    joinRoom(roomId: string): Promise<MXRoomAttributes>;
    getInvitedRooms(): Promise<MXRoomAttributes>;
    getPublicRooms(url: string): Promise<PublicRooms>;
    getUnreadEventTypes(): Promise<[string]>;
    getLastEventsForAllRooms(): Promise<[MXMessageEvent]>;
    getJoinedRooms(): Promise<[MXRoomAttributes]>;
    listenToRoom(roomId: string): Promise<void>;
    unlistenToRoom(roomId: string): Promise<void>;
    loadMessagesInRoom(roomId: string, perPage: number, initialLoad: boolean): Promise<void>;
    searchMessagesInRoom(roomId: string, searchTerm: string, nextBatch: string, beforeLimit: string, afterLimit: string);
    getMessages(roomId: string, from: string, direction: string, limit: number): Promise<MessagesFromRoom>;
    sendMessageToRoom(roomId: string, messageType: string, data: any): Promise<SuccessResponse>;
    sendReadReceipt(roomId: string, eventId: string): Promise<SuccessResponse>;
  }

  const MatrixSDK: MatrixSDKStatic;

  export default MatrixSDK;
}
