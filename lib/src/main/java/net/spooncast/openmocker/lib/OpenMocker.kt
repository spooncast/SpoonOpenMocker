package net.spooncast.openmocker.lib

import android.content.Context
import android.content.Intent
import net.spooncast.openmocker.lib.interceptor.OpenMockerInterceptor
import net.spooncast.openmocker.lib.ui.OpenMockerActivity

object OpenMocker {

    fun getInterceptor(): OpenMockerInterceptor {
        return OpenMockerInterceptor.Builder().build()
    }

    fun show(context: Context) {
        val intent = Intent(context, OpenMockerActivity::class.java)
        context.startActivity(intent)
    }
}