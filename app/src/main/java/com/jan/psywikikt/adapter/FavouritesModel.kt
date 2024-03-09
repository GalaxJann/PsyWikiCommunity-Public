package com.jan.psywikikt.adapter

class FavouritesModel(private var drug_name: String) {
    fun getDrugName(): String {
        return drug_name
    }

    fun setDrugName(drugName: String) {
        this.drug_name = drugName
    }
}