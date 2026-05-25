package com.honeytong.auth.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.honeytong.auth.security.JwtAuthenticationFilter;
import com.honeytong.auth.security.JwtTokenProvider;
import com.honeytong.common.api.ApiResponse;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = SecurityConfigPublicReadTest.TestPublicReadController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        SecurityConfigPublicReadTest.TestPublicReadController.class
})
class SecurityConfigPublicReadTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/places/nearby?lat=37.554095&lng=126.929520&radius=1000",
            "/api/places/search?keyword=%ED%99%8D%EB%8C%80",
            "/api/rankings/places?regionType=DONG&regionId=1"
    })
    void publicMapDiscoveryReads_doNotRequireAuthentication(String url) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/admin/probe",
            "/api/private/probe"
    })
    void protectedReads_stillRequireAuthentication(String url) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().isForbidden());
    }

    @RestController
    static class TestPublicReadController {

        @GetMapping("/api/places/nearby")
        ApiResponse<List<String>> getNearbyPlaces() {
            return ApiResponse.success(List.of(), "OK");
        }

        @GetMapping("/api/places/search")
        ApiResponse<List<String>> searchPlaces() {
            return ApiResponse.success(List.of(), "OK");
        }

        @GetMapping("/api/rankings/places")
        ApiResponse<List<String>> getPlaceRankings() {
            return ApiResponse.success(List.of(), "OK");
        }

        @GetMapping("/api/admin/probe")
        ApiResponse<String> getAdminProbe() {
            return ApiResponse.success("admin", "OK");
        }

        @GetMapping("/api/private/probe")
        ApiResponse<String> getPrivateProbe() {
            return ApiResponse.success("private", "OK");
        }
    }
}
