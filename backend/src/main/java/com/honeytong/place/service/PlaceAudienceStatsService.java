package com.honeytong.place.service;

import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.place.entity.Place;
import com.honeytong.place.entity.PlaceAudienceStats;
import com.honeytong.place.repository.PlaceAudienceStatsRepository;
import com.honeytong.place.repository.PlaceRepository;
import com.honeytong.user.entity.User;
import com.honeytong.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlaceAudienceStatsService {

    private final PlaceAudienceStatsRepository placeAudienceStatsRepository;
    private final PlaceRepository placeRepository;
    private final UserRepository userRepository;

    public PlaceAudienceStatsService(
            PlaceAudienceStatsRepository placeAudienceStatsRepository,
            PlaceRepository placeRepository,
            UserRepository userRepository
    ) {
        this.placeAudienceStatsRepository = placeAudienceStatsRepository;
        this.placeRepository = placeRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void recalculateStats(Long placeId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "맛집을 찾을 수 없습니다."));

        PlaceAudienceStats stats = placeAudienceStatsRepository.findByIdForUpdate(placeId)
                .orElseGet(() -> new PlaceAudienceStats(place));

        List<User> participants = userRepository.findDemographicsByPlaceId(placeId);

        int age10 = 0;
        int age20 = 0;
        int age30 = 0;
        int age40 = 0;
        int age50Plus = 0;
        int male = 0;
        int female = 0;
        int otherGender = 0;
        int foreigner = 0;
        int korean = 0;

        int currentYear = LocalDate.now().getYear();

        for (User user : participants) {
            // Age Group
            Integer birthYear = user.getBirthYear();
            if (birthYear != null) {
                int age = currentYear - birthYear;
                if (age < 20) {
                    age10++;
                } else if (age < 30) {
                    age20++;
                } else if (age < 40) {
                    age30++;
                } else if (age < 50) {
                    age40++;
                } else {
                    age50Plus++;
                }
            }

            // Gender
            String gender = user.getGender();
            if (gender != null) {
                if ("MALE".equalsIgnoreCase(gender)) {
                    male++;
                } else if ("FEMALE".equalsIgnoreCase(gender)) {
                    female++;
                } else {
                    otherGender++;
                }
            }

            // Nationality
            String natCode = user.getNationalityCode();
            if (natCode != null) {
                if ("KR".equalsIgnoreCase(natCode)) {
                    korean++;
                } else {
                    foreigner++;
                }
            }
        }

        stats.setAge10Count(age10);
        stats.setAge20Count(age20);
        stats.setAge30Count(age30);
        stats.setAge40Count(age40);
        stats.setAge50PlusCount(age50Plus);
        stats.setMaleCount(male);
        stats.setFemaleCount(female);
        stats.setOtherGenderCount(otherGender);
        stats.setKoreanCount(korean);
        stats.setForeignerCount(foreigner);

        placeAudienceStatsRepository.save(stats);
    }

    @Transactional(readOnly = true)
    public List<String> generateAudienceTags(Long placeId) {
        PlaceAudienceStats stats = placeAudienceStatsRepository.findById(placeId).orElse(null);
        if (stats == null) {
            return List.of();
        }

        int nAge = stats.getAge10Count() + stats.getAge20Count() + stats.getAge30Count()
                + stats.getAge40Count() + stats.getAge50PlusCount();
        int nGender = stats.getMaleCount() + stats.getFemaleCount() + stats.getOtherGenderCount();
        int nNat = stats.getKoreanCount() + stats.getForeignerCount();

        int totalParticipants = Math.max(nAge, Math.max(nGender, nNat));
        if (totalParticipants < 1) {
            return List.of();
        }

        List<String> tags = new ArrayList<>();

        // 1. Foreigner Tag (Foreigners / nNat >= 30%)
        if (nNat >= 1) {
            double foreignerRatio = (double) stats.getForeignerCount() / nNat;
            if (foreignerRatio >= 0.3) {
                tags.add("외국인 인기");
            }
        }

        // 2. Gender Tag (Male / nGender >= 60% or Female / nGender >= 60%)
        String dominantGender = null;
        if (nGender >= 1) {
            double maleRatio = (double) stats.getMaleCount() / nGender;
            double femaleRatio = (double) stats.getFemaleCount() / nGender;
            if (maleRatio >= 0.6) {
                dominantGender = "남성";
            } else if (femaleRatio >= 0.6) {
                dominantGender = "여성";
            }
        }

        // 3. Age Tag (Highest age group ratio >= 40%)
        String dominantAge = null;
        if (nAge >= 1) {
            double r10 = (double) stats.getAge10Count() / nAge;
            double r20 = (double) stats.getAge20Count() / nAge;
            double r30 = (double) stats.getAge30Count() / nAge;
            double r40 = (double) stats.getAge40Count() / nAge;
            double r50 = (double) stats.getAge50PlusCount() / nAge;

            double[] ratios = {r10, r20, r30, r40, r50};
            String[] labels = {"10대", "20대", "30대", "40대", "50대 이상"};

            double maxRatio = -1.0;
            for (int i = 0; i < ratios.length; i++) {
                if (ratios[i] > maxRatio) {
                    maxRatio = ratios[i];
                    dominantAge = labels[i];
                }
            }

            if (maxRatio < 0.4) {
                dominantAge = null;
            }
        }

        // 4. Combined or single tags
        if (dominantAge != null && dominantGender != null) {
            tags.add(dominantAge + " " + dominantGender + " 선호");
        } else if (dominantAge != null) {
            tags.add(dominantAge + " 선호");
        } else if (dominantGender != null) {
            tags.add(dominantGender + " 선호");
        }

        return tags;
    }
}
