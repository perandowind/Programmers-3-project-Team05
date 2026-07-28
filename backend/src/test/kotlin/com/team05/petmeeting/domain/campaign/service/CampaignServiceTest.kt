package com.team05.petmeeting.domain.campaign.service

import com.team05.petmeeting.domain.animal.service.AnimalExternalService
import com.team05.petmeeting.domain.campaign.dto.CampaignCreateReq
import com.team05.petmeeting.domain.campaign.errorcode.CampaignErrorCode
import com.team05.petmeeting.domain.campaign.repository.CampaignRepository
import com.team05.petmeeting.domain.shelter.dto.ShelterCommand
import com.team05.petmeeting.domain.shelter.entity.Shelter
import com.team05.petmeeting.domain.shelter.entity.Shelter.Companion.create
import com.team05.petmeeting.domain.shelter.repository.ShelterRepository
import com.team05.petmeeting.domain.shelter.service.ShelterService
import com.team05.petmeeting.domain.user.entity.User
import com.team05.petmeeting.domain.user.entity.User.Companion.create
import com.team05.petmeeting.domain.user.repository.UserRepository
import com.team05.petmeeting.global.exception.BusinessException
import com.team05.petmeeting.global.exception.ErrorCode
import jakarta.transaction.Transactional
import org.assertj.core.api.AssertionsForClassTypes
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.LocalDateTime

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class CampaignServiceTest {
    @MockitoBean
    var animalExternalService: AnimalExternalService? = null // 외부 의존 mock 처리

    @Autowired
    var campaignService: CampaignService? = null

    @Autowired
    var campaignRepository: CampaignRepository? = null

    @Autowired
    var shelterService: ShelterService? = null

    @Autowired
    var userRepository: UserRepository? = null

    @Autowired
    private val shelterRepository: ShelterRepository? = null

    private var userId: Long? = null
    private var otherUserId: Long? = null
    private var shelterId: String? = null


    //    @Test
    //    @DisplayName("캠페인 생성 성공")
    //    public void createCampaign() {
    //        // given - User 먼저 만들기
    //        User user = userRepository.save(User.create("test@test.com", "nickname", "realname"));
    //
    //        // 보호소에 유저 할당
    //        ShelterCommand cmd = new ShelterCommand(
    //                "123", "보호소1", "010", "주소", "소유자", "기관", LocalDateTime.now()
    //        );
    //        shelterService.createOrUpdateShelter(cmd);
    //        Shelter shelter = shelterService.findById("123");
    //        shelter.assignUser(user);
    //
    //        // when
    //        campaignService.createCampaign("123", requireNotNull(user.id), new CampaignCreateReq("사료 후원", "사료 후원 설명", 1000000));
    //
    //        // then
    //        Campaign result = campaignRepository
    //                .findByShelter_CareRegNoAndStatus("123", CampaignStatus.ACTIVE)
    //                .orElseThrow();
    //        assertThat(result.getTitle()).isEqualTo("사료 후원");
    //    }
    @BeforeEach
    fun setUp() {
        // 보호소 관리자 유저
        var user = create("test@test.com", "테스터", "홍길동")
        user = userRepository!!.save<User>(user)
        userId = user.id

        // 다른 유저 (권한 없음)
        var otherUser = create("other@test.com", "다른유저", "김철수")
        otherUser = userRepository!!.save<User>(otherUser)
        otherUserId = otherUser.id

        // 보호소 생성 및 유저 연결
        val cmd = ShelterCommand(
            "shelter-001",
            "테스트보호소",
            "010-0000-0000",
            "서울시 테스트구",
            "홍길동",
            "테스트기관",
            LocalDateTime.now()
        )
        var shelter = create(cmd)
        shelter.assignUser(user)
        shelter = shelterRepository!!.save<Shelter>(shelter)
        shelterId = shelter.careRegNo
    }

    // 캠페인 생성 성공
    @Test
    fun createCampaign_success() {
        val req = CampaignCreateReq("테스트 캠페인", "설명", 100000)
        val res = campaignService!!.createCampaign(shelterId!!, userId!!, req)

        AssertionsForClassTypes.assertThat(res.title).isEqualTo("테스트 캠페인")
    }

    // 캠페인 생성 실패 - 권한 없는 유저 (CA-004)
    @Test
    fun createCampaign_fail_unauthorized() {
        val req = CampaignCreateReq("테스트 캠페인", "설명", 100000)

        val ex = Assertions.assertThrows<BusinessException>(
            BusinessException::class.java,
            Executable { campaignService!!.createCampaign(shelterId!!, otherUserId!!, req) }
        )
        AssertionsForClassTypes.assertThat<ErrorCode?>(ex.errorCode)
            .isEqualTo(CampaignErrorCode.UNAUTHORIZED_SHELTER)
    }

    // 캠페인 생성 실패 - 이미 진행 중인 캠페인 있음 (CA-003)
    @Test
    fun createCampaign_fail_alreadyExists() {
        val req = CampaignCreateReq("테스트 캠페인", "설명", 100000)
        campaignService!!.createCampaign(shelterId!!, userId!!, req)

        val ex = Assertions.assertThrows<BusinessException>(
            BusinessException::class.java,
            Executable { campaignService!!.createCampaign(shelterId!!, userId!!, req) }
        )
        AssertionsForClassTypes.assertThat<ErrorCode?>(ex.errorCode)
            .isEqualTo(CampaignErrorCode.CAMPAIGN_ALREADY_EXISTS)
    }

    // 캠페인 종료 성공
    @Test
    fun closeCampaign_success() {
        val req = CampaignCreateReq("테스트 캠페인", "설명", 100000)
        val created = campaignService!!.createCampaign(shelterId!!, userId!!, req)

        Assertions.assertDoesNotThrow(Executable { campaignService!!.closeCampaign(userId!!, created.id) }
        )
    }

    // 캠페인 종료 실패 - 이미 마감된 캠페인 (CA-002)
    @Test
    fun closeCampaign_fail_alreadyClosed() {
        val req = CampaignCreateReq("테스트 캠페인", "설명", 100000)
        val created = campaignService!!.createCampaign(shelterId!!, userId!!, req)
        campaignService!!.closeCampaign(userId!!, created.id)

        val ex = Assertions.assertThrows<BusinessException>(
            BusinessException::class.java,
            Executable { campaignService!!.closeCampaign(userId!!, created.id) }
        )
        AssertionsForClassTypes.assertThat<ErrorCode?>(ex.errorCode).isEqualTo(CampaignErrorCode.CAMPAIGN_CLOSED)
    }

    // 캠페인 종료 실패 - 권한 없는 유저 (CA-004)
    @Test
    fun closeCampaign_fail_unauthorized() {
        val req = CampaignCreateReq("테스트 캠페인", "설명", 100000)
        val created = campaignService!!.createCampaign(shelterId!!, userId!!, req)

        val ex = Assertions.assertThrows<BusinessException>(
            BusinessException::class.java,
            Executable { campaignService!!.closeCampaign(otherUserId!!, created.id) }
        )
        AssertionsForClassTypes.assertThat<ErrorCode?>(ex.errorCode)
            .isEqualTo(CampaignErrorCode.UNAUTHORIZED_SHELTER)
    }

    // 존재하지 않는 캠페인 종료 (CA-001)
    @Test
    fun closeCampaign_fail_notFound() {
        val ex = Assertions.assertThrows<BusinessException>(
            BusinessException::class.java,
            Executable { campaignService!!.closeCampaign(userId!!, 999L) }
        )
        AssertionsForClassTypes.assertThat<ErrorCode?>(ex.errorCode)
            .isEqualTo(CampaignErrorCode.CAMPAIGN_NOT_FOUND)
    }
}
