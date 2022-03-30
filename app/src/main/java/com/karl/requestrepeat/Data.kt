package com.karl.requestrepeat

data class BaseData(val result: Triple<Status, Data?, Int?>) {
    companion object {
        fun success(value: Int) = BaseData(Triple(Status.SUCCESS, null, value))
        fun failed(value: Data) = BaseData(Triple(Status.FAILED, value, null))
        fun loading() = BaseData(Triple(Status.LOADING, null, null))
        fun cancel() = BaseData(Triple(Status.CANCEL, null, null))
        fun retry() = BaseData(Triple(Status.RETRY, null, null))
    }
}

data class Data(val throwable: Throwable?, val callback: (() -> Unit)?)
enum class Status {
    NORMAL,
    LOADING,
    SUCCESS,
    FAILED,
    CANCEL,
    RETRY
}
