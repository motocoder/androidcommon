package llc.ufwa.logging;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

import android.util.Log;

public class LogcatAppender extends AppenderSkeleton {

    private final String tag;

    public LogcatAppender(String tag) {
        this.tag = tag;        
    }
    
    @Override
    public void close() {
        
    }

    @Override
    public boolean requiresLayout() {
        return false;
    }

    @Override
    protected void append(LoggingEvent event) {
        
        final String message = event.getMessage().toString();
        
        Log.d(tag, message);
        
        if(event.getThrowableInformation() != null && event.getThrowableInformation().getThrowable() != null) {
            
            final Throwable throwable = event.getThrowableInformation().getThrowable();
            
            Log.d(tag, throwable.toString());
            
            for(final StackTraceElement elem : throwable.getStackTrace()) {
                
                Log.d(tag, elem.toString());
                
            }
            
        }
        
    }

}
