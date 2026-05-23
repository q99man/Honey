package com.honeytong.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.honeytong.place.entity.Place;
import com.honeytong.place.event.PlaceDemographicsRecalculateEvent;
import com.honeytong.place.entity.PlaceAudienceStats;
import com.honeytong.place.repository.PlaceAudienceStatsRepository;
import com.honeytong.place.repository.PlaceRepository;
import com.honeytong.region.entity.RegionCity;
import com.honeytong.region.entity.RegionDistrict;
import com.honeytong.region.entity.RegionDong;
import com.honeytong.user.entity.User;
import com.honeytong.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PlaceAudienceStatsServiceTest {

    private static final long PLACE_ID = 100L;

    @Mock
    private PlaceAudienceStatsRepository placeAudienceStatsRepository;

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private UserRepository userRepository;

    private PlaceAudienceStatsService placeAudienceStatsService;
    private Place place;

    @BeforeEach
    void setUp() {
        placeAudienceStatsService = new PlaceAudienceStatsService(
                placeAudienceStatsRepository,
                placeRepository,
                userRepository
        );

        User creator = new User("creator", "creator@example.com");
        RegionCity city = new RegionCity("Seoul", "Seoul", null, "11");
        RegionDistrict district = new RegionDistrict(city, "Mapo-gu", "Mapo-gu", null, "11440");
        RegionDong dong = new RegionDong(city, district, "Seogyo-dong", "Seogyo-dong", null, "1144066000");
        place = new Place(
                creator,
                dong,
                "Seogyo Soup",
                "KOREAN",
                "Seoul Mapo-gu Dongmak-ro 1",
                null,
                BigDecimal.valueOf(37.5500000),
                BigDecimal.valueOf(126.9100000),
                "10000_20000",
                "Soup",
                "A neighborhood soup place worth revisiting.",
                "Clear broth and fast turnover.",
                false
        );
        ReflectionTestUtils.setField(place, "id", PLACE_ID);
    }

    @Test
    void recalculateStats_calculatesCountsAndSavesToRepository() {
        // Arrange
        when(placeRepository.findById(PLACE_ID)).thenReturn(Optional.of(place));
        when(placeAudienceStatsRepository.findByIdForUpdate(PLACE_ID)).thenReturn(Optional.empty());

        int currentYear = LocalDate.now().getYear();
        
        List<User> participants = new ArrayList<>();
        // User 1: 25 years old (20s), Female, Korean
        User u1 = new User("user1", "u1@example.com");
        ReflectionTestUtils.setField(u1, "birthYear", currentYear - 25);
        ReflectionTestUtils.setField(u1, "gender", "FEMALE");
        ReflectionTestUtils.setField(u1, "nationalityCode", "KR");
        participants.add(u1);

        // User 2: 35 years old (30s), Male, US (Foreigner)
        User u2 = new User("user2", "u2@example.com");
        ReflectionTestUtils.setField(u2, "birthYear", currentYear - 35);
        ReflectionTestUtils.setField(u2, "gender", "MALE");
        ReflectionTestUtils.setField(u2, "nationalityCode", "US");
        participants.add(u2);

        // User 3: 22 years old (20s), Female, KR
        User u3 = new User("user3", "u3@example.com");
        ReflectionTestUtils.setField(u3, "birthYear", currentYear - 22);
        ReflectionTestUtils.setField(u3, "gender", "FEMALE");
        ReflectionTestUtils.setField(u3, "nationalityCode", "KR");
        participants.add(u3);

        when(userRepository.findDemographicsByPlaceId(PLACE_ID)).thenReturn(participants);

        // Act
        placeAudienceStatsService.recalculateStats(PLACE_ID);

        // Assert
        ArgumentCaptor<PlaceAudienceStats> statsCaptor = ArgumentCaptor.forClass(PlaceAudienceStats.class);
        verify(placeAudienceStatsRepository).save(statsCaptor.capture());
        
        PlaceAudienceStats savedStats = statsCaptor.getValue();
        assertThat(savedStats.getPlace().getId()).isEqualTo(PLACE_ID);
        assertThat(savedStats.getAge20Count()).isEqualTo(2);
        assertThat(savedStats.getAge30Count()).isEqualTo(1);
        assertThat(savedStats.getAge10Count()).isZero();
        
        assertThat(savedStats.getMaleCount()).isEqualTo(1);
        assertThat(savedStats.getFemaleCount()).isEqualTo(2);
        
        assertThat(savedStats.getKoreanCount()).isEqualTo(2);
        assertThat(savedStats.getForeignerCount()).isEqualTo(1);
    }

    @Test
    void generateAudienceTags_noParticipants_returnsEmptyList() {
        when(placeAudienceStatsRepository.findById(PLACE_ID)).thenReturn(Optional.empty());
        List<String> tags = placeAudienceStatsService.generateAudienceTags(PLACE_ID);
        assertThat(tags).isEmpty();
    }

    @Test
    void generateAudienceTags_combinesAgeAndGenderDominance() {
        // Arrange
        PlaceAudienceStats stats = new PlaceAudienceStats(place);
        // Total age count = 10 (20s dominant: 6/10 = 60% >= 40%)
        stats.setAge10Count(1);
        stats.setAge20Count(6);
        stats.setAge30Count(2);
        stats.setAge40Count(1);
        stats.setAge50PlusCount(0);
        // Total gender count = 10 (female dominant: 8/10 = 80% >= 60%)
        stats.setMaleCount(2);
        stats.setFemaleCount(8);
        stats.setOtherGenderCount(0);
        // Nationality: Korean
        stats.setKoreanCount(10);
        stats.setForeignerCount(0);

        when(placeAudienceStatsRepository.findById(PLACE_ID)).thenReturn(Optional.of(stats));

        // Act
        List<String> tags = placeAudienceStatsService.generateAudienceTags(PLACE_ID);

        // Assert: 20s and female are dominant -> "20대 여성 선호"
        assertThat(tags).containsExactly("20대 여성 선호");
    }

    @Test
    void generateAudienceTags_onlyAgeDominant() {
        // Arrange
        PlaceAudienceStats stats = new PlaceAudienceStats(place);
        // Total age count = 10 (30s dominant: 5/10 = 50% >= 40%)
        stats.setAge10Count(1);
        stats.setAge20Count(1);
        stats.setAge30Count(5);
        stats.setAge40Count(2);
        stats.setAge50PlusCount(1);
        // Gender is split: 5 male, 5 female -> no gender dominance
        stats.setMaleCount(5);
        stats.setFemaleCount(5);
        stats.setOtherGenderCount(0);

        when(placeAudienceStatsRepository.findById(PLACE_ID)).thenReturn(Optional.of(stats));

        // Act
        List<String> tags = placeAudienceStatsService.generateAudienceTags(PLACE_ID);

        // Assert: "30대 선호"
        assertThat(tags).containsExactly("30대 선호");
    }

    @Test
    void generateAudienceTags_onlyGenderDominant() {
        // Arrange
        PlaceAudienceStats stats = new PlaceAudienceStats(place);
        // Age is split: no group has >= 40% (max is 3/10 = 30%)
        stats.setAge10Count(3);
        stats.setAge20Count(3);
        stats.setAge30Count(2);
        stats.setAge40Count(2);
        stats.setAge50PlusCount(0);
        // Gender: male dominant (7/10 = 70% >= 60%)
        stats.setMaleCount(7);
        stats.setFemaleCount(3);
        stats.setOtherGenderCount(0);

        when(placeAudienceStatsRepository.findById(PLACE_ID)).thenReturn(Optional.of(stats));

        // Act
        List<String> tags = placeAudienceStatsService.generateAudienceTags(PLACE_ID);

        // Assert: "남성 선호"
        assertThat(tags).containsExactly("남성 선호");
    }

    @Test
    void generateAudienceTags_foreignerDominant() {
        // Arrange
        PlaceAudienceStats stats = new PlaceAudienceStats(place);
        // No age dominance (split evenly)
        stats.setAge10Count(2);
        stats.setAge20Count(2);
        stats.setAge30Count(2);
        stats.setAge40Count(2);
        stats.setAge50PlusCount(2);
        // No gender dominance (split evenly)
        stats.setMaleCount(5);
        stats.setFemaleCount(5);
        stats.setOtherGenderCount(0);
        // Foreigners = 4 / 10 = 40% >= 30%
        stats.setKoreanCount(6);
        stats.setForeignerCount(4);

        when(placeAudienceStatsRepository.findById(PLACE_ID)).thenReturn(Optional.of(stats));

        // Act
        List<String> tags = placeAudienceStatsService.generateAudienceTags(PLACE_ID);

        // Assert: "외국인 인기"
        assertThat(tags).containsExactly("외국인 인기");
    }

    @Test
    void generateAudienceTags_multipleDominances() {
        // Arrange
        PlaceAudienceStats stats = new PlaceAudienceStats(place);
        // 20s dominant: 50%
        stats.setAge10Count(1);
        stats.setAge20Count(5);
        stats.setAge30Count(2);
        stats.setAge40Count(2);
        stats.setAge50PlusCount(0);
        // Male dominant: 70%
        stats.setMaleCount(7);
        stats.setFemaleCount(3);
        stats.setOtherGenderCount(0);
        // Foreigner dominant: 50% >= 30%
        stats.setKoreanCount(5);
        stats.setForeignerCount(5);

        when(placeAudienceStatsRepository.findById(PLACE_ID)).thenReturn(Optional.of(stats));

        // Act
        List<String> tags = placeAudienceStatsService.generateAudienceTags(PLACE_ID);

        // Assert: "외국인 인기", "20대 남성 선호" (order matches logic order: nationality tag first, then demographic tag)
        assertThat(tags).containsExactly("외국인 인기", "20대 남성 선호");
    }

    @Test
    void recalculateStats_publishesEventWhenPublisherIsPresent() {
        // Arrange
        org.springframework.context.ApplicationEventPublisher mockPublisher = 
                org.mockito.Mockito.mock(org.springframework.context.ApplicationEventPublisher.class);
        org.springframework.test.util.ReflectionTestUtils.setField(
                placeAudienceStatsService, "eventPublisher", mockPublisher);

        // Act
        placeAudienceStatsService.recalculateStats(PLACE_ID);

        // Assert
        ArgumentCaptor<PlaceDemographicsRecalculateEvent> eventCaptor = 
                ArgumentCaptor.forClass(PlaceDemographicsRecalculateEvent.class);
        verify(mockPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getPlaceId()).isEqualTo(PLACE_ID);
    }
}
