package com.honeytong.region.seed;

import com.honeytong.region.entity.RegionCity;
import com.honeytong.region.entity.RegionDistrict;
import com.honeytong.region.entity.RegionDong;
import com.honeytong.region.repository.RegionCityRepository;
import com.honeytong.region.repository.RegionDistrictRepository;
import com.honeytong.region.repository.RegionDongRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegionSeedImportService {

    private final RegionCityRepository regionCityRepository;
    private final RegionDistrictRepository regionDistrictRepository;
    private final RegionDongRepository regionDongRepository;

    public RegionSeedImportService(
            RegionCityRepository regionCityRepository,
            RegionDistrictRepository regionDistrictRepository,
            RegionDongRepository regionDongRepository
    ) {
        this.regionCityRepository = regionCityRepository;
        this.regionDistrictRepository = regionDistrictRepository;
        this.regionDongRepository = regionDongRepository;
    }

    @Transactional
    public RegionSeedImportResult importRecords(List<RegionSeedRecord> records) {
        Set<String> cityCodes = new HashSet<>();
        Set<String> districtCodes = new HashSet<>();
        Set<String> dongCodes = new HashSet<>();

        for (RegionSeedRecord record : records) {
            RegionCity city = upsertCity(record);
            RegionDistrict district = upsertDistrict(record, city);
            upsertDong(record, city, district);

            cityCodes.add(record.cityCode());
            districtCodes.add(record.districtCode());
            dongCodes.add(record.dongCode());
        }

        return new RegionSeedImportResult(
                records.size(),
                cityCodes.size(),
                districtCodes.size(),
                dongCodes.size()
        );
    }

    private RegionCity upsertCity(RegionSeedRecord record) {
        return regionCityRepository.findByCode(record.cityCode())
                .map(city -> {
                    city.updateNames(record.cityNameKo(), null, null);
                    return city;
                })
                .orElseGet(() -> regionCityRepository.save(
                        new RegionCity(record.cityNameKo(), null, null, record.cityCode())
                ));
    }

    private RegionDistrict upsertDistrict(RegionSeedRecord record, RegionCity city) {
        return regionDistrictRepository.findByCode(record.districtCode())
                .map(district -> {
                    district.updateNames(city, record.districtNameKo(), null, null);
                    return district;
                })
                .orElseGet(() -> regionDistrictRepository.save(
                        new RegionDistrict(city, record.districtNameKo(), null, null, record.districtCode())
                ));
    }

    private void upsertDong(RegionSeedRecord record, RegionCity city, RegionDistrict district) {
        regionDongRepository.findByCode(record.dongCode())
                .ifPresentOrElse(
                        dong -> dong.updateNames(city, district, record.dongNameKo(), null, null),
                        () -> regionDongRepository.save(
                                new RegionDong(city, district, record.dongNameKo(), null, null, record.dongCode())
                        )
                );
    }
}
