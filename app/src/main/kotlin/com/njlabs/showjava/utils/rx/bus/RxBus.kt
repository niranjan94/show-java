package com.njlabs.showjava.utils.rx.bus

import com.jakewharton.rxrelay2.PublishRelay

import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable

class RxBus private constructor() {

    private val _bus = PublishRelay.create<Any>().toSerialized()

    fun send(o: Any) {
        _bus.accept(o)
    }

    fun asFlowable(): Flowable<Any> {
        return _bus.toFlowable(BackpressureStrategy.LATEST)
    }

    fun hasObservers(): Boolean {
        return _bus.hasObservers()
    }

    private object SingletonHolder {
        val INSTANCE = RxBus()
    }

    companion object {
        val instance: RxBus by lazy { SingletonHolder.INSTANCE }
    }
}
