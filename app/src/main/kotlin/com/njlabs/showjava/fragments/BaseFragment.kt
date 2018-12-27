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

package com.njlabs.showjava.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import com.njlabs.showjava.activities.ContainerActivity
import io.reactivex.disposables.CompositeDisposable

abstract class BaseFragment<T : ViewModel> : Fragment() {
    protected abstract val viewModelClass: Class<T>
    protected abstract val layoutResource: Int
    abstract fun init(savedInstanceState: Bundle?)

    protected lateinit var viewModel: T
    protected val disposables = CompositeDisposable()

    private val containerActivity: ContainerActivity
        get() = activity as ContainerActivity

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(layoutResource, container, false)
    }

    override fun onResume() {
        super.onResume()
        containerActivity
            .firebaseAnalytics
            .setCurrentScreen(
                containerActivity,
                this::class.java.simpleName,
                this::class.java.simpleName
            )
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(viewModelClass)
        init(savedInstanceState)
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }
}