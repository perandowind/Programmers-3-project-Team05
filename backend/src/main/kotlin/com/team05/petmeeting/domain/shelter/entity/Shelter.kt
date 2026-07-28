package com.team05.petmeeting.domain.shelter.entity

import com.team05.petmeeting.domain.campaign.entity.Campaign
import com.team05.petmeeting.domain.shelter.dto.ShelterCommand
import com.team05.petmeeting.domain.user.entity.User
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "shelters")
@EntityListeners(AuditingEntityListener::class)
class Shelter (// primary key, from 외부 api

    @Column(name = "care_reg_no") @Id
    val careRegNo: String,

    @Column(
        name = "care_nm"
    ) 
    var careNm: String?,

    @Column(name = "care_tel")
    var careTel: String?,

    @Column(
        name = "care_addr"
    )  var careAddr: String?,

    @Column(name = "care_owner_nm")
     var careOwnerNm: String?,

    @Column(
        name = "org_nm"
    )  var orgNm: String?, // 외부 api 업데이트 시간

    @Column(name = "upd_tm")
     var updTm: LocalDateTime
) {
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
     var user: User? = null

    @CreatedDate
    @Column(name = "created_at", updatable = false)
     var createdAt: LocalDateTime? = null

    @LastModifiedDate
    @Column(name = "updated_at")
     var updatedAt: LocalDateTime? = null

    @OneToMany(mappedBy = "shelter", fetch = FetchType.LAZY)
     val campaigns: MutableList<Campaign> = ArrayList()

    fun updateFrom(cmd: ShelterCommand) {
        this.careNm = cmd.careNm
        this.careTel = cmd.careTel
        this.careAddr = cmd.careAddr
        this.careOwnerNm = cmd.careOwnerNm
        this.orgNm = cmd.orgNm
        this.updTm = cmd.updTm
    }

    fun assignUser(user: User?) {
        this.user = user
    }

    fun isManagedBy(userId: Long): Boolean {
        return this.user?.id == userId
    }

    companion object {
        @JvmStatic
        fun create(cmd: ShelterCommand): Shelter {
            return Shelter(
                cmd.careRegNo,
                cmd.careNm,
                cmd.careTel,
                cmd.careAddr,
                cmd.careOwnerNm,
                cmd.orgNm,
                cmd.updTm
            )
        }
    }
}
