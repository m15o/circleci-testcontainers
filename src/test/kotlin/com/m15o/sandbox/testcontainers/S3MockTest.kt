package com.m15o.sandbox.testcontainers

import com.adobe.testing.s3mock.testcontainers.S3MockContainer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.net.URI


private const val bucket = "test-bucket"

@Testcontainers
class S3MockTest {
    companion object {
        private val S3MOCK_VERSION: String = System.getProperty("s3mock.version", "2.11.0")
        private val log = LoggerFactory.getLogger(S3MockTest::class.java)
    }

    @Container
    private val s3Mock = S3MockContainer(S3MOCK_VERSION)
        .withInitialBuckets(bucket)

    @BeforeEach
    fun before() {
        val s3 = createS3Client()
        (1..3).forEach { i ->
            val key = "item-$i"
            val request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build()
            val body = RequestBody.fromString("item #$i")
            s3.putObject(request, body)
            log.info("put s3://$bucket/$key")
        }
    }

    @Test
    fun listObjects() {
        val s3 = createS3Client()
        val request = ListObjectsV2Request.builder()
            .bucket(bucket)
            .build()
        log.info("ls s3://bucket/")
        val objectsV2 = s3.listObjectsV2(request)

        objectsV2.contents().forEach {
            log.info(it.key())
        }
        assertThat(objectsV2.contents())
            .hasSize(3)
            .extracting<String> { it.key() }
            .containsExactlyInAnyOrder("item-1", "item-2", "item-3")
    }

    private fun createS3Client(): S3Client {
        return S3Client.builder()
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("dummy", "dummy")))
            .endpointOverride(URI.create(s3Mock.httpEndpoint))
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
            .build()
    }

}
