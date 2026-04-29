package com.honeytong.region.entity;

import com.honeytong.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "region_dong")
public class RegionDong extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "city_id")
    private RegionCity city;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "district_id")
    private RegionDistrict district;

    @Column(name = "name_ko", nullable = false, length = 100)
    private String nameKo;

    @Column(name = "name_en", length = 100)
    private String nameEn;

    @Column(name = "name_ja", length = 100)
    private String nameJa;

    @Column(length = 20)
    private String code;

    protected RegionDong() {
    }

    public RegionDong(
            RegionCity city,
            RegionDistrict district,
            String nameKo,
            String nameEn,
            String nameJa,
            String code
    ) {
        this.city = city;
        this.district = district;
        this.nameKo = nameKo;
        this.nameEn = nameEn;
        this.nameJa = nameJa;
        this.code = code;
    }

    public void updateNames(
            RegionCity city,
            RegionDistrict district,
            String nameKo,
            String nameEn,
            String nameJa
    ) {
        this.city = city;
        this.district = district;
        this.nameKo = nameKo;
        this.nameEn = nameEn;
        this.nameJa = nameJa;
    }

    public Long getId() {
        return id;
    }

    public RegionCity getCity() {
        return city;
    }

    public RegionDistrict getDistrict() {
        return district;
    }

    public String getNameKo() {
        return nameKo;
    }

    public String getNameEn() {
        return nameEn;
    }

    public String getNameJa() {
        return nameJa;
    }

    public String getCode() {
        return code;
    }
}
