package llc.ufwa.activities.injecting;

import java.util.Set;

import llc.ufwa.concurrency.Callback;
import llc.ufwa.concurrency.CallbackFinalizer;
import llc.ufwa.concurrency.DefaultCallbackFinalizer;
import llc.ufwa.concurrency.WeakCallback;
import android.app.TabActivity;
import android.content.Context;
import android.os.Bundle;

@Deprecated
public abstract class AbstractInjectingTabs extends TabActivity {
    
//  private static final Logger logger = LoggerFactory.getLogger(AbstractInjectingActivity.class);
    
    private final CallbackFinalizer finalizer = new DefaultCallbackFinalizer();
    
    protected abstract Set<InjectableController<?>> getControllers();
    protected abstract Set<InjectingDisplay> getDisplays();
    protected abstract void onAppInitialized();
    protected abstract void onAppInitializing();

    private boolean connected;
    private boolean initialized;
    
    private void connect(boolean connecting) {
        
        final boolean doConnectOperation;
        
        if(connecting) {
            
            if(!connected) {
                
                doConnectOperation = true;
                connected = true;
                
            }
            else {
                doConnectOperation = false;
            }
            
        }
        else {
            
            if(connected) {
                
                doConnectOperation = true;
                connected = false;
                
            }
            else {
                doConnectOperation = false;
            }
            
        }
        
        if(doConnectOperation) {
            
            final Callback<Void, Void> callback = new Callback<Void, Void>() {
    
                @Override
                public Void call(Void value) {
                    
                    AbstractInjectingTabs.this.runOnUiThread(
                        new Runnable() {
    
                            @Override
                            public void run() {
                               
                                if(connected) {
                                    connectThings();
                                }
                                else {
                                    disconnectThings();
                                }
                                
                            }
                            
                        }
                        
                    );
                    
                    return null;
                    
                }
                
            };
            
            final Context appContext = this.getApplicationContext();
            
            if(appContext instanceof InitilizationAwareApplication) {
                
                final InitilizationAwareApplication loadingAware = (InitilizationAwareApplication)appContext;
                
                loadingAware.addLoadedCallback(
                    new WeakCallback<Void, Void>(callback, false, finalizer)                    
                );
                
            }
            else {
                callback.call(null);
            }
        }
        
    }
    
    @Override
    protected final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        final Context appContext = this.getApplicationContext();
        
        if(appContext instanceof InitilizationAwareApplication) {
            
            final InitilizationAwareApplication loadingAware = (InitilizationAwareApplication)appContext;
            
            if(loadingAware.isInit()) {
                
                this.onAppInitialized();
                initialized = true;
                
            }
            else {
                
                initialized = false;
                this.onAppInitializing();
                
            }
            
        }
        else {
            
            initialized = true;
            this.onAppInitialized();
            
        }
        
    }
    
    private void connectThings() {
        
        if(!initialized) {
            
            this.initialized = true;
            this.onAppInitialized();
            
        }
        
        for(InjectableController<?> controller : getControllers()) {
            
            for(InjectingDisplay display : getDisplays()) {
                controller.addDisplay(display);
            }
            
        }
        
    }
    
    private void disconnectThings() {
        
        for(InjectableController<?> controller : getControllers()) {
            
            for(InjectingDisplay display : getDisplays()) {
                controller.removeDisplay(display);
            }
        }
        
    }
    
    @Override
    protected final void onStart() {
        super.onStart();
        
        connect(true);
    }
    
    @Override
    protected final void onDestroy() {
        super.onDestroy();
        
        connect(false);
    }
    
    @Override
    protected final void onRestart() {
        super.onRestart();
        
        connect(true);
    }
    
    @Override
    protected final void onResume() {
        super.onResume();
        
        connect(true);
    }
    
    @Override
    protected final void onPause() {
        super.onPause();
        
        connect(false);
    }
    
    @Override
    protected final void onStop() {
        super.onStop();
        
        connect(false);
    }

}
