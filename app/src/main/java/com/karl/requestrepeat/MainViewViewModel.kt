package com.karl.requestrepeat

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*

import retry.ViewModelFailed
import retry.initRetry
import java.lang.RuntimeException

class MainViewViewModel : ViewModel(), ViewModelFailed {
    val liveData: MutableLiveData<BaseData> = MutableLiveData()


    fun request(num: Int, repeat: Int = 0) {
        liveData.value = BaseData.loading()
        viewModelScope.launch(initRetry(repeat > 0) {
            request(num,repeat - 1)
        }) {
            liveData.value = BaseData.success(simulateHttp(num))
        }
    }

    /***
     * 模拟网络请求
     * 产生随机数，为偶数模拟请求失败
     * 为奇数时则返回和调用者传递进来的数字相加并返回
     */
    private suspend fun simulateHttp(num: Int) = withContext(Dispatchers.IO) {
        delay(1000)
        (0..10).random().run {
            if (this % 2 == 0) {
                throw RuntimeException("哎呀，出错了")
            }
            plus(num)
        }

    }

    override fun requestFailed(throwable: Throwable, callback: () -> Unit) {
        liveData.value = BaseData.failed(Data(throwable, callback))
    }

    override fun requestCancel() {
        liveData.value = BaseData.cancel()
    }

    override fun onRetry() {
        liveData.value = BaseData.retry()
    }

}
