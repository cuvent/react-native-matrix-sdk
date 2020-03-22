import Foundation
import SwiftMatrixSDK

@objc(RNMatrixSDK)
class RNMatrixSDK: RCTEventEmitter {
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
        MXRestClient(homeServer: self.mxHomeServer, unrecognizedCertificateHandler: nil).login(username: username, password: password) { response in
            if response.isSuccess {
                let credentials = response.value
                self.mxCredentials = credentials

                resolve([
                    "homeServer": unNil(value: credentials?.homeServer),
                    "userId": unNil(value: credentials?.userId),
                    "accessToken": unNil(value: credentials?.accessToken),
                ])
            } else {
                reject(nil, nil, response.error)
            }
        }
    }

    @objc(startSession:rejecter:)
    func startSession(resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
//        let url = unNil(value: credentials["homeServer"])
//        let userId = unNil(value: credentials["userId"])
//        let accessToken = unNil(value: credentials["accessToken"])
//
//        if url == nil || userId == nil || accessToken == nil {
//            reject(nil, "Unvalid credentials", nil)
//            return
//        }
//
//        let credentials = MXCredentials(
//            homeServer: url as! String,
//            userId: userId as! String?,
//            accessToken: accessToken as! String?
//        )

        // Create a matrix client
        let mxRestClient = MXRestClient(credentials: self.mxCredentials, unrecognizedCertificateHandler: nil)

        // Create a matrix session
        mxSession = MXSession(matrixRestClient: mxRestClient)!

        // Launch mxSession: it will first make an initial sync with the homeserver
        mxSession.start { response in
            guard response.isSuccess else {
                reject(nil, nil, response.error)
                return
            }

            let user = self.mxSession.myUser

            resolve([
                "id": unNil(value: user?.userId),
                "displayname": unNil(value: user?.displayname),
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
