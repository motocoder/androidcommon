package llc.ufwa.util;

import android.content.res.Resources;
import android.util.TypedValue;

public class DPUtil {
    
    public static int getPixelsForDP(Resources r, int dp) {        
        return (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
    }

}
