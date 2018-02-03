package com.njlabs.showjava.activities.explorer.navigator

import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import com.njlabs.showjava.R
import com.njlabs.showjava.activities.BaseActivity
import com.njlabs.showjava.activities.explorer.navigator.adapters.FilesListAdapter
import com.njlabs.showjava.activities.explorer.viewer.CodeViewerActivity
import com.njlabs.showjava.activities.explorer.viewer.ImageViewerActivity
import com.njlabs.showjava.models.FileItem
import com.njlabs.showjava.models.SourceInfo
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_navigator.*
import timber.log.Timber
import java.io.File
import android.widget.Toast
import android.content.ActivityNotFoundException
import android.net.Uri
import android.webkit.MimeTypeMap


class NavigatorActivity : BaseActivity() {

    private lateinit var  navigationHandler: NavigatorHandler
    private lateinit var filesListAdapter: FilesListAdapter
    private var currentDirectory: File? = null

    private var fileItems = ArrayList<FileItem>()
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

        // selectedApp ?: finish()
        currentDirectory = currentDirectory ?: selectedApp?.sourceDirectory
        // currentDirectory ?: finish()

        // currentDirectory = File("${Environment.getExternalStorageDirectory()}/show-java/")
        setupList()
        filesListAdapter.updateData(fileItems)
        Timber.d(currentDirectory?.absolutePath)
        currentDirectory?.let { populateList(it) }
    }

    private fun setListVisibility(isListVisible: Boolean = true) {
        val listGroupVisibility = if (isListVisible) View.VISIBLE else View.GONE
        // val defaultGroupVisibility = if (isListVisible) View.GONE else View.VISIBLE
        filesList.visibility = listGroupVisibility
    }

    private fun populateList(startDirectory: File) {
        supportActionBar?.title = startDirectory.name
        currentDirectory = startDirectory
        navigationHandler.loadFiles(startDirectory)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError { Timber.e(it) }
                .subscribe {
                    if (selectedApp?.sourceDirectory != startDirectory) {
                        it.add(0, FileItem(File(startDirectory.parent), "Parent directory", "parent"))
                    }
                    updateList(it)
                }
    }

    private fun updateList(fileItems: ArrayList<FileItem>) {
        this.fileItems = fileItems
        filesListAdapter.updateData(fileItems)
        if (fileItems.isEmpty()) {
            setListVisibility(false)
        } else {
            setListVisibility(true)
        }
    }

    private fun setupList() {
        filesList.setHasFixedSize(true)
        filesList.layoutManager = LinearLayoutManager(context)
        filesListAdapter = FilesListAdapter(fileItems) { selectedFile ->
            if (selectedFile.file.isDirectory) {
                populateList(selectedFile.file)
            } else {
                when {
                    arrayOf("jpeg", "jpg", "png").contains(selectedFile.file.extension) -> {
                        val intent = Intent(context, ImageViewerActivity::class.java)
                        intent.putExtra("filePath", selectedFile.file.absolutePath)
                        intent.putExtra("packageName", selectedApp?.packageName)
                        startActivity(intent)
                    }
                    arrayOf(
                            "java", "xml", "json", "c",
                            "cpp", "txt", "properties",
                            "yml", "md", "kt", "html",
                            "js", "css", "scss", "sass"
                    ).contains(selectedFile.file.extension) -> {
                        val intent = Intent(context, CodeViewerActivity::class.java)
                        intent.putExtra("filePath", selectedFile.file.absolutePath)
                        intent.putExtra("packageName", selectedApp?.packageName)
                        startActivity(intent)
                    }
                    else -> {
                        val mimeTypeDetector = MimeTypeMap.getSingleton()
                        val fileIntent = Intent(Intent.ACTION_VIEW)
                        val mimeType = mimeTypeDetector.getMimeTypeFromExtension(selectedFile.file.extension)
                        fileIntent.setDataAndType(Uri.fromFile(selectedFile.file), mimeType)
                        fileIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        try {
                            context.startActivity(fileIntent)
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(context, getString(R.string.noSupportedHandlerForFileType), Toast.LENGTH_LONG).show()
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
            bundle.putString("currentDirectory", it.absolutePath)

        }
    }
}