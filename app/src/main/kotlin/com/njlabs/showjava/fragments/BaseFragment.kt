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
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import com.google.firebase.analytics.FirebaseAnalytics
import com.njlabs.showjava.activities.ContainerActivity
import com.njlabs.showjava.utils.UserPreferences
import io.reactivex.disposables.CompositeDisposable

abstract class BaseFragment<T : ViewModel> : Fragment(), SearchView.OnQueryTextListener, SearchView.OnCloseListener {
    protected open val viewModelClass: Class<T>? = null
    protected abstract val layoutResource: Int
    abstract fun init(savedInstanceState: Bundle?)

    protected lateinit var viewModel: T
    protected val disposables = CompositeDisposable()

    protected lateinit var userPreferences: UserPreferences
    protected lateinit var firebaseAnalytics: FirebaseAnalytics

    protected lateinit var containerActivity: ContainerActivity

    protected var menu: Menu? = null

    fun withMenu(menu: Menu?): BaseFragment<T> {
        this.menu = menu
        return this
    }

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
        viewModelClass?.let {
            viewModel = ViewModelProviders.of(this).get(it)
        }
        containerActivity = activity as ContainerActivity
        userPreferences = containerActivity.userPreferences
        firebaseAnalytics = containerActivity.firebaseAnalytics

        init(savedInstanceState)
    }

    fun finish() {
        containerActivity.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return true
    }

    override fun onQueryTextChange(query: String?): Boolean {
        return true
    }

    override fun onClose(): Boolean {
        return true
    }
}