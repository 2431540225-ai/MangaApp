package com.example.mangaapp.models

data class User(
    val id: Int,
    val username: String,
    val email: String,
    val avatarUrl: String = "",
    val roleId: Int = 2  // 1=admin, 2=customer
)
