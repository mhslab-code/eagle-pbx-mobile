package com.eaglesistemas.eaglepbx.data

data class EagleContact(
    val name: String,
    val numbers: List<ContactNumber>,
    val photo: String?
)

data class ContactNumber(
    val number: String,
    val label: String
)
