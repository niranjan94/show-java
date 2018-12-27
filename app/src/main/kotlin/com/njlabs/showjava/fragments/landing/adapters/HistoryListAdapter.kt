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

package com.njlabs.showjava.fragments.landing.adapters

import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.njlabs.showjava.R
import com.njlabs.showjava.data.SourceInfo
import kotlinx.android.synthetic.main.layout_app_list_item.view.*
import java.io.File

class HistoryListAdapter(
    private var historyItems: List<SourceInfo>,
    private val itemClick: (SourceInfo) -> Unit
) : androidx.recyclerview.widget.RecyclerView.Adapter<HistoryListAdapter.ViewHolder>() {

    class ViewHolder(view: View, private val itemClick: (SourceInfo) -> Unit) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {

        fun bindSourceInfo(sourceInfo: SourceInfo) {
            with(sourceInfo) {
                itemView.itemLabel.text = sourceInfo.packageLabel
                itemView.itemSecondaryLabel.text = sourceInfo.packageName
                val iconPath =
                    "${Environment.getExternalStorageDirectory()}/show-java/sources/${sourceInfo.packageName}/icon.png"
                if (File(iconPath).exists()) {
                    val iconBitmap = BitmapFactory.decodeFile(iconPath)
                    itemView.itemIcon.setImageDrawable(
                        BitmapDrawable(
                            itemView.context.resources,
                            iconBitmap
                        )
                    )
                } else {
                    itemView.itemIcon.setImageResource(R.drawable.ic_list_generic)
                }
                itemView.itemCard.cardElevation = 1F
                itemView.itemCard.setOnClickListener { itemClick(this) }
            }
        }
    }

    fun updateData(historyItems: List<SourceInfo>) {
        this.historyItems = historyItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_app_list_item, parent, false)
        return ViewHolder(view, itemClick)
    }

    override fun onBindViewHolder(holder: HistoryListAdapter.ViewHolder, position: Int) {
        holder.bindSourceInfo(historyItems[position])
    }

    override fun getItemCount(): Int {
        return historyItems.size
    }
}
