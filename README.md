[![Known Vulnerabilities](https://snyk.io/test/github/niranjan94/show-java/badge.svg?style=flat-square)](https://snyk.io/test/github/niranjan94/show-java)
  [![Build Status](https://img.shields.io/travis/com/niranjan94/show-java/development.svg?style=flat-square)](https://travis-ci.com/niranjan94/show-java) [![License Info](https://img.shields.io/badge/license-GNU_GPLv3-blue.svg?style=flat-square)](https://github.com/niranjan94/show-java) [![Play Store Info](https://img.shields.io/badge/Play_Store-v3.0.5-36B0C1.svg?style=flat-square)](https://play.google.com/store/apps/details?id=com.njlabs.showjava) [![GitHub app version](https://img.shields.io/badge/GitHub-v3.0.6-yellow.svg?style=flat-square)](https://github.com/niranjan94/show-java) [![Play Store downloads](https://img.shields.io/badge/downloads-435k%20total-E04253.svg?style=flat-square)](https://play.google.com/store/apps/details?id=com.njlabs.showjava)


![ShowJava Banner v0.1](https://res.cloudinary.com/niranjan94/image/upload/v1518341743/banner_lihb7z.png)

An APK (android application), JAR & Dex decompiler for android.

> The current GitHub master branch may be ahead of the Play Store version (and could be unstable/incomplete/buggy at some places)

[<img src="https://play.google.com/intl/en_us/badges/images/apps/en-play-badge-border.png" width="200" alt="Get Show Java on Google Play" />](https://play.google.com/store/apps/details?id=com.njlabs.showjava "Get Show Java on Google Play")
## About
This is a Decompiler that extracts the source code of an Android application (including XML files and image assets), JAR Packages and dex files. Works directly from your android device.

## Features

- Select either **CFR 0.140**, **JaDX 0.9.0** or **FernFlower [`a0a8f0a`](https://github.com/fesh0r/fernflower/tree/a0a8f0a8dd777a7010958474acbca857071ee941) (analytical decompiler)** to use as the decompiler.
- Runs directly on an android device (4.1.x and above).
- Select apk/jar/dex from sdcard (or) from a list of installed applications.
- Decompiles android resources (layouts, Drawables, Menus, AndroidManifest, image assets, values, etc).
- Easy to use source navigator with built-in media & code viewer.
- Displays code in a clean-syntax-highlighted form with zoom & line-wrap.
- The decompiled source can easily be copied from the sdcard (source is stored in `show-java` folder in the sdcard).
- Share the decompiled source easily with the built in archive + share mechanism.

## Compatibility Matrix

### Version 3.0

JaDX and Fernflower use newer Java features such as some types of lambda's which are not available on pre-N versions of Android. 

|             | CFR 0.140          | JaDX 0.9.0         | Fernflower         |
|------------:|:------------------:|:------------------:|:------------------:|
| Oreo        | :white_check_mark: | :white_check_mark: | :white_check_mark: |
| Nougat      | :white_check_mark: | :white_check_mark: | :white_check_mark: |
| Marshmallow | :white_check_mark: |        -           |         -          |
| Lollipop    | :white_check_mark: |        -           |         -          |
| KitKat      | :white_check_mark: |        -           |         -          |
| Jelly Bean  | :white_check_mark: |        -           |         -          |

> Available from the [Play Store](https://play.google.com/store/apps/details?id=com.njlabs.showjava) on all Android devices running Jelly Bean and above.

### Version 2.0

|             | CFR 0.110          | JaDX 0.6.1         | Fernflower         |
|------------:|:------------------:|:------------------:|:------------------:|
| ICS & Above | :white_check_mark: | :white_check_mark: | :white_check_mark: |

> Can be downloaded from [here](https://github.com/niranjan94/show-java/releases/tag/v2.1.0).

## Contributing to ShowJava

Head over [here](https://github.com/niranjan94/show-java/blob/master/CONTRIBUTING.md) to know more about how to contribute, report bugs and request feature additions.

## Open Source License

Unless explicitly stated otherwise all files in this repository are licensed under the [GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0-standalone.html). All projects **must** properly attribute [The Original Source](https://github.com/niranjan94/show-java). 
    
    Show Java - A java/apk decompiler for android
    Copyright (C) 2019 Niranjan Rajendran

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.

An unmodified copy of the above license text must be included in all forks.

To obtain the software under a different license, please contact [Niranjan Rajendran](https://niranjan.io) at `me <at> niranjan.io`.

## External Credits

1. A Big-Huge Thanks to Lee Benfield ([lee@benf.org](mailto:lee@benf.org)) for his awesome CFR - Class File Reader
2. Panxiaobo ([pxb1988@gmail.com](mailto:pxb1988@gmail.com)) for dex2jar.
3. [Hsiafan](https://github.com/hsiafan/apk-parser) for apk-parser.
4. [Ben Gruver](https://github.com/JesusFreke/) for dexlib2.
5. [skylot](https://github.com/skylot/jadx) for JaDX.
6. [JetBrains](https://github.com/JetBrains) for FernFlower analytical decompiler.

> Android, Google Play and the Google Play logo are trademarks of Google LLC.
