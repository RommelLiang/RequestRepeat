package retry

import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

class RetryCallback(val callback: () -> Unit) :
    AbstractCoroutineContextElement(RetryCallback) {
    companion object Key : CoroutineContext.Key<RetryCallback>
}


interface ViewModelFailed {
    fun requestFailed(throwable: Throwable, callback: () -> Unit)
    fun requestCancel()
    fun onRetry()
}

fun ViewModelFailed.initRetry(autoReTry: Boolean, callback: () -> Unit) =
    CoroutineExceptionHandler { coroutineContext, throwable ->
        val retryCallBack = {
            coroutineContext[RetryCallback]
                ?.callback?.invoke()
        }
        Log.e("autoReTry:------","$autoReTry")
        if (autoReTry) {
            onRetry()
            retryCallBack.invoke()
        } else {
            requestFailed(throwable) {
                retryCallBack
            }
        }
    } + RetryCallback(callback)