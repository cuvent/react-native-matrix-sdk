import { NativeModules } from 'react-native';
import {Credentials} from "react-native-matrix-sdk";

const { RN_MatrixSdk } = NativeModules;

export default class MatrixSDK {
  static configure(host) {
    RN_MatrixSdk.configure(host);
  }

  static async login(username, password) {
    return RN_MatrixSdk.login(username, password);
  }

  static startSession() {
    return RN_MatrixSdk.startSession();
  }
}
