# mobilewallet - Dcrwallet Mobile

[![Build Status](https://travis-ci.org/raedahgroup/mobilewallet.svg?branch=master)](https://travis-ci.org/raedahgroup/mobilewallet)

A Decred wallet library for mobile devices built from [dcrwallet](https://github.com/decred/dcrwallet)

## Build Dependencies

[Go( >= 1.11 )](http://golang.org/doc/install)  
[Gomobile](https://github.com/golang/go/wiki/Mobile#tools) (correctly init'd with gomobile init)  

## Build Instructions

To build this libary, clone the project and run the following commands in mobilewallet directory.

```bash
export GO111MODULE=on
go mod download
go mod vendor
export GO111MODULE=off
gomobile bind -target=android # -target=ios for iOS
```

Mobilewallet library can also be built targeting different architectures of android which can be configured using the `-target` command line argument Ex. `gomobile bind -target=android/arm`, `gomobile bind -target=android/386`...

Copy the generated library (mobilewallet.aar for android or Mobilewallet.framewok in the case of iOS) into `libs` directory(`Frameworks` for iOS)