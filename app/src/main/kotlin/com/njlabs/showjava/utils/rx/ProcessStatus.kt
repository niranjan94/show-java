package com.njlabs.showjava.utils.rx

class ProcessStatus<T> {

    val status: String?
    val secondaryStatus: String?
    val progress: Float
    val result: T?

    constructor(progress: Float, status: String, secondaryStatus: String) {
        this.progress = progress
        this.result = null
        this.status = status
        this.secondaryStatus = secondaryStatus
    }

    constructor(progress: Float, status: String) {
        this.progress = progress
        this.result = null
        this.status = status
        this.secondaryStatus = ""
    }

    constructor(result: T) {
        this.progress = 1f
        this.status = "Finalising …"
        this.secondaryStatus = ""
        this.result = result
    }

    val isDone: Boolean
        get() = result != null
}
