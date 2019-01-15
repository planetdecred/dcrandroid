# dcrlibwallet

[![Build Status](https://travis-ci.org/raedahgroup/dcrlibwallet.svg?branch=master)](https://travis-ci.org/raedahgroup/dcrlibwallet)

A Decred wallet library written in golang for [dcrwallet](https://github.com/decred/dcrwallet)

## Build Dependencies

[Go( >= 1.11 )](http://golang.org/doc/install)  
[Gomobile](https://github.com/golang/go/wiki/Mobile#tools) (correctly init'd with gomobile init)  

## Build Instructions using Gomobile

To build this libary, clone the project 

```bash
go get -t github.com/raedahgroup/dcrlibwallet
cd $HOME/go/src/github.com/raedahgroup/dcrlibwallet/
```
and run the following commands in dcrlibwallet directory.

```bash
export GO111MODULE=on
go mod download
go mod vendor
export GO111MODULE=off
gomobile bind -target=android # -target=ios for iOS
```

dcrlibwallet can be built targeting different architectures of android which can be configured using the `-target` command line argument Ex. `gomobile bind -target=android/arm`, `gomobile bind -target=android/386`...

Copy the generated library (dcrlibwallet.aar for android or dcrlibwallet.framewok in the case of iOS) into `libs` directory(`Frameworks` for iOS)
