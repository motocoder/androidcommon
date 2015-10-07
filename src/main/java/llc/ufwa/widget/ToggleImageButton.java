package llc.ufwa.widget;

import llc.ufwa.concurrency.Callback;
import llc.ufwa.concurrency.CallbackPublisher;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;

public class ToggleImageButton extends ImageButton {

    private boolean toggleState;
    private Drawable toggleOnImage;
    private Drawable toggleOffImage;
    
    private final CallbackPublisher<Boolean> onToggle = new CallbackPublisher<Boolean>();
    
    public ToggleImageButton(Context context) {
        super(context);
        
        initToggleListener();
        
    }

    public ToggleImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        initToggleListener();
        
    }

    public ToggleImageButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        
        initToggleListener();
        
    }
    
    public void setToggleOnImage(Drawable image) {
        
        this.toggleOnImage = image;
        
        if(toggleState) {
            this.setImageDrawable(image);
        }
    }
    
    public void setToggleOffImage(Drawable image) {
        
        this.toggleOffImage = image;
        
        if(!toggleState) {
            this.setImageDrawable(image);
        }
        
    }

    private void initToggleListener() {
        
        this.setOnClickListener(
                
            new OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    
                    if(toggleState) {
                        ToggleImageButton.this.setImageDrawable(toggleOffImage);
                    }
                    else {
                        ToggleImageButton.this.setImageDrawable(toggleOnImage);
                    }
                    
                    toggleState = !toggleState;
                    
                    onToggle.publish(toggleState);
                    
                }
                
            }
            
        );
        
    }
    
    public void addOnToggleListener(final OnToggleListener onToggleListener) {
        
        onToggle.addCallback(
            new Callback<Void, Boolean>() {

                @Override
                public Void call(Boolean value) {
                    
                    onToggleListener.onToggle(value);
                    
                    return null;
                }
            }
        );
    }
    
    public static interface OnToggleListener {
        
        void onToggle(final boolean toggleState);
    }

    public boolean isToggleOn() {
        return toggleState;
    }

    public void setToggle(boolean b) {
        
        if(b != toggleState) {
            
            this.toggleState = b;
            onToggle.publish(b);
            
            if(toggleState) {
                ToggleImageButton.this.setImageDrawable(toggleOffImage);
            }
            else {
                ToggleImageButton.this.setImageDrawable(toggleOnImage);
            }
            
            
        }
        
    }    

}
