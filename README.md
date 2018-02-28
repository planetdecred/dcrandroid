# dcrandroid - Decred Mobile Wallet
[![Build Status](https://travis-ci.org/raedahgroup/dcrandroid.svg?branch=master)](https://travis-ci.org/raedahgroup/dcrandroid)

An decred wallet for android that runs on top of [dcrwallet](https://github.com/decred/dcrwallet).

## Requirements:
Android 3.0 or above.

## Building dcrwallet
Dcrwallet is required for this app to run, you can optionally build it or use library file found in [dcrwallet dir](https://github.com/C-ollins/dcrandroid/tree/master/dcrwallet)

### Prerequisites
1. Go(1.8 or 1.9) which can be found [here](http://golang.org/doc/install)
2. Dep [latest release](https://github.com/golang/dep/releases)
3. Gomobile [Installation Instructions](https://github.com/golang/go/wiki/Mobile#tools)

To compile, run the following commands

    go get -v github.com/decred/dcrwallet
    cd $GOPATH/src/github.com/decred/dcrwallet
    git fetch https://github.com/C-ollins/dcrwallet mobile
    git branch mobile FETCH_HEAD
    git checkout mobile
    dep ensure -v
    gomobile bind -target=android/arm
Or navigate to dcrandroid clone directory and run

    ./build.sh

## Building Android Application
Android Studio(or gradle) and Android SDK is required if you wish to compile it yourself.

### Prerequisites
1. Gradle wrapper 4.1 and Gralde build tools 3.0.1
2. Android SDK with build tools 26.0.2, SDK Platform 27 and Android support repository installed

Clone dcrandroid (or fork it):

    git clone https://github.com/raedahgroup/dcrandroid.git
### Building with Gradle Command Line
On a Windows PC, open command prompt and navigate to the dcrandroid clone directory, then run:
    
    gradlew.bat

On Mac OS or Linux, open terminal and navigate to dcrandroid clone directory, then run:

    ./gradlew

### Building with Android Studio (Recommended)
* Open Android Studio
* Select `import project`
* Navigate to dcrandroid clone directory and click `OK
