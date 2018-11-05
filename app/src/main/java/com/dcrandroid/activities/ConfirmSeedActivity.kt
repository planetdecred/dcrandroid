package com.dcrandroid.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import com.dcrandroid.R
import com.dcrandroid.adapter.ConfirmSeedAdapter
import com.dcrandroid.adapter.InputSeed
import com.dcrandroid.data.Constants
import com.dcrandroid.util.DcrConstants
import kotlinx.android.synthetic.main.confirm_seed_page.*

private const val CLICK_THRESHOLD = 300 //millisecond

class ConfirmSeedActivity : AppCompatActivity(), View.OnClickListener {

    private var seed = ""
    private var restore: Boolean = false
    private var allSeeds = ArrayList<String>()
    private lateinit var linearLayoutManager: LinearLayoutManager
    private val seedsForInput = ArrayList<InputSeed>()
    private val seedsFromAdapter = ArrayList<InputSeed>()
    private var sortedList = listOf<InputSeed>()
    private lateinit var seedAdapter: ConfirmSeedAdapter
    private var confirmClicks = 0
    private var lastConfirmClick: Long = 0
    private var clickThread: Thread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val decorView = window.decorView
            decorView.systemUiVisibility = WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
        setContentView(R.layout.confirm_seed_page)

        linearLayoutManager = LinearLayoutManager(this)
        recyclerViewSeeds.layoutManager = linearLayoutManager

        button_delete_seed.setOnClickListener {
            recyclerViewSeeds.removeAllViewsInLayout()
            recyclerViewSeeds.adapter = null
            sortedList = emptyList()
            seedsFromAdapter.clear()
            initAdapter()
            seedAdapter.notifyDataSetChanged()
        }

        button_confirm_seed.setOnClickListener(this)


        prepareData()
    }

    private fun prepareData() {
        val bundle = intent.extras
        val temp = arrayOfNulls<String>(33)
        if (!bundle.isEmpty) {
            var inputSeed: InputSeed
            seed = bundle.getString(Constants.SEED)
            restore = bundle.getBoolean(Constants.RESTORE)
            allSeeds = ArrayList(seed.split(" "))
            if (restore) {
                temp.forEachIndexed { number, _ ->
                    inputSeed = InputSeed(number, " ")
                    seedsForInput.add(inputSeed)
                }
            } else {
                allSeeds.forEachIndexed { number, seed ->
                    inputSeed = InputSeed(number, seed)
                    seedsForInput.add(inputSeed)
                }
            }
            initAdapter()
        }
    }

    private fun initAdapter() {
        seedAdapter = ConfirmSeedAdapter(seedsForInput.distinct(), allSeeds, applicationContext,
                { savedSeed: InputSeed ->
                    seedsFromAdapter.add(savedSeed)
                    sortedList = seedsFromAdapter.sortedWith(compareBy { it.number }).distinct()
                },
                { removeSeed: InputSeed ->
                    seedsFromAdapter.clear()
                    seedsFromAdapter.addAll(sortedList)
                    seedsFromAdapter.remove(removeSeed)
                    sortedList = seedsFromAdapter.sortedWith(compareBy { it.number }).distinct()
                })
        recyclerViewSeeds.adapter = seedAdapter
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
                            handleSingleTap(sortedList)
                        }
                    }

                    confirmClicks = 0
                    lastConfirmClick = 0
                } catch (e: Throwable) {
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

    private fun handleSingleTap(sortedList: List<InputSeed>) {
        val dcrConstants = DcrConstants.getInstance()
        val intent = Intent(this, EncryptWallet::class.java)
        val isAllSeeds = (sortedList.size == 33)

        if (sortedList.isNotEmpty() && isAllSeeds) {
            val finalSeedsString = sortedList.joinToString(" ", "", "", -1, "...") { it.phrase }
            val isVerifiedFromDcrConstants = restore && dcrConstants.wallet.verifySeed(finalSeedsString)
            val isTypedSeedsCorrect = !restore && seed == finalSeedsString

            if (isTypedSeedsCorrect || isVerifiedFromDcrConstants) {
                intent.putExtra(Constants.SEED, finalSeedsString)
                startActivity(intent)
            } else {
                Toast.makeText(applicationContext, R.string.incorrect_seed_input, Toast.LENGTH_SHORT).show()
            }

        } else if (sortedList.isNotEmpty() && !isAllSeeds) {
            Toast.makeText(applicationContext, getString(R.string.notAllSeedsEntered), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(applicationContext, getString(R.string.theInputFieldIsEmpty), Toast.LENGTH_SHORT).show()
        }
    }


}
