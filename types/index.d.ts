declare module 'react-native-matrix-sdk' {
  export interface MatrixSdkStatic {
    configure(host: string): void;
  }

  const MatrixSdk: MatrixSdkStatic;

  export default MatrixSdk;
}
