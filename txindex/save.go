package txindex

import (
	"fmt"
	"reflect"

	"github.com/asdine/storm"
	"github.com/decred/dcrwallet/errors/v2"
)

const KeyEndBlock = "EndBlock"

// SaveOrUpdate saves a transaction to the database and would overwrite
// if a transaction with same hash exists
func (db *DB) SaveOrUpdate(emptyTxPointer, tx interface{}) (overwritten bool, err error) {
	v := reflect.ValueOf(tx)
	txHash := reflect.Indirect(v).FieldByName("Hash").String()
	err = db.txDB.One("Hash", txHash, emptyTxPointer)
	if err != nil && err != storm.ErrNotFound {
		err = errors.Errorf("error checking if tx was already indexed: %s", err.Error())
		return
	}

	v2 := reflect.ValueOf(emptyTxPointer)
	timestamp := reflect.Indirect(v2).FieldByName("Timestamp").Int()
	if timestamp > 0 {
		overwritten = true
		// delete old tx before saving new (if it exists)
		db.txDB.DeleteStruct(emptyTxPointer)
	}

	err = db.txDB.Save(tx)
	return
}

func (db *DB) SaveLastIndexPoint(endBlockHeight int32) error {
	err := db.txDB.Set(TxBucketName, KeyEndBlock, &endBlockHeight)
	if err != nil {
		return fmt.Errorf("error setting block height for last indexed tx: %s", err.Error())
	}
	return nil
}

func (db *DB) ClearSavedTransactions(emptyTxPointer interface{}) error {
	err := db.txDB.Drop(emptyTxPointer)
	if err != nil {
		return err
	}

	return db.SaveLastIndexPoint(0)
}
