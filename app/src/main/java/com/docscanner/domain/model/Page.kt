package com.docscanner.domain.model

data class Page(
    val id: String,
    val documentId: String,
    val pageNumber: Int,
    val imagePath: String,
    val createdAt: Long
)
