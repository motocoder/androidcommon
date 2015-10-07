package llc.ufwa.activities.injecting;

import java.util.HashSet;
import java.util.Set;

import llc.ufwa.concurrency.Callback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Application;

public abstract class InitilizationAwareApplication extends Application {

    private static final Logger logger = LoggerFactory.getLogger(InitilizationAwareApplication.class);
    
    private volatile boolean init;
    
    private final Set<Callback<Void, Void>> callbacks = new HashSet<Callback<Void, Void>>();
    
    public boolean isInit() {
        return init;
    }
    
    protected final void flagInitialized() {
        
        final Set<Callback<Void, Void>> toNotify = new HashSet<Callback<Void, Void>>();
        
        synchronized(callbacks) {
            
            init = true;
            toNotify.addAll(callbacks);
            
            callbacks.clear();
            
        }
        
        for(final Callback<Void, Void> callback : toNotify) {
            
            try {
                callback.call(null);
            }
            catch(Throwable t) {
                logger.error("<InitilizationAwareApplication><1>, ERROR flagging init 1:", t);
            }
            
        }
        
    }
    
    public final void addLoadedCallback(final Callback<Void, Void> callback) {
        
        final boolean runIt;
        
        synchronized(callbacks) {
            
            if(!init) {
                
                runIt = false;
                callbacks.add(callback);
                
            }
            else {
                runIt = true;
            }
            
        }
        
        if(runIt) {
            
            try {
                callback.call(null);
            }
            catch(Throwable t) {
                logger.error("<InitilizationAwareApplication><2>, ERROR flagging init 2:", t);
            }
            
        }
        
    }

}
