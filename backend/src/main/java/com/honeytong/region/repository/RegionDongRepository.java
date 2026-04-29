package com.honeytong.region.repository;

import com.honeytong.region.entity.RegionDong;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionDongRepository extends JpaRepository<RegionDong, Long> {

    List<RegionDong> findByDistrictIdOrderByNameKoAsc(Long districtId);

    Optional<RegionDong> findByCode(String code);

    Optional<RegionDong> findFirstByCityNameKoAndDistrictNameKoAndNameKo(
            String cityName,
            String districtName,
            String dongName
    );
}
