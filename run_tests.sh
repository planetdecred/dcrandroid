#!/usr/bin/env bash

# usage:
# ./run_tests.sh
#

set -ex

go test

golangci-lint run --deadline=10m \
      --disable-all \
      --enable govet \
      --enable staticcheck \
      --enable gosimple \
      --enable unconvert \
      --enable ineffassign \
      --enable structcheck \
      --enable goimports \
      --enable misspell \
      --enable unparam \