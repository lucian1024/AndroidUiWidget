package com.lucian.androiduiwidget

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.lucian.androiduiwidget.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private var binding: ActivityMainBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    fun onClick(v: View) {
        when (v.id) {
            R.id.otpview_btn -> {
                val intent = Intent(this, OtpViewActivity::class.java)
                startActivity(intent)
            }

            else -> {}
        }
    }
}
