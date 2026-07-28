package com.team05.petmeeting.domain.user.service

import com.team05.petmeeting.domain.cheer.repository.CheerRepository
import com.team05.petmeeting.domain.comment.repository.AnimalCommentRepository
import com.team05.petmeeting.domain.comment.repository.FeedCommentRepository
import com.team05.petmeeting.domain.feed.repository.FeedRepository
import com.team05.petmeeting.domain.user.dto.profile.*
import com.team05.petmeeting.domain.user.entity.User
import com.team05.petmeeting.domain.user.errorcode.UserErrorCode
import com.team05.petmeeting.domain.user.provider.Provider
import com.team05.petmeeting.domain.user.repository.UserRepository
import com.team05.petmeeting.global.exception.BusinessException
import jakarta.validation.constraints.NotBlank
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UserProfileService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val feedRepository: FeedRepository,
    private val cheerRepository: CheerRepository,
    private val animalCommentRepository: AnimalCommentRepository,
    private val feedCommentRepository: FeedCommentRepository,
) {

    fun modifyProfileImageUrl(userId: Long, profileImageUrl: String): UserProfileRes =
        getUserById(userId)
            .apply { updateProfileImageUrl(profileImageUrl) }
            .let(UserProfileRes::from)

    fun modifyNickname(userId: Long, nickname: String): UserProfileRes =
        getUserById(userId)
            .apply { updateNickname(nickname) }
            .let(UserProfileRes::from)

    fun modifyPassword(
        userId: Long,
        @NotBlank(message = "현재 비밀번호를 입력해주세요.") currentPassword: String,
        @NotBlank(message = "새 비밀번호를 입력해주세요.") newPassword: String,
    ) {
        val userAuth = getUserById(userId).userAuths
            .firstOrNull { it.provider == Provider.LOCAL }
            ?: throw BusinessException(UserErrorCode.LOCAL_NOT_FOUND)

        val encodedPassword = userAuth.password
            ?: throw BusinessException(UserErrorCode.INVALID_PASSWORD)

        if (!passwordEncoder.matches(currentPassword, encodedPassword)) {
            throw BusinessException(UserErrorCode.INVALID_PASSWORD)
        }

        if (passwordEncoder.matches(newPassword, encodedPassword)) {
            throw BusinessException(UserErrorCode.SAME_AS_OLD_PASSWORD)
        }

        val newEncodedPassword = passwordEncoder.encode(newPassword)
            ?: throw BusinessException(UserErrorCode.INVALID_PASSWORD)
        userAuth.updatePassword(newEncodedPassword)
    }

    fun getMyProfileDetails(userId: Long): MyProfileDetailRes =
        getUserById(userId).let { user ->
            MyProfileDetailRes.of(
                feedRepository.countByUser(user),
                cheerRepository.countByUser(user),
                cheerRepository.countDistinctAnimalByUser(user),
                feedCommentRepository.countFeedCommentByUser(user),
                animalCommentRepository.countAnimalCommentByUser(user),
            )
        }

    fun getMyFeeds(userId: Long): UserFeedRes =
        feedRepository.findAllByUserOrderByCreatedAtDesc(getUserById(userId))
            .let { UserFeedRes.of(it.size.toLong(), it) }

    fun getMyCheerAnimals(userId: Long): UserCheerAnimalRes =
        cheerRepository.findCheerCountsByUser(getUserById(userId))
            .map { row -> arrayOf(requireNotNull(row[0]), requireNotNull(row[1])) }
            .let(UserCheerAnimalRes::from)

    fun getUserProfile(userId: Long): UserProfileRes =
        getUserById(userId).let(UserProfileRes::from)

    fun getUserSummary(userId: Long): UserSummaryRes =
        getUserById(userId).let(UserSummaryRes::from)

    fun getMyFeedComments(userId: Long): UserFeedCommentRes =
        feedCommentRepository.findAllByUserOrderByCreatedAtDesc(getUserById(userId))
            .let { UserFeedCommentRes.of(it.size.toLong(), it) }

    fun getMyAnimalComments(userId: Long): UserAnimalCommentRes =
        animalCommentRepository.findAllByUserOrderByCreatedAtDesc(getUserById(userId))
            .let { UserAnimalCommentRes.of(it.size.toLong(), it) }

    private fun getUserById(userId: Long): User =
        userRepository.findById(userId)
            .orElseThrow { BusinessException(UserErrorCode.USER_NOT_FOUND) }
}
