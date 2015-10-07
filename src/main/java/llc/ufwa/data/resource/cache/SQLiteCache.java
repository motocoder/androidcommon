package llc.ufwa.data.resource.cache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import llc.ufwa.data.exception.ResourceException;
import llc.ufwa.data.resource.SerializingConverter;
import llc.ufwa.util.StringUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * File cache for android using a SQLite Implementation
 * 
 * @author swagner
 *
 */
public class SQLiteCache implements Cache<String, byte []> {
    
    private static final Logger logger = LoggerFactory.getLogger(SQLiteCache.class);
    
    private static final int databaseVersion = 1;
    private static final String KEY_COL = "key";
    private static final String VALUE_COL = "value";
    private static final String TIME_COL = "time";
    
    private final long expiresTimeout;
    private final long maxSize;
    private final States states = new States();
    private final SQLiteDatabase sqLiteData;
    private final Context context;
    private final String tableName;   
    private final String databaseFileName;
    private final Cache<String, Integer> sizesCache;
    
    
    /**
     * 
     * @param DATABASE_NAME name of database file
     * @param TABLE_CACHE name of table
     * @param maxSize -1 for no maximum
     * @param expiresTimeout -1 for never expiring
     * @param context
     * @param Executor
     * 
     */
    
    public SQLiteCache(
        final String DATABASE_NAME,
        final String TABLE_CACHE,
        final long maxSize,
        final long expiresTimeout,
        final Context context,
        final Executor executor
    ) {
        
        this.expiresTimeout = expiresTimeout;
        this.context = context;
        this.databaseFileName = DATABASE_NAME;
        this.tableName = TABLE_CACHE;
        this.maxSize = maxSize;
        
        if(this.maxSize >= 0) {
            
            this.sizesCache =
                new ValueConvertingCache<String, Integer, byte []>(
                    new SQLiteCache(
                        DATABASE_NAME + "_SIZES", 
                        TABLE_CACHE + "_SIZES",
                        -1,
                        -1,
                        context,
                        executor
                    ), 
                    new SerializingConverter<Integer>()
                );
                
        }
        else {
            this.sizesCache = null;
        }
        
        this.sqLiteData = new OpenHelper(this.context).getWritableDatabase();
        
    }
    
    /**
     * clears all values from the table - no clean needed because all values will be deleted anyways
     * @throws ResourceException 
     */
    @Override
    public void clear() throws ResourceException {
        
        if(this.sizesCache != null) {
            this.sizesCache.clear();
        }
            
        final String sqlDeleteAll = "DELETE FROM " + tableName + ";";
        sqLiteData.execSQL(sqlDeleteAll);
        
        states.zeroSize();
      
    }
    
    /**
     * returns the BLOB with the key "key"
     */
    @Override
    public byte [] get(String key) throws ResourceException {
        
        clean();
            
        final String selection = "SELECT * FROM " + tableName + 
            " WHERE " + KEY_COL + " = ?";
        
        final Cursor c = sqLiteData.rawQuery(
            selection, 
            new String[]{key}
            );
            
        final int columnValue = c.getColumnIndex(VALUE_COL);
            
        try {
                
            byte [] returnVal = null;
 
            if(c.moveToFirst()) {
                returnVal = c.getBlob(columnValue);
            }
                
            return returnVal;
                
        }
        finally {
           c.close();
        }
        
    }
    
    /**
     * puts a value into the SQL table with the key "key" and BLOB "value"
     * @throws ResourceException 
     */
    @Override
    public void put(String key, byte [] value) throws ResourceException { //done
            
        clean();
        
        final ContentValues contentValues = new ContentValues();
        
        contentValues.put(KEY_COL, String.valueOf(key));
        contentValues.put(VALUE_COL, value);
        contentValues.put(TIME_COL, System.currentTimeMillis());
        
        sqLiteData.insert(tableName, null, contentValues);
        
        if(this.sizesCache != null) {
            this.sizesCache.put(key, value.length);
        }
        
        states.addSize(value.length);
          
    }
    
    /**
     * checks to see if a value exists with the given key "key"
     * @throws ResourceException 
     */
    @Override
    public boolean exists(String key) throws ResourceException {
        
        clean();
            
        final String selection = "SELECT * FROM " + tableName + 
                " WHERE " + KEY_COL + " = ?";
            
        final Cursor cursor = sqLiteData.rawQuery(
            selection, 
            new String[]{key}
            );
        
        try {
            return (cursor.getCount() > 0);
        }
        finally {
            cursor.close();
        }
        
    }
    
    /**
     * removes a value from the table with the given key "key"
     * @throws ResourceException 
     */
    @Override
    public void remove(String key) throws ResourceException {
            
        clean();
            
        final String selection = "SELECT * FROM " + tableName + 
                " WHERE " + KEY_COL + " = ?";
            
        final Cursor cursor = sqLiteData.rawQuery(
            selection, 
            new String[]{key}
            );
        
        try {

            if(this.sizesCache != null) {
                
                if(cursor.moveToFirst()) {
                
                    final int size;
                    
                    try {
                        size = this.sizesCache.get(key);
                    }
                    catch(ResourceException e) {
                        throw new RuntimeException("<SQLiteCache><1>");
                    }
                    
                    this.states.removeFromSize(size);
                
                }
                
            }
            
        }
        finally {
            cursor.close();
        }
        
        final String sqlDelete = "DELETE FROM " + tableName +
                " WHERE " + KEY_COL + 
                " = '" + key + "'";
        
        sqLiteData.execSQL(sqlDelete);
        
    }

    /**
     * returns all keys in a list given with the provided key(s) "keys"
     */
    @Override
    public List<byte[]> getAll(List<String> keys) throws ResourceException {

        clean();
        
        if(keys.size() == 0) {
            throw new IllegalArgumentException("<SQLiteCache><2>, Need to specify keys > 0");
        }
        
        final Map<String, byte []> returnVals = new HashMap<String, byte []>();
        
        final String[] keysArray = Arrays.copyOf(keys.toArray(), keys.toArray().length, String[].class);
        
        String list = "SELECT * FROM " + tableName + 
                " WHERE " + KEY_COL + " IN (?";
        
        for (int x = 1; x < keysArray.length; x++) {
            list = list.concat(", ?");
        }
        
        final String selection = list.concat(")");
        
        logger.info(selection);
        
        final Cursor c = sqLiteData.rawQuery(
            selection, 
            keysArray
            );
        
        try {
            
            final int columnKeyInt = c.getColumnIndex(KEY_COL);
            final int columnValueInt = c.getColumnIndex(VALUE_COL);
            
            if(c.moveToFirst()) {
                
                do {
                    returnVals.put(c.getString(columnKeyInt), c.getBlob(columnValueInt));                    
                } while(c.moveToNext());
            
            }
            
        }
        finally {
            c.close();
        }
        
        //SORT
        final List<byte []> finalReturnVals = new ArrayList<byte []>();
        
        for(final String key : keys) {
            finalReturnVals.add(returnVals.get(key));
        }
        
        return finalReturnVals;
          
    }
    
    /**
     * cleans the table of expired values or if the table is too large
     * @throws ResourceException 
     */
    private void clean() throws ResourceException {
            
        if(this.maxSize >= 0) {
                
            {
                
                final long expires = System.currentTimeMillis() - this.expiresTimeout;
    
                final String selectionMaxTime = "SELECT * FROM " + tableName + 
                    " WHERE " + TIME_COL + "> ?;";
    
                final String[] list = new String[]{
                        String.valueOf(expires)
                        };
                
                final Cursor c =
                    sqLiteData.rawQuery(
                        selectionMaxTime,
                        list
                    );
                
                try {
                    
                    if(c.moveToFirst()) {
                        
                        final Set<String> toDelete = new HashSet<String>();
                        
                        do {
      
                            final String key = c.getString(c.getColumnIndex(KEY_COL));
                            
                            toDelete.add(key);
                            
                        } while(c.moveToNext());
                        
                        final List<String> deletingList = new ArrayList<String>(toDelete);
                     
                        final List<Integer> sizes;
                        
                        try {
                            sizes = this.sizesCache.getAll(deletingList);
                        } 
                        catch (ResourceException e) {
                            throw new RuntimeException("<SQLiteCache><3>");
                        }
                        
                        for(int i = 0; i < sizes.size(); i++) {
                            
                            final String key = deletingList.get(i);
                            final long size = sizes.get(i);
                            
                            this.states.removeFromSize(size);
                            
                            sizesCache.remove(key);
                            
                        }
                            
                        final String sqlDeleteAll =
                            "DELETE FROM " + tableName + " WHERE "
                            + KEY_COL + " IN(" + StringUtilities.join(deletingList, ",") + ");";
                        
                        sqLiteData.execSQL(sqlDeleteAll);
                        
                    }
                    
                }
                finally {
                    c.close();
                }
            }
            
            if(this.states.getCurrentSize() > this.maxSize && this.sizesCache != null) {
                
                final String query = 
                    "SELECT * FROM " + tableName  
                    + " ORDER BY " + TIME_COL + ";";
        
                final Cursor c =
                    sqLiteData.rawQuery(
                        query,
                        null
                    );
                
                final Set<String> toDelete = new HashSet<String>();
                
                try {
                
                    final int keyCol = c.getColumnIndex(KEY_COL);
                    
                    if(c.moveToFirst()) {
                        
                        do {
                            
                            final String key = c.getString(keyCol);
                            
                            try {
                                
                                this.states.removeFromSize(this.sizesCache.get(key));
                                toDelete.add(key);
                                
                            } 
                            catch (ResourceException e) {
                                throw new RuntimeException("<SQLiteCache><4>");
                            }
                            
                        } while(c.moveToNext() && this.states.getCurrentSize() > this.maxSize);
                        
                    }
                    
                }
                finally {
                    c.close();
                }
                
                if(toDelete.size() > 0) {
                    
                    final String deleteQuery =
                        "DELETE FROM " + tableName + " WHERE "
                        + KEY_COL + " IN (" + StringUtilities.join(toDelete, ",") + ");";
                    
                    sqLiteData.execSQL(deleteQuery);
                    
                }
                
            }
            
        }
            
    }
    
    /**
     * 
     * provides synchronization methods to prevent two threads from accessing the same value
     * 
     * @author swagner
     *
     */
    private static final class States {
        
        private long currentSize;
        
        public States() {
            
        }

        public void addSize(int length) {
            this.currentSize += length;
        }

        public long getCurrentSize() {
            return currentSize;
        }
        
        public void removeFromSize(long remove) {
            this.currentSize -= remove;
        }
        
        public void zeroSize() {
            this.currentSize = 0;
        }
        
    }
    
    //creates the SQL Table
    private class OpenHelper extends SQLiteOpenHelper {
        
        private final String DATABASE_CREATE = 
                "CREATE TABLE " + tableName + 
                "(" + 
                KEY_COL + " VARCHAR(255), " +
                VALUE_COL + " BLOB, " + 
                TIME_COL + " LONG " + 
                ");";
        
        OpenHelper(Context context) {
            super(context, databaseFileName, null, databaseVersion);
        }

        @Override
        public void onCreate(SQLiteDatabase database) {
            database.execSQL(DATABASE_CREATE);
        }
        
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            
            logger.info(SQLiteCache.class.getName(),
                    "Upgrading database from version " + oldVersion + " to "
                            + newVersion + ", which will destroy all old data");
            
            db.execSQL("DROP TABLE IF EXISTS " + tableName);
            onCreate(db);
            
        }
    
    }
    
}
