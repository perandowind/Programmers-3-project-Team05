package com.team05.petmeeting.infra.s3

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.util.*

@Service
class S3Service(
    private val amazonS3: AmazonS3,
    @param:Value("\${cloud.aws.s3.bucket}")
    private val bucket: String
) {
    fun upload(
        bytes: ByteArray,
        fileName: String,
        directory: String,
        contentType: String = "application/octet-stream"
    ): String {
        val key = directory.trim('/') + "/" + UUID.randomUUID() + "_" + fileName

        val metadata = ObjectMetadata()
        metadata.contentLength = bytes.size.toLong()
        metadata.contentType = contentType

        try {
            amazonS3.putObject(
                PutObjectRequest(
                    bucket, key,
                    ByteArrayInputStream(bytes), metadata
                )
            )

            return amazonS3.getUrl(bucket, key).toString()
        } catch (e: Exception) {
            throw RuntimeException("S3 업로드 실패", e)
        }
    }
}
