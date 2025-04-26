package qzwx.app.qtodo

import android.app.*
import android.content.*

class QZWXApplication : Application() {
    companion object {
        private lateinit var context : Context

        fun getContext() : Context {
            return context
        }
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
    }
}
