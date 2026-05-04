package com.honeytong.auth.service;

import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.honeytong.auth.config.PhoneVerificationSenderProperties;
import com.honeytong.auth.config.PhoneVerificationSenderProperties.Solapi;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SolapiPhoneVerificationSenderTest {

    private static final String DATE = "2026-05-04T03:00:00Z";
    private static final String SALT = "0123456789abcdef";
    private static final String SIGNATURE = "510aafdb38091989bcc30847f6832707001f1b2db22b25def2e80fb8b9e00bc6";
    private static final String AUTHORIZATION = "HMAC-SHA256 apiKey=api-key, date="
            + DATE
            + ", salt="
            + SALT
            + ", signature="
            + SIGNATURE;

    @Test
    void send_postsSmsRequestWithSolapiAuthorizationHeader() {
        RestClient.Builder restClientBuilder = RestClient.builder()
                .baseUrl("https://api.solapi.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        SolapiPhoneVerificationSender sender = new SolapiPhoneVerificationSender(
                properties(),
                restClientBuilder.build(),
                Clock.fixed(Instant.parse(DATE), ZoneOffset.UTC),
                () -> SALT
        );

        server.expect(once(), requestTo("https://api.solapi.com/messages/v4/send-many/detail"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", AUTHORIZATION))
                .andExpect(jsonPath("$.messages[0].from").value("01012345678"))
                .andExpect(jsonPath("$.messages[0].to").value("01099998888"))
                .andExpect(jsonPath("$.messages[0].text").value("[Honeytong] 인증번호 123456를 입력해 주세요."))
                .andRespond(withStatus(HttpStatus.OK));

        sender.send("01099998888", "123456");

        server.verify();
    }

    @Test
    void createSignature_usesSolapiHmacSha256Format() {
        SolapiPhoneVerificationSender sender = new SolapiPhoneVerificationSender(
                properties(),
                RestClient.builder(),
                Clock.systemUTC(),
                () -> SALT
        );

        org.assertj.core.api.Assertions.assertThat(sender.createSignature(DATE, SALT)).isEqualTo(SIGNATURE);
        org.assertj.core.api.Assertions.assertThat(sender.createAuthorizationHeader(DATE, SALT))
                .isEqualTo(AUTHORIZATION);
    }

    private PhoneVerificationSenderProperties properties() {
        return new PhoneVerificationSenderProperties(
                "solapi",
                "[Honeytong] 인증번호 {code}를 입력해 주세요.",
                null,
                new Solapi(
                        "api-key",
                        "secret-key",
                        "01012345678",
                        "https://api.solapi.com",
                        Duration.ofSeconds(3),
                        Duration.ofSeconds(5)
                )
        );
    }
}
