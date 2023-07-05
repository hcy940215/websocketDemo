package com.eurigo.websocketutils

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
    }

    fun onClick1(view: View) {
        Intent(this, MainActivity::class.java)
            .also {
                startActivity(it)
            }
    }

    fun onClick2(view: View) {
        Intent(this, OkHttpActivity::class.java)
            .also {
                startActivity(it)
            }
    }
    fun onClick3(view: View) {
        Intent(this, Main1Activity::class.java)
            .also {
                startActivity(it)
            }
    }
}