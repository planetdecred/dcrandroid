# dcrandroid - Decred Mobile Wallet

[![Build Status](https://travis-ci.org/raedahgroup/dcrandroid.svg?branch=master)](https://travis-ci.org/raedahgroup/dcrandroid)

A Decred Mobile Wallet for android that runs on top of [dcrwallet](https://github.com/decred/dcrwallet).

## Requirements

Android 3.0 or above.

### Prerequisites

1. [Android SDK](https://developer.android.com/sdk/download.html) and [NDK](https://developer.android.com/ndk/downloads/index.html)
2. [Android Studio](https://developer.android.com/studio/index.html)
3. [Go(1.8 or 1.9)](http://golang.org/doc/install)
4. [Dep](https://github.com/golang/dep/releases)
5. [Gomobile](https://github.com/golang/go/wiki/Mobile#tools) (correctly init'd with gomobile init)

Run the following commands

    git clone https://github.com/raedahgroup/dcrandroid.git
    cd dcrandroid/pkg
    mkdir bin
    export GOPATH=$(pwd)
    export PATH=$PATH:$GOPATH/bin
    cd src/mobilewallet
    dep ensure -v
    gomobile bind -target=android/arm
    cp mobilewallet.aar ../../../app/libs/mobilewallet.aar
    cd ../../../
    ./gradlew