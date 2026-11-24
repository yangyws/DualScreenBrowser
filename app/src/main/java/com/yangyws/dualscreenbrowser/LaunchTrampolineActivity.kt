package com.yangyws.dualscreenbrowser

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.Display

class LaunchTrampolineActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val targetIntent = Intent(this, MainActivity::class.java).apply {
            action = intent?.action
            data = intent?.data
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val options = ActivityOptions.makeBasic()
            .setLaunchDisplayId(Display.DEFAULT_DISPLAY)
        startActivity(targetIntent, options.toBundle())
        finish()
    }
}
