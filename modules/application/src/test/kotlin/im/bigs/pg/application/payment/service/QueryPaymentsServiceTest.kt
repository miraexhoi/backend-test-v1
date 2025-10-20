package im.bigs.pg.application.payment.service

import im.bigs.pg.application.payment.port.`in`.QueryFilter
import im.bigs.pg.application.payment.port.out.PaymentOutPort
import im.bigs.pg.application.payment.port.out.PaymentPage
import im.bigs.pg.application.payment.port.out.PaymentQuery
import im.bigs.pg.application.payment.port.out.PaymentSummaryFilter
import im.bigs.pg.application.payment.port.out.PaymentSummaryProjection
import im.bigs.pg.domain.payment.Payment
import im.bigs.pg.domain.payment.PaymentStatus
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.DisplayName
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QueryPaymentsServiceTest {
    private val paymentOutPort = mockk<PaymentOutPort>()
    private val service = QueryPaymentsService(paymentOutPort)

    @Test
    @DisplayName("커서 없이 조회 시 첫 페이지를 반환해야 한다")
    fun `커서 없이 조회 시 첫 페이지를 반환해야 한다`() {
        val filter = QueryFilter(
            partnerId = 1L,
            status = "APPROVED",
            from = LocalDateTime.of(2024, 1, 1, 0, 0),
            to = LocalDateTime.of(2024, 1, 2, 0, 0),
            cursor = null,
            limit = 20,
        )

        val mockPayment = Payment(
            id = 1L,
            partnerId = 1L,
            amount = BigDecimal("10000"),
            appliedFeeRate = BigDecimal("0.0300"),
            feeAmount = BigDecimal("300"),
            netAmount = BigDecimal("9700"),
            cardBin = "123456",
            cardLast4 = "4242",
            approvalCode = "APPROVAL-123",
            approvedAt = LocalDateTime.of(2024, 1, 1, 12, 0),
            status = PaymentStatus.APPROVED,
        )

        val mockPage = PaymentPage(
            items = listOf(mockPayment),
            hasNext = true,
            nextCursorCreatedAt = LocalDateTime.of(2024, 1, 1, 11, 0),
            nextCursorId = 0L,
        )

        val mockSummary = PaymentSummaryProjection(
            count = 1L,
            totalAmount = BigDecimal("10000"),
            totalNetAmount = BigDecimal("9700"),
        )

        every { paymentOutPort.findBy(any<PaymentQuery>()) } returns mockPage
        every { paymentOutPort.summary(any<PaymentSummaryFilter>()) } returns mockSummary

        val result = service.query(filter)

        assertEquals(1, result.items.size)
        assertEquals(1L, result.summary.count)
        assertEquals(BigDecimal("10000"), result.summary.totalAmount)
        assertTrue(result.hasNext)
        assertNotNull(result.nextCursor)
    }

    @Test
    @DisplayName("커서로 조회 시 다음 페이지를 반환해야 한다")
    fun `커서로 조회 시 다음 페이지를 반환해야 한다`() {
        val cursor = "MTczNTY0NDQ4MDAwMDA6MA==" // 2024-01-01T11:00:00Z:0
        val filter = QueryFilter(
            partnerId = 1L,
            status = "APPROVED",
            cursor = cursor,
            limit = 20,
        )

        val mockPage = PaymentPage(
            items = emptyList(),
            hasNext = false,
            nextCursorCreatedAt = null,
            nextCursorId = null,
        )

        val mockSummary = PaymentSummaryProjection(
            count = 0L,
            totalAmount = BigDecimal.ZERO,
            totalNetAmount = BigDecimal.ZERO,
        )

        every { paymentOutPort.findBy(any<PaymentQuery>()) } returns mockPage
        every { paymentOutPort.summary(any<PaymentSummaryFilter>()) } returns mockSummary

        val result = service.query(filter)

        assertTrue(result.items.isEmpty())
        assertFalse(result.hasNext)
        assertNull(result.nextCursor)
    }

    @Test
    @DisplayName("잘못된 커서는 무시하고 첫 페이지로 조회해야 한다")
    fun `잘못된 커서는 무시하고 첫 페이지로 조회해야 한다`() {
        val filter = QueryFilter(
            partnerId = 1L,
            cursor = "invalid-cursor",
            limit = 20,
        )

        val mockPage = PaymentPage(
            items = emptyList(),
            hasNext = false,
            nextCursorCreatedAt = null,
            nextCursorId = null,
        )

        val mockSummary = PaymentSummaryProjection(
            count = 0L,
            totalAmount = BigDecimal.ZERO,
            totalNetAmount = BigDecimal.ZERO,
        )

        every { paymentOutPort.findBy(any<PaymentQuery>()) } returns mockPage
        every { paymentOutPort.summary(any<PaymentSummaryFilter>()) } returns mockSummary

        val result = service.query(filter)

        assertTrue(result.items.isEmpty())
        assertFalse(result.hasNext)
        assertNull(result.nextCursor)
    }
}
