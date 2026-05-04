package com.honeytong.auth.service;

import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.honeytong.auth.config.PhoneVerificationSenderProperties;
import com.honeytong.auth.config.PhoneVerificationSenderProperties.NaverSens;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.test.web.client.MockRestServiceServer;

class NaverSensPhoneVerificationSenderTest {

    private static final String EXPECTED_SIGNATURE = "2RFxORDXGH1fdrch8qtFkbZEFOvm5K9trpVwZANNuhw=";

    @Test
    void send_postsSmsRequestWithNaverSensSignatureHeaders() {
        RestClient.Builder restClientBuilder = RestClient.builder()
                .baseUrl("https://sens.apigw.ntruss.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        NaverSensPhoneVerificationSender sender = new NaverSensPhoneVerificationSender(
                properties(),
                restClientBuilder.build(),
                Clock.fixed(Instant.ofEpochMilli(1_700_000_000_000L), ZoneOffset.UTC)
        );

        server.expect(once(), requestTo("https://sens.apigw.ntruss.com/sms/v2/services/service-id/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-ncp-apigw-timestamp", "1700000000000"))
                .andExpect(header("x-ncp-iam-access-key", "access-key"))
                .andExpect(header("x-ncp-apigw-signature-v2", EXPECTED_SIGNATURE))
                .andExpect(jsonPath("$.type").value("SMS"))
                .andExpect(jsonPath("$.contentType").value("COMM"))
                .andExpect(jsonPath("$.countryCode").value("82"))
                .andExpect(jsonPath("$.from").value("01012345678"))
                .andExpect(jsonPath("$.content").value("[Honeytong] 인증번호 123456를 입력해 주세요."))
                .andExpect(jsonPath("$.messages[0].to").value("01099998888"))
                .andExpect(jsonPath("$.messages[0].content").value("[Honeytong] 인증번호 123456를 입력해 주세요."))
                .andRespond(withStatus(HttpStatus.ACCEPTED));

        sender.send("01099998888", "123456");

        server.verify();
    }

    @Test
    void createSignature_usesNaverCloudSignatureV2Format() {
        NaverSensPhoneVerificationSender sender = new NaverSensPhoneVerificationSender(
                properties(),
                RestClient.builder(),
                Clock.systemUTC()
        );

        String signature = sender.createSignature(
                "POST",
                "/sms/v2/services/service-id/messages",
                "1700000000000"
        );

        org.assertj.core.api.Assertions.assertThat(signature).isEqualTo(EXPECTED_SIGNATURE);
    }

    private PhoneVerificationSenderProperties properties() {
        return new PhoneVerificationSenderProperties(
                        "naver-sens",
                        "[Honeytong] 인증번호 {code}를 입력해 주세요.",
                        new NaverSens(
                                "service-id",
                                "access-key",
                                "secret-key",
                                "01012345678",
                                "https://sens.apigw.ntruss.com",
                                "82",
                                Duration.ofSeconds(3),
                                Duration.ofSeconds(5)
                        ),
                        null
        );
    }
}
