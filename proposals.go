package dcrlibwallet

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"net/url"
)

const (
	apiEndpoint            = "proposals.decred.org"
	apiEndpointPath        = "/api/v1"
	apiVersionPath         = "/api/v1/version"
	apiPolicyPath          = "/policy"
	apiVettedProposalsPath = "/proposals/vetted"
)

type Politeia struct {
	csrfToken    string
	serverPolicy *ServerPolicy
}

func newPoliteia() Politeia {
	return Politeia{}
}

func (p *Politeia) prepareRequest(path, method string, queryStrings map[string]string, body []byte) (*http.Request, error) {
	req := &http.Request{
		Method: method,
		URL:    &url.URL{Scheme: "https", Host: apiEndpoint, Path: apiEndpointPath + path},
	}

	if body != nil {
		req.Body = ioutil.NopCloser(bytes.NewBuffer(body))
	}

	if queryStrings != nil {
		queryString := req.URL.Query()
		for i, v := range queryStrings {
			queryString.Set(i, v)
		}
		req.URL.RawQuery = queryString.Encode()
	}

	if method == "POST" {
		originalURL := req.URL
		if p.csrfToken == "" {
			req.URL.Path = apiVersionPath
			res, err := http.DefaultClient.Do(req)
			if err != nil {
				return nil, fmt.Errorf("error fetching csrf token")
			}

			err = p.handleResponse(res, &p.csrfToken)
			if err != nil {
				return nil, err
			}
		}
		req.URL = originalURL
		req.Header.Set("X-CSRF-TOKEN", p.csrfToken)
	}

	return req, nil
}

func (p *Politeia) makeRequest(path, method string, queryStrings map[string]string, body []byte, dest interface{}) error {
	req, err := p.prepareRequest(path, method, queryStrings, body)
	if err != nil {
		return err
	}

	res, err := http.DefaultClient.Do(req)
	if err != nil {
		return err
	}

	return p.handleResponse(res, dest)
}

func (p *Politeia) handleResponse(res *http.Response, dest interface{}) error {
	if res.StatusCode == 200 {
		return json.NewDecoder(res.Body).Decode(dest)
	}

	body, err := ioutil.ReadAll(res.Body)
	if err != nil {
		return fmt.Errorf("error reading response body: %s", err.Error())
	}
	res.Body.Close()

	return fmt.Errorf("request error: %s", string(body))
}

func (p *Politeia) getServerPolicy() (*ServerPolicy, error) {
	var serverPolicy ServerPolicy

	err := p.makeRequest(apiPolicyPath, "GET", nil, nil, &serverPolicy)
	if err != nil {
		return nil, fmt.Errorf("error fetching politeia policy: %v", err)
	}

	return &serverPolicy, nil
}

func (p *Politeia) getProposalsChunk(startHash string) ([]Proposal, error) {
	var queryStrings map[string]string
	if startHash != "" {
		queryStrings = map[string]string{
			"after": startHash,
		}
	}

	var result Proposals
	err := p.makeRequest(apiVettedProposalsPath, "GET", queryStrings, nil, &result)
	if err != nil {
		return nil, fmt.Errorf("error fetching proposals from %s: %v", startHash, err)
	}

	return result.Proposals, err
}

// GetProposalsChunk gets proposals starting after the proposal with the specified
// censorship hash. The number of proposals returned is specified in the poltieia
// policy API endpoint
func (p *Politeia) GetProposalsChunk(startHash string) (string, error) {
	proposals, err := p.getProposalsChunk(startHash)
	if err != nil {
		return "", err
	}

	jsonBytes, err := json.Marshal(proposals)
	if err != nil {
		return "", fmt.Errorf("error marshalling proposal result to json: %s", err.Error())
	}

	return string(jsonBytes), nil
}

// GetAllProposal fetches all vetted proposals from the API
func (p *Politeia) GetAllProposals() (string, error) {
	var proposalChunkResult, proposals []Proposal
	var err error

	if p.serverPolicy == nil {
		policy, err := p.getServerPolicy()
		if err != nil {
			return "", err
		}
		p.serverPolicy = policy
	}

	proposalChunkResult, err = p.getProposalsChunk("")
	if err != nil {
		return "", fmt.Errorf("error fetching all proposals: %s", err.Error())
	}
	proposals = append(proposals, proposalChunkResult...)

	for {
		if proposalChunkResult == nil || len(proposalChunkResult) < p.serverPolicy.ProposalListPageSize {
			break
		}

		proposalChunkResult, err = p.getProposalsChunk(proposalChunkResult[p.serverPolicy.ProposalListPageSize-1].CensorshipRecord.Token)
		if err != nil {
			return "", err
		}
		proposals = append(proposals, proposalChunkResult...)
	}

	jsonBytes, err := json.Marshal(proposals)
	if err != nil {
		return "", fmt.Errorf("error marshalling proposal result to json: %s", err.Error())
	}

	return string(jsonBytes), err
}
