package llc.ufwa.geo;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import llc.ufwa.concurrency.Callback;
import llc.ufwa.concurrency.NeverCrashingExecutor;
import llc.ufwa.geo.listeners.AquirePoller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.location.LocationListener;
import android.location.LocationManager;

public abstract class SoftListener {
	
	private static final Logger logger = LoggerFactory.getLogger(SoftListener.class);
	
	private final LocationManager manager;
	private final LocationListener listener;
	private final AquirePoller aquirer;
	
	private final Executor executor = new NeverCrashingExecutor(Executors.newSingleThreadExecutor());

	public SoftListener(
		final LocationManager manager,
		final LocationListener listener
	) {
		
		this.manager = manager; 
		this.listener = listener;
		this.aquirer = new AquirePoller(manager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
		
		aquirer.addCallback(
			new Callback<Void, Boolean>() {
	
				@Override
				public Void call(Boolean value) {
					
					synchronized(SoftListener.this) {
						
						if(enabled) {
							
							if(value) {		
								
								logger.info("Turning ON");
								
								on = true;
								manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, getMinTime(), getMinDistance(), listener);							
								
							}
							else {
								//TODO handle failure. Not sure if this can happen
								logger.error("WHAT THE FUCK");
							}
						}
			
						aquirring = false;
						manager.removeUpdates(aquirer);
					}
					return null;
				}
			}
		);
	}
	
	protected abstract float getMinDistance();
	protected abstract long getMinTime();
	
	boolean aquirring = false;
	boolean rolling = false;
	boolean enabled = false;
	boolean on = false;
	
	public synchronized void enable() {
		
		enabled = true;
		
		if(!on && !rolling) {
			aquire();
		}
	}
	
	private synchronized void aquire() {
		
		if(!aquirring) {
			aquirring = true;
			
			manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, aquirer);
			
		}
	}

	public synchronized void disable() {
		
		if(enabled) {
			
			enabled = false;
			
			
			if(on) {
				
				if(!rolling) {
					rolling = true;
					
					executor.execute(
						new Runnable() {
		
							@Override
							public void run() {
								try {
									Thread.sleep(30000);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
								
								synchronized(SoftListener.this) {
									if(!enabled) {
										
										manager.removeUpdates(listener);
										on = false;
									}
									
									rolling = false;
								}
							}
						}
					);
				}
			}
			else if(aquirring) {
				
				on = false;
				
				aquirring = false;
				manager.removeUpdates(aquirer);
			}
			else {
				throw new IllegalStateException("<SoftListener><1>, Should never get in this state, wierd stuff is happening");
			}
		}
	}

	public boolean isEnabled() {
		return enabled;
	}

	public boolean isOn() {
		return on;
	}
}
