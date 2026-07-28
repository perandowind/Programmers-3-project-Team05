package com.team05.petmeeting.domain.animal.entity

import com.team05.petmeeting.domain.animal.dto.external.AnimalItem
import com.team05.petmeeting.domain.comment.entity.AnimalComment
import com.team05.petmeeting.domain.shelter.entity.Shelter
import com.team05.petmeeting.global.entity.BaseEntity
import jakarta.persistence.*
import java.time.DateTimeException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.util.*

@Entity
@Table(name = "animals")
class Animal() : BaseEntity() {
    @Column(name = "desertion_no", nullable = false, length = 50, unique = true)
    var desertionNo: String = ""
        protected set

    @Column(name = "process_state", nullable = true, length = 30)
    var processState: String? = null
        protected set

    @Column(name = "state_group", nullable = false)
    var stateGroup: Int? = null
        protected set

    @Column(name = "notice_no", nullable = true, length = 50)
    var noticeNo: String? = null
        protected set

    @Column(name = "notice_edt", nullable = true)
    var noticeEdt: LocalDate? = null
        protected set

    @Column(name = "happen_place", nullable = true, length = 255)
    var happenPlace: String? = null
        protected set

    @Column(name = "up_kind_nm", nullable = true, length = 30)
    var upKindNm: String = ""
        protected set

    @Column(name = "kind_full_name", nullable = true, length = 100)
    var kindFullNm: String = ""
        protected set

    @Column(name = "color_cd", nullable = true, length = 100)
    var colorCd: String? = null
        protected set

    @Column(name = "age", nullable = true)
    var age: String? = null
        protected set

    @Column(name = "weight", nullable = true, length = 30)
    var weight: String? = null
        protected set

    @Column(name = "sex_cd", nullable = true, length = 10)
    var sexCd: String? = null
        protected set

    @Column(name = "popfile1", nullable = true, length = 500)
    var popfile1: String = ""
        protected set

    @Column(name = "popfile2", nullable = true, length = 500)
    var popfile2: String? = null
        protected set

    @Column(name = "special_mark", nullable = true, length = 500)
    var specialMark: String? = null
        protected set

    @Column(name = "care_nm", nullable = true)
    var careNm: String? = null
        protected set

    @Column(name = "care_owner_nm", nullable = true, length = 100)
    var careOwnerNm: String? = null
        protected set

    @Column(name = "care_tel", nullable = true)
    var careTel: String? = null
        protected set

    @Column(name = "care_addr", nullable = true)
    var careAddr: String? = null
        protected set

    @Column(name = "total_cheer_count", nullable = false)
    var totalCheerCount: Int = 0
        protected set

    @Column(name = "api_updated_at")
    var apiUpdatedAt: LocalDateTime? = null
        protected set

    @Column(name = "name", nullable = true)
    var name: String? = null
        protected set

    @OneToMany(mappedBy = "animal", cascade = [CascadeType.ALL], orphanRemoval = true)
    var comments: MutableList<AnimalComment> = mutableListOf()
        protected set

    @ManyToOne
    var shelter: Shelter? = null
        protected set

    private constructor(builder: Builder) : this() {
        desertionNo = builder.desertionNo ?: ""
        processState = builder.processState
        stateGroup = builder.stateGroup ?: determineStateGroup(builder.processState)
        noticeNo = builder.noticeNo
        noticeEdt = builder.noticeEdt
        happenPlace = builder.happenPlace
        upKindNm = builder.upKindNm ?: ""
        kindFullNm = builder.kindFullNm ?: ""
        colorCd = builder.colorCd
        age = builder.age
        weight = builder.weight
        sexCd = builder.sexCd
        popfile1 = builder.popfile1 ?: ""
        popfile2 = builder.popfile2
        specialMark = builder.specialMark
        careOwnerNm = builder.careOwnerNm
        careNm = builder.careNm
        careTel = builder.careTel
        careAddr = builder.careAddr
        totalCheerCount = builder.totalCheerCount ?: 0
        apiUpdatedAt = builder.apiUpdatedAt
        name = builder.name
        comments = builder.comments?.toMutableList() ?: mutableListOf()
        shelter = builder.shelter
    }

    private fun determineStateGroup(processState: String?): Int =
        if (processState != null && processState.contains("보호")) 0 else 1

    fun updateName(name: String?) {
        this.name = name
    }

    fun updateFrom(item: AnimalItem) {
        processState = item.processState
        stateGroup = determineStateGroup(item.processState)
        noticeNo = item.noticeNo
        noticeEdt = parseNoticeEdt(item.noticeEdt)
        happenPlace = item.happenPlace
        upKindNm = item.upKindNm ?: ""
        kindFullNm = item.kindFullNm ?: ""
        colorCd = item.colorCd
        age = item.age
        weight = item.weight
        sexCd = item.sexCd
        popfile1 = item.popfile1 ?: ""
        popfile2 = item.popfile2
        specialMark = item.specialMark
        careNm = item.careNm
        careOwnerNm = item.careOwnerNm
        careTel = item.careTel
        careAddr = item.careAddr
        apiUpdatedAt = parseUpdTm(item.updTm)
    }

    fun needsUpdateFrom(item: AnimalItem): Boolean {
        val incomingUpdatedAt = parseUpdTm(item.updTm)

        if (apiUpdatedAt != null && incomingUpdatedAt != null) {
            return incomingUpdatedAt.isAfter(apiUpdatedAt)
        }

        return !Objects.equals(processState, item.processState) ||
            !Objects.equals(noticeNo, item.noticeNo) ||
            !Objects.equals(happenPlace, item.happenPlace) ||
            !Objects.equals(specialMark, item.specialMark) ||
            !Objects.equals(careNm, item.careNm)
    }

    fun assignShelter(shelter: Shelter) {
        this.shelter = shelter
    }

    fun getTemperature(): Double {
        val cheerGoal = 50.0
        val temperature = totalCheerCount / cheerGoal * 100
        return kotlin.math.round(temperature * 10) / 10.0
    }

    class Builder {
        internal var desertionNo: String? = null
        internal var processState: String? = null
        internal var stateGroup: Int? = null
        internal var noticeNo: String? = null
        internal var noticeEdt: LocalDate? = null
        internal var happenPlace: String? = null
        internal var upKindNm: String? = null
        internal var kindFullNm: String? = null
        internal var colorCd: String? = null
        internal var age: String? = null
        internal var weight: String? = null
        internal var sexCd: String? = null
        internal var popfile1: String? = null
        internal var popfile2: String? = null
        internal var specialMark: String? = null
        internal var careOwnerNm: String? = null
        internal var careNm: String? = null
        internal var careTel: String? = null
        internal var careAddr: String? = null
        internal var totalCheerCount: Int? = null
        internal var apiUpdatedAt: LocalDateTime? = null
        internal var name: String? = null
        internal var comments: List<AnimalComment>? = null
        internal var shelter: Shelter? = null

        fun desertionNo(desertionNo: String?) = apply { this.desertionNo = desertionNo }
        fun processState(processState: String?) = apply { this.processState = processState }
        fun stateGroup(stateGroup: Int?) = apply { this.stateGroup = stateGroup }
        fun noticeNo(noticeNo: String?) = apply { this.noticeNo = noticeNo }
        fun noticeEdt(noticeEdt: LocalDate?) = apply { this.noticeEdt = noticeEdt }
        fun happenPlace(happenPlace: String?) = apply { this.happenPlace = happenPlace }
        fun upKindNm(upKindNm: String?) = apply { this.upKindNm = upKindNm }
        fun kindFullNm(kindFullNm: String?) = apply { this.kindFullNm = kindFullNm }
        fun colorCd(colorCd: String?) = apply { this.colorCd = colorCd }
        fun age(age: String?) = apply { this.age = age }
        fun weight(weight: String?) = apply { this.weight = weight }
        fun sexCd(sexCd: String?) = apply { this.sexCd = sexCd }
        fun popfile1(popfile1: String?) = apply { this.popfile1 = popfile1 }
        fun popfile2(popfile2: String?) = apply { this.popfile2 = popfile2 }
        fun specialMark(specialMark: String?) = apply { this.specialMark = specialMark }
        fun careOwnerNm(careOwnerNm: String?) = apply { this.careOwnerNm = careOwnerNm }
        fun careNm(careNm: String?) = apply { this.careNm = careNm }
        fun careTel(careTel: String?) = apply { this.careTel = careTel }
        fun careAddr(careAddr: String?) = apply { this.careAddr = careAddr }
        fun totalCheerCount(totalCheerCount: Int?) = apply { this.totalCheerCount = totalCheerCount }
        fun apiUpdatedAt(apiUpdatedAt: LocalDateTime?) = apply { this.apiUpdatedAt = apiUpdatedAt }
        fun name(name: String?) = apply { this.name = name }
        fun comments(comments: List<AnimalComment>?) = apply { this.comments = comments }
        fun shelter(shelter: Shelter?) = apply { this.shelter = shelter }
        fun build(): Animal = Animal(this)
    }

    companion object {
        private val API_UPDATE_TIME_FORMATTER = DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm:ss")
            .optionalStart()
            .appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, true)
            .optionalEnd()
            .toFormatter()

        @JvmStatic
        fun builder(): Builder = Builder()

        @JvmStatic
        fun from(item: AnimalItem): Animal {
            val animal = Builder()
                .desertionNo(item.desertionNo)
                .processState(item.processState)
                .noticeNo(item.noticeNo)
                .noticeEdt(parseNoticeEdt(item.noticeEdt))
                .happenPlace(item.happenPlace)
                .upKindNm(item.upKindNm)
                .kindFullNm(item.kindFullNm)
                .colorCd(item.colorCd)
                .age(item.age)
                .weight(item.weight)
                .sexCd(item.sexCd)
                .popfile1(item.popfile1)
                .popfile2(item.popfile2)
                .specialMark(item.specialMark)
                .careOwnerNm(item.careOwnerNm)
                .careNm(item.careNm)
                .careTel(item.careTel)
                .careAddr(item.careAddr)
                .totalCheerCount(0)
                .build()

            animal.apiUpdatedAt = parseUpdTm(item.updTm)
            return animal
        }

        private fun parseUpdTm(updTm: String?): LocalDateTime? {
            if (updTm.isNullOrBlank()) {
                return null
            }

            return try {
                LocalDateTime.parse(updTm.trim(), API_UPDATE_TIME_FORMATTER)
            } catch (_: DateTimeException) {
                null
            }
        }

        private fun parseNoticeEdt(noticeEdt: String?): LocalDate? {
            if (noticeEdt.isNullOrBlank()) {
                return null
            }

            return LocalDate.parse(noticeEdt, DateTimeFormatter.BASIC_ISO_DATE)
        }
    }
}
