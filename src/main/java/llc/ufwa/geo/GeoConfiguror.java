package llc.ufwa.geo;

import llc.ufwa.geo.Constants.GeniusModes;

public interface GeoConfiguror {

	void setMinDistance(double distance);
	void setGenius(boolean geniusOn);
	void setMinTime(long time);
	void setGeniusMode(GeniusModes mode);
}
