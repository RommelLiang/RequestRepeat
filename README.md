在Android开发中有一个典型场景：网络请求失败后重试：一般的逻辑是弹出一个Dialog提醒用户“网络请求失败”，并提供重试的按钮。

![](https://raw.githubusercontent.com/RommelLiang/RequestRepeat/main/img/821648629261_.pic.jpg)

如果当前页面只有一个网络请求，那么逻辑就很简单了：只需要再调用一下发起这个网络请求的方法就可以了。而当一个页面有多个网络请求时，我常用的办法为失败回调加状态，根据不同的状态调用不同的方法。但是这个方法不免有些繁琐，也有点不安全。首先，你要额外的增加状态，并将它传来传去。有些情况下，你甚至还需要重新初始化网络请求参数。更要命的是：你还要管理这个状态，一旦管理不善，就会导致调用了不该调用的方法，引入严重的BUG。

直到有一天我看到`CoroutineExceptionHandler`，灵光突现——可以使用协程上下文（可在我之前的博客[Kotlin中的协程、上下文和作用域](https://juejin.cn/post/7068901166456766472)中了解更过关于协程和上下文的信息）来保存将来可能需要重试的网络请求和Request数据，这样就能解决上面的问题了。

由于我所开发的大多数项目都是采用ViewModel实现网络请求逻辑和UI层的解耦，而网络请求基本上是采用Coroutine+Retrofit的方式实现的，基本上都是使用viewModelScope。

```
viewModelScope.launch() { 
	request()
}
```
viewModelScope本质上是一个ViewModel的扩展函数，利用它可以便捷的在ViewModel创建协程，具体的代码就不展开了。默认情况下，它的CoroutineContext由Job和CoroutineDispatcher组成。而协程的上下文本质上就是一个实现了key-value访问的方式的链表结构。我们可以通过继承`AbstractCoroutineContextElement`的方式实现自定义的CoroutineContext上下文：

```
class RetryCallback(val callback: () -> Unit) : AbstractCoroutineContextElement(RetryCallback) {
    companion object Key : CoroutineContext.Key<RetryCallback>
}
```

紧接着，当网络请求发生异常时借助`CoroutineExceptionHandler`获取到我们需要重新执行的操作：

```
val coroutineExceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
     val callback = coroutineContext[RetryCallback]
        ?.callback
}
```
紧接着，要将coroutineExceptionHandler添加到发起网络请求的协程上下文里：

```
viewModelScope.launch(exceptionHandler
      + RetryCallback { request() }) { 
	request()
}
```

此时，只要在发起网络请求的页面里获取到`callback`，并在点击重试按钮的时候调用它，就能实现重试的逻辑。

进一步对它进行封装并增加失败后自动重试逻辑，创建供ViewModel使用的接口，用来处理网络请求错误的后续逻辑：

```
interface ViewModelFailed {
    /**
     * @param throwable:异常信息
     * @param callback:需要重试的函数
     * */
    fun requestFailed(throwable: Throwable, callback: () -> Unit)
}
```

为它创建扩展函数，用来创建`CoroutineExceptionHandler`和`RetryCallback`上下文实例：

```
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
```

ViewModel需要实现ViewModelFailed接口，并在发起网络请求的协程中调用`initRetry`方法添加异常处理上下文：

```

class MainViewViewModel : ViewModel(), ViewModelFailed {
    val liveData: MutableLiveData<BaseData> = MutableLiveData()


      /**
     * @param num:用来演示Request请求数据
     * @param repeat:失败后自动重试的次数
     * */
    fun request(num: Int, repeat: Int = 0) {
        liveData.value = BaseData.loading()
        viewModelScope.launch(initRetry(repeat > 0) {
            request(num,repeat - 1)
        }) {
            liveData.value = BaseData.success(simulateHttp(num))
        }
    }

    private suspend fun simulateHttp(num: Int) = withContext(Dispatchers.IO) {
        //模拟网络请求
        ...
    }

    override fun requestFailed(throwable: Throwable, callback: () -> Unit) {
        //处理失败逻辑
        dialog()
        //重试
        callback.invoke()
    }
    
    override fun onRetry() {
        
    }

}
```

最后附上[完整代码和使用Demo](https://github.com/RommelLiang/RequestRepeat)

