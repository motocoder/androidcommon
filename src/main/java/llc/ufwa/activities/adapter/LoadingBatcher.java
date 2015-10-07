package llc.ufwa.activities.adapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import llc.ufwa.concurrency.Callback;
import llc.ufwa.concurrency.CallbackFinalizer;
import llc.ufwa.concurrency.DefaultCallbackFinalizer;
import llc.ufwa.concurrency.WeakCallback;
import llc.ufwa.data.beans.Entry;
import llc.ufwa.data.exception.ResourceException;
import llc.ufwa.data.resource.loader.ParallelResourceLoader;
import llc.ufwa.data.resource.loader.ResourceEvent;
import llc.ufwa.util.CollectionUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//test
public class LoadingBatcher<RowKey> {

    private static final Logger logger = LoggerFactory.getLogger(LoadingBatcher.class);
    
    private final Set<ParallelResourceLoader<RowKey, ?>> loaders;
    private final int amountToLoad;
    private final String loggingTag;
    private final Callback<Object, Entry<RowKey, ResourceEvent<?>>> callback;
    private final int batchSize;
    
    private final CallbackFinalizer finalizer = new DefaultCallbackFinalizer();
    
//    private final Set<RowKey> out = new HashSet<RowKey>();

    public LoadingBatcher(
        final int amountToLoad,
        final Set<ParallelResourceLoader<RowKey, ?>> loaders,
        final String loggingTag,
        final Callback<Object, Entry<RowKey, ResourceEvent<?>>> callback,
        final int batchSize
    ) {
        
        this.batchSize = batchSize;
        this.callback = callback;
        this.loggingTag = loggingTag;
        this.amountToLoad = amountToLoad;
        this.loaders = new HashSet<ParallelResourceLoader<RowKey, ?>>(loaders);
        
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void prep(final List<RowKey> keys, final int position) throws ResourceException {
        
        if(keys.size() < 2) {
            return;
        }
        
        final Set<RowKey> keysToLoad = new HashSet<RowKey>();
            
        final int chunkSize;
        
        if(keys.size() < amountToLoad) {
            chunkSize = (keys.size() /2) * 2;    
        }
        else {
            chunkSize = amountToLoad;
        }
        
        if(position >= keys.size() || position < 0) {
            
            logger.error("<LoadingBatcher><1>, " + loggingTag + "OUT OF BOUNDS!");
            throw new IndexOutOfBoundsException("<LoadingBatcher><2>, Position: " + position);
            
        }
        
        keysToLoad.addAll(CollectionUtil.loadChunkAround(keys, chunkSize, position));
        
        final List<RowKey> sorted = new ArrayList<RowKey>(keysToLoad);
        
        Collections.sort(
            sorted,
            new Comparator<RowKey>() {

                @Override
                public int compare(RowKey arg0, RowKey arg1) { 
                    
                    final int firstIndex = keys.indexOf(arg0);
                    final int secondIndex = keys.indexOf(arg1);
                    
                    return firstIndex - secondIndex;
                    
                }
            }
        );
            
        for(final ParallelResourceLoader loader : loaders) {
            
            final Set<RowKey> unsent = new HashSet<RowKey>(sorted);
            
            while(unsent.size() > 0) {
                
                final Map<RowKey, Callback<Object, ResourceEvent<?>>> callbacks =
                        new HashMap<RowKey, Callback<Object, ResourceEvent<?>>>();
                
                for(final RowKey key : sorted) {
                    
                    if(batchSize == callbacks.size()) {
                        break;
                        
                    }
                    
                    if(unsent.contains(key)) {
                        unsent.remove(key);
                    }
                    else {
                        continue;
                    }
                    
                    callbacks.put(
                        key,
                        new WeakCallback(
                            new Callback<Object, ResourceEvent<?>>() {
        
                                @Override
                                public Object call(
                                    final ResourceEvent<?> value
                                ) { 
                                    
                                    callback.call(new Entry<RowKey, ResourceEvent<?>>(key, value));
                                    return false;
                                    
                                }
                                
                            }, 
                            false,
                            finalizer
                        )
                        
                    );
                    
                }
                
//                loader.getAllParallel(new HashMap<RowKey, Callback<Object, ResourceEvent<?>>>(callbacks));
                
                for(Map.Entry<RowKey, Callback<Object, ResourceEvent<?>>> entry : callbacks.entrySet()) {
                    loader.getParallel(entry.getValue(), entry.getKey());
                }
                
                callbacks.clear();
                
            }
            
        }
        
    }

}
