package com.honeytong.region.entity;

import com.honeytong.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "region_city")
public class RegionCity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name_ko", nullable = false, length = 100)
    private String nameKo;

    @Column(name = "name_en", length = 100)
    private String nameEn;

    @Column(name = "name_ja", length = 100)
    private String nameJa;

    @Column(length = 20)
    private String code;

    protected RegionCity() {
    }

    public RegionCity(String nameKo, String nameEn, String nameJa, String code) {
        this.nameKo = nameKo;
        this.nameEn = nameEn;
        this.nameJa = nameJa;
        this.code = code;
    }

    public void updateNames(String nameKo, String nameEn, String nameJa) {
        this.nameKo = nameKo;
        this.nameEn = nameEn;
        this.nameJa = nameJa;
    }

    public Long getId() {
        return id;
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
