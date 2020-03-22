/*
 * Show Java - A java/apk decompiler for android
 * Copyright (c) 2019 Niranjan Rajendran
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

import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.njlabs.showjava.R
import com.njlabs.showjava.activities.ContainerActivity
import com.njlabs.showjava.utils.UserPreferences
import androidx.transition.TransitionInflater


abstract class BaseFragment<T : ViewModel> : Fragment(), SearchView.OnQueryTextListener, SearchView.OnCloseListener {

    protected abstract val layoutResource: Int
    abstract fun init(savedInstanceState: Bundle?)

    protected abstract val viewModel: T

    protected lateinit var userPreferences: UserPreferences
    protected lateinit var firebaseAnalytics: FirebaseAnalytics

    protected lateinit var containerActivity: ContainerActivity

    protected open var isBlack: Boolean = false
    protected var menu: Menu? = null
    private var originalTitle: String? = null
    private var originalSubtitle: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            sharedElementEnterTransition =
                    TransitionInflater.from(context).inflateTransition(android.R.transition.move)
        }
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
        menu?.let {
            onSetToolbar(it)
        }
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
        containerActivity = activity as ContainerActivity
        userPreferences = containerActivity.userPreferences
        firebaseAnalytics = containerActivity.firebaseAnalytics
        setHasOptionsMenu(true)

        if (savedInstanceState != null) {
            originalTitle = savedInstanceState.getString("originalTitle")
            originalSubtitle = savedInstanceState.getString("originalSubtitle")
        } else {
            originalTitle = containerActivity.supportActionBar?.title?.toString()
            originalSubtitle = containerActivity.supportActionBar?.subtitle?.toString()
        }

        init(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("originalTitle", originalTitle)
        outState.putString("originalSubtitle", originalSubtitle)
    }

    fun finish() {
        containerActivity.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        containerActivity.supportActionBar?.title = originalTitle
        containerActivity.setSubtitle("")
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        this.menu = menu
        onSetToolbar(menu)
    }

    override fun onPause() {
        super.onPause()
        containerActivity.supportActionBar?.title = originalTitle
        containerActivity.setSubtitle("")
        menu?.let {
            onResetToolbar(it)
        }
    }

    protected open fun setDecor(overrideIsBlack: Boolean = isBlack) {
        if (overrideIsBlack) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                containerActivity.window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.grey_900_darker)
                containerActivity.window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            }
            containerActivity.toolbar.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.grey_900))
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                containerActivity.window
                    .setFlags(
                        WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                        WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    )
            }
            containerActivity.toolbar.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
        }
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

    open fun onBackPressed(): Boolean {
        return false
    }

    open fun onSetToolbar(menu: Menu) {

    }

    open fun onResetToolbar(menu: Menu) {
    }
}