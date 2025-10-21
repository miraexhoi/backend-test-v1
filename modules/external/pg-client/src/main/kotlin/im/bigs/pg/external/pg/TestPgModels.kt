package im.bigs.pg.external.pg

/**
 * TestPG API 연동용 내부 모델들.
 * - external 모듈 내부에서만 사용되는 모델입니다.
 * - 헥사고날 아키텍처에서 어댑터 계층의 내부 구현 세부사항입니다.
 */

/**
 * TestPG 평문 요청 모델.
 * - TestPG API에 전송할 평문 JSON 구조체입니다.
 */
data class TestPgPlainRequest(
    val cardNumber: String,
    val birthDate: String,
    val expiry: String,
    val password: String,
    val amount: Int
)

/**
 * TestPG 암호화 요청 모델.
 * - AES-256-GCM 암호화된 데이터를 담는 요청 구조체입니다.
 */
data class TestPgEncryptedRequest(
    val enc: String
)

/**
 * TestPG 응답 모델.
 * - TestPG API로부터 받는 응답 구조체입니다.
 */
data class TestPgResponse(
    val approvalCode: String,
    val approvedAt: String,
    val maskedCardLast4: String,
    val amount: Int,
    val status: String
)
