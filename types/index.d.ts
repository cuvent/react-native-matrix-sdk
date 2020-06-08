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

declare interface MXRoomMember {
  membership: 'join' | 'invite' | 'leave' | 'ban' | 'kick';
  userId: string;
  name: string;
  avatarUrl: string;
}

declare interface MXMessageEvent {
  event_type: string;
  event_id: string;
  room_id: string;
  sender_id: string;
  /**
   *  The age of the event in milliseconds.
   *  As home servers clocks may be not synchronised, this relative value may be more accurate.
   *  It is computed by the user's home server each time it sends the event to a client.
   *  Then, the SDK updates it each time the property is read (this doesn't work reliable yet).
   */
  age: number;
  /**
   *  The timestamp in ms since Epoch generated by the origin homeserver when it receives the event
   *  from the client.
   */
  ts: number;
  content: any;
}

declare interface MXRoomAttributes {
  room_id: string;
  name: string;
  notification_count: number;
  highlight_count: number;
  is_direct: boolean;
  last_message: MXMessageEvent;
  isLeft: boolean;
  members: MXRoomMember[];
}

declare interface PublicRoom {
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
    /**
     * Call this to add additional custom event types that your client
     * needs to support. This is for iOS only, android will emit the custom events
     * with out the need for calling this.
     * @param types
     */
    setAdditionalEventTypes(types: string[]): void;

    configure(host: string): void;
    login(username: string, password: string): Promise<MXCredentials>;
    startSession(): Promise<MXSessionAttributes>;

    /**
     * Creates a new room with userIds
     * @param userIds doesn't need to include the user's own ID
     * @param isDirect shall be used when a room with only two participants is a 1-1 conversation
     * @param isTrustedPrivateChat join_rules is set to invite. history_visibility is set to shared. All invitees are given the same power level as the room creator.
     * @param name an optional name for the room
     */
    createRoom(userIds: Array<string>, isDirect: boolean, isTrustedPrivateChat: boolean, name: string): Promise<MXRoomAttributes>;

    /**
     * Updates the name of a room
     * @param roomId
     * @param newName
     */
    updateRoomName(roomId: string, newName: string);

    joinRoom(roomId: string): Promise<MXRoomAttributes>;

    /**
     * roomId to leave
     * @param roomId
     */
    leaveRoom(roomId: string): Promise<void>;

    /**
     * Remove a certain user from a room
     * @param roomId
     * @param userId
     */
    removeUserFromRoom(roomId: string, userId: string): Promise<void>;

    /**
     * Set a user of a room to admin (kick, ban, invite)
     * @param roomId
     * @param userId
     * @param setAdmin
     */
    changeUserPermission(roomId: string, userId: string, setAdmin: boolean): Promise<void>;

    /**
     * Invited a new user to a room
     * @param roomId
     * @param userId
     */
    inviteUserToRoom(roomId: string, userId: string): Promise<void>;

    getInvitedRooms(): Promise<MXRoomAttributes[]>;
    getPublicRooms(url: string): Promise<PublicRoom[]>;
    getUnreadEventTypes(): Promise<string[]>;
    getLastEventsForAllRooms(): Promise<MXMessageEvent[]>;
    getJoinedRooms(): Promise<MXRoomAttributes[]>;
    getLeftRooms(): Promise<MXRoomAttributes[]>;
    listenToRoom(roomId: string): Promise<void>;
    unlistenToRoom(roomId: string): Promise<void>;
    listenToRoom(roomId: string): Promise<void>;
    listen(): Promise<SuccessResponse>;
    unlisten(): void;

    /**
     * This does not only return messages but all types of events of a room
     * // TODO: rename
     * @param roomId
     * @param perPage number of entries to return max
     * @param initialLoad when you are first requesting past messages, this needs to be true as it is your initial load request
     */
    loadMessagesInRoom(roomId: string, perPage: number, initialLoad: boolean): Promise<MXMessageEvent[]>;
    searchMessagesInRoom(roomId: string, searchTerm: string, nextBatch: string, beforeLimit: string, afterLimit: string);
    getMessages(roomId: string, from: string, direction: string, limit: number): Promise<MessagesFromRoom>;

    /**
     * Sends an m.room.message event to a room
     * @param roomId
     * @param messageType the message type (text, image, video, etc - see specifications)
     * @param data
     */
    sendMessageToRoom(roomId: string, messageType: string, data: any): Promise<SuccessResponse>;

    /**
     * Sends an event to a room
     * @param roomId
     * @param eventType
     * @param data
     */
    sendEventToRoom(roomId: string, eventType: string, data: any): Promise<SuccessResponse>;

    sendReadReceipt(roomId: string, eventId: string): Promise<SuccessResponse>;

    /**
     * Marks the whole room as read.
     * @param roomId
     */
    markRoomAsRead(roomId: string): Promise<void>;

    /**
     * Adds a new pusher (service) to the user at the matrix homeserver. The matrix homeserver will
     * use this pusher service to broadcast (push) notifications to the user's device (using FCM, APNS).
     * @param appDisplayName
     * @param appId
     * @param pushServiceUrl
     * @param token (FCM ID, or APNS device token)
     */
    registerPushNotifications(appDisplayName: string, appId: string, pushServiceUrl: string, token: string): Promise<void>;

    /**
     * Updates the user's display name
     * @param displayName new display name
     */
    setUserDisplayName(displayName: string): Promise<void>;

    /**
     * Sends m.typing event into the specified room that the user is typing.
     * @param roomId
     * @param isTyping whether the user is typing or not
     * @param timeout in ms
     */
    sendTyping(roomId: string, isTyping: boolean, timeout: number): Promise<void>;
  }

  const MatrixSDK: MatrixSDKStatic;

  export default MatrixSDK;
}
