package llc.ufwa.data.resource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import llc.ufwa.data.exception.ResourceException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class StreamBitmapConverter implements Converter<Bitmap, InputStream> {

    private final BytesBitmapConverter bytesConvter = new BytesBitmapConverter();
    
    @Override
    public InputStream convert(Bitmap old) throws ResourceException {
        
        if(old == null) {
            return null;
        }
        
        return new ByteArrayInputStream(bytesConvter.convert(old));
        
    }

    @Override
    public Bitmap restore(InputStream newVal) throws ResourceException {
        return BitmapFactory.decodeStream(newVal);
    }

}
