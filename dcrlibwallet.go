package dcrlibwallet

import (
	"bytes"
	"encoding/json"
	"fmt"
	"path/filepath"
	"strings"

	"github.com/asdine/storm"
	"github.com/decred/dcrd/dcrjson"
	"github.com/decred/dcrwallet/errors"
	"github.com/decred/dcrwallet/netparams"
	"github.com/decred/dcrwallet/wallet"
	"github.com/decred/dcrwallet/wallet/txrules"
	"github.com/raedahgroup/dcrlibwallet/utils"
	"go.etcd.io/bbolt"
)

const (
	logFileName = "dcrlibwallet.log"
	txDbName    = "tx.db"

	BlockValid = 1 << 0
)

type LibWallet struct {
	walletDataDir string
	activeNet     *netparams.Params
	walletLoader  *WalletLoader
	wallet        *wallet.Wallet
	txDB          *storm.DB
	*syncData
}

func NewLibWallet(homeDir string, dbDriver string, netType string) (*LibWallet, error) {
	activeNet := utils.NetParams(netType)
	if activeNet == nil {
		return nil, fmt.Errorf("unsupported network type: %s", netType)
	}

	walletDataDir := filepath.Join(homeDir, activeNet.Name)
	return newLibWallet(walletDataDir, dbDriver, activeNet)
}

func NewLibWalletWithDbPath(walletDataDir string, activeNet *netparams.Params) (*LibWallet, error) {
	return newLibWallet(walletDataDir, "", activeNet)
}

func newLibWallet(walletDataDir, walletDbDriver string, activeNet *netparams.Params) (*LibWallet, error) {
	errors.Separator = ":: "
	initLogRotator(filepath.Join(walletDataDir, logFileName))

	// open database for indexing transactions for faster loading
	txDB, err := storm.Open(filepath.Join(walletDataDir, txDbName))
	if err != nil {
		log.Errorf("Error opening tx database for wallet: %s", err.Error())
		if err == bolt.ErrTimeout {
			// timeout error occurs if storm fails to acquire a lock on the database file
			return nil, fmt.Errorf("tx index database is in use by another process")
		}
		return nil, fmt.Errorf("error opening tx index database: %s", err.Error())
	}

	// init database for saving/reading transaction objects
	err = txDB.Init(&Transaction{})
	if err != nil {
		log.Errorf("Error initializing tx database for wallet: %s", err.Error())
		return nil, err
	}

	// init walletLoader
	defaultFees := txrules.DefaultRelayFeePerKb.ToCoin()

	stakeOptions := &StakeOptions{
		VotingEnabled: false,
		AddressReuse:  false,
		VotingAddress: nil,
		TicketFee:     defaultFees,
	}

	walletLoader := NewLoader(activeNet.Params, walletDataDir, stakeOptions, 20, false,
		defaultFees, wallet.DefaultAccountGapLimit)

	if walletDbDriver != "" {
		walletLoader.SetDatabaseDriver(walletDbDriver)
	}

	syncData := &syncData{
		syncProgressListeners: make(map[string]SyncProgressListener),
	}

	// Finally Init LibWallet
	lw := &LibWallet{
		walletDataDir: walletDataDir,
		txDB:          txDB,
		activeNet:     activeNet,
		walletLoader:  walletLoader,
		syncData:      syncData,
	}

	return lw, nil
}

func (lw *LibWallet) Shutdown() {
	log.Info("Shutting down dcrlibwallet")

	// Trigger shuttingDown signal to cancel all contexts created with `contextWithShutdownCancel`.
	shuttingDown <- true

	if lw.rpcClient != nil {
		lw.rpcClient.Stop()
	}

	lw.CancelSync()

	if logRotator != nil {
		log.Info("Shutting down log rotator")
		logRotator.Close()
	}

	if _, loaded := lw.walletLoader.LoadedWallet(); loaded {
		err := lw.walletLoader.UnloadWallet()
		if err != nil {
			log.Errorf("Failed to close wallet: %v", err)
		} else {
			log.Info("Closed wallet")
		}
	}

	if lw.txDB != nil {
		err := lw.txDB.Close()
		if err != nil {
			log.Errorf("tx db closed with error: %v", err)
		} else {
			log.Info("tx db closed successfully")
		}
	}
}

func (lw *LibWallet) CallJSONRPC(method string, args string, address string, username string, password string, caCert string) (string, error) {
	arguments := strings.Split(args, ",")
	params := make([]interface{}, 0)
	for _, arg := range arguments {
		if strings.TrimSpace(arg) == "" {
			continue
		}
		params = append(params, strings.TrimSpace(arg))
	}
	// Attempt to create the appropriate command using the arguments
	// provided by the user.
	cmd, err := dcrjson.NewCmd(method, params...)
	if err != nil {
		// Show the error along with its error code when it's a
		// dcrjson.Error as it reallistcally will always be since the
		// NewCmd function is only supposed to return errors of that
		// type.
		if jerr, ok := err.(dcrjson.Error); ok {
			log.Errorf("%s command: %v (code: %s)\n",
				method, err, jerr.Code)
			return "", err
		}
		// The error is not a dcrjson.Error and this really should not
		// happen.  Nevertheless, fallback to just showing the error
		// if it should happen due to a bug in the package.
		log.Errorf("%s command: %v\n", method, err)
		return "", err
	}

	// Marshal the command into a JSON-RPC byte slice in preparation for
	// sending it to the RPC server.
	marshalledJSON, err := dcrjson.MarshalCmd("1.0", 1, cmd)
	if err != nil {
		log.Error(err)
		return "", err
	}

	// Send the JSON-RPC request to the server using the user-specified
	// connection configuration.
	result, err := utils.SendPostRequest(marshalledJSON, address, username, password, caCert)
	if err != nil {
		log.Error(err)
		return "", err
	}

	// Choose how to display the result based on its type.
	strResult := string(result)
	if strings.HasPrefix(strResult, "{") || strings.HasPrefix(strResult, "[") {
		var dst bytes.Buffer
		if err := json.Indent(&dst, result, "", "  "); err != nil {
			log.Errorf("Failed to format result: %v", err)
			return "", err
		}
		fmt.Println(dst.String())
		return dst.String(), nil

	} else if strings.HasPrefix(strResult, `"`) {
		var str string
		if err := json.Unmarshal(result, &str); err != nil {
			log.Errorf("Failed to unmarshal result: %v", err)
			return "", err
		}
		fmt.Println(str)
		return str, nil

	} else if strResult != "null" {
		fmt.Println(strResult)
		return strResult, nil
	}
	return "", nil
}
