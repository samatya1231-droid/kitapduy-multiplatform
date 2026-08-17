package com.semtinazizi.kitapduy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.semtinazizi.kitapduy.platform.DailyQuoteScheduler

class MainActivity : ComponentActivity() {
    companion object {
        var instance: MainActivity? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
        enableEdgeToEdge()
        
        DailyQuoteScheduler.scheduleDaily(this)
        
        setContent {
            App()
        }
    }

    override fun onDestroy() {
        if (instance == this) instance = null
        super.onDestroy()
    }
}
