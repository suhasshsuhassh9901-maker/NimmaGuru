package com.nimmaguru.ui.base

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.nimmaguru.LocaleManager
import com.nimmaguru.R

/**
 * Every Activity extends BaseActivity.
 * It injects the locale and wires the global language toggle button.
 */
abstract class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    /**
     * Call this after setContentView to attach the language toggle
     * found in each layout as R.id.btnLangToggle.
     */
    protected fun attachLangToggle() {
        val btn = findViewById<TextView>(R.id.btnLangToggle) ?: return
        updateToggleLabel(btn)
        btn.setOnClickListener {
            val current = LocaleManager.getLanguage(this)
            val next    = if (current == LocaleManager.LANG_EN) LocaleManager.LANG_KN
                          else LocaleManager.LANG_EN
            LocaleManager.setLanguage(this, next)
            // Restart the current activity to apply locale change
            val intent = Intent(this, this::class.java).apply {
                putExtras(this@BaseActivity.intent ?: Intent())
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            finish()
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun updateToggleLabel(btn: TextView) {
        val isEn = LocaleManager.getLanguage(this) == LocaleManager.LANG_EN
        btn.text = if (isEn) "ಕನ್ನಡ" else "EN"
        btn.contentDescription = if (isEn) "Switch to Kannada" else "Switch to English"
    }
}