package com.team05.petmeeting.infra.s3

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class S3Config(
    @param:Value("\${cloud.aws.credentials.access-key}")
    private val accessKey: String,
    @param:Value("\${cloud.aws.credentials.secret-key}")
    private val secretKey: String,
    @param:Value("\${cloud.aws.region.static}")
    private val region: String
) {
    @Bean
    fun amazonS3(): AmazonS3 {
        val credentials = BasicAWSCredentials(accessKey, secretKey)
        return AmazonS3ClientBuilder.standard()
            .withCredentials(AWSStaticCredentialsProvider(credentials))
            .withRegion(region)
            .build()
    }
}
