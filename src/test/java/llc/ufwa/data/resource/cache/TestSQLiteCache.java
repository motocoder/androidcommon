package llc.ufwa.data.resource.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executor;

import junit.framework.TestCase;
import llc.ufwa.concurrency.ParallelControl;
import llc.ufwa.data.exception.ResourceException;
import llc.ufwa.data.resource.SerializingConverter;
import llc.ufwa.logging.LogcatAppender;
import android.content.Context;

public class TestSQLiteCache {
    
    static {

        org.apache.log4j.Logger root = org.apache.log4j.Logger.getRootLogger();
        root.addAppender(new LogcatAppender("AndroidCommonTest"));
             
    }
    
    private Cache<String,String> cache;
    private Cache<String,String> cache2;
    
    private final Random generator = new Random();
    
    private final Context context;
    
    public TestSQLiteCache(Context context) {
        this.context = context;
    }
    
    public void multiThreadedTest() {
        Executor executor = new Executor(){
    
            @Override
            public void execute(Runnable arg0) {
                
            }
            
        };
        
        cache = 
                new ValueConvertingCache<String, String, byte []>(
                    new KeyEncodingCache<byte []>(
                        new SynchronizedCache<String, byte[]>(
                            new SQLiteCache(
                                "data.db", 
                                "cache", 
                                10000, 
                                -1, 
                                context, 
                                executor
                            )
                        )
                    ),
                    new SerializingConverter<String>()
                );
                
        cache2 = 
                new ValueConvertingCache<String, String, byte []>(
                    new KeyEncodingCache<byte []>(
                        new SynchronizedCache<String, byte[]>(
                            new SQLiteCache(
                                "data2.db", 
                                "cache2", 
                                10000, 
                                -1, 
                                context, 
                                executor
                            )
                        )
                    ),
                    new SerializingConverter<String>()
                );
                
        final ParallelControl<Object> parallel = new ParallelControl<Object>();
        
        try {
    
            for (int x = 1; x < 200; x++) {
                
                final Thread daemon = 
                        new Thread() {
                        
                            @Override
                            public void run() {
                                
                               try {
                                   parallel.blockOnce();
                               }
                               
                               catch (InterruptedException e1) {
                                   e1.printStackTrace();
                               }
                               
                               try {
                                   
                                   for (int x = 1; x < 30; x++) {
                                       testAll();
                                   }
                                   
                                   for (int x = 1; x < 30; x++) {
                                       testMultiGet();
                                   }
                                   
                               }
                               
                               catch (Exception e) {
                                   e.printStackTrace();
                               }
                                
                            }
    
                            private void testAll() throws ResourceException {
                                
                                try {
                                    
                                    final String key = String.valueOf(generator.nextLong());
                                    final String value = String.valueOf(generator.nextLong());
                                    
                                    cache.put(key, value);
                                    
                                    //TestCase.assertTrue(cache.exists(key));
                                    TestCase.assertEquals(value, cache.get(key)); //test exists=true and get
                                    
                                    cache.remove(key);
                                    
                                }
                                catch (Exception e) {
                                    e.printStackTrace();
                                }
                                
                            }
                            
                            private void testMultiGet() {
                                
                                try {
                                    
                                    final List<String> keys = new ArrayList<String>();
                                    
                                    final String key2 = String.valueOf(generator.nextLong());
                                    final String key22 = String.valueOf(generator.nextLong());
                                    final String value2 = String.valueOf(generator.nextLong());
                                    final String value22 = String.valueOf(generator.nextLong());
                                    
                                    keys.add(key2);
                                    keys.add(key22);
                                    
                                    cache.put(key2, value2);
                                    cache.put(key22, value22);
                                    
                                    final List<String> results2 = cache.getAll(keys); //test getAll() method
                                    
                                    TestCase.assertNotNull(results2);
                                    TestCase.assertEquals(2, results2.size());
                                    TestCase.assertEquals(results2.get(0), value2);
                                    TestCase.assertEquals(results2.get(1), value22); //test multiple get()'s
                                    
                                    cache.remove(key2);
                                    cache.remove(key22);
                                    
                                } 
                                catch (Exception e) {
                                    e.printStackTrace();
                                }
                                
                            }
                
                };
                
                daemon.setDaemon(true);
                daemon.start();
                
            }
            
            parallel.unBlockAll();
            
        }
        
        catch (Exception e) {
            e.printStackTrace();
        }
        
    }


    //add expiring test and size test
    
    public void testClear() {
        
        try {
            
            final String key = String.valueOf(generator.nextLong());
            final String value = String.valueOf(generator.nextLong());
            
            cache2.put(key, value);
            
            TestCase.assertTrue(cache2.exists(key));
            TestCase.assertEquals(value, cache2.get(key)); //test exists=true and get
            
            cache2.clear();
            
            TestCase.assertNull(cache2.get("hi")); //test get() method without value existing
            TestCase.assertFalse(cache2.exists("hi")); //test exists=false
            
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
        try {
            
            final List<String> keys = new ArrayList<String>();
            
            final String key = String.valueOf(generator.nextLong());
            final String key2 = String.valueOf(generator.nextLong());
            final String value = String.valueOf(generator.nextLong());
            final String value2 = String.valueOf(generator.nextLong());
            
            keys.add(key);
            keys.add(key2);
            
            cache2.put(key, value);
            cache2.put(key2, value2);
            
            final List<String> results2 = cache2.getAll(keys); //test getAll() method
            
            TestCase.assertEquals(results2.get(0), value);
            TestCase.assertEquals(results2.get(1), value2); //test multiple get()'s
            
            cache2.clear();
            
            TestCase.assertNull(cache2.get("hi")); //test get() method without value existing
            TestCase.assertFalse(cache2.exists("hi")); //test exists=false
            
        } 
        catch (Exception e) {
            e.printStackTrace();
        }
        
    }
    
}
