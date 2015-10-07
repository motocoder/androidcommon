package llc.ufwa.geo;

import java.util.List;

import llc.ufwa.geo.data.beans.SatSignal;


public interface GeoStatus {

	void setSignals(List<SatSignal> signals);

	void setGpsRunning(boolean b);
	
	void setAccuracy(double accuracy);
	
	void setLastLat(double lastLat);
	void setLastLong(double lastLong);
	
	double getAccuracy();

	double getLastLat();

	double getLastLong();

	boolean isOn();

}
