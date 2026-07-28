package com.team05.petmeeting.domain.animal.controller

import com.team05.petmeeting.domain.comment.dto.AnimalCommentListRes
import com.team05.petmeeting.domain.comment.dto.AnimalCommentRes
import com.team05.petmeeting.domain.comment.dto.CommentReq
import com.team05.petmeeting.domain.comment.service.CommentService
import com.team05.petmeeting.global.security.userdetails.CustomUserDetails
import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/animals")
class AnimalCommentController(private val commentService: CommentService) {
    @Operation(summary = "동물 댓글 조회")
    @GetMapping("/{animalId}/comments")
    fun getAnimalComments(
        @PathVariable animalId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<AnimalCommentListRes> {
        log.info("=============================== 댓글 조회 호출 ================================")
        val pageable: Pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending())
        val commentPage: Page<AnimalCommentRes> = commentService.getAnimalComments(animalId, pageable)
        val res: AnimalCommentListRes = AnimalCommentListRes.from(commentPage)
        return ResponseEntity.ok(res)
    }

    @Operation(summary = "동물 댓글 작성")
    @PostMapping("/{animalId}/comments")
    fun createAnimalComment(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @PathVariable animalId: Long,
        @Valid @RequestBody commentReq: CommentReq
    ): ResponseEntity<AnimalCommentRes> {
        log.info("=============================== 댓글 작성 호출 ================================")
        val res = commentService.createAnimalComment(userDetails.userId, animalId, commentReq)
        return ResponseEntity.ok(res)
    }

    @Operation(summary = "동물 댓글 수정")
    @PatchMapping("/{animalId}/comments/{commentId}")
    fun updateComment(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @PathVariable animalId: Long,
        @PathVariable commentId: Long,
        @Valid @RequestBody commentReq: CommentReq
    ): ResponseEntity<AnimalCommentRes> {
        val res = commentService.updateAnimalComment(userDetails.userId, animalId, commentId, commentReq)
        return ResponseEntity.ok(res)
    }

    @Operation(summary = "동물 댓글 삭제")
    @DeleteMapping("/{animalId}/comments/{commentId}")
    fun deleteComment(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @PathVariable animalId: Long,
        @PathVariable commentId: Long
    ): ResponseEntity<Void> {
        commentService.deleteAnimalComment(userDetails.userId, animalId, commentId)
        return ResponseEntity.noContent().build()
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(AnimalCommentController::class.java)
    }
}
