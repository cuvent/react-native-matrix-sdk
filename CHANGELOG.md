# Changelog

## 1.0.0-alpha74

- android: fix crash when trying to send typing notifications to none existing room
## 1.0.0-alpha72

- Improved Event type to accept generic type parameters for their content and type

## 1.0.0-alpha71

- iOS: fixed crash when retrieving rooms
- iOS: fixed issue where promise of joinRoom did neither resolve/nor reject (was blocking) 

## 1.0.0-alpha70

- Fixed types

## 1.0.0-alpha69

- Fixed types

## 1.0.0-alpha68

- Removed age property from events
- Added `getRoom(roomId)` functionality

## 1.0.0-alpha67

- Fixed iOS crash issues

## 1.0.0-alpha66

- Added `setCredentials` method which can be used to just pass the credentials instead of logging in
- (Not public yet) added on android `loginWithToken`

## 1.0.0-alpha65

- Updated native SDKs

## 1.0.0-alpha64

* Fixed `uploadContent` on android to return the upload id in response, instead of string "uploadId"

## 1.0.0-alpha63

* Added `uploadContent` and `contentGetDownloadableUrl` to the SDK

## 1.0.0-alpha60

* Upgraded android matrix SDK to 0.19.35
* Upgraded iOS matrix SDK 0.16.5

## 1.0.0-alpha59

* Fixed that login / startSession won't be called when already init
* Fixed some crash causes on iOS

## 1.0.0-alpha58

* Changed android package name to a unique one

## 1.0.0-alpha57

* Changed `loadMessagesInRoom` to `backPaginate` to be in accordance with how the native SDKs work, thus supportign offline fetching of messages
* Added `canBackPaginate`

## 1.0.0-alpha55

* Added `updateRoomName`

## 1.0.0-alpha55

* Added name parameter to `createRoom` method

## 1.0.0-alpha54

* Fixed crash on iOS when requesting left rooms and user have none

## 1.0.0-alpha53

* Fixed issue on iOS where `markRoomAsRead` promise would not resolve

## 1.0.0-alpha50

* Fixed issue in iOS where `isDirect` field could be wrong

## 1.0.0-alpha49

* Fixed typescript array usage

## 1.0.0-alpha48

* Added `members` attribute to room

## 1.0.0-alpha47

* Fixed android left rooms having no room summary

## 1.0.0-alpha46

* Added `getLeftRooms`

## 1.0.0-alpha44

* Changed signature for room creation to: `createRoom(userIds: Array<string>, isDirect: boolean, isTrustedPrivateChat: boolean): Promise<MXRoomAttributes>;`
  <br />Trusted chats are: 
  ```text
  isTrustedPrivateChat: join_rules is set to invite. history_visibility is set to shared. All invitees are given the same power level as the room creator.```

## 1.0.0-alpha43

* Fixed issue where changeUserPermission to admin was only promoting user to moderator.

## 1.0.0-alpha42

* Fixed issue on iOS where room was not set to `isDirect` although `true` was passed as argument. 

## 1.0.0-alpha41

* Fixed issue in android where creating a room with `isDirect` didn't work correctly
* Fixed that message content can be `any`.

## 1.0.0-alpha40

* Fixed issue in iOS with `createRoom`

## 1.0.0-alpha39

* Added leaveRoom
* Added removeUserFromRoom
* Added changeUserPermission
* Added addUserToRoom
* Changed createRoom signature to support multiple participants: `createRoom(userIds: Array<string>, isDirect: boolean): Promise<MXRoomAttributes>;`

## 1.0.0-alpha37

* (Android) Fixed matrix olm dependency by manually including it, thus fixing build issues, related to this issue:
https://github.com/jitpack/jitpack.io/issues/3908#issuecomment-526767673

## 1.0.0-alpha35

* Added `sendEventToRoom(roomId: string, eventType: string, data: any)` API 

## 1.0.0-alpha34

* Removed event types, and set string for field event.event_type to support custom events

## 1.0.0-alpha33

* Added `setAdditionalEventTypes(types: string[]): void` for adding a list of custom events that a client needs to support (iOS only, android doesn't need this)

## 1.0.0-alpha31

* Added typing: `sendTyping(roomId: string, isTyping: boolean, timeout: number): Promise<void>`

## 1.0.0-alpha30

* Added `ts` (server timestamp) to events (don't use `age`, use `ts`)

## 1.0.0-alpha29

* Added `setUserDisplayName(displayName: string)` to SDK

## 1.0.0-alpha28

* Fixed issue on iOS related to device token encoding with base64

## 1.0.0-alpha26

* Added event types

## 1.0.0-alpha25

* Fixed issue in android where age was 0

## 1.0.0-alpha24

* Fixed issue in iOS where `getLastEventsForAllRooms` caused a crash
* Added `sendReadReceipt(roomId: string, eventId: string)` in android
* Added `markRoomAsRead(roomId)` 

## 1.0.0-alpha22

* base64 encoding device token when doing iOS push notification registration 
* add `appId` parameter to `registerPushNotification`

## 1.0.0-alpha21

* Changing name of application when doing `registerPushNotifications` to 
yourpackagename.(android|ios)

## 1.0.0-alpha20

* Fixed implementation issues with `registerPushNotifications` API

## 1.0.0-alpha19

* Added `registerPushNotifications` API (android and iOS)
```typescript
    /**
     * Adds a new pusher (service) to the user at the matrix homeserver. The matrix homeserver will
     * use this pusher service to broadcast (push) notifications to the user's device (using FCM, APNS).
     * @param appDisplayName
     * @param pushServiceUrl
     * @param token (FCM ID, or APNS device token)
     */
    registerPushNotifications(appDisplayName: string, pushServiceUrl: string, token: string): Promise<void>;
```

## 1.0.0-alpha18

* Corrected `MXRoomAttributes` name from `notificationcount` -> `notification_count`, thus fixing that `undefined is returned`

## 1.0.0-alpha17

* Android and iOS use now a file store instead of a memory store, which is a performance optimization
 
## 1.0.0-alpha16

* Changed how `loadMessagesInRoom` works. Instead of needing to register a listener, it now returns the events of the room 
as a promise array `Promise<MXMessageEvents[]>` (docs updated accordingly)

## 1.0.0-alpha14

* Fixed issue where android SDK was crashing when started listening without having started a session.

## 1.0.0-alpha13

* Fixed issue where credentials returned by `login` were a string instead of an object.

## 1.0.0-alpha12

* Added getLastEventsForAllRooms API for android

## 1.0.0-alpha11

* Added listen (and unlisten) API to listen to the overall global events of a user 

## 1.0.0-alpha10

* Added unlistenToRoom and loadMessagesInRoom API for android 
* Fixed state issues on android side. 

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
