[![Dependency Status](https://www.versioneye.com/user/projects/5749244fce8d0e004505f807/badge.svg?style=flat)](https://www.versioneye.com/user/projects/5749244fce8d0e004505f807) [![Build Status](https://img.shields.io/travis/niranjan94/show-java.svg?style=flat-square)](https://travis-ci.org/niranjan94/show-java) [![License Info](https://img.shields.io/badge/license-Apache_License_2.0-blue.svg?style=flat-square)](https://github.com/niranjan94/show-java) [![Play Store Info](https://img.shields.io/badge/Play_Store-v2.1.0-36B0C1.svg?style=flat-square)](https://play.google.com/store/apps/details?id=com.njlabs.showjava) [![GitHub app version](https://img.shields.io/badge/GitHub-v2.1.0-yellow.svg?style=flat-square)](https://github.com/niranjan94/show-java) [![Play Store downloads](https://img.shields.io/badge/downloads-146k%20total-E04253.svg?style=flat-square)](https://play.google.com/store/apps/details?id=com.njlabs.showjava) [![Crowdin](https://d322cqt584bo4o.cloudfront.net/show-java/localized.svg)](https://crowdin.com/project/show-java)

![ShowJava Banner v0.1](https://raw.githubusercontent.com/niranjan94/show-java/master/banner.png?v1)

An apk decompiler for android. Built on Android Studio 1.5 with gradle 2.8

> The current GitHub master branch may be ahead of the Play Store version (and could be unstable/incomplete/buggy at some places)

[<img src="https://play.google.com/intl/en_us/badges/images/apps/en-play-badge-border.png" width="200" alt="Get Show Java on Google Play" />](https://play.google.com/store/apps/details?id=com.njlabs.showjava "Get Show Java on Google Play")
## About
This is a Decompiler that extracts the source code of an Android application (including XML files and image assets). Works directly from your android device.

## Features

- Select either **CFR 0.110**, **JaDX 0.6.1** or **FernFlower (analytical decompiler)** to use as the decompiler (more to come).
- Runs directly on an android device (4.x and above).
- Select apk from sdcard (or) from a list of installed applications.
- Easy to use.
- Decompiles resources too (layouts, Drawables, Menus, AndroidManifest, image assets).
- Displays code in a clean-syntax-highlighted form.
- The decompiled source can easily be copied from the sdcard (source is stored in ShowJava folder in the sdcard).
- Simple source browser with a summary of all decompilation errors.
- Each decompiled source file has commented references to classes that could not be decompiled.
- Share the decompiled source easily with the built in archive + share mechanism.

## Known Issues
1. Does not work with system applications in most of the phones (especially ones that are not de-odexed).
2. Slow on phones with single core processors.

## Contributing to ShowJava

Head over [here](https://github.com/niranjan94/show-java/blob/master/CONTRIBUTING.md) to know more about how to contribute, report bugs and request feature additions.

## Open Source License

Unless explicitly stated otherwise all files in this repository are licensed under the [Apache Software License 2.0](http://www.apache.org/licenses/LICENSE-2.0.html). All projects **must** properly attribute [The Original Source](https://github.com/niranjan94/show-java). You are **not** allowed to release unofficial/forked versions of Show Java without the permission of the [Original Author](https://www.github.com/niranjan94).
    
    Copyright 2015 Niranjan Rajendran
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at : 
    
    http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
    
An unmodified copy of the above license text must be included in all forks.

## External Credits

1. A Big-Huge Thanks to Lee Benfield ([lee@benf.org](mailto:lee@benf.org)) for his awesome CFR - Class File Reader
2. Panxiaobo ([pxb1988@gmail.com](mailto:pxb1988@gmail.com)) for dex2jar.
3. [Liu Dong](https://github.com/xiaxiaocao) for apk-parser.
4. [Ben Gruver](https://github.com/JesusFreke/) for dexlib2.
5. [skylot](https://github.com/skylot) for JaDX.
6. [JetBrains](https://github.com/JetBrains) for FernFlower analytical decompiler.

> Android, Google Play and the Google Play logo are trademarks of Google Inc.
