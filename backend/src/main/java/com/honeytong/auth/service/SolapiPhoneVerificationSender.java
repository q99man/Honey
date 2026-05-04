package com.honeytong.auth.service;

import com.honeytong.auth.config.PhoneVerificationSenderProperties;
import com.honeytong.auth.config.PhoneVerificationSenderProperties.Solapi;
import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.function.Supplier;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@ConditionalOnProperty(
        prefix = "app.security.phone-verification.sender",
        name = "provider",
        havingValue = "solapi"
)
public class SolapiPhoneVerificationSender implements PhoneVerificationSender {

    private static final Logger log = LoggerFactory.getLogger(SolapiPhoneVerificationSender.class);
    private static final String SEND_PATH = "/messages/v4/send-many/detail";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final PhoneVerificationSenderProperties properties;
    private final Solapi solapi;
    private final RestClient restClient;
    private final Clock clock;
    private final Supplier<String> saltSupplier;

    @Autowired
    public SolapiPhoneVerificationSender(
            PhoneVerificationSenderProperties properties,
            RestClient.Builder restClientBuilder
    ) {
        this(properties, restClientBuilder, Clock.systemUTC(), SolapiPhoneVerificationSender::generateSalt);
    }

    SolapiPhoneVerificationSender(
            PhoneVerificationSenderProperties properties,
            RestClient.Builder restClientBuilder,
            Clock clock,
            Supplier<String> saltSupplier
    ) {
        this(
                properties,
                restClientBuilder
                        .baseUrl(properties.safeSolapi().baseUrl())
                        .requestFactory(requestFactory(properties.safeSolapi()))
                        .build(),
                clock,
                saltSupplier
        );
    }

    SolapiPhoneVerificationSender(
            PhoneVerificationSenderProperties properties,
            RestClient restClient,
            Clock clock,
            Supplier<String> saltSupplier
    ) {
        this.properties = properties;
        this.solapi = properties.safeSolapi();
        this.restClient = restClient;
        this.clock = clock;
        this.saltSupplier = saltSupplier;
    }

    @Override
    public void send(String phone, String code) {
        validateConfiguration();

        String date = Instant.now(clock).toString();
        String salt = saltSupplier.get();
        String authorization = createAuthorizationHeader(date, salt);
        SendManyRequest request = new SendManyRequest(List.of(new Message(
                solapi.from(),
                phone,
                properties.renderMessage(code)
        )));

        try {
            restClient.post()
                    .uri(SEND_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", authorization)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Phone verification SMS requested through SOLAPI. phone={}", maskPhone(phone));
        } catch (RestClientException exception) {
            log.warn("Phone verification SOLAPI request failed. phone={}", maskPhone(phone));
            throw new ApiException(ErrorCode.EXTERNAL_SERVICE_ERROR, "인증번호 SMS 발송에 실패했습니다.");
        }
    }

    String createAuthorizationHeader(String date, String salt) {
        return "HMAC-SHA256 apiKey="
                + solapi.apiKey()
                + ", date="
                + date
                + ", salt="
                + salt
                + ", signature="
                + createSignature(date, salt);
    }

    String createSignature(String date, String salt) {
        try {
            SecretKeySpec signingKey = new SecretKeySpec(
                    solapi.apiSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKey);
            return HexFormat.of().formatHex(mac.doFinal((date + salt).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new ApiException(ErrorCode.EXTERNAL_SERVICE_ERROR, "SMS 서명 생성에 실패했습니다.");
        }
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(solapi.apiKey())
                || !StringUtils.hasText(solapi.apiSecret())
                || !StringUtils.hasText(solapi.from())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "SOLAPI SMS 발송 설정이 완료되지 않았습니다.");
        }
    }

    private static SimpleClientHttpRequestFactory requestFactory(Solapi solapi) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(solapi.connectTimeout());
        requestFactory.setReadTimeout(solapi.readTimeout());
        return requestFactory;
    }

    private static String generateSalt() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "****";
        }
        return "****" + phone.substring(phone.length() - 4);
    }

    private record SendManyRequest(
            List<Message> messages
    ) {
    }

    private record Message(
            String from,
            String to,
            String text
    ) {
    }
}
