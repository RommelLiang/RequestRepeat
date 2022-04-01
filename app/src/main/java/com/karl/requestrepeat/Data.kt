package com.karl.requestrepeat

//回调数据构建
data class BaseData(val result: Triple<Status, Data?, Int?>) {
    companion object {
        fun success(value: Int) = BaseData(Triple(Status.SUCCESS, null, value))
        fun failed(value: Data) = BaseData(Triple(Status.FAILED, value, null))
        fun loading() = BaseData(Triple(Status.LOADING, null, null))
        fun cancel() = BaseData(Triple(Status.CANCEL, null, null))
        fun retry() = BaseData(Triple(Status.RETRY, null, null))
    }
}

//错误信息数据类型
data class Data(val throwable: Throwable?, val callback: (() -> Unit)?)

//请求状态处理
enum class Status {
    NORMAL,
    LOADING,
    SUCCESS,
    FAILED,
    CANCEL,
    RETRY
}
