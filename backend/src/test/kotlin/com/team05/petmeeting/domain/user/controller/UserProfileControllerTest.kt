package com.team05.petmeeting.domain.user.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.team05.petmeeting.domain.donation.service.DonationService
import com.team05.petmeeting.domain.user.dto.profile.MyProfileDetailRes
import com.team05.petmeeting.domain.user.dto.profile.NicknameReq
import com.team05.petmeeting.domain.user.dto.profile.PasswordReq
import com.team05.petmeeting.domain.user.dto.profile.UserDonationRes
import com.team05.petmeeting.domain.user.dto.profile.UserProfileRes
import com.team05.petmeeting.domain.user.entity.User
import com.team05.petmeeting.domain.user.errorcode.UserErrorCode
import com.team05.petmeeting.domain.user.service.UserProfileService
import com.team05.petmeeting.global.exception.BusinessException
import com.team05.petmeeting.global.security.filter.JwtAuthenticationFilter
import com.team05.petmeeting.global.security.test.WithCustomUser
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@WebMvcTest(UserProfileController::class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@WithCustomUser(userId = 100L)
class UserProfileControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var userProfileService: UserProfileService

    @MockitoBean
    private lateinit var donationService: DonationService

    @MockitoBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    private val objectMapper = ObjectMapper().findAndRegisterModules()

    @Test
    fun changeNickname_success() {
        val req = NicknameReq("newNick")
        val user = User.create("email", "newNick", "name")
        setCreatedAt(user, LocalDateTime.now())
        val res = UserProfileRes.from(user)

        doReturn(res).`when`(userProfileService).modifyNickname(100L, "newNick")

        mockMvc.perform(
            patch("/api/v1/me/nickname")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.nickname").value("newNick"))
    }

    @Test
    fun changeNickname_fail_blank() {
        val req = NicknameReq("")

        mockMvc.perform(
            patch("/api/v1/me/nickname")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun changeNickname_fail_duplicate() {
        val req = NicknameReq("dupNick")

        doThrow(BusinessException(UserErrorCode.DUPLICATE_NICKNAME))
            .`when`(userProfileService).modifyNickname(100L, "dupNick")

        mockMvc.perform(
            patch("/api/v1/me/nickname")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun getProfile_success() {
        val res = MyProfileDetailRes(3L, 5L, 2L, 2L, 1L)

        doReturn(res).`when`(userProfileService).getMyProfileDetails(100L)

        mockMvc.perform(get("/api/v1/me"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.feedCount").value(3))
            .andExpect(jsonPath("$.cheerCount").value(5))
            .andExpect(jsonPath("$.cheerAnimalCount").value(2))
    }

    @Test
    fun getProfile_fail_userNotFound() {
        doThrow(BusinessException(UserErrorCode.USER_NOT_FOUND))
            .`when`(userProfileService).getUserProfile(100L)

        mockMvc.perform(get("/api/v1/me/profile"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun getUserProfile_success_hasLocalAuth() {
        val res = UserProfileRes(
            profileImageUrl = "",
            nickname = "tester",
            email = "test@test.com",
            name = "홍길동",
            createdAt = LocalDateTime.now(),
            hasLocalAuth = true,
        )

        doReturn(res).`when`(userProfileService).getUserProfile(100L)

        mockMvc.perform(get("/api/v1/me/profile"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.hasLocalAuth").value(true))
    }

    @Test
    fun changePassword_success() {
        val req = PasswordReq("currentPw1!", "NewPassword1!")

        doNothing().`when`(userProfileService).modifyPassword(100L, "currentPw1!", "NewPassword1!")

        mockMvc.perform(
            patch("/api/v1/me/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)),
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun changePassword_fail_invalidFormat() {
        val req = PasswordReq("currentPw1!", "weak")

        mockMvc.perform(
            patch("/api/v1/me/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun changePassword_fail_invalidPassword() {
        val req = PasswordReq("wrongPw1!", "NewPassword1!")

        doThrow(BusinessException(UserErrorCode.INVALID_PASSWORD))
            .`when`(userProfileService).modifyPassword(100L, "wrongPw1!", "NewPassword1!")

        mockMvc.perform(
            patch("/api/v1/me/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun changePassword_fail_sameAsOld() {
        val req = PasswordReq("CurrentPw1!", "CurrentPw1!")

        doThrow(BusinessException(UserErrorCode.SAME_AS_OLD_PASSWORD))
            .`when`(userProfileService).modifyPassword(100L, "CurrentPw1!", "CurrentPw1!")

        mockMvc.perform(
            patch("/api/v1/me/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun getMyProfile_success() {
        val res = MyProfileDetailRes.of(5L, 7L, 3L, 5L, 5L)

        doReturn(res).`when`(userProfileService).getMyProfileDetails(100L)

        mockMvc.perform(get("/api/v1/me"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.cheerCount").value(7))
            .andExpect(jsonPath("$.cheerAnimalCount").value(3))
            .andExpect(jsonPath("$.feedCount").value(5))
            .andExpect(jsonPath("$.feedCommentCount").value(5))
            .andExpect(jsonPath("$.animalCommentCount").value(5))
    }

    @Test
    fun getDonations_success() {
        val res = mock(UserDonationRes::class.java)

        doReturn(res).`when`(donationService).getMyDonations(100L)

        mockMvc.perform(get("/api/v1/me/donations"))
            .andExpect(status().isOk)
    }

    private fun setCreatedAt(user: User, createdAt: LocalDateTime) {
        val createdAtField = user.javaClass.superclass.getDeclaredField("createdAt")
        createdAtField.isAccessible = true
        createdAtField.set(user, createdAt)
    }
}
