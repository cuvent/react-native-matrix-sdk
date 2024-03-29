![badge](https://img.shields.io/npm/v/react-native-matrix-sdk)

<hr />

⚠️ <b>NOTICE:</b> This repository is unmaintained and may be used for experimental purposes. We don't actively use this package in production anymore.
We fall back to using the `matrix-js-sdk` SDK. Part of the reason was, that using this native module all the data had to be brought from the native side to JS via the bridge. As chat data can be a lot it caused severe performance impacts. This might not be an issue with the upcoming TurboModules/JSI architecture. However, the JS SDK is also more feature complete.

<hr />

# react-native-matrix-sdk

This is a **native** react-native library for [matrix.org](https://matrix.org). 

**Attention:** This is still under development and not ready for being used, yet. 
Any contribution is welcomed (especially if you have iOS/Swift/Obj-C skills).
The most recent versions are the `*-alpha*` versions, don't use any other!

## Getting started

`$ npm install react-native-matrix-sdk@1.0.0-alpha50 --save`

### Mostly automatic installation

`$ react-native link react-native-matrix-sdk`

### Complete android setup

These steps need to be done, regardless of whether you linked already (this is not handled by linking)!

#### Step 1:
In your `android/build.gradle` you need to add the following repository: 
```groovy
allprojects {
    repositories {
        ....
        maven {
            url "https://github.com/vector-im/jitsi_libre_maven/raw/master/releases"
        }
....
```

#### Step 2:

Add or change in your `android/app/src/main/AndroidManifest.xml` the `allowBackup` property to `true`:
```xml
...
    <application
      android:allowBackup="true"
...
```

#### Step 3:

As the matrix-android-sdk includes quite a lot classes you will very likely exceed the maximum allowed classes
limit. The error looks something like this `D8: Cannot fit requested classes in a single dex file (# methods: 74762 > 65536)`.
Therefore, you need to enable "multidex-ing" for your android project. You can enable it by adding `multiDexEnabled true` to your 
`android/app/build.gradle`:

```groovy
android {
    ....
    defaultConfig {
        ...
        multiDexEnabled true
    }
```


We are looking forward to make steps (1-2) obsolete in the future. There exists no shortcut/workaround for step 3.

### Complete iOS Setup

#### Step 1: add pods

Add the following to your pods file
```text
  pod 'react-native-matrix-sdk', :path => '../node_modules/react-native-matrix-sdk'
  pod 'AFNetworking', :modular_headers => true
  pod 'GZIP', :modular_headers => true
  pod 'OLMKit', :modular_headers => true
  pod 'Realm', :modular_headers => true
  pod 'libbase58', :modular_headers => true
  pod 'MatrixSDK/SwiftSupport', :git => 'https://github.com/hannojg/matrix-ios-sdk.git', :branch => 'develop'
```

Before you can run `pod install` you need to setup a Swift/Objective-C bridging header, as this library uses 
Swift code this is needed for RN to work.

#### Step 2: Create Swift/Obj-C bridging header:

1. Adding a new Swift file and a Brigde header:

1) `File -> New -> File` (click on your project first in project navigator)
[![`File -> New -> File`][1]][1]

2) Select `Swift File` [![Select `Swift File`][2]][2]

3) Confirm `Create Bridging Header` [![enter image description here][3]][3]

2. Go to `Build Settings` and set `Always Embed Swift Standard Libraries` to `YES` [![Always Embed Swift Standard Libraries][4]][4]


  [1]: https://i.stack.imgur.com/mI7pc.jpg
  [2]: https://i.stack.imgur.com/VbeJb.png
  [3]: https://i.stack.imgur.com/WXTz2.png
  [4]: https://i.stack.imgur.com/XtABe.png
  
#### Step 3: install Pods

Now you can install all the pods:
```shell script
cd ios/ && pod install && cd ..
```

## Usage

For up to date API capabilities check the types file: https://github.com/hannojg/react-native-matrix-sdk/blob/master/types/index.d.ts
Various use cases: (**Attention: the following section isn't updated**)

```javascript
import MatrixSdk from 'react-native-matrix-sdk';

MatrixSdk.configure('https://your-matrix-homeserver.org');

try {
  // The credentials will be also saved to the MatrixSdk instance
  // but they can be returned anyways.
  const credentials = await MatrixSdk.login('test', 'test');
  
  // Session will return MXSessionAttributes
  const session = await MatrixSdk.startSession();
                                                                                       
  // Create room, invite person & send message
  const roomCreation = await MatrixSDK.createRoom('@alice:your-matrix-homeserver.org');
  const roomId = roomCreation.room_id;
  const successMessage = await MatrixSDK.sendMessageToRoom(roomId, 'text', {
    body: 'Hello Alice 🚀',
    msgtype: 'm.text',
  });
} catch (e) {
  console.error(e);
}
```

### Listen to global new events 

You can listen to any matrix event here you can imagine. Things like typing, new room invitations
users leaving rooms etc. 
After the example you will find a list of all supported events:

```javascript
  // Add listener for events
  const matrixGlobalEventEmitter = new NativeEventEmitter(MatrixSDK);
  // This will notify us about any member changes of all rooms of a user
  // this includes things like new invitations
  matrixGlobalEventEmitter.addListener('m.room.member', event => {
    // do something with the event
  });

  // We also need to start to listen to the events
  await MatrixSDK.listen();

  // When we are done listening we should unlisten
  MatrixSDK.unlisten();
```

### Listening to new events in a room

For listening to events in a specific chat room, add a event listener to that room.
Don't forget to `unlisten` when your component dismounts!

```javascript 
  // Add listener for events
  const matrixRoomTestEmitter = new NativeEventEmitter(MatrixSDK);
  // Only listen to future events, thus using 'matrix.room.forwards'
  // If you want to listen to past events use 'matrix.room.backwards'
  matrixRoomTestEmitter.addListener('matrix.room.forwards', event => {
    if (event.event_type === 'm.room.message') {
      console.log(event.content.body);
    }
  });

  await MatrixSDK.listenToRoom(roomId);
  console.log('Subscription to room has been made, Captain!');
```

### Getting (past) messages/events of a room

```javascript
const events = await MatrixSDK.loadMessagesInRoom(roomId, 50, true);
// Load further 50 messages
const furtherEvents = await MatrixSDK.loadMessagesInRoom(roomId, 50, false);
``` 
