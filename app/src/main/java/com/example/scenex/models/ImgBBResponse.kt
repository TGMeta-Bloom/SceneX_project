package com.example.scenex.models

data class ImgBBResponse(
    val data: ImgData?,
    val success: Boolean,
    val status: Int
)

data class ImgData(
    val id: String,
    val title: String,
    val url: String,
    val display_url: String,
    val delete_url: String
)