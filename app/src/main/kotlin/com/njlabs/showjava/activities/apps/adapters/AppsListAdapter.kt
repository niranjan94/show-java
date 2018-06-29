package com.njlabs.showjava.activities.apps.adapters

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.njlabs.showjava.R
import com.njlabs.showjava.models.PackageInfo
import kotlinx.android.synthetic.main.layout_app_list_item.view.*

class AppsListAdapter(
    private val apps: List<PackageInfo>,
    private val itemClick: (PackageInfo) -> Unit
) : RecyclerView.Adapter<AppsListAdapter.ViewHolder>() {

    class ViewHolder(view: View, private val itemClick: (PackageInfo) -> Unit) :
        RecyclerView.ViewHolder(view) {
        fun bindPackageInfo(packageInfo: PackageInfo) {
            with(packageInfo) {
                itemView.itemLabel.text = packageInfo.packageLabel
                itemView.itemSecondaryLabel.text = packageInfo.packageVersion
                itemView.itemIcon.setImageDrawable(packageInfo.packageIcon)
                itemView.itemCard.cardElevation = 1F
                itemView.itemCard.setOnClickListener { itemClick(this) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_app_list_item, parent, false)
        return ViewHolder(view, itemClick)
    }

    override fun onBindViewHolder(holder: AppsListAdapter.ViewHolder, position: Int) {
        holder.bindPackageInfo(apps[position])
    }

    override fun getItemCount(): Int {
        return apps.size
    }
}
