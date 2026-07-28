package com.team05.petmeeting.domain.ads.service

import com.team05.petmeeting.domain.animal.entity.Animal
import com.team05.petmeeting.infra.s3.S3Service
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import java.awt.Color
import java.awt.image.BufferedImage
import java.nio.file.Path
import javax.imageio.ImageIO

@ExtendWith(MockitoExtension::class)
internal class CardNewsServiceTest {

    @Mock
    private lateinit var geminiService: GeminiService

    @Mock
    private lateinit var s3Service: S3Service

    @InjectMocks
    private lateinit var cardNewsService: CardNewsService

    @TempDir
    private lateinit var tempDir: Path

    @Test
    @DisplayName("카드뉴스 생성 테스트 (외부 API Mock)")
    fun generateCardNews() {
        val animal = Mockito.mock(Animal::class.java)
        val imageUrl = createLocalImageUrl()

        Mockito.`when`(animal.kindFullNm).thenReturn("골든 리트리버")
        Mockito.`when`(animal.specialMark).thenReturn("사람을 좋아함")
        Mockito.`when`(animal.popfile1).thenReturn(imageUrl)
        Mockito.`when`(animal.desertionNo).thenReturn("123")
        Mockito.`when`(animal.age).thenReturn("2살")
        Mockito.`when`(animal.sexCd).thenReturn("M")
        Mockito.`when`(animal.careNm).thenReturn("서울보호소")
        Mockito.`when`(geminiService.generate(Mockito.contains("골든 리트리버")))
            .thenReturn("사람을 좋아하는 밝은 친구예요.\n장난도 좋아하고 곁에 있는 걸 좋아하는 골든 리트리버예요. 천천히 알아가며 가족이 되어줄 분을 기다리고 있어요.")
        Mockito.`when`(s3Service.upload(anyByteArray(), eqString("123.png"),
            eqString("cardnews"), eqString("image/png")))
            .thenReturn("https://s3-url.com/image.png")

        val result = cardNewsService.generateCardNews(animal)

        assertThat(result.imageUrl).isEqualTo("https://s3-url.com/image.png")
        assertThat(result.caption).contains("사람을 좋아하는 밝은 친구예요.")
        assertThat(result.caption).contains("품종: 골든 리트리버")
        assertThat(result.caption).contains("나이: 2살")
        assertThat(result.caption).contains("성별: 수컷")
        assertThat(result.caption).contains("보호소: 서울보호소")
        assertThat(result.caption).contains("특징: 사람을 좋아함")
        Mockito.verify(geminiService).generate(Mockito.contains("사람을 좋아함"))
        Mockito.verify(geminiService).generate(Mockito.contains("보호소: 서울보호소"))
        Mockito.verify(geminiService).generate(Mockito.contains("과장된 문학 표현"))
    }

    private fun createLocalImageUrl(): String {
        val image = BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        graphics.color = Color.WHITE
        graphics.fillRect(0, 0, 10, 10)
        graphics.dispose()

        val imageFile = tempDir.resolve("animal.png").toFile()
        ImageIO.write(image, "png", imageFile)
        return imageFile.toURI().toURL().toString()
    }

    private fun anyByteArray(): ByteArray {
        return any(ByteArray::class.java) ?: ByteArray(0)
    }

    private fun eqString(value: String): String {
        return eq(value) ?: value
    }
}
