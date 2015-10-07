package llc.ufwa.gen;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

public class AndroidClassDataGenerator {
    
    public static View inflate(Context context, GenDataReader reader) throws XmlPullParserException {
        
        final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser xpp = factory.newPullParser();
        
        try {
            xpp.setInput(reader.getInputStream(), "");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
//        inflater.inflate(parser, root)
//        returnVal = cont
//        
        
        return null;
        
    }

}
