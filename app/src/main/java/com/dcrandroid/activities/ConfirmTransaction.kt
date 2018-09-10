package com.dcrandroid.activities

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.dcrandroid.R
import com.dcrandroid.data.Constants
import com.dcrandroid.util.CoinFormat
import com.dcrandroid.util.DcrConstants
import com.dcrandroid.util.Utils
import mobilewallet.Mobilewallet
import java.text.DecimalFormat
import java.util.*

class ConfirmTransaction : AppCompatActivity(), View.OnClickListener {

    private var destinationAddress : String? = null
    private var txAmount : Long = 0
    private var srcAccount : Int = 0
    private var requiredConfs : Int = 0
    private var sendAll : Boolean = false

    private val wallet = DcrConstants.getInstance().wallet

    override fun onClick(v: View?) {
        val wallet = DcrConstants.getInstance().wallet
        val pd = Utils.getProgressDialog(this, false, false, "Processing...")
        pd.show()
        Thread {
            run {
                try {
                    val passPhrase = intent.getStringExtra(Constants.PASSPHRASE).toByteArray(Charsets.UTF_8)
                    val serializedTx = wallet.sendTransaction(passPhrase, destinationAddress, txAmount, srcAccount, requiredConfs, sendAll)
                    val hashList = ArrayList<Byte>()
                    for (aSerializedTx in serializedTx) {
                        hashList.add(aSerializedTx)
                    }
                    hashList.reverse()
                    val sb = StringBuilder()
                    for (b in hashList) {
                        sb.append(String.format(Locale.getDefault(), "%02x", b))
                    }
                    runOnUiThread {
                        pd.dismiss()
                        showTxSuccessDialog(sb.toString())
                    }
                }catch (e: Exception){
                    e.printStackTrace()
                    runOnUiThread {
                        pd.dismiss()
                        Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }.start()
    }

    private fun showTxSuccessDialog(txHash: String) {
        val dialogBuilder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.tx_confrimation_display, null)
        dialogBuilder.setCancelable(false)
        dialogBuilder.setView(dialogView)

        val txHashtv = dialogView.findViewById<TextView>(R.id.tx_hash_confirm_view)
        txHashtv.text = txHash

        txHashtv.setOnClickListener { Utils.copyToClipboard(this, txHashtv.text.toString(), getString(R.string.tx_hash_copy)) }

        dialogBuilder.setTitle("Transaction was successful")
        dialogBuilder.setPositiveButton("OKAY") { dialog, _ ->
            dialog.dismiss()
        }

        dialogBuilder.setNeutralButton("VIEW ON DCRDATA") { dialog, _ ->
            dialog.dismiss()
            val url = "https://testnet.dcrdata.org/tx/$txHash"
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(browserIntent)
        }

        dialogBuilder.setOnDismissListener {
            setResult(Activity.RESULT_OK)
            finish()
        }

        val b = dialogBuilder.create()
        b.show()
        b.getButton(DialogInterface.BUTTON_NEUTRAL).setTextColor(Color.BLUE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_confirm_transaction)
        findViewById<Button>(R.id.btn_confirm_tx).setOnClickListener(this)
        val amount = findViewById<TextView>(R.id.amount)
        val address = findViewById<TextView>(R.id.address)
        val totalAmount = findViewById<TextView>(R.id.total_amount)
        val fee = findViewById<TextView>(R.id.fee)

        destinationAddress = intent.getStringExtra(Constants.ADDRESS)
        address.text = "to $destinationAddress"

        txAmount = intent.getLongExtra(Constants.AMOUNT, 0)

        srcAccount = intent.getIntExtra(Constants.ACCOUNT_NUMBER, 0)

        requiredConfs = intent.getIntExtra(Constants.CONFIRMATIONS, 0)

        sendAll = intent.getBooleanExtra(Constants.SENDALL, false)

        val unsignedTransaction = wallet.constructTransaction(destinationAddress, txAmount, srcAccount, requiredConfs, sendAll)

        amount.text = CoinFormat.format(txAmount)

        val estFee = Utils.signedSizeToAtom(unsignedTransaction.estimatedSignedSize)

        val format = DecimalFormat()
        format.applyPattern("#.########")

        fee.text = CoinFormat.format("${format.format(Mobilewallet.amountCoin(estFee))} DCR added as a transaction fee (${format.format(unsignedTransaction.estimatedSignedSize / 1024F)} kB)")

        totalAmount.text = CoinFormat.format(Mobilewallet.amountCoin(txAmount + estFee))

    }
}

