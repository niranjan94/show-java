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

package com.njlabs.showjava.fragments.explorer.navigator

import android.app.ProgressDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import com.njlabs.showjava.R
import com.njlabs.showjava.activities.explorer.viewer.CodeViewerActivity
import com.njlabs.showjava.activities.explorer.viewer.ImageViewerActivity
import com.njlabs.showjava.data.FileItem
import com.njlabs.showjava.data.SourceInfo
import com.njlabs.showjava.fragments.BaseFragment
import com.njlabs.showjava.fragments.explorer.navigator.adapters.FilesListAdapter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_navigator.*
import timber.log.Timber
import java.io.File

class NavigatorFragment: BaseFragment<NavigatorViewModel>() {
    override val viewModelClass = NavigatorViewModel::class.java

    override val layoutResource = R.layout.fragment_navigator

    private lateinit var filesListAdapter: FilesListAdapter
    private var currentDirectory: File? = null
    private var sourceArchive: File? = null

    private var zipProgressDialog: ProgressDialog? = null

    private var fileItems: ArrayList<FileItem> = ArrayList()
    private var selectedApp: SourceInfo? = null

    private var originalTitle: String? = null
    private var originalSubtitle: String? = null

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

            originalTitle = savedInstanceState.getString("originalTitle")
            originalSubtitle = savedInstanceState.getString("originalSubtitle")

        } else {
            originalTitle = containerActivity.supportActionBar?.title?.toString()
            originalSubtitle = containerActivity.supportActionBar?.subtitle?.toString()
        }

        containerActivity.supportActionBar?.title = selectedApp?.packageLabel
        containerActivity.setSubtitle(selectedApp?.packageName)
        currentDirectory = currentDirectory ?: selectedApp?.sourceDirectory
        setupList()
        filesListAdapter.updateData(fileItems)
        currentDirectory?.let { populateList(it) }

        swipeRefresh.setOnRefreshListener {
            currentDirectory?.let { populateList(it) }
        }

        setHasOptionsMenu(true)
    }

    private fun setListVisibility(isListVisible: Boolean = true) {
        val listGroupVisibility = if (isListVisible) View.VISIBLE else View.GONE
        filesList.visibility = listGroupVisibility
    }

    private fun populateList(startDirectory: File) {
        currentDirectory = startDirectory
        val packageName = selectedApp?.packageName
        if (isAtRoot()) {
            containerActivity.setSubtitle(packageName)
        } else {
            containerActivity.setSubtitle(
                startDirectory.canonicalPath.replace(
                    "${Environment.getExternalStorageDirectory()}/show-java/sources/$packageName/",
                    ""
                )
            )
        }
        swipeRefresh.isRefreshing = true
        disposables.add(viewModel.loadFiles(startDirectory)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .onErrorReturn {
                Timber.e(it)
                Toast.makeText(context, R.string.errorLoadingFiles, Toast.LENGTH_SHORT).show()
                ArrayList()
            }
            .subscribe {
                updateList(it)
                swipeRefresh.isRefreshing = false
            }
        )
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
                        val intent = Intent(context, ImageViewerActivity::class.java)
                        intent.putExtra("filePath", selectedFile.file.canonicalPath)
                        intent.putExtra("name", selectedApp?.packageName)
                        startActivity(intent)
                    }
                    arrayOf(
                        "java", "xml", "json", "txt", "properties",
                        "yml", "yaml", "md", "html", "class",
                        "js", "css", "scss", "sass"
                    ).contains(selectedFile.file.extension) -> {
                        val intent = Intent(context, CodeViewerActivity::class.java)
                        intent.putExtra("filePath", selectedFile.file.canonicalPath)
                        intent.putExtra("name", selectedApp?.packageName)
                        startActivity(intent)
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

    override fun onSaveInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)
        if (fileItems.size <= 1000) {
            bundle.putParcelableArrayList("fileItems", fileItems)
        }
        selectedApp?.let {
            bundle.putParcelable("selectedApp", it)
        }
        currentDirectory?.let {
            bundle.putString("currentDirectory", it.canonicalPath)
        }
        bundle.putString("originalTitle", originalTitle)
        bundle.putString("originalSubtitle", originalSubtitle)
    }

    private fun showProgressDialog() {
        if (zipProgressDialog == null) {
            zipProgressDialog = ProgressDialog(context)
            zipProgressDialog!!.isIndeterminate = false
            zipProgressDialog!!.setCancelable(false)
            zipProgressDialog!!.setInverseBackgroundForced(false)
            zipProgressDialog!!.setCanceledOnTouchOutside(false)
            zipProgressDialog!!.setMessage(getString(R.string.compressingSource))
        }
        zipProgressDialog!!.show()
    }

    private fun dismissProgressDialog() {
        if (zipProgressDialog != null && zipProgressDialog!!.isShowing) {
            zipProgressDialog!!.dismiss()
        }
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
            dismissProgressDialog()
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

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        menu?.let {
            it.findItem(R.id.share_code).isVisible = true
            it.findItem(R.id.delete_code).isVisible = true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                if (goBack()) {
                    return true
                }
            }
            R.id.share_code -> {
                sourceArchive?.let {
                    shareArchive(it)
                    return true
                }
                showProgressDialog()
                disposables.add(viewModel.archiveDirectory(
                    selectedApp!!.sourceDirectory, selectedApp!!.packageName
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .onErrorReturn {
                        Timber.e(it)
                        null
                    }
                    .subscribe {
                        sourceArchive = it
                        shareArchive(it)
                    }
                )
                return true

            }
            R.id.delete_code -> {
                AlertDialog.Builder(context!!)
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
            deleteProgress.visibility = View.VISIBLE
            filesList.visibility = View.GONE
            disposables.add(viewModel.deleteDirectory(it.sourceDirectory)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    Toast.makeText(
                        context,
                        getString(R.string.sourceDeleted),
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        menu?.let {
            it.findItem(R.id.share_code).isVisible = true
            it.findItem(R.id.delete_code).isVisible = true
        }
    }

    override fun onPause() {
        super.onPause()
        menu?.let {
            it.findItem(R.id.share_code).isVisible = false
            it.findItem(R.id.delete_code).isVisible = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        containerActivity.supportActionBar?.title = originalTitle
        containerActivity.setSubtitle(originalSubtitle)

    }

    override fun onBackPressed():Boolean {
        if (isAtRoot()) {
            return false
        }
        return goBack()
    }
}