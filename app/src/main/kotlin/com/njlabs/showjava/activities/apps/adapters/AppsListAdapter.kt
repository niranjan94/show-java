package com.njlabs.showjava.activities.apps.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.njlabs.showjava.R
import com.njlabs.showjava.data.PackageInfo
import kotlinx.android.synthetic.main.layout_app_list_item.view.*

class AppsListAdapter(
    private var apps: List<PackageInfo>,
    private val itemClick: (PackageInfo, View) -> Unit
) : androidx.recyclerview.widget.RecyclerView.Adapter<AppsListAdapter.ViewHolder>() {

    class ViewHolder(view: View, private val itemClick: (PackageInfo, View) -> Unit) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
        fun bindPackageInfo(packageInfo: PackageInfo) {
            with(packageInfo) {
                itemView.itemLabel.text = packageInfo.label
                itemView.itemSecondaryLabel.text = packageInfo.version
                itemView.itemIcon.setImageDrawable(packageInfo.icon)
                itemView.itemCard.cardElevation = 1F
                itemView.itemCard.setOnClickListener { itemClick(this, itemView) }
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

    fun updateList(apps: List<PackageInfo>) {
        this.apps = apps
        notifyDataSetChanged()
    }
}
