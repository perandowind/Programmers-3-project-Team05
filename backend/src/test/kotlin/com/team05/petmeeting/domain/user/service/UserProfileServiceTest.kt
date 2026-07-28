package com.team05.petmeeting.domain.user.service

import com.team05.petmeeting.domain.cheer.repository.CheerRepository
import com.team05.petmeeting.domain.comment.repository.AnimalCommentRepository
import com.team05.petmeeting.domain.comment.repository.FeedCommentRepository
import com.team05.petmeeting.domain.feed.repository.FeedRepository
import com.team05.petmeeting.domain.user.entity.User
import com.team05.petmeeting.domain.user.entity.UserAuth
import com.team05.petmeeting.domain.user.errorcode.UserErrorCode
import com.team05.petmeeting.domain.user.provider.Provider
import com.team05.petmeeting.domain.user.repository.UserRepository
import com.team05.petmeeting.global.exception.BusinessException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime
import java.util.Optional

class UserProfileServiceTest {

    private lateinit var userProfileService: UserProfileService
    private lateinit var userRepository: UserRepository
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var feedRepository: FeedRepository
    private lateinit var cheerRepository: CheerRepository
    private lateinit var animalCommentRepository: AnimalCommentRepository
    private lateinit var feedCommentRepository: FeedCommentRepository

    private var userId: Long = 0L
    private lateinit var user: User

    @BeforeEach
    fun setUp() {
        userRepository = mock(UserRepository::class.java)
        passwordEncoder = mock(PasswordEncoder::class.java)
        feedRepository = mock(FeedRepository::class.java)
        cheerRepository = mock(CheerRepository::class.java)
        animalCommentRepository = mock(AnimalCommentRepository::class.java)
        feedCommentRepository = mock(FeedCommentRepository::class.java)

        userProfileService = UserProfileService(
            userRepository,
            passwordEncoder,
            feedRepository,
            cheerRepository,
            animalCommentRepository,
            feedCommentRepository,
        )

        `when`(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(ENCODED_NEW_PASSWORD)

        user = User.create("test@test.com", "테스터", "홍길동").apply {
            addAuth(UserAuth.create(Provider.LOCAL, "test@test.com", ENCODED_PASSWORD))
        }
        ReflectionTestUtils.setField(user, "id", USER_ID)
        ReflectionTestUtils.setField(user, "createdAt", LocalDateTime.now())

        `when`(userRepository.findById(USER_ID)).thenReturn(Optional.of(user))
        userId = USER_ID
    }

    @Test
    fun modifyPassword_success() {
        `when`(passwordEncoder.matches(CURRENT_PASSWORD, ENCODED_PASSWORD)).thenReturn(true)
        `when`(passwordEncoder.matches(NEW_PASSWORD, ENCODED_PASSWORD)).thenReturn(false)

        assertDoesNotThrow {
            userProfileService.modifyPassword(userId, CURRENT_PASSWORD, NEW_PASSWORD)
        }
    }

    @Test
    fun modifyPassword_fail_invalidPassword() {
        val ex = assertThrows(BusinessException::class.java) {
            userProfileService.modifyPassword(userId, WRONG_PASSWORD, NEW_PASSWORD)
        }

        assertThat(ex.errorCode).isEqualTo(UserErrorCode.INVALID_PASSWORD)
    }

    @Test
    fun modifyPassword_fail_sameAsOld() {
        `when`(passwordEncoder.matches(CURRENT_PASSWORD, ENCODED_PASSWORD)).thenReturn(true)

        val ex = assertThrows(BusinessException::class.java) {
            userProfileService.modifyPassword(userId, CURRENT_PASSWORD, CURRENT_PASSWORD)
        }

        assertThat(ex.errorCode).isEqualTo(UserErrorCode.SAME_AS_OLD_PASSWORD)
    }

    @Test
    fun modifyPassword_fail_socialUser() {
        val socialUser = User.create("social@test.com", "소셜유저", "홍길동").apply {
            addAuth(UserAuth.create(Provider.GOOGLE, "google-id-123", null))
        }
        ReflectionTestUtils.setField(socialUser, "id", SOCIAL_USER_ID)
        `when`(userRepository.findById(SOCIAL_USER_ID)).thenReturn(Optional.of(socialUser))

        val ex = assertThrows(BusinessException::class.java) {
            userProfileService.modifyPassword(SOCIAL_USER_ID, CURRENT_PASSWORD, NEW_PASSWORD)
        }

        assertThat(ex.errorCode).isEqualTo(UserErrorCode.LOCAL_NOT_FOUND)
    }

    @Test
    fun modifyNickname_success() {
        val res = userProfileService.modifyNickname(userId, "새닉네임")

        assertThat(res.nickname).isEqualTo("새닉네임")
    }

    @Test
    fun getUserProfile_success_localUser() {
        val res = userProfileService.getUserProfile(USER_ID)

        assertThat(res.hasLocalAuth).isTrue()
    }

    @Test
    fun getUserProfile_success_socialUser() {
        val socialUser = User.create("social@test.com", "소셜유저", "홍길동").apply {
            addAuth(UserAuth.create(Provider.GOOGLE, "google-id-123", null))
        }
        ReflectionTestUtils.setField(socialUser, "id", SOCIAL_USER_ID)
        ReflectionTestUtils.setField(socialUser, "createdAt", LocalDateTime.now())
        `when`(userRepository.findById(SOCIAL_USER_ID)).thenReturn(Optional.of(socialUser))

        val res = userProfileService.getUserProfile(SOCIAL_USER_ID)

        assertThat(res.hasLocalAuth).isFalse()
    }

    @Test
    fun getUserProfile_fail_userNotFound() {
        `when`(userRepository.findById(NOT_FOUND_USER_ID)).thenReturn(Optional.empty())

        val ex = assertThrows(BusinessException::class.java) {
            userProfileService.getUserProfile(NOT_FOUND_USER_ID)
        }

        assertThat(ex.errorCode).isEqualTo(UserErrorCode.USER_NOT_FOUND)
    }

    @Test
    fun getMyProfileDetails_success() {
        `when`(feedRepository.countByUser(user)).thenReturn(2L)
        `when`(cheerRepository.countByUser(user)).thenReturn(7L)
        `when`(cheerRepository.countDistinctAnimalByUser(user)).thenReturn(3L)
        `when`(feedCommentRepository.countFeedCommentByUser(user)).thenReturn(4L)
        `when`(animalCommentRepository.countAnimalCommentByUser(user)).thenReturn(5L)

        val res = userProfileService.getMyProfileDetails(USER_ID)

        assertThat(res.feedCount).isEqualTo(2L)
        assertThat(res.cheerCount).isEqualTo(7L)
        assertThat(res.cheerAnimalCount).isEqualTo(3L)
        assertThat(res.feedCommentCount).isEqualTo(4L)
        assertThat(res.animalCommentCount).isEqualTo(5L)
    }

    companion object {
        private const val USER_ID = 1L
        private const val SOCIAL_USER_ID = 2L
        private const val NOT_FOUND_USER_ID = 999L
        private const val CURRENT_PASSWORD = "CurrentPw1!"
        private const val NEW_PASSWORD = "NewPassword1!"
        private const val WRONG_PASSWORD = "WrongPw1!"
        private const val ENCODED_PASSWORD = "encodedPw!"
        private const val ENCODED_NEW_PASSWORD = "encodedNewPw!"
    }
}
