package txindex

import (
	"github.com/asdine/storm"
	"github.com/asdine/storm/q"
	"github.com/planetdecred/dcrlibwallet/txhelper"
)

const (
	TxFilterAll         int32 = 0
	TxFilterSent        int32 = 1
	TxFilterReceived    int32 = 2
	TxFilterTransferred int32 = 3
	TxFilterStaking     int32 = 4
	TxFilterCoinBase    int32 = 5
	TxFilterRegular     int32 = 6
)

func TxMatchesFilter(txType string, txDirection, txFilter int32) bool {
	switch txFilter {
	case TxFilterSent:
		return txType == txhelper.TxTypeRegular && txDirection == txhelper.TxDirectionSent
	case TxFilterReceived:
		return txType == txhelper.TxTypeRegular && txDirection == txhelper.TxDirectionReceived
	case TxFilterTransferred:
		return txType == txhelper.TxTypeRegular && txDirection == txhelper.TxDirectionTransferred
	case TxFilterStaking:
		return txType != txhelper.TxTypeRegular && txType != txhelper.TxTypeCoinBase
	case TxFilterCoinBase:
		return txType == txhelper.TxTypeCoinBase
	case TxFilterRegular:
		return txType == txhelper.TxTypeRegular
	case TxFilterAll:
		return true
	}

	return false
}

func (db *DB) prepareTxQuery(txFilter int32) (query storm.Query) {
	switch txFilter {
	case TxFilterSent:
		query = db.txDB.Select(
			q.Eq("Type", txhelper.TxTypeRegular),
			q.Eq("Direction", txhelper.TxDirectionSent),
		)
	case TxFilterReceived:
		query = db.txDB.Select(
			q.Eq("Type", txhelper.TxTypeRegular),
			q.Eq("Direction", txhelper.TxDirectionReceived),
		)
	case TxFilterTransferred:
		query = db.txDB.Select(
			q.Eq("Type", txhelper.TxTypeRegular),
			q.Eq("Direction", txhelper.TxDirectionTransferred),
		)
	case TxFilterStaking:
		query = db.txDB.Select(
			q.Not(
				q.Eq("Type", txhelper.TxTypeRegular),
				q.Eq("Type", txhelper.TxTypeCoinBase),
			),
		)
	case TxFilterCoinBase:
		query = db.txDB.Select(
			q.Eq("Type", txhelper.TxTypeCoinBase),
		)
	case TxFilterRegular:
		query = db.txDB.Select(
			q.Eq("Type", txhelper.TxTypeRegular),
		)
	default:
		query = db.txDB.Select(
			q.True(),
		)
	}

	return
}
