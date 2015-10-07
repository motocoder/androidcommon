package llc.ufwa.data.resource;

import llc.ufwa.data.exception.ResourceException;
import android.graphics.Bitmap;

public class BitmapSizeConverter implements Converter<Bitmap, Integer> {

    @Override
    public Integer convert(Bitmap old) throws ResourceException {
        
        final int returnVal = old.getRowBytes() *old.getHeight();
               
        return returnVal;
        
    }

    @Override
    public Bitmap restore(Integer newVal) throws ResourceException {
        return null;
    }

}
