package im.bigs.pg.external.pg

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

/**
 * PG 클라이언트 설정.
 * - TestPG 연동을 위한 RestTemplate 구성
 */
@Configuration
class PgClientConfig {
    
    @Bean
    fun restTemplate(): RestTemplate {
        return RestTemplate()
    }
}
