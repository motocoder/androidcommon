package llc.ufwa.geo.listeners;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import llc.ufwa.geo.GeoStatus;
import llc.ufwa.geo.data.beans.SatSignal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
import android.location.LocationManager;

public class StatusListener implements Listener {
	
	private static final Logger logger = LoggerFactory.getLogger(StatusListener.class);
	private final GeoStatus status;
	private final LocationManager manager;
	private GpsStatus lastStatus;
	private final Executor executor;

	public StatusListener(final GeoStatus status, final LocationManager manager, final Executor executor) {
		this.status = status;
		this.manager = manager;
		this.executor = executor;
	}

	@Override
	public void onGpsStatusChanged(int event) {
		
		switch(event) {
			case GpsStatus.GPS_EVENT_FIRST_FIX:
			{
				logger.info("First fix event");
				
				break;
			}
			case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
			{
				final GpsStatus lastStatus   = 
			    this.lastStatus              = manager.getGpsStatus(this.lastStatus); //recycle status object i guess
				
				executor.execute(new StatusProcessor(lastStatus));
				
				logger.info("satellite status event");
				break;
			}
			case GpsStatus.GPS_EVENT_STARTED:
			{
				logger.info("gps started event");
				status.setGpsRunning(true);
				break;
			}
			case GpsStatus.GPS_EVENT_STOPPED:
			{
				logger.info("gps stopped event");
				status.setGpsRunning(false);
				break;
			}
		}
	}
	
	private class StatusProcessor implements Runnable {
		
		private final GpsStatus gpsStatus;

		StatusProcessor(GpsStatus status) {
			this.gpsStatus = status;
		}

		@Override
		public void run() {
			
			final List<SatSignal> signals = new ArrayList<SatSignal>();
			
			for(GpsSatellite satInfo : gpsStatus.getSatellites()) {
				
				final float snr = satInfo.getSnr();
				signals.add(new SatSignal(snr));
			}
			
			status.setSignals(signals);
		}
		
	}

}
