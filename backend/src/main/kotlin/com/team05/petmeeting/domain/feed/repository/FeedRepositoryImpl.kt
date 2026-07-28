package com.team05.petmeeting.domain.feed.repository

import com.querydsl.core.types.Expression
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import com.team05.petmeeting.domain.comment.entity.QFeedComment
import com.team05.petmeeting.domain.feed.dto.FeedListRes
import com.team05.petmeeting.domain.feed.entity.QFeed
import com.team05.petmeeting.domain.feed.entity.QFeedLike
import com.team05.petmeeting.domain.feed.enums.FeedCategory
import com.team05.petmeeting.domain.feed.errorcode.FeedErrorCode
import com.team05.petmeeting.global.exception.BusinessException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

class FeedRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : FeedRepositoryCustom {

    override fun findFeeds(
        pageable: Pageable,
        userId: Long?,
        category: FeedCategory?
    ): Page<FeedListRes> {
        val feed = QFeed.feed
        val feedLike = QFeedLike.feedLike
        val feedComment = QFeedComment.feedComment

        val likeCountExpression: Expression<Long> = JPAExpressions
            .select(feedLike.count())
            .from(feedLike)
            .where(feedLike.feed.eq(feed))

        val commentCountExpression: Expression<Long> = JPAExpressions
            .select(feedComment.count())
            .from(feedComment)
            .where(feedComment.feed.eq(feed))

        val isLikedExpression: Expression<Boolean> = if (userId != null) {
            JPAExpressions
                .selectOne()
                .from(feedLike)
                .where(
                    feedLike.feed.eq(feed),
                    feedLike.user.id.eq(userId)
                )
                .exists()
        } else {
            Expressions.FALSE
        }

        val content = queryFactory
            .select(
                feed,
                feed.user.id,
                feed.user.profileImageUrl,
                feed.user.nickname,
                feed.animal.id,
                likeCountExpression,
                commentCountExpression,
                isLikedExpression
            )
            .from(feed)
            .join(feed.user)
            .leftJoin(feed.animal)
            .where(categoryEq(category))
            .orderBy(feed.createdAt.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()
            .map { tuple ->
                val foundFeed = tuple.get(feed)
                    ?: throw BusinessException(FeedErrorCode.FEED_NOT_FOUND)

                val authorId = tuple.get(feed.user.id)
                    ?: throw BusinessException(FeedErrorCode.FEED_NOT_FOUND)

                val nickname = tuple.get(feed.user.nickname)
                    ?: throw BusinessException(FeedErrorCode.FEED_NOT_FOUND)

                FeedListRes.from(
                    feed = foundFeed,
                    userId = authorId,
                    profileImageUrl = tuple.get(feed.user.profileImageUrl),
                    nickname = nickname,
                    animalId = tuple.get(feed.animal.id),
                    likeCount = tuple.get(likeCountExpression) ?: 0L,
                    commentCount = tuple.get(commentCountExpression) ?: 0L,
                    isLiked = tuple.get(isLikedExpression) ?: false
                )
            }

        val total = queryFactory
            .select(feed.count())
            .from(feed)
            .where(categoryEq(category))
            .fetchOne() ?: 0L

        return PageImpl(content, pageable, total)
    }

    private fun categoryEq(category: FeedCategory?): BooleanExpression? {
        val feed = QFeed.feed
        return category?.let { feed.category.eq(it) }
    }
}
