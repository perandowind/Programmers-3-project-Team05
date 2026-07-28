package com.team05.petmeeting.domain.feed.controller

import com.team05.petmeeting.domain.comment.dto.CommentReq
import com.team05.petmeeting.domain.comment.dto.FeedCommentListRes
import com.team05.petmeeting.domain.comment.dto.FeedCommentRes
import com.team05.petmeeting.domain.comment.service.CommentService
import com.team05.petmeeting.domain.feed.dto.*
import com.team05.petmeeting.domain.feed.enums.FeedCategory
import com.team05.petmeeting.domain.feed.service.FeedLikeService
import com.team05.petmeeting.domain.feed.service.FeedService
import com.team05.petmeeting.domain.user.entity.User
import com.team05.petmeeting.domain.user.errorcode.UserErrorCode
import com.team05.petmeeting.domain.user.repository.UserRepository
import com.team05.petmeeting.global.exception.BusinessException
import com.team05.petmeeting.global.security.userdetails.CustomUserDetails
import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/feeds")
class FeedController(
    private val commentService: CommentService,
    private val feedService: FeedService,
    private val userRepository: UserRepository,
    private val feedLikeService: FeedLikeService
) {
    private val log = LoggerFactory.getLogger(FeedController::class.java)

    @Operation(summary = "피드 댓글 작성")
    @PostMapping("/{feedId}/comments")
    fun createFeedComment(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @PathVariable feedId: Long,
        @Valid @RequestBody commentReq: CommentReq
    ): ResponseEntity<FeedCommentRes> {
        log.info("=============== 피드 댓글 작성 =================")
        val res = commentService.createFeedComment(userDetails.userId, feedId, commentReq)
        return ResponseEntity.ok(res)
    }

    @Operation(summary = "피드 댓글 수정")
    @PatchMapping("/{feedId}/comments/{commentId}")
    fun updateComment(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @PathVariable feedId: Long,
        @PathVariable commentId: Long,
        @Valid @RequestBody commentReq: CommentReq
    ): ResponseEntity<FeedCommentRes> {
        val res = commentService.updateFeedComment(userDetails.userId, feedId, commentId, commentReq)
        return ResponseEntity.ok(res)
    }

    @Operation(summary = "피드 댓글 삭제")
    @DeleteMapping("/{feedId}/comments/{commentId}")
    fun deleteComment(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @PathVariable feedId: Long,
        @PathVariable commentId: Long
    ): ResponseEntity<Void> {
        commentService.deleteFeedComment(userDetails.userId, feedId, commentId)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "피드 댓글 목록 조회")
    @GetMapping("/{feedId}/comments")
    fun getFeedComments(
        @PathVariable feedId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<FeedCommentListRes> {
        log.info("=============== 피드 댓글 조회 =================")
        val pageable = PageRequest.of(page, size, org.springframework.data.domain.Sort.by("createdAt").ascending())
        val commentPage = commentService.getFeedComments(feedId, pageable)
        val res = FeedCommentListRes.from(commentPage)
        return ResponseEntity.ok(res)
    }

    @Operation(summary = "피드 이미지 업로드")
    @PostMapping("/images", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadImage(
        @RequestPart file: MultipartFile
    ): ResponseEntity<FeedImageUploadRes> {
        val imageUrl = feedService.uploadImage(file)
        return ResponseEntity.ok(FeedImageUploadRes(imageUrl))
    }

    @Operation(summary = "피드 글 작성")
    @PostMapping
    fun write(
        @Valid @RequestBody request: FeedReq,
        @AuthenticationPrincipal userDetails: CustomUserDetails
    ): ResponseEntity<FeedRes> {
        val user = getUserOrThrow(userDetails)
        val res = feedService.write(request, user)
        return ResponseEntity.status(201).body(res)
    }

    @Operation(summary = "피드 글 수정")
    @PutMapping("/{feedId}")
    fun modify(
        @PathVariable feedId: Long,
        @Valid @RequestBody request: FeedReq,
        @AuthenticationPrincipal userDetails: CustomUserDetails
    ): ResponseEntity<FeedRes> {
        val user = getUserOrThrow(userDetails)
        val res = feedService.modify(feedId, request, user)
        return ResponseEntity.ok(res)
    }

    @Operation(summary = "피드 글 삭제")
    @DeleteMapping("/{feedId}")
    fun delete(
        @PathVariable feedId: Long,
        @AuthenticationPrincipal userDetails: CustomUserDetails
    ): ResponseEntity<Void> {
        val user = getUserOrThrow(userDetails)
        feedService.delete(feedId, user)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "피드 상세 조회")
    @GetMapping("/{feedId}")
    fun getFeed(
        @PathVariable feedId: Long
    ): ResponseEntity<FeedRes> {
        val res = feedService.getFeed(feedId)
        return ResponseEntity.ok(res)
    }

    @Operation(summary = "피드 목록 조회")
    @GetMapping
    fun getFeeds(
        pageable: Pageable,
        @AuthenticationPrincipal userDetails: CustomUserDetails?,
        @RequestParam(required = false) category: FeedCategory?
    ): ResponseEntity<Page<FeedListRes>> {
        val userId = userDetails?.userId
        val feeds = feedService.getFeeds(pageable, userId, category)
        return ResponseEntity.ok(feeds)
    }

    @Operation(summary = "피드 좋아요 토글")
    @PostMapping("/{feedId}/likes")
    fun toggleLike(
        @PathVariable feedId: Long,
        @AuthenticationPrincipal userDetails: CustomUserDetails
    ): ResponseEntity<FeedLikeRes> {
        val user = getUserOrThrow(userDetails)
        val res = feedLikeService.toggleLike(feedId, user)
        return ResponseEntity.ok(res)
    }

    @Operation(summary = "입양 후기 작성용 동물 목록 조회")
    @GetMapping("/adoptable-animals")
    fun getAdoptedAnimals(
        @AuthenticationPrincipal userDetails: CustomUserDetails
    ): ResponseEntity<List<AdoptedAnimalRes>> {
        val res = feedService.getAdoptedAnimals(userDetails.userId)
        return ResponseEntity.ok(res)
    }

    private fun getUserOrThrow(userDetails: CustomUserDetails): User {
        return userRepository.findById(userDetails.userId)
            .orElseThrow { BusinessException(UserErrorCode.USER_NOT_FOUND) }
    }
}
