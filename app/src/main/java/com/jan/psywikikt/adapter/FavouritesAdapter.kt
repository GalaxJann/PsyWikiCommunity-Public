package com.jan.psywikikt.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import com.jan.psywikikt.InfoActivity
import com.jan.psywikikt.R

class FavouritesAdapter(
    private val context: Context,
    favModelArrayList: ArrayList<FavouritesModel>
): RecyclerView.Adapter<FavouritesAdapter.Viewholder>() {

    private val favModelArrayList: ArrayList<FavouritesModel>

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FavouritesAdapter.Viewholder {
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.fav_item, parent, false)
        return Viewholder(view)
    }

    override fun onBindViewHolder(holder: FavouritesAdapter.Viewholder, position: Int) {
        val model: FavouritesModel = favModelArrayList[position]
        holder.drugName.text = model.getDrugName()
        holder.drugName.setOnClickListener { view ->
            val intent = Intent(context, InfoActivity::class.java)
            intent.putExtra("substance", holder.drugName.text)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return favModelArrayList.size
    }

    class Viewholder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val drugName: MaterialTextView
        init {
            drugName = itemView.findViewById(R.id.favName)
        }
    }

    init {
        this.favModelArrayList = favModelArrayList
    }
}