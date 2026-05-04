package com.honeytong.auth.service;

import com.honeytong.auth.config.PhoneVerificationSenderProperties;
import com.honeytong.auth.config.PhoneVerificationSenderProperties.NaverSens;
import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Base64;
import java.util.List;
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
        havingValue = "naver-sens"
)
public class NaverSensPhoneVerificationSender implements PhoneVerificationSender {

    private static final Logger log = LoggerFactory.getLogger(NaverSensPhoneVerificationSender.class);
    private static final String SEND_METHOD = "POST";

    private final PhoneVerificationSenderProperties properties;
    private final NaverSens naverSens;
    private final RestClient restClient;
    private final Clock clock;

    @Autowired
    public NaverSensPhoneVerificationSender(
            PhoneVerificationSenderProperties properties,
            RestClient.Builder restClientBuilder
    ) {
        this(properties, restClientBuilder, Clock.systemUTC());
    }

    NaverSensPhoneVerificationSender(
            PhoneVerificationSenderProperties properties,
            RestClient.Builder restClientBuilder,
            Clock clock
    ) {
        this(
                properties,
                restClientBuilder
                        .baseUrl(properties.safeNaverSens().baseUrl())
                        .requestFactory(requestFactory(properties.safeNaverSens()))
                        .build(),
                clock
        );
    }

    NaverSensPhoneVerificationSender(
            PhoneVerificationSenderProperties properties,
            RestClient restClient,
            Clock clock
    ) {
        this.properties = properties;
        this.naverSens = properties.safeNaverSens();
        this.restClient = restClient;
        this.clock = clock;
    }

    @Override
    public void send(String phone, String code) {
        validateConfiguration();

        String requestPath = "/sms/v2/services/" + naverSens.serviceId() + "/messages";
        String timestamp = Long.toString(clock.millis());
        String signature = createSignature(SEND_METHOD, requestPath, timestamp);
        SendMessageRequest request = new SendMessageRequest(
                "SMS",
                "COMM",
                naverSens.countryCode(),
                naverSens.from(),
                properties.renderMessage(code),
                List.of(new Recipient(phone, properties.renderMessage(code)))
        );

        try {
            restClient.post()
                    .uri(requestPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("x-ncp-apigw-timestamp", timestamp)
                    .header("x-ncp-iam-access-key", naverSens.accessKey())
                    .header("x-ncp-apigw-signature-v2", signature)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Phone verification SMS requested. phone={}", maskPhone(phone));
        } catch (RestClientException exception) {
            log.warn("Phone verification SMS request failed. phone={}", maskPhone(phone));
            throw new ApiException(ErrorCode.EXTERNAL_SERVICE_ERROR, "인증번호 SMS 발송에 실패했습니다.");
        }
    }

    String createSignature(String method, String requestPath, String timestamp) {
        try {
            String message = method + " " + requestPath + "\n" + timestamp + "\n" + naverSens.accessKey();
            SecretKeySpec signingKey = new SecretKeySpec(
                    naverSens.secretKey().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKey);
            byte[] rawHmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception exception) {
            throw new ApiException(ErrorCode.EXTERNAL_SERVICE_ERROR, "SMS 서명 생성에 실패했습니다.");
        }
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(naverSens.serviceId())
                || !StringUtils.hasText(naverSens.accessKey())
                || !StringUtils.hasText(naverSens.secretKey())
                || !StringUtils.hasText(naverSens.from())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "SMS 발송 설정이 완료되지 않았습니다.");
        }
    }

    private static SimpleClientHttpRequestFactory requestFactory(NaverSens naverSens) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(naverSens.connectTimeout());
        requestFactory.setReadTimeout(naverSens.readTimeout());
        return requestFactory;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "****";
        }
        return "****" + phone.substring(phone.length() - 4);
    }

    private record SendMessageRequest(
            String type,
            String contentType,
            String countryCode,
            String from,
            String content,
            List<Recipient> messages
    ) {
    }

    private record Recipient(
            String to,
            String content
    ) {
    }
}
