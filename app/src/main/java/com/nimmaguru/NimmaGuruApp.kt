package com.nimmaguru

import android.app.Application
import android.content.Context

class NimmaGuruApp : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleManager.wrap(base))
    }
}