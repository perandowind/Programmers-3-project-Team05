package com.team05.petmeeting.domain.feed.service

import com.team05.petmeeting.domain.feed.entity.Feed
import com.team05.petmeeting.domain.feed.entity.FeedLike
import com.team05.petmeeting.domain.feed.enums.FeedCategory
import com.team05.petmeeting.domain.feed.errorcode.FeedErrorCode
import com.team05.petmeeting.domain.feed.repository.FeedLikeRepository
import com.team05.petmeeting.domain.feed.repository.FeedRepository
import com.team05.petmeeting.domain.user.entity.User
import com.team05.petmeeting.domain.user.entity.User.Companion.create
import com.team05.petmeeting.global.entity.BaseEntity
import com.team05.petmeeting.global.exception.BusinessException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import java.util.*

@ExtendWith(MockitoExtension::class)
internal class FeedLikeServiceTest {
    @InjectMocks
    private lateinit var feedLikeService: FeedLikeService

    @Mock
    private lateinit var feedLikeRepository: FeedLikeRepository

    @Mock
    private lateinit var feedRepository: FeedRepository

    private lateinit var user: User
    private lateinit var feed: Feed

    @BeforeEach
    @Throws(Exception::class)
    fun setUp() {
        user = create("test@test.com", "테스터", "홍길동")
        val idField = BaseEntity::class.java.getDeclaredField("id")
        idField.setAccessible(true)
        idField.set(user, 1L)

        feed = Feed(user, FeedCategory.FREE, "제목", "내용", null, null)
        idField.set(feed, 1L)
    }

    @Test
    @DisplayName("toggleLike - 좋아요 안 눌렀을 때 좋아요 추가 성공")
    fun toggleLike_add_success() {
        // given
        val feedId = 1L

        Mockito.`when`(feedRepository.findById(feedId)).thenReturn(Optional.of(feed))
        Mockito.`when`(feedLikeRepository.findByUserAndFeed(user, feed)).thenReturn(Optional.empty())
        Mockito.`when`(feedLikeRepository.countByFeed(feed)).thenReturn(1L)
        Mockito.`when`(feedLikeRepository.save(any(FeedLike::class.java)))
            .thenAnswer { invocation -> invocation.getArgument(0) }

        // when
        val res = feedLikeService.toggleLike(feedId, user)

        // then
        assertThat(res.likeCount).isEqualTo(1)
        assertThat(res.isLiked).isTrue()
        verify(feedLikeRepository).save(any(FeedLike::class.java))
    }

    @Test
    @DisplayName("toggleLike - 이미 좋아요 눌렀을 때 좋아요 취소 성공")
    fun toggleLike_cancel_success() {
        // given
        val feedId = 1L
        val existingLike = FeedLike(user, feed)

        Mockito.`when`(feedRepository.findById(feedId)).thenReturn(Optional.of(feed))
        Mockito.`when`(feedLikeRepository.findByUserAndFeed(user, feed))
            .thenReturn(Optional.of(existingLike))
        Mockito.`when`(feedLikeRepository.countByFeed(feed)).thenReturn(0L)

        // when
        val res = feedLikeService.toggleLike(feedId, user)

        // then
        assertThat(res.likeCount).isEqualTo(0)
        assertThat(res.isLiked).isFalse()
        verify(feedLikeRepository).delete(existingLike)
        verify(feedLikeRepository, never()).save(any(FeedLike::class.java))
    }

    @Test
    @DisplayName("toggleLike - 존재하지 않는 피드에 좋아요 시 예외 발생")
    fun toggleLike_feed_not_found_throws_exception() {
        // given
        val feedId = 999L

        Mockito.`when`(feedRepository.findById(feedId)).thenReturn(Optional.empty())

        // when & then
        assertThatThrownBy { feedLikeService.toggleLike(feedId, user) }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(FeedErrorCode.FEED_NOT_FOUND)
    }
}
