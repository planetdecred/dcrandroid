package com.dcrandroid.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Toast
import com.dcrandroid.R
import com.dcrandroid.data.Constants
import com.dcrandroid.util.DcrConstants
import kotlinx.android.synthetic.main.confirm_seed_page.*
import java.util.*

class ConfirmSeedActivity : AppCompatActivity(), View.OnClickListener {

    private var seeds: MutableList<String>? = null
    private var tempSeeds: Set<String>? = null
    private var seed = ""
    private var adapter: ArrayAdapter<String>? = null
    private var restore: Boolean? = null
    private var confirmClicks = 0
    private var lastConfirmClick: Long = 0
    private val CLICK_THRESHOLD = 300 //millisecond
    private var clickThread: Thread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val decorView = window.decorView
            decorView.systemUiVisibility = WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }

        setContentView(R.layout.confirm_seed_page)

        autoCompleteSeed.setSingleLine(true)
        autoCompleteSeed.completionHint = getString(R.string.tap_to_select)

        button_confirm_seed.setOnClickListener(this)

        button_clear_seed.setOnClickListener { seed_display_confirm.text = "" }

        button_delete_seed.setOnClickListener {
            val enteredSeed = seed_display_confirm.text.toString().trim { it <= ' ' }.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val temp = ArrayList(Arrays.asList(*enteredSeed))
            if (temp.size > 0) {
                //remove last seed
                temp.removeAt(temp.size - 1)
                val sb = StringBuilder()
                for (i in temp.indices) {
                    sb.append(" ")
                    sb.append(temp[i])
                }
                seed_display_confirm.text = sb.toString().trim { it <= ' ' }
            } else {
                seed_display_confirm.text = ""
            }
        }

        autoCompleteSeed.setOnItemClickListener { parent, _, position, _ ->
            val s = parent.getItemAtPosition(position) as String
            seed_display_confirm.text = String.format("%s %s", seed_display_confirm.text.toString().trim { it <= ' ' }, s)
            autoCompleteSeed.setText("")
        }

        prepareData()
    }

    override fun onClick(v: View?) {
        var lastClick: Long = 0
        if (lastConfirmClick != 0L) {
            lastClick = System.currentTimeMillis() - lastConfirmClick
        }

        if (lastClick < CLICK_THRESHOLD && confirmClicks < 10) {
            confirmClicks++
            lastConfirmClick = System.currentTimeMillis()
            if (clickThread != null && clickThread!!.isAlive) {
                clickThread!!.interrupt()
            }

            clickThread = Thread {
                try {
                    Thread.sleep((CLICK_THRESHOLD + 5).toLong())
                    var lastClick: Long = 0
                    if (lastConfirmClick != 0L) {
                        lastClick = System.currentTimeMillis() - lastConfirmClick
                    }
                    if (lastClick > CLICK_THRESHOLD) {
                        runOnUiThread {
                            handleSingleTap()
                        }
                    }

                    confirmClicks = 0
                    lastConfirmClick = 0
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }

            clickThread!!.start()
        } else if (lastClick <= CLICK_THRESHOLD && confirmClicks == 10) {
            confirmClicks = 0
            lastConfirmClick = 0
            val enteredSeed = ""
            val i = Intent(this@ConfirmSeedActivity, EncryptWallet::class.java)
                    .putExtra(Constants.SEED, enteredSeed)
            startActivity(i)
        }

        lastConfirmClick = System.currentTimeMillis()
    }

    private fun handleSingleTap() {
        val enteredSeed = seed_display_confirm.text.toString().trim { it <= ' ' }
        if (!restore!!) {
            if (seed == enteredSeed) {
                val i = Intent(this@ConfirmSeedActivity, EncryptWallet::class.java)
                        .putExtra(Constants.SEED, enteredSeed)
                startActivity(i)
            } else {
                Toast.makeText(this@ConfirmSeedActivity, R.string.incorrect_seed_input, Toast.LENGTH_SHORT).show()
            }
        } else {
            val constants = DcrConstants.getInstance()
            if (constants.wallet.verifySeed(enteredSeed)) {
                val i = Intent(this@ConfirmSeedActivity, EncryptWallet::class.java)
                        .putExtra(Constants.SEED, enteredSeed)
                startActivity(i)
            } else {
                Toast.makeText(this@ConfirmSeedActivity, R.string.incorrect_seed_input, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun prepareData() {
        val i = intent
        val b = i.extras
        if (b != null) {
            seed = b.getString(Constants.SEED)!!.trim { it <= ' ' }
            restore = b.getBoolean(Constants.RESTORE)
            seeds = ArrayList()
            val seedsArray = seed.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            tempSeeds = HashSet(Arrays.asList(*seedsArray))
            val list = ArrayList(tempSeeds!!)
            seeds!!.addAll(Arrays.asList(*seedsArray))
            if (restore!!) {
                Collections.sort(seeds, SortIgnoreCase())
            } else {
                seeds!!.shuffle()
            }
            adapter = ArrayAdapter(this,
                    android.R.layout.simple_dropdown_item_1line, list)
            autoCompleteSeed.setAdapter<ArrayAdapter<String>>(adapter)
        } else {
            Toast.makeText(this, R.string.error_bundle_null, Toast.LENGTH_SHORT).show()
        }
    }

    inner class SortIgnoreCase : Comparator<String> {
        override fun compare(s: String, t1: String): Int {
            return s.toLowerCase().compareTo(t1.toLowerCase())
        }
    }
}