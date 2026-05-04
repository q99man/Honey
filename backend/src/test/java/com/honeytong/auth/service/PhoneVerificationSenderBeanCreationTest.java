package com.honeytong.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.honeytong.auth.config.PhoneVerificationSenderProperties;
import com.honeytong.auth.config.PhoneVerificationSenderProperties.NaverSens;
import com.honeytong.auth.config.PhoneVerificationSenderProperties.Solapi;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;

class PhoneVerificationSenderBeanCreationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(RestClient.Builder.class, RestClient::builder);

    @Test
    void solapiSender_canBeCreatedBySpring() {
        contextRunner
                .withBean(PhoneVerificationSenderProperties.class, this::solapiProperties)
                .withInitializer(context -> registerBeanDefinition(
                        context.getBeanFactory(),
                        "solapiPhoneVerificationSender",
                        SolapiPhoneVerificationSender.class
                ))
                .run(context -> assertThat(context).hasSingleBean(SolapiPhoneVerificationSender.class));
    }

    @Test
    void naverSensSender_canBeCreatedBySpring() {
        contextRunner
                .withBean(PhoneVerificationSenderProperties.class, this::naverSensProperties)
                .withInitializer(context -> registerBeanDefinition(
                        context.getBeanFactory(),
                        "naverSensPhoneVerificationSender",
                        NaverSensPhoneVerificationSender.class
                ))
                .run(context -> assertThat(context).hasSingleBean(NaverSensPhoneVerificationSender.class));
    }

    private void registerBeanDefinition(
            ConfigurableListableBeanFactory beanFactory,
            String beanName,
            Class<?> beanClass
    ) {
        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
        registry.registerBeanDefinition(
                beanName,
                BeanDefinitionBuilder.genericBeanDefinition(beanClass).getBeanDefinition()
        );
    }

    private PhoneVerificationSenderProperties solapiProperties() {
        return new PhoneVerificationSenderProperties(
                "solapi",
                "[Honeytong] verification code {code}",
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

    private PhoneVerificationSenderProperties naverSensProperties() {
        return new PhoneVerificationSenderProperties(
                "naver-sens",
                "[Honeytong] verification code {code}",
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
