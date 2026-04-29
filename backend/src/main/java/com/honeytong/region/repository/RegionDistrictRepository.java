package com.honeytong.region.repository;

import com.honeytong.region.entity.RegionDistrict;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionDistrictRepository extends JpaRepository<RegionDistrict, Long> {

    List<RegionDistrict> findByCityIdOrderByNameKoAsc(Long cityId);

    Optional<RegionDistrict> findByCode(String code);
}
