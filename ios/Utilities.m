//
//  Utilities.m
//  MatrixSdk
//
//  Created by Hanno  Gödecke on 06.04.20.
//  Copyright © 2020 Facebook. All rights reserved.
//

#import "Utilities.h"

@implementation Utilities

+ (NSData *)dataFromHexString:(NSString *)string
{
    NSMutableData *stringData = [[NSMutableData alloc] init];
    unsigned char whole_byte;
    char byte_chars[3] = {'\0','\0','\0'};
    int i;
    for (i=0; i < [string length] / 2; i++) {
        byte_chars[0] = [string characterAtIndex:i*2];
        byte_chars[1] = [string characterAtIndex:i*2+1];
        whole_byte = strtol(byte_chars, NULL, 16);
        [stringData appendBytes:&whole_byte length:1];
    }
    return stringData;
}

@end
