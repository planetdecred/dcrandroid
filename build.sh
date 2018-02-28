go get -v github.com/decred/dcrwallet
cd $GOPATH/src/github.com/decred/dcrwallet
git fetch https://github.com/C-ollins/dcrwallet mobile
git branch mobile FETCH_HEAD
git checkout mobile
dep ensure -v
gomobile bind -target=android/arm