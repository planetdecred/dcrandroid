# dcrandroid - Decred Mobile Wallet
[![Build Status](https://travis-ci.org/raedahgroup/dcrandroid.svg?branch=master)](https://travis-ci.org/raedahgroup/dcrandroid)

An decred wallet for android that runs ontop of [dcrwallet](https://github.com/decred/dcrwallet).

## Requirements:
Android 3.0 or above.

## Building
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
* Navigate to dcrandroid clone directory and click `OK`
## Contributing
dcrandroid is an open source project.

Contributions to dcrandroid are appreciated.
### Filing issues
When [filing an issue](https://github.com/raedahgroup/dcrandroid/issues/new), make sure state the following:
1. What error occurred.
2. How to recreate the error
3. What device and Android version you're using.
### Contributing code (Pull Request)
Setting up for a pull request
* [Register on GitHub](http://github.com)
* Visit and fork the [dcrandroid repository](https://github.com/raedahgroup/dcrandroid)
* Clone your fork
* Modify the code and add whatever you want.
* Create a pull request and explain properly the changes you made.
* It is recommended that you squash your commits, tested your build and make sure you do not have CI errors before opening a pull request.