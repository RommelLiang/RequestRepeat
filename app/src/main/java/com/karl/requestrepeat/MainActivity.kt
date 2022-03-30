package com.karl.requestrepeat

import android.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import java.util.*

class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewViewModel by viewModels()

    private var status: Status = Status.NORMAL
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val tv = findViewById<TextView>(R.id.tv)
        val button1 = findViewById<Button>(R.id.button)
        val button2 = findViewById<Button>(R.id.retry)
        viewModel.liveData.observe(this) {
            when (it.result.first) {
                Status.LOADING -> {
                    if (status != Status.RETRY) {
                        tv.text = "正在加载中..."
                    }
                }
                Status.RETRY -> tv.text = "正在重试中..."
                Status.SUCCESS -> tv.text = "测试：${it.result.third}"
                Status.FAILED -> {
                    tv.text = "测试"
                    it.result.second?.let { it1 -> showDialog(it1) }
                }
                else -> tv.text = "测试"
            }
            status  = it.result.first

        }
        button1.setOnClickListener {
            if(status == Status.RETRY || status == Status.LOADING) return@setOnClickListener
            viewModel.request(8)
        }
        button2.setOnClickListener {
            if(status == Status.RETRY || status == Status.LOADING) return@setOnClickListener
            viewModel.request(8, repeat = 2)
        }
    }

    private fun showDialog(data: Data) {
        AlertDialog.Builder(this).setMessage("${data.throwable?.message}")
            .setPositiveButton(
                "重试"
            ) { _, _ ->
                data.callback?.invoke()
            }
            .setNegativeButton(
                "取消", null
            )
            .create()
            .show()

    }


}

