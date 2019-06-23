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

package com.njlabs.showjava.fragments.explorer.navigator.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.njlabs.showjava.R
import com.njlabs.showjava.data.FileItem
import kotlinx.android.synthetic.main.layout_app_list_item.view.*

/**
 * List adapter for the code navigator
 */
class FilesListAdapter(
    private var fileItems: List<FileItem>,
    private val itemClick: (FileItem) -> Unit
) : androidx.recyclerview.widget.RecyclerView.Adapter<FilesListAdapter.ViewHolder>() {

    class ViewHolder(view: View, private val itemClick: (FileItem) -> Unit) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
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

    fun updateData(fileItems: List<FileItem>?) {
        if (fileItems != null) {
            this.fileItems = fileItems
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_app_list_item, parent, false)
        return ViewHolder(view, itemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindSourceInfo(fileItems[position])
    }

    override fun getItemCount(): Int {
        return fileItems.size
    }
}
