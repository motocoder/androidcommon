package llc.ufwa.util;

import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import llc.ufwa.concurrency.Callback;
import llc.ufwa.concurrency.CallbackFinalizer;
import llc.ufwa.concurrency.WeakCallback;
import llc.ufwa.data.exception.ResourceException;
import llc.ufwa.data.resource.loader.ParallelResourceLoader;
import llc.ufwa.data.resource.loader.ResourceEvent;
import android.os.Handler;
import android.os.Looper;

public class AndroidResourceLoaderUtil {

    private static final Logger logger = LoggerFactory.getLogger(AndroidResourceLoaderUtil.class);
    
    public static <Key, Value> void callParallelAndRun(
            final Handler runner, 
            final ParallelResourceLoader<Key, Value> loader, 
            final CallbackFinalizer finalizer,
            final Callback<Object, ResourceEvent<Value>> callback, 
            final Key key,
            final Executor postTo
        ) throws ResourceException {
            
        
        final Runnable runnable = new Runnable() {

            @Override
            public void run() {
                
                try {
                    
                    loader.getParallel(
                            
                        new WeakCallback<Object, ResourceEvent<Value>>(
                            new Callback<Object, ResourceEvent<Value>>() {
   
                                @Override
                                public Object call(final ResourceEvent<Value> value) {
                                        
                                    final Runnable runnable = new Runnable() {
   
                                        @Override
                                        public void run() {
                                            callback.call(value);
                                        }
                                            
                                    };
                                        
                                    if(Looper.myLooper() == runner.getLooper()) {
                                        runnable.run(); 
                                    }
                                    else {
                                        
                                        runner.post(
                                            runnable
                                        );
                                        
                                    }
                                    return null;
                                    
                                }
                            },
                            false,
                            finalizer
                        ),
                        key
                    );
                    
                } 
                catch (ResourceException e) {
                    logger.error("ERROR:", e);                    
                }
                
            }
            
        };
        
        if(postTo != null) {
            
            postTo.execute(
                runnable
            );
            
        }
        else {
            runnable.run();
        }
        
    }
    
    public static <Key, Value> void callParallelAndRun(
            final Handler runner, 
            final ParallelResourceLoader<Key, Value> loader, 
            final CallbackFinalizer finalizer,
            final Callback<Object, ResourceEvent<Value>> callback, 
            final Key key
        ) throws ResourceException {
        
        callParallelAndRun(runner, loader, finalizer, callback, key, null);
    }
    
}
