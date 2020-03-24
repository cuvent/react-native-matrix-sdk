![badge](https://img.shields.io/npm/v/react-native-matrix-sdk)

# react-native-matrix-sdk

**Attention:** This is still under development and not ready for being used, yet. 
Any contribution is welcomed (especially if you have iOS/Swift/Obj-C skills).
the most recent versions are the `*-alpha*` versions, don't use any other!

## Getting started

`$ npm install react-native-matrix-sdk --save`

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
  pod 'SwiftMatrixSDK'
```

Before you can run `pod install` you need to setup a Swift/Objective-C briding header, as this library uses 
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
  });
} catch (e) {
  console.error(e);
}
```
