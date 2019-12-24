package dcrlibwallet

import (
	"context"
	"math"
	"time"

	"github.com/decred/dcrwallet/errors"
	w "github.com/decred/dcrwallet/wallet/v3"
)

func (mw *MultiWallet) RescanBlocks(walletID int) error {

	wallet := mw.WalletWithID(walletID)
	if wallet == nil {
		return errors.E(ErrNotExist)
	}

	netBackend, err := wallet.internal.NetworkBackend()
	if err != nil {
		return errors.E(ErrNotConnected)
	}

	if mw.IsRescanning() || !mw.IsSynced() {
		return errors.E(ErrInvalid)
	}

	go func() {
		defer func() {
			mw.syncData.mu.Lock()
			mw.syncData.rescanning = false
			mw.syncData.cancelRescan = nil
			mw.syncData.mu.Unlock()
		}()

		mw.syncData.mu.Lock()
		mw.syncData.rescanning = true

		ctx, cancel := wallet.shutdownContextWithCancel()
		mw.syncData.cancelRescan = cancel

		mw.syncData.mu.Unlock()

		if mw.blocksRescanProgressListener != nil {
			mw.blocksRescanProgressListener.OnBlocksRescanStarted(walletID)
		}

		progress := make(chan w.RescanProgress, 1)
		go wallet.internal.RescanProgressFromHeight(ctx, netBackend, 0, progress)

		rescanStartTime := time.Now().Unix()

		for p := range progress {
			if p.Err != nil {
				log.Error(p.Err)
				if mw.blocksRescanProgressListener != nil {
					mw.blocksRescanProgressListener.OnBlocksRescanEnded(walletID, p.Err)
				}
				return
			}

			rescanProgressReport := &HeadersRescanProgressReport{
				CurrentRescanHeight: p.ScannedThrough,
				TotalHeadersToScan:  wallet.GetBestBlock(),
				WalletID:            walletID,
			}

			elapsedRescanTime := time.Now().Unix() - rescanStartTime
			rescanRate := float64(p.ScannedThrough) / float64(rescanProgressReport.TotalHeadersToScan)

			rescanProgressReport.RescanProgress = int32(math.Round(rescanRate * 100))
			estimatedTotalRescanTime := int64(math.Round(float64(elapsedRescanTime) / rescanRate))
			rescanProgressReport.RescanTimeRemaining = estimatedTotalRescanTime - elapsedRescanTime

			rescanProgressReport.GeneralSyncProgress = &GeneralSyncProgress{
				TotalSyncProgress:         rescanProgressReport.RescanProgress,
				TotalTimeRemainingSeconds: rescanProgressReport.RescanTimeRemaining,
			}

			if mw.blocksRescanProgressListener != nil {
				mw.blocksRescanProgressListener.OnBlocksRescanProgress(rescanProgressReport)
			}

			select {
			case <-ctx.Done():
				log.Info("Rescan canceled through context")

				if mw.blocksRescanProgressListener != nil {
					if ctx.Err() != nil && ctx.Err() != context.Canceled {
						mw.blocksRescanProgressListener.OnBlocksRescanEnded(walletID, ctx.Err())
					} else {
						mw.blocksRescanProgressListener.OnBlocksRescanEnded(walletID, nil)
					}
				}

				return
			default:
				continue
			}
		}

		err := wallet.reindexTransactions()
		if mw.blocksRescanProgressListener != nil {
			mw.blocksRescanProgressListener.OnBlocksRescanEnded(walletID, err)
		}
	}()

	return nil
}

func (mw *MultiWallet) CancelRescan() {
	mw.syncData.mu.Lock()
	defer mw.syncData.mu.Unlock()
	if mw.syncData.cancelRescan != nil {
		mw.syncData.cancelRescan()
		mw.syncData.cancelRescan = nil

		log.Info("Rescan canceled.")
	}
}

func (mw *MultiWallet) IsRescanning() bool {
	mw.syncData.mu.RLock()
	defer mw.syncData.mu.RUnlock()
	return mw.syncData.rescanning
}

func (mw *MultiWallet) SetBlocksRescanProgressListener(blocksRescanProgressListener BlocksRescanProgressListener) {
	mw.blocksRescanProgressListener = blocksRescanProgressListener
}
