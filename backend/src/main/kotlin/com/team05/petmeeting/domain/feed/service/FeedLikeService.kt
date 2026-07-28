package com.team05.petmeeting.domain.feed.service

import com.team05.petmeeting.domain.feed.dto.FeedLikeRes
import com.team05.petmeeting.domain.feed.entity.FeedLike
import com.team05.petmeeting.domain.feed.errorcode.FeedErrorCode
import com.team05.petmeeting.domain.feed.repository.FeedLikeRepository
import com.team05.petmeeting.domain.feed.repository.FeedRepository
import com.team05.petmeeting.domain.user.entity.User
import com.team05.petmeeting.global.exception.BusinessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FeedLikeService(
    private val feedLikeRepository: FeedLikeRepository,
    private val feedRepository: FeedRepository
) {

    @Transactional
    fun toggleLike(feedId: Long, user: User): FeedLikeRes {
        val feed = feedRepository.findById(feedId)
            .orElseThrow { BusinessException(FeedErrorCode.FEED_NOT_FOUND) }

        val existingLike = feedLikeRepository.findByUserAndFeed(user, feed)

        val isLiked = if (existingLike.isPresent) {
            feedLikeRepository.delete(existingLike.get())
            false
        } else {
            feedLikeRepository.save(FeedLike(user, feed))
            true
        }

        val likeCount = feedLikeRepository.countByFeed(feed).toInt()

        return FeedLikeRes(
            likeCount = likeCount,
            isLiked = isLiked
        )
    }
}