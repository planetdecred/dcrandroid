package dcrlibwallet

import "github.com/decred/dcrwallet/wallet/v3"

func ticketStatusString(ticketStatus wallet.TicketStatus) string {
	switch ticketStatus {
	case wallet.TicketStatusUnknown:
		return "UNKNOWN"
	case wallet.TicketStatusUnmined:
		return "UNMINED"
	case wallet.TicketStatusImmature:
		return "IMMATURE"
	case wallet.TicketStatusLive:
		return "LIVE"
	case wallet.TicketStatusVoted:
		return "VOTED"
	case wallet.TicketStatusMissed:
		return "MISSED"
	case wallet.TicketStatusExpired:
		return "EXPIRED"
	case wallet.TicketStatusRevoked:
		return "REVOKED"
	default:
		return "UNKNOWN"
	}
}
