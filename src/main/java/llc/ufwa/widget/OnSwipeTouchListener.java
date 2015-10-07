package llc.ufwa.widget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

/**
 * Taken from: http://stackoverflow.com/questions/4139288/android-how-to-handle-right-to-left-swipe-gestures
 * 
 * @author Sean Wagner
 *
 */
public abstract class OnSwipeTouchListener implements OnTouchListener {
    
    private static final Logger logger = LoggerFactory.getLogger(OnSwipeTouchListener.class);
    
    private final GestureDetector gestureDetector;
    private MotionEvent downAction;

    public OnSwipeTouchListener(final Context context) {
        this.gestureDetector = new GestureDetector(context, new GestureListener());        
    }
    
    @Override
    public boolean onTouch(final View view, final MotionEvent motionEvent) {
        
        final boolean returnVal = gestureDetector.onTouchEvent(motionEvent);
                
        if(motionEvent.getAction() == MotionEvent.ACTION_UP) {
            
            if(downAction != null) {
                
                downAction = null;
                
                logger.debug("not fling");
                
                onNotFling();
                
            }
            
        }
        else if(motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            downAction = motionEvent;
        }
        
        return returnVal;
        
    }

    private final class GestureListener extends SimpleOnGestureListener {

        private static final int SWIPE_THRESHOLD = 50;
        private static final int SWIPE_VELOCITY_THRESHOLD = 50;

        @Override
        public boolean onDown(MotionEvent e) {
            
            return true;
        }
        
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            
            final boolean result;
            
            float diffY = e2.getY() - e1.getY();
            float diffX = e2.getX() - e1.getX();
            
            if(Math.abs(diffX) > Math.abs(diffY)) {
                
                if(Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    
                    if (diffX > 0) {
                        
                        result = onSwipeRight();
                        downAction = null;
                        
                    }
                    else {
                        
                        result = onSwipeLeft();
                        downAction = null;
                        
                    }
                    
                }
                else {
                    result = false;
                }
                
            } 
            else {
                
                if(Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    
                    if (diffY > 0) {
                        
                        result = onSwipeBottom();
                        downAction = null;
                        
                    } 
                    else {
                        
                        result = onSwipeTop();
                        downAction = null;
                        
                    }
                    
                }
                else {
                    result = false;
                }
                
            }
                
            
            return result;
        }
    }

    public abstract boolean onNotFling();
    
    public abstract boolean onSwipeRight();

    public abstract boolean onSwipeLeft();

    public abstract boolean onSwipeTop();

    public abstract boolean onSwipeBottom();
    
}
