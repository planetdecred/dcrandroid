package txindex

import (
	"github.com/asdine/storm"
	"github.com/asdine/storm/q"
	"github.com/raedahgroup/dcrlibwallet/txhelper"
)

const (
	TxFilterAll         int32 = 0
	TxFilterSent        int32 = 1
	TxFilterReceived    int32 = 2
	TxFilterTransferred int32 = 3
	TxFilterStaking     int32 = 4
	TxFilterCoinBase    int32 = 5
)

func DetermineTxFilter(txType string, txDirection int32) int32 {
	if txType == txhelper.TxTypeCoinBase {
		return TxFilterCoinBase
	}
	if txType != txhelper.TxTypeRegular {
		return TxFilterStaking
	}

	switch txDirection {
	case txhelper.TxDirectionSent:
		return TxFilterSent
	case txhelper.TxDirectionReceived:
		return TxFilterReceived
	default:
		return TxFilterTransferred
	}
}

func (db *DB) prepareTxQuery(txFilter int32) (query storm.Query) {
	switch txFilter {
	case TxFilterSent:
		query = db.txDB.Select(
			q.Eq("Direction", txhelper.TxDirectionSent),
		)
	case TxFilterReceived:
		query = db.txDB.Select(
			q.Eq("Direction", txhelper.TxDirectionReceived),
		)
	case TxFilterTransferred:
		query = db.txDB.Select(
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
	default:
		query = db.txDB.Select(
			q.True(),
		)
	}

	query = query.OrderBy("Timestamp").Reverse()
	return
}
