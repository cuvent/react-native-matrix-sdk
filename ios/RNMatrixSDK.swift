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

    var roomEventsListeners: [String: Any] = [:]
    var roomPaginationTokens: [String : String] = [:]
    var globalListener: Any?
    var additionalTypes: [String] = []


    @objc
    override func supportedEvents() -> [String]! {
        var supportedTypes = ["matrix.room.backwards",
                "matrix.room.forwards",
                "m.fully_read",
                MXEventType.roomName.identifier,
                MXEventType.roomTopic.identifier,
                MXEventType.roomAvatar.identifier,
                MXEventType.roomMember.identifier,
                MXEventType.roomCreate.identifier,
                MXEventType.roomJoinRules.identifier,
                MXEventType.roomPowerLevels.identifier,
                MXEventType.roomAliases.identifier,
                MXEventType.roomCanonicalAlias.identifier,
                MXEventType.roomEncrypted.identifier,
                MXEventType.roomEncryption.identifier,
                MXEventType.roomGuestAccess.identifier,
                MXEventType.roomHistoryVisibility.identifier,
                MXEventType.roomKey.identifier,
                MXEventType.roomForwardedKey.identifier,
                MXEventType.roomKeyRequest.identifier,
                MXEventType.roomMessage.identifier,
                MXEventType.roomMessageFeedback.identifier,
                MXEventType.roomRedaction.identifier,
                MXEventType.roomThirdPartyInvite.identifier,
                MXEventType.roomTag.identifier,
                MXEventType.presence.identifier,
                MXEventType.typing.identifier,
                MXEventType.callInvite.identifier,
                MXEventType.callCandidates.identifier,
                MXEventType.callAnswer.identifier,
                MXEventType.callHangup.identifier,
                MXEventType.reaction.identifier,
                MXEventType.receipt.identifier,
                MXEventType.roomTombStone.identifier,
                MXEventType.keyVerificationStart.identifier,
                MXEventType.keyVerificationAccept.identifier,
                MXEventType.keyVerificationKey.identifier,
                MXEventType.keyVerificationMac.identifier,
            MXEventType.keyVerificationCancel.identifier]
        // add any additional types the user provided
        supportedTypes += additionalTypes
        return supportedTypes;
    }

    @objc(setAdditionalEventTypes:)
    func setAdditionalEventTypes(types: [String]) {
        additionalTypes = types
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

        // Make the matrix session open the file store
        // This will preload user's messages and other data
        let store = MXFileStore()

        mxSession.setStore(store) { (response) in
            guard response.isSuccess else {
                reject(self.E_MATRIX_ERROR, nil, response.error)
                return
            }

            // Launch mxSession: it will sync with the homeserver from the last stored data
            // Then it will listen to new coming events and update its data
            self.mxSession.start { response in
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

    @objc(createRoom:resolver:rejecter:)
    func createRoom(userId: String, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
        if mxSession == nil {
            reject(nil, "client is not connected yet", nil)
            return
        }

        mxSession.createRoom(name: nil, visibility: nil, alias: nil, topic: nil, invite: [userId], invite3PID: nil, isDirect: true, preset: nil) { response in
            if response.isSuccess {
                let roomId = response.value?.roomId
                let roomName = response.value?.summary.displayname
                let notificationCount = response.value?.summary.notificationCount
                let highlightCount = response.value?.summary.highlightCount
                let isDirect = response.value?.isDirect
                let lastMessage = response.value?.summary.lastMessageEvent

                resolve([
                    "room_id": unNil(value: roomId),
                    "name": unNil(value: roomName),
                    "notification_count": unNil(value: notificationCount),
                    "highlight_count": unNil(value: highlightCount),
                    "is_direct": unNil(value: isDirect),
                    "last_message": convertMXEventToDictionary(event: lastMessage),
                ])
            } else {
                reject(nil, nil, response.error)
            }
        }
    }

    @objc(joinRoom:resolver:rejecter:)
    func joinRoom(roomId: String, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
        if mxSession == nil {
            reject(nil, "client is not connected yet", nil)
            return
        }

        mxSession.joinRoom(roomId) { response in
            if response.isSuccess {
                let roomId = response.value?.roomId
                let roomName = response.value?.summary.displayname
                let notificationCount = response.value?.summary.localUnreadEventCount
                let highlightCount = response.value?.summary.highlightCount
                let isDirect = response.value?.isDirect
                let lastMessage = response.value?.summary.lastMessageEvent

                resolve([
                    "room_id": unNil(value: roomId),
                    "name": unNil(value: roomName),
                    "notification_count": unNil(value: notificationCount),
                    "highlight_count": unNil(value: highlightCount),
                    "is_direct": unNil(value: isDirect),
                    "last_message": convertMXEventToDictionary(event: lastMessage),
                ])
            } else {
                reject(nil, nil, response.error)
            }
        }
    }

    @objc(getInvitedRooms:rejecter:)
    func getInvitedRooms(resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
        if mxSession == nil {
            reject(nil, "client is not connected yet", nil)
            return
        }

        let rooms = mxSession.invitedRooms().map({
            (r: MXRoom) -> [String: Any?] in
            let room = mxSession.room(withRoomId: r.roomId)
            let lastMessage = room?.summary.lastMessageEvent

            return [
                "room_id": unNil(value: room?.roomId),
                "name": unNil(value: room?.summary.displayname),
                "notification_count": unNil(value: room?.summary.notificationCount),
                "highlight_count": unNil(value: room?.summary.highlightCount),
                "is_direct": unNil(value: room?.summary.isDirect),
                "last_message": convertMXEventToDictionary(event: lastMessage),
            ]
        })

        resolve(rooms)
    }

    @objc(getPublicRooms:resolver:rejecter:)
    func getPublicRooms(url: String, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
        let homeServerUrl = URL(string: url)!
        let mxRestClient = MXRestClient(homeServer: homeServerUrl, unrecognizedCertificateHandler: nil)
        // TODO: make limit definable through user
        mxRestClient.publicRooms(onServer: self.mxHomeServer.absoluteString, limit: nil) { (response) in
            switch response {
            case let .success(rooms):
                let data = rooms.chunk.map { [
                    "id": $0.roomId!,
                    "aliases": unNil(value: $0.aliases) ?? [],
                    "name": unNil(value: $0.name) ?? "",
                    "guestCanJoin": $0.guestCanJoin,
                    "numJoinedMembers": $0.numJoinedMembers,
                    ] }

                resolve(data)
                break
            case let .failure(error):
                reject(nil, nil, error)
                break
            }
        }
    }

    @objc(getUnreadEventTypes:rejecter:)
    func getUnreadEventTypes(resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
        if mxSession == nil {
            reject(nil, "client is not connected yet", nil)
            return
        }

        resolve(mxSession.unreadEventTypes)
    }

    @objc(getLastEventsForAllRooms:rejecter:)
    func getLastEventsForAllRooms(resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
        if mxSession == nil {
            reject(nil, "client is not connected yet", nil)
            return
        }

        let recentEvents = mxSession.roomsSummaries()

        let response = recentEvents.map({
            (roomLastEvent: [MXRoomSummary]) -> [[String: Any]] in
            roomLastEvent.map { (summary) -> [String: Any] in
                return convertMXEventToDictionary(event: summary.lastMessageEvent)
            }
        })

        resolve(response)
    }

    @objc(getJoinedRooms:rejecter:)
    func getJoinedRooms(resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
        if mxSession == nil {
            reject(nil, "client is not connected yet", nil)
            return
        }

        let rooms = mxSession.rooms.map({
            (r: MXRoom) -> [String: Any?] in
            let room = mxSession.room(withRoomId: r.roomId)
            let lastMessage = room?.summary.lastMessageEvent

            return [
                "room_id": unNil(value: room?.roomId),
                "name": unNil(value: room?.summary.displayname),
                "notification_count": unNil(value: room?.summary.notificationCount),
                "highlight_count": unNil(value: room?.summary.highlightCount),
                "is_direct": unNil(value: room?.summary.isDirect),
                "last_message": convertMXEventToDictionary(event: lastMessage),
            ]
        })

        resolve(rooms)
    }

    @objc(listenToRoom:resolver:rejecter:)
    func listenToRoom(roomId: String, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
        if mxSession == nil {
            reject(nil, "client is not connected yet", nil)
            return
        }

        let room = mxSession.room(withRoomId: roomId)

        if room == nil {
            reject(nil, "Room not found", nil)
            return
        }

        if roomEventsListeners[roomId] != nil {
            reject(nil, "Only allow 1 listener to 1 room for now. Room id: " + roomId, nil)
            return
        }

        room?.liveTimeline({ (timeline) in
            let listener = timeline?.listenToEvents {
                event, direction, _ in
                switch direction {
                case .backwards:
                    if self.bridge != nil {
                        self.sendEvent(
                            withName: "matrix.room.backwards",
                            body: convertMXEventToDictionary(event: event)
                        )
                    }
                    break
                case .forwards:
                    if self.bridge != nil {
                        self.sendEvent(
                            withName: "matrix.room.forwards",
                            body: convertMXEventToDictionary(event: event)
                        )
                    }
                    break
                }
            }

            self.roomEventsListeners[roomId] = listener

            resolve(nil)
        })



    }

    @objc(unlistenToRoom:resolver:rejecter:)
    func unlistenToRoom(roomId: String, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
        if mxSession == nil {
            reject(nil, "client is not connected yet", nil)
            return
        }

        let room = mxSession.room(withRoomId: roomId)

        if room == nil {
            reject(nil, "Room not found", nil)
            return
        }

        if roomEventsListeners[roomId] == nil {
            reject(nil, "No listener for this room. Room id: " + roomId, nil)
            return
        }

        room?.liveTimeline({ (timeline) in
            timeline?.removeListener(self.roomEventsListeners[roomId])
            self.roomEventsListeners[roomId] = nil

            resolve(nil)
        })
    }

    @objc(listen:rejecter:)
    func listen(resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
        if mxSession == nil {
            reject(E_MATRIX_ERROR, "client is not connected yet", nil)
            return
        }

        if self.globalListener != nil {
            reject(E_MATRIX_ERROR, "You already started listening, only one global listener is supported. You maybe forget to call `unlisten()`", nil)
            return
        }

        // additionalObject: additional contect for the event. In case of room event, `customObject` is a `RoomState` instance. In the case of a presence, `customObject` is `nil`.
        self.globalListener = mxSession.listenToEvents { (event: MXEvent, timelineDirection: MXTimelineDirection, additionalObject) in
            // Only listen to future events
            if timelineDirection == .forwards && self.bridge != nil {
                self.sendEvent(
                    withName: event.type,
                    body: convertMXEventToDictionary(event: event)
                )
            }
        }


        resolve(["success": true])
    }

    @objc
    func unlisten() {
        if mxSession != nil && globalListener != nil {
            mxSession.removeListener(globalListener)
            globalListener = nil
        }
    }

    @objc(loadMessagesInRoom:perPage:initialLoad:resolver:rejecter:)
    func loadMessagesInRoom(roomId: String, perPage: NSNumber, initialLoad: Bool, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
        var fromToken = ""
        if(!initialLoad) {
            fromToken = roomPaginationTokens[roomId] ?? ""
            if(fromToken.isEmpty) {
                print("Warning: trying to load not initial messages, but the SDK has no token set for this room currently. You need to run with initialLoad: true first!")
            }
        }

        getMessages(roomId: roomId, from: fromToken, direction: "backwards", limit: perPage, resolve: resolve, reject: reject)
    }

    @objc(getMessages:from:direction:limit:resolver:rejecter:)
    func getMessages(roomId: String, from: String, direction: String, limit: NSNumber, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
        if mxSession == nil {
            reject(nil, "client is not connected yet", nil)
            return
        }

        let roomEventFilter = MXRoomEventFilter()
        let timelimeDirection = direction == "backwards" ? MXTimelineDirection.backwards : MXTimelineDirection.forwards

        mxSession.matrixRestClient.messages(forRoom: roomId, from: from, direction: timelimeDirection, limit: UInt(truncating: limit), filter: roomEventFilter) { response in
            if response.error != nil {
                reject(nil, nil, response.error)
                return
            }

            let results = response.value?.chunk.map {
                $0.map( {
                    convertMXEventToDictionary(event: $0 as MXEvent)
                } )
            }

            // Store pagination token
            self.roomPaginationTokens[roomId] = response.value?.end

            resolve(results)
        }
    }

    @objc(searchMessagesInRoom:searchTerm:nextBatch:beforeLimit:afterLimit:resolver:rejecter:)
    func searchMessagesInRoom(roomId: String, searchTerm: String, nextBatch: String, beforeLimit: NSNumber, afterLimit: NSNumber, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
        if mxSession == nil {
            reject(nil, "client is not connected yet", nil)
            return
        }

        let roomEventFilter = MXRoomEventFilter()
        roomEventFilter.rooms = [roomId]

        mxSession.matrixRestClient.searchMessages(withPattern: searchTerm, roomEventFilter: roomEventFilter, beforeLimit: UInt(beforeLimit), afterLimit: UInt(afterLimit), nextBatch: nextBatch) { results in
            if results.isFailure {
                reject(nil, nil, results.error)
                return
            }

            if results.value == nil || results.value?.results == nil {
                resolve([
                    "count": 0,
                    "next_batch": nil,
                    "results": [],
                ])
                return
            }

            let events = results.value?.results.map({ (result: MXSearchResult) -> [String: Any] in
                let context = result.context
                let eventsBefore = context?.eventsBefore ?? []
                let eventsAfter = context?.eventsAfter ?? []

                return [
                    "event": convertMXEventToDictionary(event: result.result),
                    "context": [
                        "before": eventsBefore.map(convertMXEventToDictionary) as Any,
                        "after": eventsAfter.map(convertMXEventToDictionary) as Any,
                    ],
                    "token": [
                        "start": unNil(value: context?.start),
                        "end": unNil(value: context?.end),
                    ],
                ]
            })

            resolve([
                "next_batch": unNil(value: results.value?.nextBatch),
                "count": unNil(value: results.value?.count),
                "results": events,
            ])
        }
    }

    @objc(sendMessageToRoom:messageType:data:resolver:rejecter:)
    func sendMessageToRoom(roomId: String, messageType: String, data: [String: Any], resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
        if mxSession == nil {
            reject(nil, "client is not connected yet", nil)
            return
        }

        let room = mxSession.room(withRoomId: roomId)

        if room == nil {
            reject(nil, "Room not found", nil)
            return
        }

        mxSession.matrixRestClient.sendMessage(toRoom: roomId, messageType: convertStringToMXMessageType(type: messageType), content: data) { (response) in
            if(response.isFailure) {
                reject(self.E_MATRIX_ERROR, nil, response.error)
                return
            }

            resolve(["success": response.value])
        }
    }

    @objc(sendEventToRoom:eventType:data:resolver:rejecter:)
    func sendEventToRoom(roomId: String, eventType: String, data: [String: Any], resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
        if mxSession == nil {
            reject(nil, "client is not connected yet", nil)
            return
        }

        let room = mxSession.room(withRoomId: roomId)

        if room == nil {
            reject(nil, "Room not found", nil)
            return
        }

        mxSession.matrixRestClient.sendEvent(toRoom: roomId, eventType: MXEventType.custom(eventType), content: data, txnId: UUID().uuidString) { (response) in
            if(response.isFailure) {
                reject(self.E_MATRIX_ERROR, nil, response.error)
                return
            }

            resolve(["success": response.value])
        }
    }

    @objc(markRoomAsRead:resolver:rejecter:)
    func markRoomAsRead(roomId: String, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
        if mxSession == nil {
            reject(nil, "client is not connected yet", nil)
            return
        }

        let room = mxSession.room(withRoomId: roomId)

        if room == nil {
            reject(nil, "Room not found", nil)
            return
        }

        room?.markAllAsRead()
    }

    @objc(sendReadReceipt:eventId:resolver:rejecter:)
    func sendReadReceipt(roomId: String, eventId: String, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
        if mxSession == nil {
            reject(nil, "client is not connected yet", nil)
            return
        }

        mxSession.matrixRestClient.sendReadReceipt(toRoom: roomId, forEvent: eventId) { response in
            if response.error != nil {
                reject(nil, nil, response.error)
                return
            }

            resolve(["success": response.value])
        }
    }

    @objc(registerPushNotifications:appId:pushServiceUrl:token:resolver:rejecter:)
    func registerPushNotifications(displayName: String, appId: String, pushServiceUrl: String, token: String, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
        if mxSession == nil {
            reject(E_MATRIX_ERROR, "client is not connected yet", nil)
            return
        }

        let tag = calculateTag(session: mxSession)
        let skr: Data = Utilities.data(fromHexString: token)
        let b64Token = (skr.base64EncodedString())

        mxSession.matrixRestClient.setPusher(pushKey: b64Token, kind: MXPusherKind.http, appId: appId, appDisplayName: displayName, deviceDisplayName: UIDevice.current.name, profileTag: tag, lang: Locale.current.languageCode ?? "en", data: ["url": pushServiceUrl], append: false) { response in
            if response.error != nil {
                reject(nil, nil, response.error)
                return
            }

            resolve(["success": response.value])
        }
    }

    @objc(setUserDisplayName:resolver:rejecter:)
    func setUserDisplayName(displayName: String, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
        if mxSession == nil {
            reject(E_MATRIX_ERROR, "client is not connected yet", nil)
            return
        }

        mxSession.myUser.setDisplayName(displayName, success: {
            resolve(true)
        }) { (error) in
            reject(self.E_MATRIX_ERROR, "Failed to update display name", error)
        }
    }

    @objc(sendTyping:isTyping:timeout:resolver:rejecter:)
    func sendTyping(roomId: String, isTyping: Bool, timeout: NSNumber, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
        if mxSession == nil {
            reject(E_MATRIX_ERROR, "client is not connected yet", nil)
            return
        }
        let timeoutConv = isTyping ? TimeInterval(timeout.doubleValue) : 1

        mxSession.room(withRoomId: roomId)?.sendTypingNotification(typing: isTyping, timeout: timeoutConv, completion: { (response: MXResponse<Void>) in
            if response.error != nil {
                reject(nil, nil, response.error)
                return
            }

            resolve(["success": response.value])
        })
    }

    @objc(updatePresence:resolver:rejecter:)
    func updatePresence(isOnline: Bool, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
        if mxSession == nil {
            reject(E_MATRIX_ERROR, "client is not connected yet", nil)
            return
        }

        let presence: MXPresence = isOnline ? MXPresenceOnline : MXPresenceOffline
        mxSession.myUser.setPresence(presence, andStatusMessage: "", success: {
            resolve(["success": "true"])
        }) { (error) in
            reject(self.E_MATRIX_ERROR, "Failed to update presence", error)
        }
    }
}

internal func calculateTag(session: MXSession) -> String {
    var tag = "mobile_" + String(abs(session.myUser.userId.hashValue))

    if(tag.count > 32) {
        tag = String(abs(tag.hashValue))
    }

    return tag
}

internal func unNil(value: Any?) -> Any? {
    guard let value = value else {
        return nil
    }
    return value
}


internal func convertMXEventToDictionary(event: MXEvent?) -> [String: Any] {
    return [
        "event_type": unNil(value: event?.type) as Any,
        "event_id": unNil(value: event?.eventId) as Any,
        "room_id": unNil(value: event?.roomId) as Any,
        "sender_id": unNil(value: event?.sender) as Any,
        "age": unNil(value: event?.age) as Any,
        "content": unNil(value: event?.content) as Any,
        "ts": unNil(value: event?.originServerTs) as Any,
    ]
}

internal func convertStringToMXMessageType(type: String) -> MXMessageType {
    switch type {
    case "image":
        return MXMessageType.image
    case "video":
        return MXMessageType.video
    case "file":
        return MXMessageType.file
    case "audio":
        return MXMessageType.audio
    case "emote":
        return MXMessageType.emote
    case "location":
        return MXMessageType.location
    default:
        return MXMessageType.text
    }
}
