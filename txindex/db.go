package txindex

import (
	"fmt"
	"os"

	"github.com/asdine/storm"
	bolt "go.etcd.io/bbolt"
)

const (
	DbName = "tx.db"

	TxBucketName = "TxIndexInfo"
	KeyDbVersion = "DbVersion"

	// Necessary to force re-indexing if changes are made to the structure of data being stored.
	// Increment this version number if db structure changes such that client apps need to re-index.
	TxDbVersion uint32 = 1
)

type DB struct {
	txDB  *storm.DB
	Close func() error
}

// Initialize opens the existing storm db at `dbPath`
// and checks the database version for compatibility.
// If there is a version mismatch or the db does not exist at `dbPath`,
// a new db is created and the current db version number saved to the db.
func Initialize(dbPath string, data interface{}) (*DB, error) {
	txDB, err := openOrCreateDB(dbPath)
	if err != nil {
		return nil, err
	}

	txDB, err = ensureDatabaseVersion(txDB, dbPath)
	if err != nil {
		return nil, err
	}

	// init database for saving/reading transaction objects
	err = txDB.Init(data)
	if err != nil {
		return nil, fmt.Errorf("error initializing tx database for wallet: %s", err.Error())
	}

	return &DB{
		txDB,
		txDB.Close,
	}, nil
}

func openOrCreateDB(dbPath string) (*storm.DB, error) {
	var isNewDbFile bool

	// first check if db file exists at dbPath, if not we'll need to create it and set the db version
	if _, err := os.Stat(dbPath); err != nil {
		if os.IsNotExist(err) {
			isNewDbFile = true
		} else {
			return nil, fmt.Errorf("error checking tx index database file: %s", err.Error())
		}
	}

	txDB, err := storm.Open(dbPath)
	if err != nil {
		switch err {
		case bolt.ErrTimeout:
			// timeout error occurs if storm fails to acquire a lock on the database file
			return nil, fmt.Errorf("tx index database is in use by another process")
		default:
			return nil, fmt.Errorf("error opening tx index database: %s", err.Error())
		}
	}

	if isNewDbFile {
		err = txDB.Set(TxBucketName, KeyDbVersion, TxDbVersion)
		if err != nil {
			os.RemoveAll(dbPath)
			return nil, fmt.Errorf("error initializing tx index db: %s", err.Error())
		}
	}

	return txDB, nil
}

// ensureDatabaseVersion checks the version of the existing db against `TxDbVersion`.
// If there's a difference, the current tx index db file is deleted and a new one created.
func ensureDatabaseVersion(txDB *storm.DB, dbPath string) (*storm.DB, error) {
	var currentDbVersion uint32
	err := txDB.Get(TxBucketName, KeyDbVersion, &currentDbVersion)
	if err != nil && err != storm.ErrNotFound {
		// ignore key not found errors as earlier db versions did not set a version number in the db.
		return nil, fmt.Errorf("error checking tx index database version: %s", err.Error())
	}

	if currentDbVersion != TxDbVersion {
		if err = os.RemoveAll(dbPath); err != nil {
			return nil, fmt.Errorf("error deleting outdated tx index database: %s", err.Error())
		}
		return openOrCreateDB(dbPath)
	}

	return txDB, nil
}
