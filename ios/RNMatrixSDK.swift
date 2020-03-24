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
                "is_direct": unNil(value: room?.isDirect),
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

    @objc(getRecentEvents:rejecter:)
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
                "is_direct": unNil(value: room?.isDirect),
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

    @objc(loadMessagesInRoom:perPage:initialLoad:resolver:rejecter:)
    func loadMessagesInRoom(roomId: String, perPage: NSNumber, initialLoad: Bool, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
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
            reject(E_MATRIX_ERROR, "You need to listen to the room first. You will then receive the messages in the event listener of the room!", nil)
            return
        }

        if initialLoad {
            room?.liveTimeline({ (timeline) in
                timeline?.resetPagination()
            })
        }

        room?.liveTimeline({ (timeline) in
            _ = timeline?.paginate(UInt(perPage), direction: .backwards, onlyFromStore: false) { response in
                if response.error != nil {
                    reject(nil, nil, response.error)
                    return
                }

                resolve(["success": true])
            }
        })

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

            let results = response.value?.chunk.map { convertMXEventToDictionary(event: $0 as? MXEvent) }

            resolve([
                "start": unNil(value: response.value?.start),
                "end": unNil(value: response.value?.end),
                "results": results,
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
