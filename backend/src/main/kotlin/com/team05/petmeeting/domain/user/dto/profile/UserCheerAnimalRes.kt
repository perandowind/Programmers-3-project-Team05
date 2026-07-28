package com.team05.petmeeting.domain.user.dto.profile

import com.team05.petmeeting.domain.animal.entity.Animal

data class UserCheerAnimalRes(
    val totalAnimalCount: Long,
    val animals: List<CheerAnimalItem>
) {

    data class CheerAnimalItem(
        val animalId: Long,
        val species: String,  // upKindNm (개/고양이)
        val breed: String,  // kindFullNm (믹스견 등)
        val imageUrl: String,  // popfile1
        val myCheerCount: Long,  // "5개 보냄" (내가 이 동물에게 보낸 개수)
        val temperature: Double // "85.0C" (동물의 총 응원 수 기반 온도)
    ) {
        companion object {
            @JvmStatic
            fun from(animal: Animal, myCheerCount: Long): CheerAnimalItem {
                return CheerAnimalItem(
                    requireNotNull(animal.id),
                    animal.upKindNm,
                    animal.kindFullNm,
                    animal.popfile1,
                    myCheerCount,
                    animal.getTemperature()
                )
            }
        }
    }

    companion object {
        @JvmStatic
        fun from(animalCountMap: List<Array<Any>>): UserCheerAnimalRes {
            val items = animalCountMap
                .map { row ->
                    CheerAnimalItem.from(
                        (row[0] as Animal),
                        (row[1] as Long)
                    )
                }

            return UserCheerAnimalRes(items.size.toLong(), items)
        }
    }
}
