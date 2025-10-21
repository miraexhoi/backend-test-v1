package im.bigs.pg.external.pg

import com.fasterxml.jackson.databind.ObjectMapper
import im.bigs.pg.application.pg.port.out.PgApproveRequest
import im.bigs.pg.domain.payment.PaymentStatus
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestTemplate
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * TestPG 실제 연동 통합 테스트.
 * - 실제 TestPG API를 호출하여 연동을 검증합니다.
 * - AES-256-GCM 암호화/복호화가 올바르게 동작하는지 확인합니다.
 * - 네트워크 의존적이므로 CI/CD에서는 제외할 수 있습니다.
 */
class TestPgClientTest {
    
    private val objectMapper = ObjectMapper()
    private val restTemplate = RestTemplate()
    
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
    @DisplayName("TestPG 실제 API 연동 및 AES-256-GCM 암호화 검증")
    fun `TestPG 실제 API 연동 및 AES-256-GCM 암호화 검증`() {
        val request = PgApproveRequest(
            partnerId = 2L,
            amount = BigDecimal("10000"),
            cardBin = "111111",
            cardLast4 = "1111",
            productName = "TestPG 통합 테스트"
        )
        
        // 실제 TestPG API 호출
        val result = client.approve(request)
        
        // 실제 응답 검증
        assertTrue(result.approvalCode.isNotBlank(), "승인번호가 비어있지 않아야 함")
        assertEquals(PaymentStatus.APPROVED, result.status, "결제 상태는 APPROVED여야 함")
        assertTrue(result.approvedAt != null, "승인시각이 null이 아니어야 함")
        
        println("✅ TestPG 실제 연동 성공:")
        println("   - 승인번호: ${result.approvalCode}")
        println("   - 승인시각: ${result.approvedAt}")
        println("   - 상태: ${result.status}")
    }
}
