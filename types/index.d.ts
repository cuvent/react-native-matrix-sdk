declare module 'react-native-matrix-sdk' {

  export interface MXCredentials {
    user_id: string;
    home_server: string;
    access_token: string;
    refresh_token: string | undefined;
    device_id: string;
  }

  export interface MXSessionAttributes {
    user_id: string;
    display_name: string;
    avatar: string;
    last_active: number;
    status: string;
  }

  export interface MatrixSDKStatic {
    configure(host: string): void;
    // TODO: actually credentials are returned as string, and not as Credentials Type
    login(username: string, password: string): Promise<MXCredentials>;
    startSession(): Promise<MXSessionAttributes>;
  }

  const MatrixSDK: MatrixSDKStatic;

  export default MatrixSDK;
}
