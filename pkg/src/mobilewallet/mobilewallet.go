package mobilewallet

import (
	"bytes"
	"context"
	"errors"
	"fmt"
	"path/filepath"
	//"fmt"
	"encoding/json"
	"net"
	"sync"
	"time"

	"github.com/decred/dcrd/chaincfg"
	"github.com/decred/dcrd/dcrutil"
	"github.com/decred/dcrd/hdkeychain"
	"github.com/decred/dcrd/txscript"
	"github.com/decred/dcrd/wire"
	"github.com/decred/dcrwallet/chain"
	"github.com/decred/dcrwallet/loader"
	"github.com/decred/dcrwallet/netparams"
	"github.com/decred/dcrwallet/wallet"
	"github.com/decred/dcrwallet/wallet/txrules"
	walletseed "github.com/decred/dcrwallet/walletseed"
)

type LibWallet struct {
	dbDir      string
	wallet     *wallet.Wallet
	rpcClient  *chain.RPCClient
	loader     *loader.Loader
	syncer     *chain.RPCSyncer
	netBackend wallet.NetworkBackend
	mu         sync.Mutex
}

func NewLibWallet(dbDir string) *LibWallet {
	lw := &LibWallet{
		dbDir: dbDir,
	}
	initLogRotator(filepath.Join("/data/data/com.dcrandroid/files/dcrwallet", "dcrwallet.log"))
	return lw
}

//cfgutil/normalization.go
func NormalizeAddress(addr string, defaultPort string) (hostport string, err error) {
	// If the first SplitHostPort errors because of a missing port and not
	// for an invalid host, add the port.  If the second SplitHostPort
	// fails, then a port is not missing and the original error should be
	// returned.
	host, port, origErr := net.SplitHostPort(addr)
	if origErr == nil {
		return net.JoinHostPort(host, port), nil
	}
	addr = net.JoinHostPort(addr, defaultPort)
	_, _, err = net.SplitHostPort(addr)
	if err != nil {
		return "", origErr
	}
	return addr, nil
}

//rpc/rpcserver
func decodeAddress(a string, params *chaincfg.Params) (dcrutil.Address, error) {
	addr, err := dcrutil.DecodeAddress(a)
	if err != nil {
		return nil, err
	}
	if !addr.IsForNet(params) {
		return nil, errors.New(fmt.Sprintf("address %v is not intended for use on %v", a, params.Name))
	}
	return addr, nil
}

func (lw *LibWallet) InitLoader() {
	stakeOptions := &loader.StakeOptions{
		VotingEnabled: false,
		AddressReuse:  false,
		VotingAddress: nil,
		TicketFee:     10e8,
	}
	loader := loader.NewLoader(netparams.TestNet2Params.Params, lw.dbDir, stakeOptions,
		20, false, 10e5)
	lw.loader = loader
}

func (lw *LibWallet) CreateWallet(passphrase string, seedMnemonic string) error {
	fmt.Println("Creating wallet")

	pubPass := []byte(wallet.InsecurePubPassphrase)
	privPass := []byte(passphrase)
	seed, err := walletseed.DecodeUserInput(seedMnemonic)
	if err != nil {
		return err
	}

	w, err := lw.loader.CreateNewWallet(pubPass, privPass, seed)
	if err != nil {
		return err
	}
	lw.wallet = w
	// err = w.UpgradeToSLIP0044CoinType()
	// if err != nil {
	// 	return err
	// }
	fmt.Println("Created Wallet")
	return nil
}

func (lw *LibWallet) GenerateSeed() (string, error) {
	seed, err := hdkeychain.GenerateSeed(hdkeychain.RecommendedSeedLen)
	if err != nil {
		return "", err
	}

	return walletseed.EncodeMnemonic(seed), nil
}

func (lw *LibWallet) VerifySeed(seedMnemonic string) bool {
	_, err := walletseed.DecodeUserInput(seedMnemonic)
	if err != nil {
		return false
	}
	return true
}

func (lw *LibWallet) StartRpcClient(rpcHost string, rpcUser string, rpcPass string, certs []byte) bool {
	fmt.Println("Connecting to rpc client")
	ctx := context.Background()
	networkAddress, err := NormalizeAddress(rpcHost, "19109")
	if err != nil {
		fmt.Println(err)
		return false
	}
	c, err := chain.NewRPCClient(netparams.TestNet2Params.Params, networkAddress,
		rpcUser, rpcPass, certs, false)
	if err != nil {
		fmt.Println(err)
		return false
	}

	err = c.Start(ctx, false)
	if err != nil {
		fmt.Println(err)
		return false
	}

	fmt.Println("Connected to rpc client")

	lw.netBackend = chain.BackendFromRPCClient(c.Client)
	lw.rpcClient = c
	lw.loader.SetChainClient(c.Client)
	//lw.wallet.SetNetworkBackend(lw.netBackend)

	// syncer := chain.NewRPCSyncer(lw.wallet, c)
	// lw.syncer = syncer
	// go syncer.Run(ctx, true)

	// err = c.NotifyBlocks()
	// if err != nil {
	// 	fmt.Println(err)
	// 	return false
	// }
	return true
}

func (lw *LibWallet) DiscoverActiveAddresses(discoverAccounts bool, privPass []byte) error {
	wallet, ok := lw.loader.LoadedWallet()
	if !ok {
		return errors.New("Wallet has not been loaded")
	}

	lw.mu.Lock()
	chainClient := lw.rpcClient
	lw.mu.Unlock()
	if chainClient == nil {
		return errors.New("Consensus server RPC client has not been loaded")
	}

	if discoverAccounts && len(privPass) == 0 {
		return errors.New("private passphrase is required for discovering accounts")
	}

	if discoverAccounts {
		lock := make(chan time.Time, 1)
		defer func() {
			lock <- time.Time{}
			defer func() {
				for i := range privPass {
					privPass[i] = 0
				}
			}()
		}()
		err := wallet.Unlock(privPass, lock)
		if err != nil {
			return err
		}
	}

	n := chain.BackendFromRPCClient(chainClient.Client)
	err := wallet.DiscoverActiveAddresses(n, discoverAccounts)
	return err
}

func (lw *LibWallet) FetchHeaders() (int32, error) {
	fmt.Println("Fetching Headers")
	count, _, rescanFromHeight, _, _, err := lw.wallet.FetchHeaders(lw.netBackend)
	if err != nil {
		return 0, err
	}
	fmt.Printf("Fetched %v New Headers", count)
	if count > 0 {
		return rescanFromHeight, nil
	}
	return -1, nil
}

func (lw *LibWallet) LoadActiveDataFilters() error {
	fmt.Println("Loading Active Data Filters")
	err := lw.wallet.LoadActiveDataFilters(lw.netBackend)
	if err != nil {
		fmt.Println(err)
	}
	return err
}

func (lw *LibWallet) SubscribeToBlockNotifications() error {
	wallet, ok := lw.loader.LoadedWallet()
	if !ok {
		return errors.New("Wallet has not been loaded")
	}
	if lw.rpcClient == nil {
		return errors.New("Consensus server RPC client has not been loaded")
	}

	err := lw.rpcClient.NotifyBlocks()
	if err != nil {
		return err
	}

	syncer := chain.NewRPCSyncer(lw.wallet, lw.rpcClient)
	go syncer.Run(context.Background(), false)
	wallet.SetNetworkBackend(chain.BackendFromRPCClient(lw.rpcClient.Client))
	return nil
}

func (lw *LibWallet) OpenWallet() error {

	pubPass := []byte(wallet.InsecurePubPassphrase)
	w, err := lw.loader.OpenExistingWallet(pubPass)
	if err != nil {
		return err
	}
	lw.wallet = w
	// err = w.DiscoverActiveAddresses(lw.netBackend, true)
	// if err != nil {
	// 	return err
	// }

	// err = w.LoadActiveDataFilters(lw.netBackend)
	// if err != nil {
	// 	return err
	// }

	// lets skip loading all headers for now...
	// _, _, _, _, _, err = w.FetchHeaders(lw.netBackend)
	// if err != nil {
	// 	return err
	// }

	// no need to rescan on this test.
	// if fetchedHeaderCount > 0 {
	// 	err = w.Rescan(ctx, lw.netBackend, &rescanStart)
	// 	if err != nil {
	// 		return err
	// 	}
	// }

	return nil
}

func (lw *LibWallet) Rescan(startHeight int32, response BlockScanResponse) {
	go func() {
		if lw.netBackend == nil {
			response.OnError(1, "No network backend")
			return
		}
		if startHeight < 0 {
			response.OnError(2, "Begin height must be non-negative")
			return
		}
		progress := make(chan wallet.RescanProgress, 1)
		ctx := context.Background()
		n, _ := lw.wallet.NetworkBackend()
		var totalHeight int32 = 0
		go lw.wallet.RescanProgressFromHeight(ctx, n, startHeight, progress)
		for p := range progress {
			if p.Err != nil {
				fmt.Println(p.Err)
				response.OnError(-1, p.Err.Error())
				return
			}
			totalHeight += p.ScannedThrough
			response.OnScan(p.ScannedThrough)
		}
		select {
		case <-ctx.Done():
			response.OnEnd(totalHeight, true)
		default:
			response.OnEnd(totalHeight, false)
		}
	}()
}

func (lw *LibWallet) IsAddressMine(address string) bool {
	addr, err := decodeAddress(address, lw.wallet.ChainParams())
	if err != nil {
		return false
	}
	_, err = lw.wallet.AddressInfo(addr)
	if err != nil {
		return false
	}
	return true
}

func (lw *LibWallet) GetAccountName(account int32) string {
	name, err := lw.wallet.AccountName(uint32(account))
	if err != nil {
		fmt.Println(err)
		return "Account not found"
	}

	return name
}

func (lw *LibWallet) GetTransactions(response GetTransactionsResponse) error {
	ctx := context.Background()
	var startBlock, endBlock *wallet.BlockIdentifier
	minedTransactions := make([]Transaction, 0)
	unMinedTransactions := make([]Transaction, 0)
	rangeFn := func(block *wallet.Block) (bool, error) {
		if block.Height != -1 {
			for _, transaction := range block.Transactions {
				var amount int64
				tempCredits := make([]TransactionCredit, len(transaction.MyOutputs))
				for index, credit := range transaction.MyOutputs {
					if lw.IsAddressMine(credit.Address.String()) {
						amount += int64(credit.Amount)
					}
					tempCredits[index] = TransactionCredit{
						Index:    int32(credit.Index),
						Account:  int32(credit.Account),
						Internal: credit.Internal,
						Amount:   int64(credit.Amount),
						Address:  credit.Address.String()}
				}
				tempDebits := make([]TransactionDebit, len(transaction.MyInputs))
				for index, debit := range transaction.MyInputs {
					tempDebits[index] = TransactionDebit{
						Index:           int32(debit.Index),
						PreviousAccount: int32(debit.PreviousAccount),
						PreviousAmount:  int64(debit.PreviousAmount),
						AccountName:     lw.GetAccountName(int32(debit.PreviousAccount))}
				}
				tempTransaction := Transaction{
					Fee:       int64(transaction.Fee),
					Hash:      fmt.Sprintf("%02x", reverse(transaction.Hash[:])),
					Timestamp: transaction.Timestamp,
					Type:      transactionType(transaction.Type),
					Credits:   &tempCredits,
					Amount:    amount,
					Height:    block.Height,
					Status:    "confirmed",
					Debits:    &tempDebits}
				minedTransactions = append(minedTransactions, tempTransaction)
			}
		} else {
			for _, transaction := range block.Transactions {
				var amount int64
				tempCredits := make([]TransactionCredit, len(transaction.MyOutputs))
				for index, credit := range transaction.MyOutputs {
					if lw.IsAddressMine(credit.Address.String()) {
						amount += int64(credit.Amount)
					}
					tempCredits[index] = TransactionCredit{
						Index:    int32(credit.Index),
						Account:  int32(credit.Account),
						Internal: credit.Internal,
						Amount:   int64(credit.Amount),
						Address:  credit.Address.String()}
				}
				tempDebits := make([]TransactionDebit, len(transaction.MyInputs))
				for index, debit := range transaction.MyInputs {
					tempDebits[index] = TransactionDebit{
						Index:           int32(debit.Index),
						PreviousAccount: int32(debit.PreviousAccount),
						PreviousAmount:  int64(debit.PreviousAmount),
						AccountName:     lw.GetAccountName(int32(debit.PreviousAccount))}
				}
				tempTransaction := Transaction{
					Fee:       int64(transaction.Fee),
					Hash:      fmt.Sprintf("%02x", reverse(transaction.Hash[:])),
					Timestamp: transaction.Timestamp,
					Type:      transactionType(transaction.Type),
					Credits:   &tempCredits,
					Amount:    amount,
					Height:    0,
					Status:    "pending",
					Debits:    &tempDebits}
				minedTransactions = append(minedTransactions, tempTransaction)
			}
		}
		select {
		case <-ctx.Done():
			return true, ctx.Err()
		default:
			return false, nil
		}
	}
	fmt.Println("Getting transactions")
	err := lw.wallet.GetTransactions(rangeFn, startBlock, endBlock)
	fmt.Println("Got transactions")
	result, _ := json.Marshal(getTransactionsResponse{ErrorOccurred: false, Mined: minedTransactions, UnMined: unMinedTransactions})
	response.OnResult(string(result))
	if err != nil {
		return err
	}

	return nil

}

func reverse(hash []byte) []byte {
	for i := 0; i < len(hash)/2; i++ {
		j := len(hash) - i - 1
		hash[i], hash[j] = hash[j], hash[i]
	}
	return hash
}

func transactionType(txType wallet.TransactionType) string {
	switch txType {
	case wallet.TransactionTypeCoinbase:
		return "COINBASE"
	case wallet.TransactionTypeTicketPurchase:
		return "TICKET_PURCHASE"
	case wallet.TransactionTypeVote:
		return "VOTE"
	case wallet.TransactionTypeRevocation:
		return "REVOCATION"
	default:
		return "REGULAR"
	}
}

func (lw *LibWallet) GetBestBlock() int32{
	_, height := lw.wallet.MainChainTip()
	return height
}

func (lw *LibWallet) PublishUnminedTransactions() error {
	if lw.netBackend == nil{
		return errors.New("wallet is not associated with a consensus server RPC client")
	}
	err := lw.wallet.PublishUnminedTransactions(context.Background(), lw.netBackend)
	return err
}

func (lw *LibWallet) SpendableForAccount(account int32, requiredConfirmations int32) (int64, error) {
	bals, err := lw.wallet.CalculateAccountBalance(uint32(account), requiredConfirmations)
	if err != nil {
		return 0, err
	}

	return int64(bals.Spendable), nil
}

func (lw *LibWallet) AddressForAccount(account int32) (string, error) {
	var callOpts []wallet.NextAddressCallOption
	callOpts = append(callOpts, wallet.WithGapPolicyWrap())

	addr, err := lw.wallet.NewExternalAddress(uint32(account), callOpts...)
	if err != nil {
		fmt.Println(err)
		return "", err
	}
	return addr.EncodeAddress(), nil
}

func (lw *LibWallet) ConstructTransaction(destAddr string, amount int64, srcAccount int32, requiredConfirmations int32) (*ConstructTxResponse, error) {
	// output destination
	addr, err := dcrutil.DecodeAddress(destAddr)
	if err != nil {
		return nil, err
	}
	pkScript, err := txscript.PayToAddrScript(addr)
	if err != nil {
		return nil, err
	}
	version := txscript.DefaultScriptVersion

	// pay output
	outputs := make([]*wire.TxOut, 1)
	outputs[0] = &wire.TxOut{
		Value:    amount,
		Version:  version,
		PkScript: pkScript,
	}
	var algo wallet.OutputSelectionAlgorithm = wallet.OutputSelectionAlgorithmDefault
	feePerKb := txrules.DefaultRelayFeePerKb

	// create tx
	tx, err := lw.wallet.NewUnsignedTransaction(outputs, feePerKb, uint32(srcAccount),
		requiredConfirmations, algo, nil)
	if err != nil {
		return nil, err
	}

	var txBuf bytes.Buffer
	txBuf.Grow(tx.Tx.SerializeSize())
	err = tx.Tx.Serialize(&txBuf)
	if err != nil {
		return nil, err
	}
	var totalOutput dcrutil.Amount
	for _, txOut := range outputs {
		totalOutput += dcrutil.Amount(txOut.Value)
	}
	return &ConstructTxResponse{
		TotalOutputAmount:         int64(totalOutput),
		UnsignedTransaction:       txBuf.Bytes(),
		TotalPreviousOutputAmount: int64(tx.TotalInput),
		EstimatedSignedSize:       int32(tx.EstimatedSignedSerializeSize)}, nil
}

func (lw *LibWallet) SignTransaction(rawTransaction []byte, privPass []byte) ([]byte, error) {
	defer func() {
		for i := range privPass {
			privPass[i] = 0
		}
	}()
	var tx wire.MsgTx
	err := tx.Deserialize(bytes.NewReader(rawTransaction))
	if err != nil {
		//Bytes do not represent a valid raw transaction
		return nil, err
	}

	lock := make(chan time.Time, 1)
	defer func() {
		lock <- time.Time{} // send matters, not the value
	}()

	err = lw.wallet.Unlock(privPass, lock)
	if err != nil {
		return nil, err
	}

	var additionalPkScripts map[wire.OutPoint][]byte

	invalidSigs, err := lw.wallet.SignTransaction(&tx, txscript.SigHashAll, additionalPkScripts, nil, nil)
	if err != nil {
		return nil, err
	}

	invalidInputIndexes := make([]uint32, len(invalidSigs))
	for i, e := range invalidSigs {
		invalidInputIndexes[i] = e.InputIndex
	}

	var serializedTransaction bytes.Buffer
	serializedTransaction.Grow(tx.SerializeSize())
	err = tx.Serialize(&serializedTransaction)
	if err != nil {
		return nil, err
	}
	return serializedTransaction.Bytes(), nil
}

func (lw *LibWallet) PublishTransaction(signedTransaction []byte) ([]byte, error) {
	n, err := lw.wallet.NetworkBackend()
	if err != nil {
		return nil, err
	}

	var msgTx wire.MsgTx
	err = msgTx.Deserialize(bytes.NewReader(signedTransaction))
	if err != nil {
		//Invalid tx
		return nil, err
	}

	txHash, err := lw.wallet.PublishTransaction(&msgTx, signedTransaction, n)
	if err != nil {
		return nil, err
	}

	return txHash[:], nil
}

func (lw *LibWallet) GetAccounts() (string, error) {
	resp, err := lw.wallet.Accounts()
	if err != nil {
		return "", errors.New("Unable to get accounts from wallet")
	}
	accounts := make([]Account, len(resp.Accounts))
	for i := range resp.Accounts {
		a := &resp.Accounts[i]
		bals, err := lw.wallet.CalculateAccountBalance(a.AccountNumber, 0)
		if err != nil {
			return "", errors.New(fmt.Sprintf("Unable to calculate balance for account %v", a.AccountNumber))
		}
		balance := Balance{
			Total:                   int64(bals.Total),
			Spendable:               int64(bals.Spendable),
			ImmatureReward:          int64(bals.ImmatureCoinbaseRewards),
			ImmatureStakeGeneration: int64(bals.ImmatureStakeGeneration),
			LockedByTickets:         int64(bals.LockedByTickets),
			VotingAuthority:         int64(bals.VotingAuthority),
			UnConfirmed:             int64(bals.Unconfirmed),
		}
		fmt.Println("Total Balance: " + bals.Total.String() + " For account: " + string(a.AccountNumber))
		accounts[i] = Account{
			Number:             int32(a.AccountNumber),
			Name:               a.AccountName,
			TotalBalance:       int64(a.TotalBalance),
			Balance:            &balance,
			External_key_count: int32(a.LastUsedExternalIndex + 20),
			Internal_key_count: int32(a.LastUsedInternalIndex + 20),
			Imported_key_count: int32(a.ImportedKeyCount),
		}
	}
	accountsResponse := &Accounts{
		Count:                len(resp.Accounts),
		Current_block_hash:   resp.CurrentBlockHash[:],
		Current_block_height: resp.CurrentBlockHeight,
		Acc:                  &accounts,
		ErrorOccurred:        false,
	}
	result, _ := json.Marshal(accountsResponse)
	return string(result), nil
}

func (lw *LibWallet) NextAccount(accountName string, privPass []byte) bool {
	lock := make(chan time.Time, 1)
	defer func() {
		for i := range privPass {
			privPass[i] = 0
		}
		lock <- time.Time{} // send matters, not the value
	}()
	err := lw.wallet.Unlock(privPass, lock)
	if err != nil {
		fmt.Println(err)
		return false
	}

	_, err = lw.wallet.NextAccount(accountName)
	if err != nil {
		fmt.Println(err)
		return false
	}
	return true
}
