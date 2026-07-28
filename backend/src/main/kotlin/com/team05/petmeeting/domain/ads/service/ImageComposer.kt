package com.team05.petmeeting.domain.ads.service

import org.springframework.stereotype.Component
import java.awt.Color
import java.awt.Font
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URI
import javax.imageio.ImageIO

@Component
class ImageComposer {
    fun compose(originImageUrl: String, text: String): ByteArray {
        try {
            val url = URI.create(originImageUrl).toURL()
            val originalImage = ImageIO.read(url)

            if (originalImage == null) {
                throw RuntimeException("이미지를 불러올 수 없습니다: " + originImageUrl)
            }

            val width = originalImage.width
            val height = originalImage.height

            val combined = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
            val g = combined.createGraphics()

            g.drawImage(originalImage, 0, 0, null)

            g.color = Color(0, 0, 0, 150)
            g.fillRect(0, height - (height / 4), width, height / 4)

            g.color = Color.WHITE
            g.font = Font("Dialog", Font.BOLD, width / 20)

            val fm = g.fontMetrics
            val x = 50
            var y = height - (height / 6)

            val lines = text.split("\n", limit = 2)
            for (line in lines) {
                g.drawString(line, x, y)
                y += fm.height
            }

            g.dispose()

            val baos = ByteArrayOutputStream()
            ImageIO.write(combined, "png", baos)
            return baos.toByteArray()
        } catch (e: IOException) {
            throw RuntimeException("이미지 합성 중 오류 발생", e)
        } catch (e: IllegalArgumentException) {
            throw RuntimeException("이미지 합성 중 오류 발생", e)
        }
    }
}
