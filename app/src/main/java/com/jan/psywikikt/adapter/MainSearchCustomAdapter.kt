package com.jan.psywikikt.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.TextView
import com.jan.psywikikt.R

class MainSearchCustomAdapter(
    activity: Activity,
    dataSource: ArrayList<String>,
    subtitles: ArrayList<String>
): BaseAdapter() {
    private val activity: Activity
    private val dataSource: ArrayList<String>
    private val subtitles: ArrayList<String>
    private val layoutInflater: LayoutInflater

    init {
        this.activity = activity
        this.dataSource = dataSource
        this.subtitles = subtitles
        this.layoutInflater = activity.layoutInflater
    }

    override fun getCount(): Int {
        return dataSource.size
    }

    override fun getItem(position: Int): String {
        return dataSource[position]
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    @SuppressLint("InflateParams")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val holder: ViewHolder
        var mConvertView: View? = convertView
        if(mConvertView == null) {
            holder = ViewHolder()
            mConvertView = layoutInflater.inflate(R.layout.list_item, null)
            holder.title = mConvertView.findViewById(R.id.substanceName)
            holder.subtitle = mConvertView.findViewById(R.id.psychoactiveClass)
            mConvertView.tag = holder
        } else {
            holder = mConvertView.tag as ViewHolder
        }

        holder.title!!.text = getItem(position)
        if(subtitles[position] == "<DISPLAYNONE>") {
            holder.subtitle!!.visibility = LinearLayout.GONE
        } else {
            holder.subtitle!!.visibility = LinearLayout.VISIBLE
            holder.subtitle!!.text = subtitles[position]
        }

        return mConvertView!!
    }

    class ViewHolder {
        var title: TextView? = null
        var subtitle: TextView? = null
    }
}