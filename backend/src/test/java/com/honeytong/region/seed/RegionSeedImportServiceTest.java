package com.honeytong.region.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.honeytong.region.entity.RegionCity;
import com.honeytong.region.entity.RegionDistrict;
import com.honeytong.region.entity.RegionDong;
import com.honeytong.region.repository.RegionCityRepository;
import com.honeytong.region.repository.RegionDistrictRepository;
import com.honeytong.region.repository.RegionDongRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegionSeedImportServiceTest {

    @Mock
    private RegionCityRepository regionCityRepository;

    @Mock
    private RegionDistrictRepository regionDistrictRepository;

    @Mock
    private RegionDongRepository regionDongRepository;

    private RegionSeedImportService importService;

    @BeforeEach
    void setUp() {
        importService = new RegionSeedImportService(
                regionCityRepository,
                regionDistrictRepository,
                regionDongRepository
        );
    }

    @Test
    void importRecords_createsMissingRegionHierarchy() {
        RegionSeedRecord record = new RegionSeedRecord(
                "11",
                "서울특별시",
                "11440",
                "마포구",
                "1144066000",
                "서교동"
        );

        when(regionCityRepository.findByCode("11")).thenReturn(Optional.empty());
        when(regionDistrictRepository.findByCode("11440")).thenReturn(Optional.empty());
        when(regionDongRepository.findByCode("1144066000")).thenReturn(Optional.empty());
        when(regionCityRepository.save(any(RegionCity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(regionDistrictRepository.save(any(RegionDistrict.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(regionDongRepository.save(any(RegionDong.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegionSeedImportResult result = importService.importRecords(List.of(record));

        assertThat(result.rowCount()).isEqualTo(1);
        assertThat(result.cityCount()).isEqualTo(1);
        assertThat(result.districtCount()).isEqualTo(1);
        assertThat(result.dongCount()).isEqualTo(1);

        ArgumentCaptor<RegionDong> dongCaptor = ArgumentCaptor.forClass(RegionDong.class);
        verify(regionDongRepository).save(dongCaptor.capture());
        assertThat(dongCaptor.getValue().getNameKo()).isEqualTo("서교동");
        assertThat(dongCaptor.getValue().getCode()).isEqualTo("1144066000");
    }
}
