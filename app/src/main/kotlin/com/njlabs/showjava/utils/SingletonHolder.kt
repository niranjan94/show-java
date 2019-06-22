package com.njlabs.showjava.utils

/*
 * Code borrowed from https://medium.com/@BladeCoder/kotlin-singletons-with-argument-194ef06edd9e
 * Copyright (C) 2018 Christophe Beyls
 */
open class SingletonHolder<out T, in A>(creator: (A) -> T) {
    private var creator: ((A) -> T)? = creator
    @Volatile private var instance: T? = null

    fun getInstance(arg: A? = null): T {
        val i = instance
        if (i != null) {
            return i
        }
        return synchronized(this) {
            val i2 = instance
            if (i2 != null) {
                i2
            } else {
                val created = creator!!(arg!!)
                instance = created
                creator = null
                created
            }
        }
    }

    fun init(arg: A? = null) {
        getInstance(arg)
    }
}
