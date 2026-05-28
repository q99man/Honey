package com.honeytong.place.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

class PlaceRepositoryQueryTest {

    @Test
    void nearbyQueryDoesNotRequireGeneratedLocationColumn() throws Exception {
        Method method = PlaceRepository.class.getMethod(
                "findNearbyPlaces",
                String.class,
                String.class,
                int.class
        );

        String query = method.getAnnotation(Query.class).value();

        assertThat(query).doesNotContain("p.location");
        assertThat(query).contains("p.longitude");
        assertThat(query).contains("p.latitude");
    }
}
