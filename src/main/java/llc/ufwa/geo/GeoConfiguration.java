package llc.ufwa.geo;

import llc.ufwa.data.exception.ResourceException;
import llc.ufwa.data.resource.cache.Cache;
import llc.ufwa.geo.Constants.GeniusModes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GeoConfiguration implements GeoConfiguror, GeoProperties {
	
	private static final Logger logger = LoggerFactory.getLogger(GeoConfiguration.class);
	
	private boolean isGenius;
	private GeniusModes geniusMode;
	private long minTime;
	private double minDistance;
	private final Cache<String, String> manager;
	 
	public GeoConfiguration(Cache<String, String> manager) throws ResourceException {
		
		this.manager = manager;
		
		final String geniusMode = manager.get("core.geoconfig.geniusmode");
		
		if(geniusMode != null) {
			this.geniusMode = GeniusModes.valueOf(geniusMode);
		}
		else {
			this.setGeniusMode(GeniusModes.STANDARD);
		}
		
		final String minDistance = manager.get("core.geoconfig.mindistance");
		
		if(geniusMode != null) {
			this.minDistance = Double.valueOf(minDistance);
		}
		else {
			this.setMinDistance(25.0D);
		}
		
		final String isGenius = manager.get("core.geoconfig.isgenius");
		
		if(geniusMode != null) {
			this.isGenius = Boolean.valueOf(isGenius);
		}
		else {
			this.setGenius(true);
		}
		
		final String minTime = manager.get("core.geoconfig.mintime");
		
		if(geniusMode != null) {
			this.minTime = Long.valueOf(minTime);
		}
		else {
			this.setMinTime(25000L);
		}
		
		logger.info("isGenius " + this.isGenius);
		logger.info("geniusMode " + this.geniusMode);
		logger.info("minTime " + this.minTime);
		logger.info("minDistance " + this.minDistance);
		
	}

	@Override
	public boolean isGenius() {
		return isGenius;
	}

	@Override
	public GeniusModes getGeniusMode() {
		return this.geniusMode;
	}

	@Override
	public long getMinTime() {
		return minTime;
	}

	@Override
	public double getMinDistance() {
		return minDistance;
	}

	@Override
	public void setMinDistance(double distance) {
		
		try {
            manager.put("core.geoconfig.mindistance", String.valueOf(distance));
        } catch (ResourceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
		this.minDistance = distance;
	}

	@Override
	public void setGenius(boolean geniusOn) {
		
		try {
            manager.put("core.geoconfig.isgenius", String.valueOf(geniusOn));
        } catch (ResourceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
		this.isGenius = geniusOn;
	}

	@Override
	public void setMinTime(long time) {
		
		try {
            manager.put("core.geoconfig.mintime", String.valueOf(time));
        } catch (ResourceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
		this.minTime = time;
	}

	@Override
	public void setGeniusMode(GeniusModes mode) {
		
		try {
            manager.put("core.geoconfig.geniusmode", String.valueOf(mode));
        } catch (ResourceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
		this.geniusMode = mode;
	}
}
