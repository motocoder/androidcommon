package llc.ufwa.activities.inverseView;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

public class InverseView extends ViewGroup {

    private View view;
    private int angle = 0;

    private final Matrix layoutTransitionMatrix = new Matrix();

    private final Rect viewRectRotated = new Rect();

    private final RectF layoutRectF = new RectF();
    private final RectF layoutRectFRotated = new RectF();

    private final float[] touchPoint = new float[2];
    private final float[] childTouchPoint = new float[2];
    
    /**
     * 
     * @param context - for android
     */
    
    public InverseView(Context context) {
        super(context);
    }
    
    /**
     * 
     * @param context - for android
     * @param attrs - attributes, required for android
     */
    
    public InverseView(Context context, AttributeSet attrs) {
        
        super(context, attrs);
        setWillNotDraw(false);
        
    }

    public View getView() { //returns the view
        return view;
    }

    @Override
    protected void onFinishInflate() { //changes the view after inflate
        
        super.onFinishInflate();
        view = getChildAt(0);
        
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) { //measures the screen width according to the angle (landscape vs portrait)
        
        if(Math.abs(angle % 180) == 90) {
            
            measureChild(view, heightMeasureSpec, widthMeasureSpec);
            
            setMeasuredDimension(
                resolveSize(view.getMeasuredHeight(), widthMeasureSpec), 
                resolveSize(view.getMeasuredWidth(), heightMeasureSpec));
            
        }
        else {
            
            measureChild(view, widthMeasureSpec, heightMeasureSpec);
            
            setMeasuredDimension(
                resolveSize(view.getMeasuredWidth(), widthMeasureSpec), 
                resolveSize(view.getMeasuredHeight(), heightMeasureSpec));
            
        }
        
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) { //changes the layout method
       
        if(changed) {
            
            layoutRectF.set(0, 0, r - l, b - t);
            layoutTransitionMatrix.setRotate(angle, layoutRectF.centerX(), layoutRectF.centerY());
            layoutTransitionMatrix.mapRect(layoutRectFRotated, layoutRectF);
            layoutRectFRotated.round(viewRectRotated);
            
        }

        view.layout(viewRectRotated.left, viewRectRotated.top, viewRectRotated.right, viewRectRotated.bottom);
  
    }

    @Override
    protected void dispatchDraw(Canvas canvas) { //changes the way the canvas is drawn and rotates it to the angle
        
        canvas.save();
        canvas.rotate(angle, getWidth() / 2f, getHeight() / 2f);
        super.dispatchDraw(canvas);
        canvas.restore();
        
    }

    @Override
    public ViewParent invalidateChildInParent(int[] location, Rect dirty) { //invalidates the child
        
        invalidate();
        return super.invalidateChildInParent(location, dirty);
        
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) { //makes it so touch events work by getting x and y coordinates and making the event
        
        touchPoint[0] = event.getX();
        touchPoint[1] = event.getY();

        layoutTransitionMatrix.mapPoints(childTouchPoint, touchPoint);
        event.setLocation(childTouchPoint[0], childTouchPoint[1]);
        return super.dispatchTouchEvent(event);
        
    }
    
    public void changeAngle(int angle) {
        
        this.angle = angle;
        invalidate();
        
    }

}
