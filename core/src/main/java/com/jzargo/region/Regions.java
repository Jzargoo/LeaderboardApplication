package com.jzargo.region;

public enum Regions {
    AFRICA("AF"),
    GLOBAL("ZZ"),
    ASIA("AS"),
    EUROPE("EU"),
    NORTH_AMERICA("NA"),
    SOUTH_AMERICA("SA"),
    OCEANIA("OC"),
    ANTARCTICA("AQ");

    final String code;

    Regions(String code) {
        this.code =code;
    }

    public String getCode() {
        return code;
    }

    public static Regions fromStringCode(String code){
        for(Regions region: Regions.values()){
            if(region.getCode().equalsIgnoreCase(code)) return region;
        }
        return GLOBAL;
    }
}
