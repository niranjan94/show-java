package com.njlabs.showjava.activities.explorer.navigator.adapters

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.njlabs.showjava.R
import com.njlabs.showjava.models.FileItem
import kotlinx.android.synthetic.main.layout_app_list_item.view.*

class FilesListAdapter(private var fileItems: List<FileItem>, private val itemClick: (FileItem) -> Unit) : RecyclerView.Adapter<FilesListAdapter.ViewHolder>() {

    class ViewHolder(view: View, val itemClick: (FileItem) -> Unit) : RecyclerView.ViewHolder(view) {
        fun bindSourceInfo(fileItem: FileItem) {
            with(fileItem) {
                itemView.itemLabel.text = fileItem.name
                itemView.itemSecondaryLabel.text = fileItem.fileSize
                itemView.itemIcon.setImageResource(fileItem.iconResource)
                itemView.itemCard.cardElevation = 1F
                itemView.itemCard.setOnClickListener { itemClick(this) }
            }
        }
    }

    fun updateData(fileItems: List<FileItem>) {
        this.fileItems = fileItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder? {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_app_list_item, parent, false)
        return ViewHolder(view, itemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindSourceInfo(fileItems[position])
    }

    override fun getItemCount(): Int {
        return fileItems.size
    }
}
