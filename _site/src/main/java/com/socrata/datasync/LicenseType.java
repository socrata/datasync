package com.socrata.datasync;

public enum LicenseType {
	no_license("-- No License --", ""), //this doesn't work, need to figure out what value is needed to remove a license.  Tried "", "''", "null"
	
	cc0_10("Creative Commons (CC) - 1.0 Universal", "CC0_10"),
	
    cc_30_by_aus("CC - Attribution 3.0 Australia", "CC_30_BY_AUS"),	
    
    cc_30_by("CC - Attribution 3.0 Unported", "CC_30_BY"), 
    
    cc_30_by_nd("CC - Attribution | No Derivative Works 3.0 Unported", "CC_30_BY_ND"),
    
    cc_30_by_nc("CC - Attribution | Noncommercial 3.0 Unported", "CC_30_BY_NC"),
    
    cc_30_by_nc_nd("CC - Attribution | Noncommercial | No Derivative Works 3.0 Unported", "CC_30_BY_NC_ND"),
    
    cc_30_by_nc_sa("CC - Attribution | Noncommercial | Share Alike 3.0 Unported", "CC_30_BY_NC_SA"),
    
    cc_30_by_sa("CC - Share Alike 3.0 Unported", "CC_30_BY_SA"),

    iodl("Italian Open Data License 2.0", "IODL"),
	
	open_database_license("Open Database License", "OPEN_DATABASE_LICENSE"),
	
	public_domain("Public Domain", "PUBLIC_DOMAIN");


    private String label;
    private String value;

    private LicenseType(final String label, final String value) {
        this.label = label;
        this.value = value;
    }

    public String toString() {
        return label;
    }
    
    public String getLabel() {
    	return label;
    }
    
    public String getValue() {
    	return value;
    }
    
    public static LicenseType getLicenseTypeForValue(String valueToMatch) {
        for(LicenseType licenseType : LicenseType.values()) {
        	if (licenseType.getValue().equals(valueToMatch)) {
        		return licenseType;
        	}
        }      	
        return LicenseType.no_license;
    }
    
     public static LicenseType getLicenseTypeForLabel(String labelToMatch) {
        for(LicenseType licenseType : LicenseType.values()) {
        	if (licenseType.getLabel().equals(labelToMatch)) {
        		return licenseType;
        	}
        }      	
        return LicenseType.no_license;
     }
}

