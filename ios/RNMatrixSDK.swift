import Foundation
import SwiftMatrixSDK

@objc(RNMatrixSDK)
class RNMatrixSDK: RCTEventEmitter {
    var E_MATRIX_ERROR: String! = "E_MATRIX_ERROR";
    var E_NETWORK_ERROR: String! = "E_NETWORK_ERROR";
    var E_UNEXCPECTED_ERROR: String! = "E_UNKNOWN_ERROR";

    var mxSession: MXSession!
    var mxCredentials: MXCredentials!
    var mxHomeServer: URL!


    @objc
    override func supportedEvents() -> [String]! {
        return ["matrix.room.backwards", "matrix.room.forwards"]
    }


    @objc(configure:)
    func configure(url: String) {
        self.mxHomeServer = URL(string: url)
    }

    @objc(login:password:resolver:rejecter:)
    func login(username: String, password: String, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
        // New user login
        let dummyCredentials = MXCredentials(homeServer: self.mxHomeServer.absoluteString, userId: nil, accessToken: nil)

        let restClient = MXRestClient.init(credentials: dummyCredentials, unrecognizedCertificateHandler: nil)
        let session = MXSession(matrixRestClient: restClient)

        session?.matrixRestClient.login(username: username, password: password, completion: { (credentials) in
            if credentials.isSuccess {
                self.mxCredentials = credentials.value
                resolve([
                    "home_server": unNil(value: self.mxCredentials?.homeServer),
                    "user_id": unNil(value: self.mxCredentials?.userId),
                    "access_token": unNil(value: self.mxCredentials?.accessToken),
                    "device_id": unNil(value: self.mxCredentials?.deviceId),
                ])
            } else {
                reject(self.E_MATRIX_ERROR, nil, credentials.error)
            }
        })
    }

    @objc(startSession:rejecter:)
    func startSession(resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
        // Create a matrix client
        let mxRestClient = MXRestClient(credentials: self.mxCredentials, unrecognizedCertificateHandler: nil)

        // Create a matrix session
        mxSession = MXSession(matrixRestClient: mxRestClient)!

        // Launch mxSession: it will first make an initial sync with the homeserver
        mxSession.start { response in
            guard response.isSuccess else {
                reject(self.E_MATRIX_ERROR, nil, response.error)
                return
            }

            let user = self.mxSession.myUser

            resolve([
                "user_id": unNil(value: user?.userId),
                "display_name": unNil(value: user?.displayname),
                "avatar": unNil(value: user?.avatarUrl),
                "last_active": unNil(value: user?.lastActiveAgo),
                "status": unNil(value: user?.statusMsg),
            ])
        }
    }
}

internal func unNil(value: Any?) -> Any? {
    guard let value = value else {
        return nil
    }
    return value
}
