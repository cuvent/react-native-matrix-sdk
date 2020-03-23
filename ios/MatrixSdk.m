#if __has_include("RCTBridgeModule.h")
#import "RCTBridgeModule.h"
#else
#import <React/RCTBridgeModule.h>
#endif


// Will expose a RN_MatrixSdk native module to JS
@interface RCT_EXTERN_REMAP_MODULE(RN_MatrixSdk, RNMatrixSDK, NSObject)

+ (BOOL)requiresMainQueueSetup
{
    return false;
}

RCT_EXTERN_METHOD(supportedEvents)

RCT_EXTERN_METHOD(constantsToExport)

RCT_EXTERN_METHOD(configure:(NSString *)url)

RCT_EXTERN_METHOD(login:(nonnull NSString *)username password:(nonnull NSString *)password resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(startSession:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(createRoom:(NSString *)userId resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(joinRoom:(NSString *)roomId resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(getPublicRooms:(NSString *)url resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(getUnreadEventTypes:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(getLastEventsForAllRooms:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(getInvitedRooms:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(getJoinedRooms:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(listenToRoom:(NSString *)roomId resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(unlistenToRoom:(NSString *)roomId resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(loadMessagesInRoom:(NSString *)roomId perPage:(nonnull NSNumber *)perPage initialLoad:(BOOL *)initialLoad resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(searchMessagesInRoom:(NSString *)roomId searchTerm:(nonnull NSString *)searchTerm nextBatch:(nonnull NSString *)nextBatch beforeLimit:(nonnull NSNumber *)beforeLimit afterLimit:(nonnull NSNumber *)afterLimit resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(getMessages:(NSString *)roomId from:(nonnull NSString *)from direction:(nonnull NSString *)direction limit:(nonnull NSNumber *)limit resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(sendMessageToRoom:(NSString *)roomId messageType:(NSString *)messageType data:(NSDictionary *)data resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(sendReadReceipt:(NSString *)roomId eventId:(NSString *)eventId resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)

@end
