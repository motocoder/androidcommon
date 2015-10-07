package llc.ufwa.data.resource;

import java.io.ByteArrayOutputStream;

import llc.ufwa.data.exception.ResourceException;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;

public class BytesBitmapConverter implements Converter<Bitmap, byte []> {

    public BytesBitmapConverter() {
    }
    

    @Override
    public byte [] convert(Bitmap bitmap) throws ResourceException {
        
        if(bitmap == null) {
            return null;
        }
        
        final ByteArrayOutputStream bos = new ByteArrayOutputStream(); 
        bitmap.compress(CompressFormat.PNG, 0, bos); 
        return bos.toByteArray();
        
    }

    @Override
    public Bitmap restore(byte [] inBytes) throws ResourceException {
        return BitmapFactory.decodeByteArray(inBytes, 0, inBytes.length);                   
    }
    
}
