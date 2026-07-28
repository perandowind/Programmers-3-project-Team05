package com.team05.petmeeting.domain.feed.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.team05.petmeeting.domain.comment.service.CommentService
import com.team05.petmeeting.domain.feed.dto.FeedLikeRes
import com.team05.petmeeting.domain.feed.dto.FeedListRes
import com.team05.petmeeting.domain.feed.dto.FeedReq
import com.team05.petmeeting.domain.feed.dto.FeedRes
import com.team05.petmeeting.domain.feed.enums.FeedCategory
import com.team05.petmeeting.domain.feed.errorcode.FeedErrorCode
import com.team05.petmeeting.domain.feed.service.FeedLikeService
import com.team05.petmeeting.domain.feed.service.FeedService
import com.team05.petmeeting.domain.user.entity.User
import com.team05.petmeeting.domain.user.entity.User.Companion.create
import com.team05.petmeeting.domain.user.repository.UserRepository
import com.team05.petmeeting.global.exception.BusinessException
import com.team05.petmeeting.global.security.config.JacksonConfig
import com.team05.petmeeting.global.security.handler.JwtAuthenticationEntryPoint
import com.team05.petmeeting.global.security.test.WithCustomUser
import com.team05.petmeeting.global.security.userdetails.CustomUserDetails
import com.team05.petmeeting.global.security.util.JwtUtil
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.multipart.MultipartFile
import java.util.Optional

@WebMvcTest(FeedController::class)
@AutoConfigureMockMvc(addFilters = false)
@Import(JacksonConfig::class)
@WithCustomUser(userId = 100L)
internal class FeedControllerTest {
    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var userRepository: UserRepository

    @MockitoBean
    private lateinit var commentService: CommentService

    @MockitoBean
    private lateinit var feedService: FeedService

    @MockitoBean
    private lateinit var feedLikeService: FeedLikeService

    @MockitoBean
    private lateinit var jwtUtil: JwtUtil

    @MockitoBean
    private lateinit var jwtAuthenticationEntryPoint: JwtAuthenticationEntryPoint

    private val userId = 100L
    private val feedId = 1L
    private lateinit var user: User

    @BeforeEach
    fun setUp() {
        user = create("test@test.com", "테스터", "홍길동")
        Mockito.`when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
    }

    @Test
    @DisplayName("피드 이미지 업로드 성공")
    fun uploadImage_success() {
        val file = MockMultipartFile(
            "file",
            "feed.png",
            MediaType.IMAGE_PNG_VALUE,
            "test-image".toByteArray()
        )

        Mockito.`when`(feedService.uploadImage(anyMultipartFile()))
            .thenReturn("https://s3-url.com/feed.png")

        mvc.perform(
            multipart("/api/v1/feeds/images")
                .file(file)
                .with(authentication(auth()))
        )
            .andExpect(handler().handlerType(FeedController::class.java))
            .andExpect(handler().methodName("uploadImage"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.imageUrl").value("https://s3-url.com/feed.png"))
    }


    @Test
    @DisplayName("피드 작성 성공")
    fun write_success() {
        val req = FeedReq(FeedCategory.FREE, "새 피드 제목", "새 피드 내용입니다.", null, null)
        val res = feedRes(
            title = "새 피드 제목",
            content = "새 피드 내용입니다."
        )

        Mockito.`when`(feedService.write(req, user)).thenReturn(res)

        mvc.perform(
            post("/api/v1/feeds")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
                .with(authentication(auth()))
        )
            .andExpect(handler().handlerType(FeedController::class.java))
            .andExpect(handler().methodName("write"))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.title").value("새 피드 제목"))
            .andExpect(jsonPath("$.content").value("새 피드 내용입니다."))
    }

    @Test
    @DisplayName("피드 작성 실패 - 제목이 비어있으면 400 응답")
    fun write_blank_title_fail() {
        val request = FeedReq(
            category = FeedCategory.FREE,
            title = "",
            content = "10자 이상인 테스트 내용입니다.",
            imageUrl = null,
            animalId = null
        )

        mvc.perform(
            post("/api/v1/feeds")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(authentication(auth()))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
            .andExpect(jsonPath("$.details[0].field").value("title"))
    }

    @Test
    @DisplayName("피드 단건 조회 성공")
    fun getFeed_success() {
        Mockito.`when`(feedService.getFeed(feedId)).thenReturn(
            feedRes(title = "테스트 피드", content = "내용")
        )

        mvc.perform(get("/api/v1/feeds/{feedId}", feedId))
            .andExpect(handler().handlerType(FeedController::class.java))
            .andExpect(handler().methodName("getFeed"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("테스트 피드"))
            .andExpect(jsonPath("$.content").value("내용"))
    }

    @Test
    @DisplayName("피드 단건 조회 실패 - 없는 피드")
    fun getFeed_not_found() {
        Mockito.`when`(feedService.getFeed(999L))
            .thenThrow(BusinessException(FeedErrorCode.FEED_NOT_FOUND))

        mvc.perform(get("/api/v1/feeds/{feedId}", 999L))
            .andExpect(handler().handlerType(FeedController::class.java))
            .andExpect(handler().methodName("getFeed"))
            .andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("피드 목록 조회 성공")
    fun getFeeds_success() {
        Mockito.`when`(
            feedService.getFeeds(
                anyPageable(),
                eq(userId),
                isNull<FeedCategory>()
            )
        ).thenReturn(PageImpl<FeedListRes>(emptyList()))


        mvc.perform(get("/api/v1/feeds"))
            .andExpect(handler().handlerType(FeedController::class.java))
            .andExpect(handler().methodName("getFeeds"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isArray)
    }

    @Test
    @DisplayName("피드 수정 성공")
    fun modify_success() {
        val req = FeedReq(FeedCategory.FREE, "수정된 제목", "수정된 내용입니다.", null, null)
        val res = feedRes(title = "수정된 제목", content = "수정된 내용입니다.")

        Mockito.`when`(feedService.modify(feedId, req, user)).thenReturn(res)

        mvc.perform(
            put("/api/v1/feeds/{feedId}", feedId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
                .with(authentication(auth()))
        )
            .andExpect(handler().handlerType(FeedController::class.java))
            .andExpect(handler().methodName("modify"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("수정된 제목"))
            .andExpect(jsonPath("$.content").value("수정된 내용입니다."))
    }

    @Test
    @DisplayName("피드 수정 실패 - 없는 피드")
    fun modify_not_found() {
        val req = FeedReq(FeedCategory.FREE, "수정된 제목", "수정된 내용입니다.", null, null)

        Mockito.`when`(feedService.modify(999L, req, user))
            .thenThrow(BusinessException(FeedErrorCode.FEED_NOT_FOUND))

        mvc.perform(
            put("/api/v1/feeds/{feedId}", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
                .with(authentication(auth()))
        )
            .andExpect(handler().handlerType(FeedController::class.java))
            .andExpect(handler().methodName("modify"))
            .andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("피드 삭제 성공")
    fun delete_success() {
        mvc.perform(
            delete("/api/v1/feeds/{feedId}", feedId)
                .with(authentication(auth()))
        )
            .andExpect(handler().handlerType(FeedController::class.java))
            .andExpect(handler().methodName("delete"))
            .andExpect(status().isNoContent)
    }

    @Test
    @DisplayName("피드 삭제 실패 - 없는 피드")
    fun delete_not_found() {
        Mockito.doThrow(BusinessException(FeedErrorCode.FEED_NOT_FOUND))
            .`when`(feedService)
            .delete(999L, user)

        mvc.perform(
            delete("/api/v1/feeds/{feedId}", 999L)
                .with(authentication(auth()))
        )
            .andExpect(handler().handlerType(FeedController::class.java))
            .andExpect(handler().methodName("delete"))
            .andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("피드 수정 실패 - 다른 사람 피드 수정 시도 → 403")
    fun modify_forbidden() {
        val req = FeedReq(FeedCategory.FREE, "수정된 제목", "수정된 내용입니다.", null, null)

        Mockito.`when`(feedService.modify(feedId, req, user))
            .thenThrow(BusinessException(FeedErrorCode.FORBIDDEN))

        mvc.perform(
            put("/api/v1/feeds/{feedId}", feedId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
                .with(authentication(auth()))
        )
            .andExpect(handler().handlerType(FeedController::class.java))
            .andExpect(handler().methodName("modify"))
            .andExpect(status().isForbidden)
    }

    @Test
    @DisplayName("피드 삭제 실패 - 다른 사람 피드 삭제 시도 → 403")
    fun delete_forbidden() {
        Mockito.doThrow(BusinessException(FeedErrorCode.FORBIDDEN))
            .`when`(feedService)
            .delete(feedId, user)

        mvc.perform(
            delete("/api/v1/feeds/{feedId}", feedId)
                .with(authentication(auth()))
        )
            .andExpect(handler().handlerType(FeedController::class.java))
            .andExpect(handler().methodName("delete"))
            .andExpect(status().isForbidden)
    }

    @Test
    @DisplayName("좋아요 토글 성공")
    fun toggleLike_success() {
        Mockito.`when`(feedLikeService.toggleLike(feedId, user))
            .thenReturn(FeedLikeRes(1, true))

        mvc.perform(
            post("/api/v1/feeds/{feedId}/likes", feedId)
                .with(authentication(auth()))
        )
            .andExpect(handler().handlerType(FeedController::class.java))
            .andExpect(handler().methodName("toggleLike"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.likeCount").value(1))
            .andExpect(jsonPath("$.isLiked").value(true))
    }

    private fun auth(): UsernamePasswordAuthenticationToken {
        val userDetails = CustomUserDetails(userId, emptyList<GrantedAuthority>())
        return UsernamePasswordAuthenticationToken(userDetails, null, emptyList<GrantedAuthority>())
    }

    private fun anyPageable(): Pageable {
        return any(Pageable::class.java) ?: Pageable.unpaged()
    }

    private fun anyMultipartFile(): MultipartFile {
        return any(MultipartFile::class.java) ?: MockMultipartFile(
            "file",
            ByteArray(0)
        )
    }

    private fun feedRes(
        title: String,
        content: String,
        category: FeedCategory = FeedCategory.FREE
    ): FeedRes {
        return FeedRes(
            profileImageUrl = null,
            nickname = "테스터",
            feedId = feedId,
            userId = userId,
            animalId = null,
            category = category,
            title = title,
            content = content,
            imageUrl = null,
            likeCount = 0,
            commentCount = 0,
            comments = emptyList(),
            createdAt = null,
            updatedAt = null
        )
    }
}
