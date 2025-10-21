package im.bigs.pg.external.pg

import com.fasterxml.jackson.databind.ObjectMapper
import im.bigs.pg.application.pg.port.out.PgApproveRequest
import im.bigs.pg.domain.payment.PaymentStatus
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestPgClientTest {
    
    private val objectMapper = ObjectMapper()
    private val restTemplate = object : RestTemplate() {
        override fun <T> exchange(url: String, method: org.springframework.http.HttpMethod, entity: org.springframework.http.HttpEntity<*>, responseType: Class<T>): ResponseEntity<T> {
            // Mock 응답: TestPG 성공 응답 시뮬레이션
            val mockResponse = TestPgResponse(
                approvalCode = "10080728",
                approvedAt = "2025-01-01T12:00:00",
                maskedCardLast4 = "1111",
                amount = 10000,
                status = "APPROVED"
            )
            return ResponseEntity(mockResponse as T, HttpStatus.OK)
        }
    }
    
    private val client = TestPgClient(
        restTemplate = restTemplate,
        objectMapper = objectMapper,
        apiUrl = "https://api-test-pg.bigs.im",
        apiKey = "11111111-1111-4111-8111-111111111111",
        iv = "AAAAAAAAAAAAAAAA"
    )

    @Test
    @DisplayName("TestPG는 짝수 파트너 ID를 지원해야 한다")
    fun `TestPG는 짝수 파트너 ID를 지원해야 한다`() {
        assertTrue(client.supports(2L))
        assertTrue(client.supports(4L))
        assertFalse(client.supports(1L))
        assertFalse(client.supports(3L))
    }

    @Test
    @DisplayName("TestPG 승인 요청이 올바른 형식으로 처리되어야 한다")
    fun `TestPG 승인 요청이 올바른 형식으로 처리되어야 한다`() {
        val request = PgApproveRequest(
            partnerId = 2L,
            amount = BigDecimal("10000"),
            cardBin = "111111",
            cardLast4 = "1111",
            productName = "TestPG 테스트"
        )
        
        val result = client.approve(request)
        
        assertEquals("10080728", result.approvalCode)
        assertEquals(PaymentStatus.APPROVED, result.status)
        assertEquals(LocalDateTime.of(2025, 1, 1, 12, 0), result.approvedAt)
    }
}
