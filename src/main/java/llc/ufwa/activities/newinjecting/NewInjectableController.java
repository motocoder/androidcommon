package llc.ufwa.activities.newinjecting;

import java.util.concurrent.Executor;

import llc.ufwa.activities.injecting.AllDisplays;
import llc.ufwa.activities.injecting.InjectingDisplay;
import llc.ufwa.concurrency.SerialExecutor;

/**
 * This is the controller to manage displays being injected into it. 
 * 
 * It handles lifecycle of displays.
 * 
 * @author seanwagner
 *
 * @param <T>
 */
public abstract class NewInjectableController<T extends InjectingDisplay> {
    
    private final AllDisplays<T> displays;
    public final Class<T> displayClass;
    private final Executor configureRunner;
    
    public NewInjectableController(
        final Class<T> clazz,
        final SerialExecutor configureRunner
     
    ) {
        
        this.configureRunner = configureRunner;        
        this.displayClass = clazz;        
        displays = new AllDisplays<T>(clazz, getClass().getClassLoader());
        
    }

    
    protected abstract void configureDisplay(T display);
    protected abstract void createDisplay(T display);
    
    @SuppressWarnings("unchecked")
    public final void createdDisplay(final InjectingDisplay display) {
        
        if(displayClass.isInstance(display)) {
            
            displays.addDisplay((T)display);
            
            configureRunner.execute(
                new Runnable() {
        
                    @Override
                    public void run() {
                        createDisplay((T)display);
                    }
                    
                }
                
            );
            
        }
        
    }
    
	@SuppressWarnings("unchecked")
    public final void addDisplay(final InjectingDisplay display) {
	    
		if(displayClass.isInstance(display)) {
		    
		    displays.addDisplay((T)display);
		    
		    configureRunner.execute(
		        new Runnable() {
        
                    @Override
                    public void run() {
                        configureDisplay((T)display);
                    }
                    
                }
		        
		    );
			
		}
		
	}
	
    protected abstract void onDisplayRemoved();
    
	@SuppressWarnings("unchecked")
    protected final void removeDisplay(final InjectingDisplay display) {
	    
	    if(displayClass.isInstance(display)) { 
	        
            onDisplayRemoved();
            
            configureRunner.execute(
                new Runnable() {
    
                    @Override
                    public void run() {
                        displays.removeDisplay((T)display);
                    }
                }
            );
            
        }
	    
	}
	
	public T getAllDisplays() {
	    return displays.getAllDisplays();
	}
	
	public boolean hasDisplays() {
	    return displays.hasDisplays();
	}
	
	
}
