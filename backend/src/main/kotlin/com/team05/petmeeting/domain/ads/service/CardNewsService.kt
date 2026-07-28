package com.team05.petmeeting.domain.ads.service

import com.team05.petmeeting.domain.ads.dto.CardNewsResult
import com.team05.petmeeting.domain.animal.entity.Animal
import com.team05.petmeeting.infra.s3.S3Service
import org.springframework.stereotype.Service
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URI
import javax.imageio.ImageIO

@Service
class CardNewsService(
    private val geminiService: GeminiService,
    private val s3Service: S3Service
) {
    fun generateCardNews(animal: Animal): CardNewsResult {
        val caption = createCaption(animal).take(MAX_CAPTION_LENGTH)

        val finalImage = createCombinedImage(animal.popfile1, caption, animal)

        val fileName = animal.desertionNo + ".png"
        val uploadedUrl = s3Service.upload(finalImage, fileName, "cardnews", "image/png")

        return CardNewsResult(uploadedUrl, caption)
    }

    private fun createCaption(animal: Animal): String {
        val kind = getKindName(animal)
        val age = getAge(animal)
        val gender = getGender(animal)
        val shelter = getShelterName(animal)
        val specialMark = getSpecialMark(animal)

        val prompt = """
            유기동물 입양 홍보 글을 한국어로 작성해주세요.
            품종: $kind
            나이: $age
            성별: $gender
            보호소: $shelter
            특징: $specialMark

            규칙:
            - 한국어만 사용
            - 첫번째 줄: 카드 이미지에 들어갈 짧고 친근한 한 문장
            - 두번째 줄: 인스타그램 본문에 들어갈 자연스러운 소개 문장
            - 문구 두 줄만 출력, 다른 말 하지 말것
            - 설명이나 번호 붙이지 말것
            - 과장된 문학 표현, 슬픈 표현, 동정심을 자극하는 표현은 피할 것
            - 친구에게 말하듯 따뜻하고 담백하게 작성할 것
            - 특징은 부정적으로 단정하지 말고 귀엽거나 일상적인 성격으로 자연스럽게 풀어쓸 것
            - 입양을 강하게 요구하지 말고  부드럽게 말할 것
        """.trimIndent()

        val generatedText = geminiService.generate(prompt)
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n")

        return listOf(
            generatedText,
            "",
            "품종: $kind",
            "나이: $age",
            "성별: $gender",
            "보호소: $shelter",
            "특징: $specialMark",
        ).joinToString("\n")
    }

    private fun getKindName(animal: Animal): String {
        return animal.kindFullNm
            .takeIf { it.isNotBlank() }
            ?: "알 수 없음"
    }

    private fun getAge(animal: Animal): String {
        return animal.age
            ?.takeIf { it.isNotBlank() }
            ?: "나이 미상"
    }

    private fun getGender(animal: Animal): String {
        return when (animal.sexCd) {
            "M" -> "수컷"
            "F" -> "암컷"
            else -> "성별 미상"
        }
    }

    private fun getShelterName(animal: Animal): String {
        return animal.careNm
            ?.takeIf { it.isNotBlank() }
            ?: "보호소 정보 없음"
    }

    private fun getSpecialMark(animal: Animal): String {
        return animal.specialMark
            ?.takeUnless { it.isBlank() || it == "." }
            ?: "특징 정보 없음"
    }

    private fun createCombinedImage(originImageUrl: String, text: String, animal: Animal): ByteArray {
        try {
            val url = URI.create(originImageUrl).toURL()
            val animalImage = ImageIO.read(url)

            if (animalImage == null) {
                throw RuntimeException("이미지를 불러올 수 없습니다: " + originImageUrl)
            }

            val cardWidth = 1080
            val imageHeight = 1080
            val infoHeight = 400
            val totalHeight = imageHeight + infoHeight

            val card = BufferedImage(cardWidth, totalHeight, BufferedImage.TYPE_INT_RGB)
            val g = card.createGraphics()

            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            // 1. 동물 이미지 (원본 그대로, 위쪽)
            g.drawImage(animalImage, 0, 0, cardWidth, imageHeight, null)

            // 2. 하단 정보 박스 (크림색 배경)
            g.setColor(Color(255, 250, 240))
            g.fillRect(0, imageHeight, cardWidth, infoHeight)

            // 3. 상단 타이틀 바 (주황색)
            g.setColor(Color(255, 140, 0))
            g.fillRect(0, imageHeight, cardWidth, 70)

            // 타이틀 텍스트
            g.setColor(Color.WHITE)
            g.setFont(Font("Dialog", Font.BOLD, 32))
            g.drawString("이번 주 가장 많은 응원을 받은 친구예요!", 30, imageHeight + 48)

            // 4. 동물 정보
            g.setColor(Color(60, 60, 60))

            // 품종
            g.setFont(Font("Dialog", Font.BOLD, 48))
            val breed = getKindName(animal)
            g.drawString(breed, 40, imageHeight + 140)

            // 나이 / 성별
            g.setFont(Font("Dialog", Font.PLAIN, 36))
            val age = getAge(animal)
            val gender = getGender(animal)
            g.drawString("나이: " + age + "   |   성별: " + gender, 40, imageHeight + 200)

            // 보호소
            val shelter = getShelterName(animal)
            g.drawString("보호소: " + shelter, 40, imageHeight + 260)

            // 홍보 문구
            g.setFont(Font("Dialog", Font.ITALIC, 32))
            g.setColor(Color(255, 100, 0))
            val lines = text.split("\n", limit = 2)
            drawFittedString(
                g = g,
                text = "\"${lines[0].trim()}\"",
                x = 40,
                y = imageHeight + 330,
                maxWidth = cardWidth - 80,
                baseFont = Font("Dialog", Font.ITALIC, 32),
                minFontSize = 24
            )

            // 5. 하단 구분선
            g.setColor(Color(255, 140, 0))
            g.fillRect(0, totalHeight - 10, cardWidth, 10)

            g.dispose()

            val baos = ByteArrayOutputStream()
            ImageIO.write(card, "png", baos)
            return baos.toByteArray()
        } catch (e: IOException) {
            throw RuntimeException("이미지 합성 중 오류 발생", e)
        } catch (e: IllegalArgumentException) {
            throw RuntimeException("이미지 합성 중 오류 발생", e)
        }
    }

    private fun drawFittedString(
        g: Graphics2D,
        text: String,
        x: Int,
        y: Int,
        maxWidth: Int,
        baseFont: Font,
        minFontSize: Int
    ) {
        var fontSize = baseFont.size
        var fittedFont = baseFont
        var metrics = g.getFontMetrics(fittedFont)

        while (fontSize > minFontSize && metrics.stringWidth(text) > maxWidth) {
            fontSize -= 1
            fittedFont = baseFont.deriveFont(fontSize.toFloat())
            metrics = g.getFontMetrics(fittedFont)
        }

        g.font = fittedFont
        g.drawString(truncateToWidth(text, metrics, maxWidth), x, y)
    }

    private fun truncateToWidth(text: String, metrics: java.awt.FontMetrics, maxWidth: Int): String {
        if (metrics.stringWidth(text) <= maxWidth) {
            return text
        }

        val ellipsis = "..."
        var endIndex = text.length
        while (endIndex > 0 && metrics.stringWidth(text.take(endIndex) + ellipsis) > maxWidth) {
            endIndex -= 1
        }

        return text.take(endIndex) + ellipsis
    }

    companion object {
        private const val MAX_CAPTION_LENGTH = 700
    }
}
