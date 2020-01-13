FROM golang 

COPY . /root

WORKDIR /root
RUN curl -sfL https://install.goreleaser.com/github.com/golangci/golangci-lint.sh | sh -s -- -b $(go env GOPATH)/bin v1.19.1

ENV GO111MODULE on

CMD ["./run_tests.sh"]
