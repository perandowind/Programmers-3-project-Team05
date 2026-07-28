package com.team05.petmeeting.domain.naming.entity

import com.team05.petmeeting.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "bad_words")
class BadWord(
    @field:Column(nullable = false, unique = true)
    val word: String
) : BaseEntity()

