[![Build Status](https://travis-ci.org/niranjan94/show-java.svg?branch=master)](https://travis-ci.org/niranjan94/show-java)

![ShowJava Banner v0.1](https://raw.githubusercontent.com/niranjan94/show-java/master/banner.png)

An apk decompiler for android. Build on Android Studio 1.4 Preview with gradle 2.4.

> The current GitHub master branch is ahead of the Play Store version and maybe unstable/incomplete at some points.

[![Get Show Java on Google Play](https://developer.android.com/images/brand/en_generic_rgb_wo_45.png)](https://play.google.com/store/apps/details?id=com.njlabs.showjava "Get Show Java on Google Play")

## About ##
This is a Java Decompiler that extracts the .java source code from an APK( Android application) file. There are many such decompilers that run on a PC. But, everything either requires you to install some dependencies (or) are hard to use. 

## Features ##

- Runs directly on an android device (4.x and above)
- Select apk from sdcard (or) from a list of installed applications
- Easy to use
- Displays code in a clean-syntax-highlighted form
- The decompiled source can easily be copied from the sdcard (source is stored in ShowJava folder in the sdcard)
- Simple source browser with a summary of all decompilation errors
- Each decompiled source file has commented references to classes that could not be decompiled

## Know Issues ##
1. Does not display the source code properly on devices running 4.0.x (Icecream Sandwich) due to an internal bug in Android.
1. Does not work with system applications in most of the phones (especially one's that are not de-odexed)
1. java.lang.OutOfMemoryError crashes when decompiling large apps on a mobile phone with low ram and Java VM Heap space.

## Open Source License ##

Unless explicitly stated otherwise all files in this repository are licensed under the [Apache Software License 2.0](http://choosealicense.com/licenses/apache-2.0/). All projects must properly attribute [The Original Source](https://github.com/niranjan94/show-java).
    
    Copyright 2015 Niranjan Rajendran
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
    http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

## External Credits ##

1. A Big-Huge Thanks to Lee Benfield ([lee@benf.org](mailto:lee@benf.org)) for his awesome CFR - Class File Reader
2. Panxiaobo ([pxb1988@gmail.com](mailto:pxb1988@gmail.com)) for dex2jar
3. Liu Dong ([github.com/xiaxiaocao](https://github.com/xiaxiaocao)) for apk-parser