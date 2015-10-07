package llc.ufwa.geo;

import llc.ufwa.geo.Constants.GeniusModes;

public interface GeoProperties {

	boolean isGenius();
	GeniusModes getGeniusMode();
	
	long getMinTime();
	double getMinDistance();
}
