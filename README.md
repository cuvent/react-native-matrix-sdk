# react-native-matrix-sdk

**Attention:** This is still under development and not ready for being used, yet. 
Any contribution is welcomed (especially if you have iOS/Swift/Obj-C skills).

## Getting started

`$ npm install react-native-matrix-sdk --save`

### Mostly automatic installation

`$ react-native link react-native-matrix-sdk`

### Additional android setup

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

## Usage
```javascript
import MatrixSdk from 'react-native-matrix-sdk';

MatrixSdk.configure('https://your-matrix-homeserver.org');

try {
  // The credentials will be also saved to the MatrixSdk instance
  // but they can be returned anyways.
  const credentials = await MatrixSdk.login('test', 'test');
  
  // Session will be true or false.
  const session = await MatrixSdk.startSession();
  console.log(`Session created: ${session}`);
} catch (e) {
  console.error(e);
}
```
