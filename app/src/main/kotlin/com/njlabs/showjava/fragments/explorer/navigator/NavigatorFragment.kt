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

package com.njlabs.showjava.fragments.explorer.navigator

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.njlabs.showjava.R
import com.njlabs.showjava.data.FileItem
import com.njlabs.showjava.data.SourceInfo
import com.njlabs.showjava.fragments.BaseFragment
import com.njlabs.showjava.fragments.explorer.navigator.adapters.FilesListAdapter
import com.njlabs.showjava.fragments.explorer.viewer.CodeViewerFragment
import com.njlabs.showjava.fragments.explorer.viewer.ImageViewerFragment
import com.njlabs.showjava.utils.ktx.bundleOf
import com.njlabs.showjava.utils.ktx.sourceDir
import kotlinx.android.synthetic.main.fragment_navigator.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File


class NavigatorFragment : BaseFragment<NavigatorViewModel>() {
    override val viewModel by viewModels<NavigatorViewModel>()

    override val layoutResource = R.layout.fragment_navigator

    private lateinit var filesListAdapter: FilesListAdapter
    private var currentDirectory: File? = null
    private var sourceArchive: File? = null

    private var fileItems: ArrayList<FileItem> = ArrayList()
    private var selectedApp: SourceInfo? = null

    override fun init(savedInstanceState: Bundle?) {
        selectedApp = arguments?.getParcelable("selectedApp")
        if (savedInstanceState != null) {
            savedInstanceState.getParcelableArrayList<FileItem>("fileItems")?.let {
                fileItems = it
            }
            selectedApp = selectedApp ?: savedInstanceState.getParcelable("selectedApp")
            val currentDirectoryString = savedInstanceState.getString("currentDirectory")
            currentDirectoryString?.let {
                currentDirectory = File(it)
            }
        }

        currentDirectory = currentDirectory ?: selectedApp?.sourceDirectory
        setupList()
        filesListAdapter.updateData(fileItems)
        currentDirectory?.let { populateList(it) }

        swipeRefresh.setOnRefreshListener {
            currentDirectory?.let { populateList(it) }
        }
    }

    private fun setListVisibility(isListVisible: Boolean = true) {
        val listGroupVisibility = if (isListVisible) View.VISIBLE else View.GONE
        filesList.visibility = listGroupVisibility
    }

    private fun populateList(startDirectory: File) {
        currentDirectory = startDirectory
        updateToolbarTitle()
        swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            val listItems: ArrayList<FileItem> = try {
                viewModel.loadFiles(startDirectory)
            } catch (e: Exception) {
                Timber.e(e)
                Toast.makeText(context, R.string.errorLoadingFiles, Toast.LENGTH_SHORT).show()
                arrayListOf()
            }
            swipeRefresh.isRefreshing = false
            updateList(listItems)
        }
    }

    private fun updateList(fileItems: ArrayList<FileItem>?) {
        if (fileItems != null) {
            this.fileItems = fileItems
            filesListAdapter.updateData(fileItems)
            if (fileItems.isEmpty()) {
                setListVisibility(false)
            } else {
                setListVisibility(true)
            }

        }
    }

    private fun setupList() {
        filesList.setHasFixedSize(true)
        filesList.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        filesListAdapter = FilesListAdapter(fileItems) { selectedFile ->
            if (selectedFile.file.isDirectory) {
                populateList(selectedFile.file)
            } else {
                when {
                    arrayOf("jpeg", "jpg", "png").contains(selectedFile.file.extension) -> {
                        containerActivity.gotoFragment(
                            ImageViewerFragment(), bundleOf(
                                "filePath" to selectedFile.file.canonicalPath,
                                "name" to selectedApp?.packageName
                            )
                        )
                    }
                    arrayOf(
                        "java", "xml", "json", "txt", "properties",
                        "yml", "yaml", "md", "html", "class",
                        "js", "css", "scss", "sass"
                    ).contains(selectedFile.file.extension) -> {
                        containerActivity.gotoFragment(
                            CodeViewerFragment(), bundleOf(
                                "filePath" to selectedFile.file.canonicalPath,
                                "name" to selectedApp?.packageName
                            )
                        )
                    }
                    else -> {
                        context?.let {
                            val mimeTypeDetector = MimeTypeMap.getSingleton()
                            val fileIntent = Intent(Intent.ACTION_VIEW)
                            val mimeType =
                                mimeTypeDetector.getMimeTypeFromExtension(selectedFile.file.extension)
                            fileIntent.setDataAndType(
                                FileProvider.getUriForFile(
                                    it,
                                    it.applicationContext.packageName + ".provider",
                                    selectedFile.file
                                ),
                                mimeType
                            )
                            fileIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            fileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            try {
                                it.startActivity(fileIntent)
                            } catch (e: ActivityNotFoundException) {
                                Toast.makeText(
                                    context,
                                    getString(R.string.noSupportedHandlerForFileType),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }

                    }
                }
            }
        }
        filesList.adapter = filesListAdapter
        updateList(fileItems)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (fileItems.size <= 1000) {
            outState.putParcelableArrayList("fileItems", fileItems)
        }
        selectedApp?.let {
            outState.putParcelable("selectedApp", it)
        }
        currentDirectory?.let {
            outState.putString("currentDirectory", it.canonicalPath)
        }
    }

    private fun showProgressView() {
        ioProgress.visibility = View.VISIBLE
        filesList.visibility = View.GONE
    }

    private fun hideProgressView() {
        ioProgress.visibility = View.GONE
        filesList.visibility = View.VISIBLE
    }

    /**
     * Check if the current folder the user is in, is the root
     */
    private fun isAtRoot(): Boolean {
        return currentDirectory?.canonicalPath == selectedApp?.sourceDirectory?.canonicalPath
    }

    private fun shareArchive(file: File?) {
        if (file == null) {
            Toast.makeText(context, R.string.genericError, Toast.LENGTH_SHORT).show()
            return
        }

        context?.let {
            hideProgressView()
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.setDataAndType(
                FileProvider.getUriForFile(
                    it,
                    it.applicationContext.packageName + ".provider",
                    file
                ),
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)
            )
            startActivity(
                Intent.createChooser(
                    shareIntent,
                    getString(R.string.sendSourceVia)
                )
            )
        }
    }

    private fun goBack(): Boolean {
        if (isAtRoot()) {
            finish()
            return true
        }
        currentDirectory?.parent?.let {
            populateList(File(it))
            return true
        }
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                if (goBack()) {
                    return true
                }
            }
            R.id.save_code -> {

            }
            R.id.share_code -> {
                sourceArchive?.let {
                    shareArchive(it)
                    return true
                }
                showProgressView()
                lifecycleScope.launch {
                    sourceArchive = try {
                        viewModel.archiveDirectory(
                            selectedApp!!.sourceDirectory, selectedApp!!.packageName
                        )
                    } catch (e: Exception) {
                        Timber.e(e)
                        null
                    }
                    shareArchive(sourceArchive)
                }

                return true

            }
            R.id.delete_code -> {
                AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.deleteSource))
                    .setMessage(getString(R.string.deleteSourceConfirm))
                    .setIcon(R.drawable.ic_error_outline_black)
                    .setPositiveButton(
                        android.R.string.yes
                    ) { _, _ ->
                        deleteSource()
                    }
                    .setNegativeButton(android.R.string.no, null)
                    .show()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }


    private fun deleteSource() {
        selectedApp?.let {
            showProgressView()
            lifecycleScope.launch {
                viewModel.deleteDirectory(it.sourceDirectory)
                Toast.makeText(
                    context,
                    getString(R.string.sourceDeleted),
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private fun updateToolbarTitle() {
        containerActivity.supportActionBar?.title = selectedApp?.packageLabel
        containerActivity.setSubtitle(selectedApp?.packageName)
        if (isAtRoot()) {
            containerActivity.setSubtitle(selectedApp?.packageName)
        } else {
            containerActivity.setSubtitle(
                currentDirectory?.canonicalPath?.replace(
                    sourceDir(selectedApp?.packageName!!).canonicalPath,
                    ""
                )
            )
        }
    }

    override fun onSetToolbar(menu: Menu) {
        super.onSetToolbar(menu)
        menu.findItem(R.id.share_code).isVisible = true
        menu.findItem(R.id.delete_code).isVisible = true
        menu.findItem(R.id.save_code).isVisible = true
        updateToolbarTitle()
    }

    override fun onResetToolbar(menu: Menu) {
        super.onResetToolbar(menu)
        menu.findItem(R.id.share_code).isVisible = false
        menu.findItem(R.id.delete_code).isVisible = false
        menu.findItem(R.id.save_code).isVisible = false
    }

    override fun onBackPressed(): Boolean {
        if (isAtRoot()) {
            return false
        }
        return goBack()
    }
}