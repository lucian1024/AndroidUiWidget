package com.lucian.androiduiwidget

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.lucian.androiduiwidget.databinding.ActivityOtpViewBinding
import com.lucian.otpview.OtpView

class OtpViewActivity : AppCompatActivity() {
    private var binding: ActivityOtpViewBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_otp_view)
        binding?.otpview?.setOnCompleteListener { content ->
            if ("12345" == content) {
                binding?.otpview?.setState(OtpView.SUCCESS_STATE)
            } else {
                binding?.otpview?.setState(OtpView.ERROR_STATE)
            }
        }
    }
}
