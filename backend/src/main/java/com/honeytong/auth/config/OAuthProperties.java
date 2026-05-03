package com.honeytong.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.oauth")
public class OAuthProperties {

    private Provider kakao = new Provider("https://kapi.kakao.com/v2/user/me");
    private Provider naver = new Provider("https://openapi.naver.com/v1/nid/me");
    private Provider google = new Provider("https://www.googleapis.com/oauth2/v3/userinfo");

    public Provider getKakao() {
        return kakao;
    }

    public void setKakao(Provider kakao) {
        this.kakao = kakao;
    }

    public Provider getNaver() {
        return naver;
    }

    public void setNaver(Provider naver) {
        this.naver = naver;
    }

    public Provider getGoogle() {
        return google;
    }

    public void setGoogle(Provider google) {
        this.google = google;
    }

    public static class Provider {

        private String userInfoUrl;

        public Provider() {
        }

        public Provider(String userInfoUrl) {
            this.userInfoUrl = userInfoUrl;
        }

        public String getUserInfoUrl() {
            return userInfoUrl;
        }

        public void setUserInfoUrl(String userInfoUrl) {
            this.userInfoUrl = userInfoUrl;
        }
    }
}
