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

package com.njlabs.showjava.activities.explorer.navigator

import android.app.ProgressDialog
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
import com.crashlytics.android.Crashlytics
import com.njlabs.showjava.R
import com.njlabs.showjava.activities.BaseActivity
import com.njlabs.showjava.activities.explorer.navigator.adapters.FilesListAdapter
import com.njlabs.showjava.activities.explorer.viewer.CodeViewerActivity
import com.njlabs.showjava.activities.explorer.viewer.ImageViewerActivity
import com.njlabs.showjava.data.FileItem
import com.njlabs.showjava.data.SourceInfo
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_navigator.*
import org.apache.commons.io.FileUtils
import timber.log.Timber
import java.io.File
import java.io.IOException


class NavigatorActivity : BaseActivity() {

    private lateinit var navigationHandler: NavigatorHandler
    private lateinit var filesListAdapter: FilesListAdapter
    private var currentDirectory: File? = null
    private var filesLoadSubscription: Disposable? = null
    private var zipProgressDialog: ProgressDialog? = null

    private var fileItems: ArrayList<FileItem>? = ArrayList()
    private var selectedApp: SourceInfo? = null

    override fun init(savedInstanceState: Bundle?) {
        setupLayout(R.layout.activity_navigator)
        selectedApp = intent.extras?.getParcelable("selectedApp")
        navigationHandler = NavigatorHandler(this)

        if (savedInstanceState != null) {
            fileItems = savedInstanceState.getParcelableArrayList<FileItem>("fileItems")
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
    }

    private fun setListVisibility(isListVisible: Boolean = true) {
        val listGroupVisibility = if (isListVisible) View.VISIBLE else View.GONE
        filesList.visibility = listGroupVisibility
    }

    private fun populateList(startDirectory: File) {
        supportActionBar?.title = startDirectory.name
        currentDirectory = startDirectory
        filesLoadSubscription = navigationHandler.loadFiles(startDirectory)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError { Timber.e(it) }
            .subscribe {
                if (selectedApp?.sourceDirectory != startDirectory) {
                    // it.add(0, FileItem(File(startDirectory.parent), "Parent directory", "parent"))
                }
                updateList(it)
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
        filesListAdapter = FilesListAdapter(fileItems!!) { selectedFile ->
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
                        val mimeTypeDetector = MimeTypeMap.getSingleton()
                        val fileIntent = Intent(Intent.ACTION_VIEW)
                        val mimeType =
                            mimeTypeDetector.getMimeTypeFromExtension(selectedFile.file.extension)
                        fileIntent.setDataAndType(
                            FileProvider.getUriForFile(
                                context,
                                context.applicationContext.packageName + ".provider",
                                selectedFile.file
                            ),
                            mimeType
                        )
                        fileIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        fileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        try {
                            context.startActivity(fileIntent)
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
        filesList.adapter = filesListAdapter
        updateList(fileItems)
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)
        bundle.putParcelableArrayList("fileItems", fileItems)
        selectedApp?.let {
            bundle.putParcelable("selectedApp", it)
        }
        currentDirectory?.let {
            bundle.putString("currentDirectory", it.canonicalPath)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        menu.findItem(R.id.share_code).isVisible = true
        menu.findItem(R.id.delete_code).isVisible = true
        return true
    }

    private fun showProgressDialog() {
        if (zipProgressDialog == null) {
            zipProgressDialog = ProgressDialog(this)
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                if (currentDirectory?.canonicalPath == selectedApp?.sourceDirectory?.canonicalPath) {
                    finish()
                    return true
                }
                currentDirectory?.parent ?.let {
                    populateList(File(it))
                    return true
                }
            }
            R.id.share_code -> {
                showProgressDialog()
                filesLoadSubscription = navigationHandler.archiveDirectory(
                    selectedApp!!.sourceDirectory, selectedApp!!.packageName
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnError { Timber.e(it) }
                    .subscribe {
                        dismissProgressDialog()
                        val shareIntent = Intent()
                        shareIntent.action = Intent.ACTION_SEND
                        shareIntent.setDataAndType(
                            FileProvider.getUriForFile(
                                context,
                                context.applicationContext.packageName + ".provider",
                                it
                            ),
                            MimeTypeMap.getSingleton().getMimeTypeFromExtension(it.extension)
                        )
                        startActivity(Intent.createChooser(shareIntent, getString(R.string.sendSourceVia)))
                    }

            }
            R.id.delete_code -> {
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.deleteSource))
                    .setMessage(getString(R.string.deleteSourceConfirm))
                    .setIcon(R.drawable.ic_error_outline_black)
                    .setPositiveButton(android.R.string.yes
                    ) { _, _ ->
                        deleteSource()
                    }
                    .setNegativeButton(android.R.string.no, null)
                    .show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun deleteSource() {
        try {
            selectedApp?.let {
                if (it.sourceDirectory.exists()) {
                    FileUtils.deleteDirectory(it.sourceDirectory)
                }
            }
        } catch (e: IOException) {
            Crashlytics.logException(e)
        }
        Toast.makeText(
            baseContext,
            getString(R.string.sourceDeleted),
            Toast.LENGTH_SHORT
        ).show()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (filesLoadSubscription?.isDisposed != true) {
            filesLoadSubscription?.dispose()
        }

    }
}