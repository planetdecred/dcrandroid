package txhelper

const (
	TxDirectionInvalid     int32 = -1
	TxDirectionSent        int32 = 0
	TxDirectionReceived    int32 = 1
	TxDirectionTransferred int32 = 2

	TxTypeRegular        = "REGULAR"
	TxTypeCoinBase       = "COINBASE"
	TxTypeTicketPurchase = "TICKET_PURCHASE"
	TxTypeVote           = "VOTE"
	TxTypeRevocation     = "REVOCATION"
)
