package com.baes.inspector.utils

import android.app.AlertDialog
import android.content.Context
import com.baes.inspector.R

object DialogManager {
  fun showLocEnableDialog(context: Context, listener: Listener){
        val build = AlertDialog.Builder(context)
        val dialog = build.create()
        dialog.setTitle(R.string.location_disabled)
        dialog.setMessage(context.getString(R.string.location_dialod_message))
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes"){
            _, _ -> listener.onClick()
        }
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "No"){
                _, _ -> dialog.dismiss()
        }
      dialog.show()
    }

    interface Listener{
        fun onClick()
    }
}