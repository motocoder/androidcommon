package llc.ufwa.widget;

import llc.ufwa.util.StopWatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;

public class LinearListView extends LinearLayout {

    private static final Logger logger = LoggerFactory.getLogger(LinearListView.class);
    
    private final DataSetObserver observer = new DataSetObserver() {

        @Override
        public void onChanged() {
            super.onChanged();
            
            logger.debug("changed");
            
            post(
                new Runnable() {

                    @Override
                    public void run() {
                        repopulate();                        
                    }
                }
            );
            
            
            
        }

        @Override
        public void onInvalidated() {
            super.onInvalidated();
            
            logger.debug("invalidated");
            
            post(
                new Runnable() {

                    @Override
                    public void run() {
                        repopulate();                        
                    }
                }
            );
            
        }
        
        
    };
    
    private BaseAdapter adapter = null;

    private OnItemClickListener clickListener;

    public LinearListView(Context context) {
        super(context);
        
        this.setOrientation(LinearLayout.VERTICAL);
    }
    
    public LinearListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        this.setOrientation(LinearLayout.VERTICAL);
        
    }

    public LinearListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        
        this.setOrientation(LinearLayout.VERTICAL);
        
    }
    
    public void setAdapter(final BaseAdapter adapter) {
        
        if(this.adapter != null) {
            this.adapter.unregisterDataSetObserver(observer);
        }
        this.adapter = adapter;
        adapter.registerDataSetObserver(observer);
    }
    
    private void repopulate() {
        
        
        final StopWatch watch = new StopWatch();
        watch.start();
        
        final BaseAdapter myAdapter = adapter;
        
        logger.debug("repopulating");
        
        this.removeAllViews();
        
        for(int i = 0; i < myAdapter.getCount(); i++) {
            
            final View view = myAdapter.getView(i, null, this);
            
            final int myI = i;
            
            view.setOnClickListener(
                    
                new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        
                        final OnItemClickListener myClickListener = clickListener;
                        
                        if(myClickListener != null) {
                            myClickListener.onItemClick(null, view, myI, myAdapter.getItemId(myI));
                        }
                    }
                    
                }
                
            );
            
            logger.debug("Adding view " + view);
            
            this.addView(view, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            
        }
        
        logger.debug("time " + watch.getTime());
        
    }

    public void setOnItemClickListener(
        final OnItemClickListener clickListener
    ) {
        
        this.clickListener = clickListener;
        repopulate(); 
        
    }
}
