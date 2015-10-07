package llc.ufwa.geo.listeners;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import llc.ufwa.concurrency.Callback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

public class AquirePoller implements LocationListener {
	
	private static final Logger logger = LoggerFactory.getLogger(AquirePoller.class);
	
	private final Stack<Location> lastLocations = new Stack<Location>();
	private int gathered;	
	private final Set<Callback<Void, Boolean>> callbacks = new HashSet<Callback<Void, Boolean>>();
	
	public AquirePoller(final Location lastKnowLocation) {
		lastLocations.push(lastKnowLocation);
	}

	@Override
	public void onLocationChanged(Location location) {
		
		if(location == null) {
			logger.info("Location was null for some reason... doing nothing");
			return;
		}
		
		Location lastLocation = lastLocations.peek();
		
		gathered++;
		
		if(location.getAccuracy() < lastLocation.getAccuracy()) {
			lastLocations.push(location);
		}
		
		if(gathered > 10 || lastLocations.size() > 5 || location.getAccuracy() < 5) {
			sendResult(true);					
		}
		
	}

	@Override
	public void onProviderDisabled(String provider) {
		sendResult(false);
	}

	@Override
	public void onProviderEnabled(String provider) {
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		
	}

	public void addCallback(Callback<Void, Boolean> callback) {
		
		synchronized(callbacks) {
			callbacks.add(callback);
		}
	}
	
	private void sendResult(boolean result) {
		
		synchronized(callbacks) {
			for(Callback<Void, Boolean> callback : callbacks) {
				callback.call(result);
			}
		}
	}

}
