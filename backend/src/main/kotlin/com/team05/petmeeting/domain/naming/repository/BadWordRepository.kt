package com.team05.petmeeting.domain.naming.repository

import com.team05.petmeeting.domain.naming.entity.BadWord
import org.springframework.data.jpa.repository.JpaRepository

interface BadWordRepository : JpaRepository<BadWord, Long>
