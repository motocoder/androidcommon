package llc.ufwa.geo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

import llc.ufwa.concurrency.Callback;
import llc.ufwa.geo.data.beans.SatSignal;
import llc.ufwa.geo.exception.ShittySignalException;
import llc.ufwa.geo.listeners.StatusListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;



public class GeoEngine {
	
	private static final Logger logger = LoggerFactory.getLogger(GeoEngine.class);
	
	private final Set<PointGiver> givers = new HashSet<PointGiver>();	
	private final Object onOffLock = new Object();
	private final SoftListener softListener;
	private Status status;

	public GeoEngine(
		final LocationManager manager,
		final Executor executor,
		final GeoConfiguration configIn
	)  {
		
		this.config = configIn;
		this.status = this.new Status();
		final StatusListener topListener = new StatusListener(status, manager, executor);
		final MainListener mainListener = this.new MainListener();
				
		executor.execute(
			new Runnable() {
	
				@Override
				public void run() {
					
					Looper.prepare();
					
					while(!manager.addGpsStatusListener(topListener)) {
						logger.error("<GeoEngine><1>, Failed to add gps status listener, retrying in 30 seconds");
						try {
							Thread.sleep(30000);
						} catch (InterruptedException e) {
							logger.error("<GeoEngine><2>, interrupted for some reason");
							break;
						}
					}
				}
				
			}
		);
		
		this.softListener = new SoftListener(manager, mainListener) {

			@Override
			protected float getMinDistance() {
				synchronized(configurationLock) {
					return (float) config.getMinDistance();
				}
			}

			@Override
			protected long getMinTime() {
				synchronized(configurationLock) {
					return config.getMinTime();
				}
			}
		};
	}
	
	private final Object configurationLock = new Object();
//	private final Genius genius = new Genius();
	private final GeoConfiguration config;
	
	public void reconfigure(GeoProperties properties) {
	    synchronized(configurationLock) {
	    	
	    }
	}
	
	public void addGatherer(PointGiver pipeOut) throws ShittySignalException {
	
		synchronized(onOffLock) {
			
			synchronized(givers) {
				givers.add(pipeOut);
				softListener.enable();
			}
		}
	}
	
	public void removeGatherer(PointGiver pipeOut) throws ShittySignalException {

		synchronized(onOffLock) {
			
			synchronized(givers) {
				if(givers.remove(pipeOut)) {;
					if(givers.size() == 0) {
						softListener.disable();
					}
				}
			}
		}
	}
	
	public boolean isTurningOff() {
		
		synchronized(onOffLock) {
			
			return !softListener.isEnabled();
		}
		
	}
	
	public boolean isOn() {
		
		synchronized(onOffLock) {
			
			return softListener.isOn() || softListener.isEnabled();
		}
	}
	
	private class Status implements GeoStatus {
		
		private final Set<Callback<Object, GeoStatus>> statusCallbacks = new HashSet<Callback<Object, GeoStatus>>();
		
		public void addStatusCallback(final Callback<Object, GeoStatus> callback) {
			synchronized(this.statusCallbacks) {
				this.statusCallbacks.add(callback);
			}
		}
		
		private double accuracy;
		private boolean on;
		private double lastLong;
		private double lastLat;

		@Override
		public synchronized void setSignals(List<SatSignal> signals) {
			
		}

		@Override
		public synchronized void setGpsRunning(boolean on) {
			this.on = on;
		}

		@Override
		public synchronized void setAccuracy(double accuracy) {
			this.accuracy = accuracy;	
			
		}

		@Override
		public double getAccuracy() {
			return accuracy;
		}

		@Override
		public double getLastLat() {
			return lastLat;
		}

		@Override
		public double getLastLong() {
			return lastLong;
		}

		@Override
		public boolean isOn() {
			return on;
		}

		@Override
		public void setLastLat(double lastLat) {
			this.lastLat = lastLat;
		}

		@Override
		public void setLastLong(double lastLong) {
			this.lastLong = lastLong;
		}
		
		public void notifyCallbacks() {
			
			synchronized(statusCallbacks) {
				for(Callback<Object, GeoStatus> callback : statusCallbacks) {
					callback.call(this);
				}
			}
		}
		
	}
	
	private static final double TEN_E6 = 1000000D;
	
	private class MainListener implements LocationListener {

		double lastAccuracy = 0;
		
		@Override
		public void onLocationChanged(Location location) {
			synchronized(givers) {
				for(PointGiver giver : givers) {
					giver.addRaw(
							new RawPoint(
					    	location.getLatitude() * TEN_E6,
					    	location.getLongitude() * TEN_E6,
					    	location.getTime(),
					    	location.getSpeed(),
					    	location.getSpeed(),
					    	location.getBearing(),
					    	location.getAccuracy()
					    )
					);
				}
				
				lastAccuracy = location.getAccuracy();
				
				status.setAccuracy(lastAccuracy);
				status.setLastLat(location.getLatitude() * TEN_E6);
				status.setLastLong(location.getLongitude() * TEN_E6);
				status.setGpsRunning(true);
				
				status.notifyCallbacks();
			}
		}

		@Override
		public void onProviderDisabled(String provider) {
			
		}

		@Override
		public void onProviderEnabled(String provider) {
			
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			
		}
	}
	
	
	
	public void addStatusCallback(final Callback<Object, GeoStatus> callback) {
		
		status.addStatusCallback(callback);
	}
}
