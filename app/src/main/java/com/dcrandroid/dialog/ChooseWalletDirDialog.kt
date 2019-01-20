package com.dcrandroid.dialog

import android.annotation.TargetApi
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Build
import androidx.core.content.ContextCompat
import com.dcrandroid.R
import com.dcrandroid.util.PreferenceUtil
import com.dcrandroid.util.Utils
import java.io.File


object ChooseWalletDirDialog{

    @TargetApi(19)
public fun diplayDialogue(context : Context) {
    val builder = AlertDialog.Builder(context)
    val dirTypes = mutableListOf<String>()
        val dirCommonNames = mutableListOf<String>()
    val dirTypeCommonNames = mutableListOf<String>()
    var currentDirIndex = 0


    val externalDirs = if (Build.VERSION.SDK_INT >= 19) {
        context.getExternalFilesDirs(null)
    } else {
        ContextCompat.getExternalFilesDirs(context, null)
    }
    val prefs = PreferenceUtil(context)
    var initialDirType = prefs.get(context.getString(R.string.key_wallet_dir_type))
        if(initialDirType.isEmpty()) {
            initialDirType = context.getString(R.string.wallet_dir_internal)
        }

    dirTypes.add(context.getString(R.string.wallet_dir_internal))
    dirTypeCommonNames.add(context.getString(R.string.wallet_dir_change_dialogue_internal))
        dirCommonNames.add(context.getString(R.string.hidden))
    dirTypes.add(context.getString(R.string.wallet_dir_external))
    dirTypeCommonNames.add(context.getString(R.string.wallet_dir_change_dialogue_external))
        dirCommonNames.add(externalDirs[0].toString() + "/wallet")
    if (externalDirs.size > 1) {
        dirTypes.add(context.getString(R.string.wallet_dir_external_removable))
        dirTypeCommonNames.add(context.getString(R.string.wallet_dir_change_dialogue_external_removable))
        dirCommonNames.add(externalDirs[1].toString() + context.getApplicationContext().getPackageName() + "/wallet")
    }

        val dirTypeCommonNamesOutArray  = Array(dirTypeCommonNames.size) {
            dirTypeCommonNames[it]
        }

    for (index in 0 until dirTypes.size) {
        if (dirTypes[index] == initialDirType)
            currentDirIndex = index
    }

        print("dir type " + dirTypeCommonNamesOutArray.toString())

    builder
            .setTitle(R.string.wallet_dir_change_dialoge_title)
            .setSingleChoiceItems(dirTypeCommonNamesOutArray, currentDirIndex) {_, index : Int ->
                currentDirIndex = index
            }
            .setNegativeButton(R.string.cancel) { dialogInterface : DialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .setPositiveButton(R.string.ok) { dialogInterface : DialogInterface, _ ->
             if (dirTypes[currentDirIndex] != initialDirType){
                 displayRestartDialogue(context, prefs, dirTypes[currentDirIndex], dirTypeCommonNames[currentDirIndex], dirCommonNames[currentDirIndex])
                     return@setPositiveButton
             }
                prefs.set(context.getString(R.string.key_wallet_dir_type), dirTypes[currentDirIndex])
                dialogInterface.dismiss()
            }
            .create()
            .show()
}

private fun displayRestartDialogue(context: Context , prefs : PreferenceUtil, dirType : String, dirTypeCommon : String, dirNameCommon : String) {
    val builder = AlertDialog.Builder(context)

    builder
            .setTitle(context.getString(R.string.wallet_dir_change_restart_dialoge_title))
            .setMessage(dirTypeCommon + "\n" + dirNameCommon)
            .setNegativeButton(R.string.cancel) { dialogInterface : DialogInterface, _ ->
                diplayDialogue(context)
            }
            .setPositiveButton(R.string.ok) { dialogInterface : DialogInterface, _ ->
                prefs.set(context.getString(R.string.key_wallet_dir_type), dirType)
                Utils.restartApp(context)
            }
            .create()
            .show()

}


}



