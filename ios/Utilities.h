//
//  Utilities.h
//  MatrixSdk
//
//  Created by Hanno  Gödecke on 06.04.20.
//  Copyright © 2020 Facebook. All rights reserved.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface Utilities : NSObject

+ (NSData *)dataFromHexString:(NSString *)string;

@end

NS_ASSUME_NONNULL_END
