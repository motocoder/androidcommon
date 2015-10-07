package llc.ufwa;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.view.OrientationEventListener;
import android.view.WindowManager;


public class OrientationListener {
    
    private static final Logger logger = LoggerFactory.getLogger(OrientationListener.class);
    
    private final static Set<Integer> ALL_LANDSCAPE = new HashSet<Integer>();
    private final static Set<Integer> ALL_PORTRAIT = new HashSet<Integer>();
    
    public static boolean isPortrait(Integer orientation) {
        return ALL_PORTRAIT.contains(orientation);
    }
    
    public static boolean isLandscape(Integer orientation) {
        return ALL_LANDSCAPE.contains(orientation);
    }
    
    static {
    
        ALL_LANDSCAPE.add(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
        ALL_LANDSCAPE.add(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        ALL_LANDSCAPE.add(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        ALL_LANDSCAPE.add(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);

        ALL_PORTRAIT.add(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
        ALL_PORTRAIT.add(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        ALL_PORTRAIT.add(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        ALL_PORTRAIT.add(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);
        
    }
    
    private final WindowManager windowManager;
    private final OrientationEventListener orientationListener;
    
    private volatile int currentOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;
    
    public OrientationListener(final Context context) {
        
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        
        orientationListener = new OrientationEventListener(context) {
        
            @Override
            public void onOrientationChanged(int orientation) {
                
                int newOrientation;
                
                if(orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
                    //newOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT; //set to portrait when unknown orientation. IE flat
                }
                
                if(isTablet()) { 

                    orientation -= 90;   //switched from += -90, since tablet, orient differently
                    if(orientation < 0) {  //Check if we have gone too far back, keep the result between 0-360
                        orientation += 360;
                    }  
                    
                }
                
                final int portraitUpperBound;
                final int portraitLowerBound;
                
                final int landscapeUpperBound;
                final int landscapeLowerBound;
                
                final int revPortraitUpperBound;
                final int revPortraitLowerBound;
                
                final int revLandscapeUpperBound;
                final int revLandscapeLowerBound;
                

                    
                landscapeUpperBound = 300;
                landscapeLowerBound = 240;
                
                revLandscapeUpperBound = 120;
                revLandscapeLowerBound = 60;
                
                portraitUpperBound = 30;
                portraitLowerBound = 330;
                
                revPortraitUpperBound = 210;
                revPortraitLowerBound = 150;
                 
                final String phoneModel = android.os.Build.MODEL.toLowerCase(Locale.getDefault());
                
                if((orientation >= portraitLowerBound || orientation <= portraitUpperBound) && portraitUpperBound != Integer.MIN_VALUE) {
                    newOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    
                }
                else if(orientation >= revLandscapeLowerBound && orientation < revLandscapeUpperBound) {                                        
                    
                    
                    if(phoneModel.toLowerCase().equals("kfthwi")) {
                        
                        //screen flip for kindle hdx
                        newOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                        
                        
                    } else {
                        
                        newOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                        
                    }
                    
                }
                else if(orientation >= revPortraitLowerBound && orientation < revPortraitUpperBound) {
                    
                    
                    if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH 
                        || isTablet())
                    ) {                             
                        
                        newOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;          //switched to reverse port from 9  
                        
                    } else {
                        
                        newOrientation = currentOrientation;
                        
                    }
                    
                }
                else if(orientation >= landscapeLowerBound && orientation < landscapeUpperBound) {
                    
                    if (isTablet()) {

                            
                            newOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;  //since it is a tablet, set orientation to sensor landscape                                                                 
                        
                        
                    } else {                            
                        
                        if(phoneModel.toLowerCase().equals("kfthwi")) {

                            //screen flip for kindle hdx
                            newOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                                              
                            
                        } else {
                            
                            newOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE; //set to regular landscape if not tablet
                                                        
                        }            
                        
                    }
                    
                }
                else {
                    newOrientation = currentOrientation;
                }
                
                if(currentOrientation != newOrientation) {
                    
                    currentOrientation = newOrientation;
                    
                }
               
            }

           
            
        };
    }
    
    public static boolean isLocked(final Context context) {
        
        try {
            
            final boolean orientationLocked = Settings.System.getInt(
                context.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION) == 0;
            
            if(orientationLocked) {
                return true;
                
            }
                
        
        }
        catch (SettingNotFoundException e) {
            logger.error("<Overlay><2>, " + "SETTING NOT FOUND:", e);

        }
        
        return false;
        
    }
    
    private boolean isTablet() {
        
        final boolean returnVal;
        
        final int orientation = windowManager.getDefaultDisplay().getRotation(); // 0 port 3 land 2 port 1 land  //switch to getRotation() from getOrientation()
    
        final int height = windowManager.getDefaultDisplay().getHeight();
        final int width = windowManager.getDefaultDisplay().getWidth();
        
        if(orientation == 3 || orientation == 1) { // in landscape orientation  //switched from orientation % 2 == 0
            
            if(width < height) {
                returnVal = true;
            }
            else {
                returnVal = false;
            }
            
        }
        else {
            
            if(height < width) {
                returnVal = true;
            }
            else {
                returnVal = false;
            }
            
        }
        
        return returnVal; 
        
    }
    
    public void enable() {
        this.orientationListener.enable();
    }
    
    public void disable() {
        this.orientationListener.disable();
    }

    public int getCurrentOrientation() {
        return currentOrientation;
    }
    
    public boolean isPortrait() {        
        return ALL_PORTRAIT.contains(getCurrentOrientation());
    }
    
    public boolean isLandscape() {        
        return ALL_LANDSCAPE.contains(getCurrentOrientation());
    }

}
