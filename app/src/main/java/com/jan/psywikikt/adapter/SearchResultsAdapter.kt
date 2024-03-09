package com.jan.psywikikt.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import com.jan.psywikikt.InfoActivity
import com.jan.psywikikt.R

class SearchResultsAdapter(
    private val context: Context,
    searchResultList: ArrayList<String>,
    searchResultSubList: ArrayList<String>
): RecyclerView.Adapter<SearchResultsAdapter.Viewholder>() {
    private val searchResultList: ArrayList<String>
    private val searchResultSubList: ArrayList<String>

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Viewholder {
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.search_res_item, parent, false)
        return Viewholder(view)
    }

    override fun getItemCount(): Int {
        return searchResultList.size
    }

    override fun onBindViewHolder(holder: Viewholder, position: Int) {
        holder.drugName.text = searchResultList[position]
        holder.psychoactiveClass.text = searchResultSubList[position]
        holder.linearResult.setOnClickListener {
            val intent = Intent(context, InfoActivity::class.java)
            intent.putExtra("substance", holder.drugName.text)
            context.startActivity(intent)
        }
    }

    class Viewholder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val drugName: MaterialTextView
        val psychoactiveClass: MaterialTextView
        val linearResult: LinearLayout

        init {
            drugName = itemView.findViewById(R.id.resultText)
            psychoactiveClass = itemView.findViewById(R.id.resultSubText)
            linearResult = itemView.findViewById(R.id.linearResult)
        }
    }

    init {
        this.searchResultList = searchResultList
        this.searchResultSubList = searchResultSubList
    }
}