# Reading collection for iOS RN development

As I have just a little iOS/Swift experience, integrating the iOS part is the most challenging part
of this library. 
I want to have a list of articles/tutorials that helped me to understand certain concepts that looked weird
on a first glance.

### Using Swift instead of Obj-C
One important inside is, that the RN native bridge is only interoperable with the Obj-C runtime. 
Thus, we need to make our Swift code available in the Obj-C runtime. A good article on that:

https://teabreak.e-spres-oh.com/swift-in-react-native-the-ultimate-guide-part-1-modules-9bb8d054db03

### How to tame RCT_EXTERN_METHOD

`RCT_EXTERN_METHOD` macro is used to expose our methods to JS, and this method feels really weird. 
Here is a good article on understanding it properly:

[https://medium.com/@andrei.pfeiffer/react-natives-rct-extern-method-c61c17bf17b2](https://medium.com/@andrei.pfeiffer/react-natives-rct-extern-method-c61c17bf17b2)

Some important inside from this article:
```objective-c
RCT_EXTERN_METHOD(
  methodName:         (paramType1)internalParamName1 
  externalParamName2: (paramType2)internalParamName2
  externalParamName3: (paramType3)internalParamName3
  ...
)
```
