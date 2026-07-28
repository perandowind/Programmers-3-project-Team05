package com.team05.petmeeting.domain.feed.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.team05.petmeeting.domain.comment.dto.CommentReq
import com.team05.petmeeting.domain.comment.dto.FeedCommentListRes
import com.team05.petmeeting.domain.comment.dto.FeedCommentRes
import com.team05.petmeeting.domain.comment.errorcode.CommentErrorCode
import com.team05.petmeeting.domain.comment.service.CommentService
import com.team05.petmeeting.global.exception.BusinessException
import com.team05.petmeeting.global.security.test.WithCustomUser
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithAnonymousUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@WithCustomUser(userId = 100L)
internal class FeedControllerCommentTest {
    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var commentService: CommentService


    // 피드 댓글 작성 성공
    @Test
    @DisplayName("피드 댓글 생성 성공")
    @Throws(Exception::class)
    fun createFeedComment_success() {
        val req = CommentReq("테스트 댓글입니다.")
        val res = FeedCommentRes(100L, "테스터", "", 1L, "테스트 댓글입니다.", 1L, LocalDateTime.now())
        Mockito.`when`<FeedCommentRes>(commentService.createFeedComment(100L, 1L, req)).thenReturn(res)

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/feeds/{feedId}/comments", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.content").value("테스트 댓글입니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.feedId").value(1L))
    }

    // 피드 댓글 작성 실패 - 비로그인 (401)
    @Test
    @WithAnonymousUser
    @DisplayName("피드 댓글 작성 실패 - 비로그인 (401)")
    @Throws(Exception::class)
    fun createFeedComment_fail_unauthorized() {
        val req = CommentReq("테스트 댓글입니다.")

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/feeds/{feedId}/comments", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
        )
            .andExpect(MockMvcResultMatchers.status().isUnauthorized())
    }

    // 피드 댓글 작성 실패 - 빈 내용 (400)
    @Test
    @DisplayName("피드 댓글 작성 실패 - 빈 내용 (400)")
    @Throws(Exception::class)
    fun createFeedComment_fail_blank() {
        val req = CommentReq("")

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/feeds/{feedId}/comments", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
    }

    // 피드 댓글 수정 성공
    @Test
    @DisplayName("피드 댓글 수정 성공")
    @Throws(Exception::class)
    fun updateFeedComment_success() {
        val req = CommentReq("수정된 댓글입니다.")
        val res = FeedCommentRes(100L, "테스터", "", 1L, "수정된 댓글입니다.", 1L, LocalDateTime.now())
        Mockito.`when`<FeedCommentRes>(commentService.updateFeedComment(100L, 1L, 1L, req)).thenReturn(res)

        mvc.perform(
            MockMvcRequestBuilders.patch("/api/v1/feeds/{feedId}/comments/{commentId}", 1L, 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.content").value("수정된 댓글입니다."))
    }

    // 피드 댓글 수정 실패 - 권한 없는 유저 (401)
    @Test
    @DisplayName("피드 댓글 수정 실패 - 권한 없는 유저 (401)")
    @Throws(Exception::class)
    fun updateFeedComment_fail_unauthorized() {
        val req = CommentReq("수정된 댓글입니다.")
        Mockito.`when`<FeedCommentRes>(commentService.updateFeedComment(100L, 1L, 1L, req))
            .thenThrow(BusinessException(CommentErrorCode.UNAUTHORIZED))

        mvc.perform(
            MockMvcRequestBuilders.patch("/api/v1/feeds/{feedId}/comments/{commentId}", 1L, 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
        )
            .andExpect(MockMvcResultMatchers.status().isUnauthorized())
    }

    // 피드 댓글 삭제 성공
    @Test
    @DisplayName("피드 댓글 삭제 성공")
    @Throws(Exception::class)
    fun deleteFeedComment_success() {
        Mockito.doNothing().`when`<CommentService>(commentService).deleteFeedComment(100L, 1L, 1L)

        mvc.perform(MockMvcRequestBuilders.delete("/api/v1/feeds/{feedId}/comments/{commentId}", 1L, 1L))
            .andExpect(MockMvcResultMatchers.status().isNoContent())
    }

    // 피드 댓글 삭제 실패 - 존재하지 않는 댓글 (404)
    @Test
    @DisplayName("피드 댓글 삭제 실패 - 존재하지 않는 댓글 (404)")
    @Throws(Exception::class)
    fun deleteFeedComment_fail_notFound() {
        Mockito.doThrow(BusinessException(CommentErrorCode.COMMENT_NOT_FOUND))
            .`when`<CommentService>(commentService).deleteFeedComment(100L, 1L, 1L)

        mvc.perform(MockMvcRequestBuilders.delete("/api/v1/feeds/{feedId}/comments/{commentId}", 1L, 1L))
            .andExpect(MockMvcResultMatchers.status().isNotFound())
    }

    // 피드 댓글 목록 조회 성공
    @DisplayName("피드 댓글 목록 조회 성공")
    @Test
    @Throws(Exception::class)
    fun getFeedComments_success() {
        val emptyPage = PageImpl<FeedCommentRes>(emptyList())
        Mockito.`when`(commentService.getFeedComments(anyLong(), any()))
            .thenReturn(emptyPage)

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/feeds/{feedId}/comments", 1L))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.totalCount").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.totalPages").value(1))
    }

    // 피드 댓글 목록 조회 - 비로그인도 가능
    @Test
    @WithAnonymousUser
    @DisplayName("피드 댓글 목록 조회 - 비로그인도 가능")
    @Throws(Exception::class)
    fun getFeedComments_anonymous_success() {
        val emptyPage = PageImpl<FeedCommentRes>(emptyList())
        Mockito.`when`(commentService.getFeedComments(anyLong(), any()))
            .thenReturn(emptyPage)

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/feeds/{feedId}/comments", 1L))
            .andExpect(MockMvcResultMatchers.status().isOk())
    }
}
