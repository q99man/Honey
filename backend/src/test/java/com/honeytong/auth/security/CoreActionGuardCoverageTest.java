package com.honeytong.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.honeytong.comment.controller.CommentController;
import com.honeytong.comment.dto.CommentRequest;
import com.honeytong.place.controller.PlaceController;
import com.honeytong.place.dto.PlaceCreateRequest;
import com.honeytong.recommendation.controller.RecommendationController;
import com.honeytong.visit.controller.VisitController;
import com.honeytong.visit.dto.VisitVerifyRequest;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class CoreActionGuardCoverageTest {

    @Test
    void placeRegistrationRequiresPhoneVerificationAndNoActiveSanction() throws NoSuchMethodException {
        assertCoreWriteGuarded(PlaceController.class, "createPlace", Long.class, PlaceCreateRequest.class, jakarta.servlet.http.HttpServletRequest.class);
    }

    @Test
    void recommendationCreationRequiresPhoneVerificationAndNoActiveSanction() throws NoSuchMethodException {
        assertCoreWriteGuarded(RecommendationController.class, "recommend", Long.class, Long.class, jakarta.servlet.http.HttpServletRequest.class);
    }

    @Test
    void visitVerificationRequiresPhoneVerificationAndNoActiveSanction() throws NoSuchMethodException {
        assertCoreWriteGuarded(VisitController.class, "verifyVisit", Long.class, Long.class, VisitVerifyRequest.class, jakarta.servlet.http.HttpServletRequest.class);
    }

    @Test
    void commentCreateAndUpdateRequirePhoneVerificationAndNoActiveSanction() throws NoSuchMethodException {
        assertCoreWriteGuarded(CommentController.class, "createComment", Long.class, Long.class, CommentRequest.class, jakarta.servlet.http.HttpServletRequest.class);
        assertCoreWriteGuarded(CommentController.class, "updateComment", Long.class, Long.class, CommentRequest.class);
    }

    private void assertCoreWriteGuarded(
            Class<?> controllerClass,
            String methodName,
            Class<?>... parameterTypes
    ) throws NoSuchMethodException {
        Method method = controllerClass.getMethod(methodName, parameterTypes);
        assertThat(method.isAnnotationPresent(RequirePhoneVerified.class))
                .as("%s.%s must require phone verification", controllerClass.getSimpleName(), methodName)
                .isTrue();
        assertThat(method.isAnnotationPresent(RequireNoActiveSanction.class))
                .as("%s.%s must reject active blocking sanctions", controllerClass.getSimpleName(), methodName)
                .isTrue();
    }
}
