package im.bigs.pg.external.pg

import com.fasterxml.jackson.databind.ObjectMapper
import im.bigs.pg.application.pg.port.out.PgApproveRequest
import im.bigs.pg.application.pg.port.out.PgApproveResult
import im.bigs.pg.application.pg.port.out.PgClientOutPort
import im.bigs.pg.domain.payment.PaymentStatus
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * TestPG 실제 연동 클라이언트.
 * - AES-256-GCM 암호화를 사용하여 TestPG API와 통신합니다.
 * - 문서: https://api-test-pg.bigs.im/docs/index.html
 */
@Component
class TestPgClient(
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper,
    @Value("\${pg.test.api-url:https://api-test-pg.bigs.im}")
    private val apiUrl: String,
    @Value("\${pg.test.api-key:11111111-1111-4111-8111-111111111111}")
    private val apiKey: String,
    @Value("\${pg.test.iv:AAAAAAAAAAAAAAAA}")
    private val iv: String,
) : PgClientOutPort {
    
    private val log = LoggerFactory.getLogger(javaClass)

    override fun supports(partnerId: Long): Boolean = partnerId % 2L == 0L

    override fun approve(request: PgApproveRequest): PgApproveResult {
        log.info("TestPG 승인 요청: partnerId={}, amount={}", request.partnerId, request.amount)
        
        try {
            // 1. 평문 JSON 생성
            val plainText = TestPgPlainRequest(
                cardNumber = "1111-1111-1111-1111", // 테스트 성공 카드
                birthDate = "19900101",
                expiry = "1227",
                password = "12",
                amount = request.amount.toInt()
            )
            
            val plainJson = objectMapper.writeValueAsString(plainText)
            // 보안: 민감정보 로깅 금지
            
            // 2. AES-256-GCM 암호화
            val encrypted = encrypt(plainJson)
            
            // 3. TestPG API 호출
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set("API-KEY", apiKey)
            }
            
            val requestBody = mapOf("enc" to encrypted)
            val entity = HttpEntity(requestBody, headers)
            
            val response = restTemplate.exchange(
                "$apiUrl/api/v1/pay/credit-card",
                HttpMethod.POST,
                entity,
                TestPgResponse::class.java
            )
            
            val result = response.body ?: throw IllegalStateException("TestPG 응답이 비어있습니다")
            
            log.info("TestPG 승인 완료: approvalCode={}", result.approvalCode)
            
            return PgApproveResult(
                approvalCode = result.approvalCode,
                approvedAt = LocalDateTime.parse(result.approvedAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                status = PaymentStatus.valueOf(result.status)
            )
            
        } catch (e: Exception) {
            log.error("TestPG 승인 실패: partnerId={}, error={}", request.partnerId, e.message, e)
            throw IllegalStateException("TestPG 승인 처리 중 오류가 발생했습니다: ${e.message}", e)
        }
    }
    
    /**
     * AES-256-GCM 암호화
     * - Key: SHA-256(API-KEY)
     * - IV: Base64URL 디코딩된 12바이트
     * - 결과: Base64URL(ciphertext||tag)
     */
    private fun encrypt(plainText: String): String {
        val plainBytes = plainText.toByteArray(StandardCharsets.UTF_8)
        
        // Key 생성: SHA-256(API-KEY)
        val keyBytes = MessageDigest.getInstance("SHA-256")
            .digest(apiKey.toByteArray(StandardCharsets.UTF_8))
        val secretKey = SecretKeySpec(keyBytes, "AES")
        
        // IV 생성: Base64URL 디코딩
        val ivBytes = Base64.getUrlDecoder().decode(iv)
        
        // AES-256-GCM 암호화
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, ivBytes) // 128비트 태그
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
        
        val cipherBytes = cipher.doFinal(plainBytes)
        
        // Base64URL 인코딩 (패딩 없음)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(cipherBytes)
    }
}

