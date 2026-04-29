package com.honeytong.region.repository;

import com.honeytong.region.entity.RegionCity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionCityRepository extends JpaRepository<RegionCity, Long> {

    List<RegionCity> findAllByOrderByNameKoAsc();

    Optional<RegionCity> findByCode(String code);
}
