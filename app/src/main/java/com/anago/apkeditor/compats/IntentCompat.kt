package com.anago.apkeditor.compats

import android.content.Intent
import android.os.Build
import java.io.Serializable

object IntentCompat {
    inline fun <reified T : Serializable> Intent.getCSerializableExtra(name: String): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getSerializableExtra(name, T::class.java)
        } else {
            @Suppress("DEPRECATION") getSerializableExtra(name) as? T?
        }
    }
}