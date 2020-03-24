# Changelog

## 1.0.0-alpha9

* Added following methods for android:
  * createRoom(userId: string): Promise<MXRoomAttributes>;
  * getJoinedRooms(): Promise<[MXRoomAttributes]>;
  * listenToRoom(roomId: string): Promise<void>; -> will send events to RN
  * sendMessageToRoom(roomId: string, messageType: string, data: any): Promise<SuccessResponse>;

## 1.0.0-alpha8

* Added following methods for iOS - android will follow tomorrow. (Not all methods have been intensively tested yet!):
  *   createRoom(userId: string): Promise<MXRoomAttributes>;
  *   joinRoom(roomId: string): Promise<MXRoomAttributes>;
  *   getInvitedRooms(): Promise<MXRoomAttributes>;
  *   getPublicRooms(url: string): Promise<PublicRooms>;
  *   getUnreadEventTypes(): Promise<[string]>;
  *   getLastEventsForAllRooms(): Promise<[MXMessageEvent]>;
  *   getJoinedRooms(): Promise<[MXRoomAttributes]>;
  *   listenToRoom(roomId: string): Promise<void>; -> will send events to RN
  *   unlistenToRoom(roomId: string): Promise<void>;
  *   loadMessagesInRoom(roomId: string, perPage: number, initialLoad: boolean): Promise<void>;
  *   searchMessagesInRoom(roomId: string, searchTerm: string, nextBatch: string, beforeLimit: string, afterLimit: string);
  *   getMessages(roomId: string, from: string, direction: string, limit: string): Promise<MessagesFromRoom>;
  *   sendMessageToRoom(roomId: string, messageType: string, data: any): Promise<SuccessResponse>;
  *   sendReadReceipt(roomId: string, eventId: string): Promise<SuccessResponse>;

## 1.0.0-alpha7

* Fixed iOS implementation of login
* Android and iOS do now resolve according to the `types/index.d.ts`

## 1.0.0-alpha6

* Added iOS build setup and added implementation of methods in iOS (not tested yet).

## 1.0.0-alpha5

* Removing unnecessary files

## 1.0.0-alpha4

* Actually implemented `configure`, `login`, `startSession` functionality in library
  * This is for android only implemented, next version will implement those in iOS
* Renamed API `MatrixSdk` -> `MatrixSDK`
* Using appropriate versioning, and suffixing library version with `-alpha` o give clear indication that this isn't ready yet. 

## 1.0.0-alpha3

* Fixed issue when including project `General error during semantic analysis: Unsupported class file major version 57`
* Add static types (just for `configure` for now)
* Added documentation on how to setup library for android

## 1.0.0-alpha2

* Fixed android build issue
