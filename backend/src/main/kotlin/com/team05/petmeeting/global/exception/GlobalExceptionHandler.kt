package com.team05.petmeeting.global.exception

import jakarta.validation.ConstraintViolationException
import jakarta.validation.Path
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice

class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(BusinessException::class)
    fun handleBusiness(
        e: BusinessException
    ): ResponseEntity<ErrorResponse> {
        log.error("BusinessException: {}", e.message)

        val code = e.errorCode

        return ResponseEntity
            .status(code.status)
            .body(ErrorResponse.from(code))
    }

    // @Valid 유효성 검사 실패 시 처리 (@RequestBody 검증)
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(
        e: MethodArgumentNotValidException
    ): ResponseEntity<ErrorResponse> {

        log.error("MethodArgumentNotValidException 발생")

        val errors = e.bindingResult.fieldErrors.map {
            ValidationError(
                it.field,
                it.defaultMessage ?: "잘못된 입력값입니다."
            )
        }

        return ResponseEntity
            .badRequest()
            .body(
                ErrorResponse.of(
                    "INVALID_INPUT",
                    "입력값이 올바르지 않습니다.",
                    errors
                )
            )
    }

    // @Validated 유효성 검사 실패 처리 (@RequestParam, @PathVariable 검증)
    @ExceptionHandler(ConstraintViolationException::class)
    fun handleValidation(
        e: ConstraintViolationException
    ): ResponseEntity<ErrorResponse> {

        log.error("ConstraintViolationException 발생")

        val errors = e.constraintViolations.map {
            ValidationError(
                extractField(it.propertyPath),
                it.message
            )
        }

        val errorCode = GlobalErrorCode.INVALID_INPUT_VALUE

        return ResponseEntity
            .status(errorCode.status)
            .body(
                ErrorResponse.of(
                    errorCode.code,
                    errorCode.message,
                    errors
                )
            )
    }

    // ConstraintViolationException에서 필드명만 추출
    private fun extractField(path: Path): String {
        var field = ""

        for (node in path) {
            field = node.name
        }

        return field
    }

}
