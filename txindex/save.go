package txindex

import (
	"fmt"

	"github.com/asdine/storm"
	"reflect"
)

const KeyEndBlock = "EndBlock"

func (db *DB) SaveOrUpdate(emptyTxPointer, tx interface{}) error {
	v := reflect.ValueOf(tx)
	txHash := reflect.Indirect(v).FieldByName("Hash").String()
	err := db.txDB.One("Hash", txHash, emptyTxPointer)
	if err != nil && err != storm.ErrNotFound {
		return fmt.Errorf("error checking if tx was already indexed: %s", err.Error())
	}

	if err == nil {
		// delete old tx before saving new
		err = db.txDB.DeleteStruct(emptyTxPointer)
		if err != nil {
			return fmt.Errorf("error deleting previously indexed tx: %s", err.Error())
		}
	}

	return db.txDB.Save(tx)
}

func (db *DB) SaveLastIndexPoint(endBlockHeight int32) error {
	err := db.txDB.Set(TxBucketName, KeyEndBlock, &endBlockHeight)
	if err != nil {
		return fmt.Errorf("error setting block height for last indexed tx: %s", err.Error())
	}
	return nil
}
