package com.team05.petmeeting.domain.comment.service

import com.team05.petmeeting.domain.animal.service.AnimalService
import com.team05.petmeeting.domain.comment.dto.AnimalCommentRes
import com.team05.petmeeting.domain.comment.dto.AnimalCommentRes.Companion.from
import com.team05.petmeeting.domain.comment.dto.CommentReq
import com.team05.petmeeting.domain.comment.dto.FeedCommentRes
import com.team05.petmeeting.domain.comment.entity.AnimalComment
import com.team05.petmeeting.domain.comment.entity.FeedComment
import com.team05.petmeeting.domain.comment.entity.FeedComment.Companion.create
import com.team05.petmeeting.domain.comment.errorcode.CommentErrorCode
import com.team05.petmeeting.domain.comment.repository.AnimalCommentRepository
import com.team05.petmeeting.domain.comment.repository.FeedCommentRepository
import com.team05.petmeeting.domain.feed.service.FeedService
import com.team05.petmeeting.domain.user.entity.User
import com.team05.petmeeting.domain.user.errorcode.UserErrorCode
import com.team05.petmeeting.domain.user.repository.UserRepository
import com.team05.petmeeting.global.exception.BusinessException
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CommentService(
    private val feedCommentRepository: FeedCommentRepository,
    private val animalCommentRepository: AnimalCommentRepository,
    private val animalService: AnimalService,
    private val feedService: FeedService,
    private val userRepository: UserRepository
) {
    @Transactional
    fun createAnimalComment(userId: Long, animalId: Long, commentReq: CommentReq): AnimalCommentRes {
        val user = getUserOrThrow(userId)
        val animal = animalService.findByAnimalId(animalId)
        val comment = AnimalComment.create(user, animal, commentReq.content)
        return from(animalCommentRepository.save<AnimalComment>(comment))
    }

    @Transactional
    fun createFeedComment(userId: Long, feedId: Long, commentReq: CommentReq): FeedCommentRes {
        val user = getUserOrThrow(userId)
        val feed = feedService.findByFeedId(feedId)
        val comment = create(user, feed, commentReq.content)
        val savedComment = feedCommentRepository.save<FeedComment>(comment)
        return FeedCommentRes.from(savedComment)
    }

    @Transactional
    fun updateAnimalComment(userId: Long, animalId: Long, commentId: Long, @Valid commentReq: CommentReq): AnimalCommentRes {
        val comment = animalCommentRepository.findById(commentId)
            .orElseThrow{ BusinessException(CommentErrorCode.COMMENT_NOT_FOUND) }
        validateAnimalCommentBelongsToAnimal(animalId, comment)
        validateAnimalCommentAuthor(userId, comment)
        comment.updateContent(commentReq.content)
        return from(animalCommentRepository.save(comment))
    }

    @Transactional
    fun updateFeedComment(userId: Long, feedId: Long, commentId: Long, @Valid commentReq: CommentReq): FeedCommentRes {
        val comment = feedCommentRepository.findById(commentId)
            .orElseThrow { BusinessException(CommentErrorCode.COMMENT_NOT_FOUND) }
        validateFeedCommentBelongsToFeed(feedId, comment)
        validateFeedCommentAuthor(userId, comment)
        comment.updateContent(commentReq.content)
        return FeedCommentRes.from(feedCommentRepository.save(comment))
    }

    @Transactional
    fun deleteAnimalComment(userId: Long, animalId: Long, commentId: Long) {
        val comment = animalCommentRepository.findById(commentId)
            .orElseThrow{ BusinessException(CommentErrorCode.COMMENT_NOT_FOUND) }
        validateAnimalCommentBelongsToAnimal(animalId, comment)
        validateAnimalCommentAuthor(userId, comment)
        animalCommentRepository.delete(comment)
    }

    @Transactional
    fun deleteFeedComment(userId: Long, feedId: Long, commentId: Long) {
        val comment = feedCommentRepository.findById(commentId)
            .orElseThrow{ BusinessException(CommentErrorCode.COMMENT_NOT_FOUND) }
        validateFeedCommentBelongsToFeed(feedId, comment)
        validateFeedCommentAuthor(userId, comment)
        feedCommentRepository.delete(comment)
    }

    fun getAnimalComments(animalId: Long): List<AnimalCommentRes> {
        return animalCommentRepository.findByAnimal_Id(animalId)
            .map { animalComment -> from(animalComment) }
    }

    fun getAnimalComments(animalId: Long, pageable: Pageable): Page<AnimalCommentRes> {
        return animalCommentRepository.findByAnimal_Id(animalId, pageable)
            .map { animalComment -> from(animalComment) }
    }

    fun getFeedComments(feedId: Long): List<FeedCommentRes> {
        return feedCommentRepository.findByFeed_Id(feedId)
            .map { feedComment -> FeedCommentRes.from(feedComment) }
    }

    fun getFeedComments(feedId: Long, pageable: Pageable): Page<FeedCommentRes> {
        return feedCommentRepository.findByFeed_Id(feedId, pageable)
            .map { feedComment -> FeedCommentRes.from(feedComment) }
    }

    private fun getUserOrThrow(userId: Long): User {
        return userRepository.findById(userId)
            .orElseThrow { BusinessException(UserErrorCode.USER_NOT_FOUND) }
    }

    private fun validateAnimalCommentBelongsToAnimal(animalId: Long, comment: AnimalComment) {
        if (comment.animal.id != animalId) {
            throw BusinessException(CommentErrorCode.COMMENT_NOT_FOUND)
        }
    }

    private fun validateFeedCommentBelongsToFeed(feedId: Long, comment: FeedComment) {
        if (comment.feed.id != feedId) {
            throw BusinessException(CommentErrorCode.COMMENT_NOT_FOUND)
        }
    }

    private fun validateAnimalCommentAuthor(userId: Long, comment: AnimalComment) {
        if (comment.user.id != userId) {
            throw BusinessException(CommentErrorCode.UNAUTHORIZED)
        }
    }

    private fun validateFeedCommentAuthor(userId: Long, comment: FeedComment) {
        if (comment.user.id != userId) {
            throw BusinessException(CommentErrorCode.UNAUTHORIZED)
        }
    }
}
