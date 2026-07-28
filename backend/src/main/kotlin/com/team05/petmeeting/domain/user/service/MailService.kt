package com.team05.petmeeting.domain.user.service

import jakarta.mail.internet.MimeMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context

@Service
class MailService(
    private val mailSender: JavaMailSender,
    private val templateEngine: TemplateEngine,
) {

    fun sendMail(email: String, code: String) {
        runCatching {
            mailSender.createMimeMessage()
                .also { message ->
                    message.writeSignupOtpMail(email, code)
                    mailSender.send(message)
                }
        }.getOrElse {
            throw RuntimeException("메일 발송 실패", it)
        }
    }

    private fun MimeMessage.writeSignupOtpMail(email: String, code: String) {
        val html = templateEngine.process(
            "mail/signupotp",
            Context().apply { setVariable("code", code) },
        )

        MimeMessageHelper(this, true, "UTF-8").apply {
            setTo(email)
            setSubject("Pet Meeting 이메일 인증 코드")
            setText(html, true)
            setFrom("no-reply@petmeeting.com", "PetMeeting")
        }
    }
}
