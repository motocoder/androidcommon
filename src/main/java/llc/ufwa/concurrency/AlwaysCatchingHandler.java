package llc.ufwa.concurrency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

public class AlwaysCatchingHandler extends Handler {

    private static final Logger logger = LoggerFactory.getLogger(AlwaysCatchingHandler.class);
            
    public AlwaysCatchingHandler(Looper looper) {
        super(looper);
    }
    
    public AlwaysCatchingHandler() {
        super();
    }

    @Override
    public void handleMessage(Message msg) {
        
        try {
            super.handleMessage(msg);
        }
        catch(Throwable t) {
            logger.error("ERROR:", t);
        }
        
    }

    @Override
    public void dispatchMessage(Message msg) {
        
        try {
            super.dispatchMessage(msg);
        }
        catch(Throwable t) {
            logger.error("ERROR:", t);
        }
        
    }  
        
}
