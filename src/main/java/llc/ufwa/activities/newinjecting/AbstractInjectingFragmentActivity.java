package llc.ufwa.activities.newinjecting;

import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import llc.ufwa.activities.injecting.InitilizationAwareApplication;
import llc.ufwa.activities.injecting.InjectingDisplay;
import llc.ufwa.activities.injecting.RunOnUIDisplay;
import llc.ufwa.collections.IdentityHashSet;
import llc.ufwa.concurrency.Callback;
import llc.ufwa.concurrency.CallbackFinalizer;
import llc.ufwa.concurrency.DefaultCallbackFinalizer;
import llc.ufwa.concurrency.RunOnActivityUIExecutor;
import llc.ufwa.concurrency.WeakCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public abstract class AbstractInjectingFragmentActivity extends FragmentActivity {
		
    private static final Logger logger = LoggerFactory.getLogger(AbstractInjectingFragmentActivity.class);
    
    private final CallbackFinalizer finalizer = new DefaultCallbackFinalizer();
    
    private final Executor nonUIThread = Executors.newSingleThreadExecutor();
    
	protected abstract Set<NewInjectableController<?>> getControllers();
	protected abstract Set<InjectingDisplay> getDisplays();
	protected abstract void onAppInitialized();
    protected abstract void onAppInitializing();

	private boolean connected;
	private boolean initialized;
	
	private boolean created;

    private IdentityHashSet<InjectingDisplay> displays;

    private IdentityHashSet<NewInjectableController<?>> controllers;
	
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
                    
                    nonUIThread.execute(
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
    protected void onCreate(Bundle savedInstanceState) {
	    
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
        
        logger.debug("connect" + displays);
        
        if(!initialized) {
                        
            this.initialized = true;
            this.onAppInitialized();
            
        }
        
        if(controllers == null) {
            controllers = new IdentityHashSet<NewInjectableController<?>>(getControllers());   
        }
        
        //create the weak reference wrapped display objects
        if(displays == null) {
            
            displays = new IdentityHashSet<InjectingDisplay>();
            
            for(NewInjectableController<?> controller : controllers) {
                
                for(final InjectingDisplay display : getDisplays()) {
                    
                    if(controller.displayClass.isInstance(display)) {
                    
                        @SuppressWarnings({ "unchecked", "rawtypes" })
                        final InjectingDisplay wrappedDisplay =
                            new RunOnUIDisplay(
                                new RunOnActivityUIExecutor(this),
                                display,
                                controller.displayClass
                            ).getWrapped();
                    
                        displays.add(wrappedDisplay);
                        
                    }
                    
                }
            
            }
            
        }
        
	    for(NewInjectableController<?> controller : controllers) {
	        
            for(InjectingDisplay display : displays) {
                
                if(!created) {
                    controller.createdDisplay(display);
                }
                
                controller.addDisplay(display);
                
            }
            
        }
	    
	    created = true;
	    
	}
	
	private void disconnectThings() {
	    
	    logger.debug("disconnect" + displays);
	    
	    if(displays == null || controllers == null) {
	        return;
	    }
	    	    	    
	    for(final NewInjectableController<?> controller : controllers) {
            
            for(InjectingDisplay display : displays) {
                controller.removeDisplay(display);
            }
            
        }
	    
	}
	
	@Override
	protected void onStart() {
	    
		super.onStart();
		
		connect(true);
	}
	
	@Override
	protected void onDestroy() {
	    
		super.onDestroy();
		
		connect(false);
		
	}
	
	@Override
	protected void onRestart() {
	    
		super.onRestart();
		
		connect(true);
	}
	
	@Override
	protected void onResume() {
	    
		super.onResume();
		
		connect(true);
	}
	
	@Override
	protected void onPause() {
	    
		super.onPause();
		
		connect(false);
	}
	
	@Override
	protected void onStop() {
	    
		super.onStop();
		
		connect(false);
	}

}
