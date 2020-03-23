import { NativeModules } from 'react-native';
import {Credentials} from "react-native-matrix-sdk";

const { RN_MatrixSdk: MatrixSDK } = NativeModules;

export default MatrixSDK;
