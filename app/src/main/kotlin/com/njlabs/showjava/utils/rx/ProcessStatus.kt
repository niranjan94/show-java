/*
 * Show Java - A java/apk decompiler for android
 * Copyright (c) 2018 Niranjan Rajendran
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
        this.status = ""
        this.secondaryStatus = ""
        this.result = result
    }

    val isDone: Boolean
        get() = result != null
}
