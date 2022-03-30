package retry

import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

class RetryCallback(val callback: () -> Unit) : AbstractCoroutineContextElement(RetryCallback) {
    companion object Key : CoroutineContext.Key<RetryCallback>
}


interface ViewModelFailed {
    /**
     * @param throwable:异常信息
     * @param callback:需要重试的函数
     * */
    fun requestFailed(throwable: Throwable, callback: () -> Unit)
    fun requestCancel()
    /**
     * 请求正在重试逻辑*/
    fun onRetry()
}


/**
 * @param autoReTry:是否自动重试
 * @param callback:需要重试的函数
 * */
fun ViewModelFailed.initRetry(autoReTry: Boolean = false, callback: () -> Unit) =
    CoroutineExceptionHandler { coroutineContext, throwable ->
        val retryCallBack = {
            coroutineContext[RetryCallback]
                ?.callback?.invoke()
        }
        if (autoReTry) {
            //自动开始重试逻辑
            onRetry()
            retryCallBack.invoke()
        } else {
            //不自动开始重试，后续操作交给用户决定
            requestFailed(throwable) {
                retryCallBack
            }
        }
    } + RetryCallback(callback)