package llc.ufwa.widget;

/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.R;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.MediaController;
import android.widget.MediaController.MediaPlayerControl;

/**
 * Displays a video file.  The VideoView class
 * can load images from various sources (such as resources or content
 * providers), takes care of computing its measurement from the video so that
 * it can be used in any layout manager, and provides various display options
 * such as scaling and tinting.
 */
public class VideoView extends SurfaceView implements MediaPlayerControl {
    
    private static final Logger logger = LoggerFactory.getLogger(VideoView.class);
    
    private final String TAG = "VideoView";
    // settable by the client
    private Uri         mUri;
    private int         mDuration;

    // all possible internal states
    private static final int STATE_ERROR              = -1;
    private static final int STATE_IDLE               = 0;
    private static final int STATE_PREPARING          = 1;
    private static final int STATE_PREPARED           = 2;
    private static final int STATE_PLAYING            = 3;
    private static final int STATE_PAUSED             = 4;
    private static final int STATE_PLAYBACK_COMPLETED = 5;

    // mCurrentState is a VideoView object's current state.
    // mTargetState is the state that a method caller intends to reach.
    // For instance, regardless the VideoView object's current state,
    // calling pause() intends to bring the object to a target state
    // of STATE_PAUSED.
    private int mCurrentState = STATE_IDLE;
    private int mTargetState  = STATE_IDLE;

    // All the stuff we need for playing and showing a video
    private SurfaceHolder mSurfaceHolder = null;
    private MediaPlayer mMediaPlayer = null;
    private int         mVideoWidth;
    private int         mVideoHeight;
    private int         mSurfaceWidth;
    private int         mSurfaceHeight;
    private MediaController mMediaController;
    private OnCompletionListener mOnCompletionListener;
    private MediaPlayer.OnPreparedListener mOnPreparedListener;
    private int         mCurrentBufferPercentage;
    private OnErrorListener mOnErrorListener;
    private int         mSeekWhenPrepared;  // recording the seek position while preparing
    private boolean     mCanPause;
    private boolean     mCanSeekBack;
    private boolean     mCanSeekForward;
    private final Context context;

    public VideoView(final Context context) {
        super(context);
        initVideoView();
        
        this.context = context;
        
    }

    public VideoView(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
        initVideoView();
    }

    public VideoView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        initVideoView();
        
        this.context = context;
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        //Log.i("@@@@", "onMeasure");
        int width = getDefaultSize(mVideoWidth, widthMeasureSpec);
        int height = getDefaultSize(mVideoHeight, heightMeasureSpec);
        if (mVideoWidth > 0 && mVideoHeight > 0) {
            if ( mVideoWidth * height  > width * mVideoHeight ) {
                //Log.i("@@@", "image too tall, correcting");
                height = width * mVideoHeight / mVideoWidth;
            } else if ( mVideoWidth * height  < width * mVideoHeight ) {
                //Log.i("@@@", "image too wide, correcting");
                width = height * mVideoWidth / mVideoHeight;
            } else {
                //Log.i("@@@", "aspect ratio is correct: " +
                        //width+"/"+height+"="+
                        //mVideoWidth+"/"+mVideoHeight);
            }
        }
        //Log.i("@@@@@@@@@@", "setting size: " + width + 'x' + height);
        setMeasuredDimension(width, height);
    }

    public int resolveAdjustedSize(final int desiredSize, final int measureSpec) {
        int result = desiredSize;
        final int specMode = MeasureSpec.getMode(measureSpec);
        final int specSize =  MeasureSpec.getSize(measureSpec);

        switch (specMode) {
            case MeasureSpec.UNSPECIFIED:
                /* Parent says we can be as big as we want. Just don't be larger
                 * than max size imposed on ourselves.
                 */
                result = desiredSize;
                break;

            case MeasureSpec.AT_MOST:
                /* Parent says we can be as big as we want, up to specSize.
                 * Don't be larger than specSize, and don't be larger than
                 * the max size imposed on ourselves.
                 */
                result = Math.min(desiredSize, specSize);
                break;

            case MeasureSpec.EXACTLY:
                // No choice. Do what we are told.
                result = specSize;
                break;
        }
        return result;
}

    @SuppressWarnings("deprecation") //TODO update so not deprecated
    private void initVideoView() {
        mVideoWidth = 0;
        mVideoHeight = 0;
        getHolder().addCallback(mSHCallback);
        getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
        mCurrentState = STATE_IDLE;
        mTargetState  = STATE_IDLE;
    }

    public void setVideoPath(final String path) {
        setVideoURI(Uri.parse(path));
    }

    public void setVideoURI(final Uri uri) {
        setVideoURI(uri, null);
    }

    /**
     * @hide
     */
    public void setVideoURI(final Uri uri, final Map<String, String> headers) {
        mUri = uri;
        mSeekWhenPrepared = 0;
        openVideo();
        requestLayout();
        invalidate();
    }

    public void stopPlayback() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mCurrentState = STATE_IDLE;
            mTargetState  = STATE_IDLE;
        }
    }

    private void openVideo() {
        if (mUri == null || mSurfaceHolder == null) {
            // not ready for playback just yet, will try again later
            return;
        }
        // Tell the music playback service to pause
        // TODO: these constants need to be published somewhere in the framework.
        // KEH:  It appears that the intention of the following code was to 
        // pause playback of any background audio player, which would be annoying to
        // someone who was listening to music in the background, gets an ad, then has
        // to go back to the music player and restart.  Also, this was being called 
        // right after an ad was loaded, which could interfere with the sound of the 
        // containing app.
//        final Intent i = new Intent("com.android.music.musicservicecommand");
//        i.putExtra("command", "pause");
//        context.sendBroadcast(i);

        // we shouldn't clear the target state, because somebody might have
        // called start() previously
        release(false);
        try {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnPreparedListener(mPreparedListener);
            mMediaPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
            mDuration = -1;
            mMediaPlayer.setOnCompletionListener(mCompletionListener);
            mMediaPlayer.setOnErrorListener(mErrorListener);
            mMediaPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
            mCurrentBufferPercentage = 0;
            mMediaPlayer.setDataSource(context, mUri);
            mMediaPlayer.setDisplay(mSurfaceHolder);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setScreenOnWhilePlaying(true);
            mMediaPlayer.prepareAsync();
            // we don't set the target state here either, but preserve the
            // target state that was there before.
            mCurrentState = STATE_PREPARING;
            attachMediaController();
        } catch (final IOException ex) {
            Log.w(TAG, "<1> Unable to open content: " + mUri, ex);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
            return;
        } catch (final IllegalArgumentException ex) {
            Log.w(TAG, "<2> Unable to open content: " + mUri, ex);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
            return;
        }
    }

    public void setMediaController(final MediaController controller) {
        if (mMediaController != null) {
            mMediaController.hide();
        }
        mMediaController = controller;
        attachMediaController();
    }

    private void attachMediaController() {
        if (mMediaPlayer != null && mMediaController != null) {
            mMediaController.setMediaPlayer(this);
            final View anchorView = this.getParent() instanceof View ?
                    (View)this.getParent() : this;
            mMediaController.setAnchorView(anchorView);
            mMediaController.setEnabled(isInPlaybackState());
        }
    }

    MediaPlayer.OnVideoSizeChangedListener mSizeChangedListener =
        new MediaPlayer.OnVideoSizeChangedListener() {
            @Override
			public void onVideoSizeChanged(final MediaPlayer mp, final int width, final int height) {
                mVideoWidth = mp.getVideoWidth();
                mVideoHeight = mp.getVideoHeight();
                if (mVideoWidth != 0 && mVideoHeight != 0) {
                    getHolder().setFixedSize(mVideoWidth, mVideoHeight);
                }
            }
    };

    MediaPlayer.OnPreparedListener mPreparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
		public void onPrepared(final MediaPlayer mp) {
            mCurrentState = STATE_PREPARED;

            Object data = null;
            try {
                final Method method = MediaPlayer.class.getMethod("getMetadata", boolean.class, boolean.class);
               
                
                if(method != null) {
                    data = method.invoke(mp, false, false);
                }
                else {
                    data = null;
                }
                
            } 
            catch (final SecurityException e) {
                logger.error("<VideoView><3>, videoview error security exception:", e);
            }
            catch (final NoSuchMethodException e) {
                logger.error("<VideoView><4>, videoview error no such method exception:", e);
            }
            catch (final IllegalArgumentException e) {
                logger.error("<VideoView><5>, error:", e);
            }
            catch (final IllegalAccessException e) {
                logger.error("<VideoView><6>, error:", e);
            }
            catch (final InvocationTargetException e) {
                logger.error("<VideoView><7>, error:", e);
            }
            finally {
                
            }
            
            // Get the capabilities of the player for this stream
//             = mp.getMetadata(false,
//                                      false);

            try {
                
                if (data != null) {
                    
                    final Method hasMethod = data.getClass().getMethod("has", int.class);
                    final Method getBooleanMethod = data.getClass().getMethod("getBoolean", int.class);
                    
                    mCanPause = !(Boolean) hasMethod.invoke(data, 29)//data.has(Metadata.PAUSE_AVAILABLE)
                            || (Boolean) getBooleanMethod.invoke(data, 29);//data.getBoolean(Metadata.PAUSE_AVAILABLE);
                    mCanSeekBack = !(Boolean) hasMethod.invoke(data, 30) //data.has(Metadata.SEEK_BACKWARD_AVAILABLE)
                            || (Boolean) getBooleanMethod.invoke(data, 30); //data.getBoolean(Metadata.SEEK_BACKWARD_AVAILABLE);
                    mCanSeekForward = !(Boolean) hasMethod.invoke(data, 31)//data.has(Metadata.SEEK_FORWARD_AVAILABLE)
                            || (Boolean) getBooleanMethod.invoke(data, 31);//data.getBoolean(Metadata.SEEK_FORWARD_AVAILABLE);
                }
                else {
                    mCanPause = mCanSeekBack = mCanSeekForward = true;
                }
                
            } 
            catch (final IllegalArgumentException e) {
                logger.error("<VideoView><8>, ERROR:", e);
            }
            catch (final IllegalAccessException e) {
                logger.error("<VideoView><9>, ERROR:", e);
            }
            catch (final InvocationTargetException e) {
                logger.error("<VideoView><10>, ERROR:", e);
            } 
            catch (final SecurityException e) {
                logger.error("<VideoView><11>, ERROR:", e);
            }
            catch (final NoSuchMethodException e) {
                logger.error("<VideoView><12>, ERROR:", e);
            }
            finally {
                
            }

            if (mOnPreparedListener != null) {
                mOnPreparedListener.onPrepared(mMediaPlayer);
            }
            if (mMediaController != null) {
                mMediaController.setEnabled(true);
            }
            mVideoWidth = mp.getVideoWidth();
            mVideoHeight = mp.getVideoHeight();

            final int seekToPosition = mSeekWhenPrepared;  // mSeekWhenPrepared may be changed after seekTo() call
            if (seekToPosition != 0) {
                seekTo(seekToPosition);
            }
            if (mVideoWidth != 0 && mVideoHeight != 0) {
                //Log.i("@@@@", "video size: " + mVideoWidth +"/"+ mVideoHeight);
                getHolder().setFixedSize(mVideoWidth, mVideoHeight);
                if (mSurfaceWidth == mVideoWidth && mSurfaceHeight == mVideoHeight) {
                    // We didn't actually change the size (it was already at the size
                    // we need), so we won't get a "surface changed" callback, so
                    // start the video here instead of in the callback.
                    if (mTargetState == STATE_PLAYING) {
                        start();
                        if (mMediaController != null) {
                            mMediaController.show();
                        }
                    } else if (!isPlaying() &&
                               (seekToPosition != 0 || getCurrentPosition() > 0)) {
                       if (mMediaController != null) {
                           // Show the media controls when we're paused into a video and make 'em stick.
                           mMediaController.show(0);
                       }
                   }
                }
            } else {
                // We don't know the video size yet, but should start anyway.
                // The video size might be reported to us later.
                if (mTargetState == STATE_PLAYING) {
                    start();
                }
            }
        }
    };

    private final MediaPlayer.OnCompletionListener mCompletionListener =
        new MediaPlayer.OnCompletionListener() {
        @Override
		public void onCompletion(final MediaPlayer mp) {
            mCurrentState = STATE_PLAYBACK_COMPLETED;
            mTargetState = STATE_PLAYBACK_COMPLETED;
            if (mMediaController != null) {
                mMediaController.hide();
            }
            if (mOnCompletionListener != null) {
                mOnCompletionListener.onCompletion(mMediaPlayer);
            }
        }
    };

    private final MediaPlayer.OnErrorListener mErrorListener =
        new MediaPlayer.OnErrorListener() {
        @Override
		public boolean onError(final MediaPlayer mp, final int framework_err, final int impl_err) {
            Log.d(TAG, "Error: " + framework_err + "," + impl_err);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            if (mMediaController != null) {
                mMediaController.hide();
            }

            /* If an error handler has been supplied, use it and finish. */
            if (mOnErrorListener != null) {
                if (mOnErrorListener.onError(mMediaPlayer, framework_err, impl_err)) {
                    return true;
                }
            }

            /* Otherwise, pop up an error dialog so the user knows that
             * something bad has happened. Only try and pop up the dialog
             * if we're attached to a window. When we're going away and no
             * longer have a window, don't bother showing the user an error.
             */
            if (getWindowToken() != null) {
                int messageId;
                
                if (framework_err == MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK) {
                    messageId = R.string.VideoView_error_text_invalid_progressive_playback;
                } else {
                    messageId = R.string.VideoView_error_text_unknown;
                }
                
                logger.error("ERROR:" + framework_err);

//                new AlertDialog.Builder(context)
//                        .setTitle(R.string.VideoView_error_title)
//                        .setMessage(messageId)
//                        .setPositiveButton(R.string.VideoView_error_button,
//                                new DialogInterface.OnClickListener() {
//                                    public void onClick(DialogInterface dialog, int whichButton) {
//                                        /* If we get here, there is no onError listener, so
//                                         * at least inform them that the video is over.
//                                         */
//                                        if (mOnCompletionListener != null) {
//                                            mOnCompletionListener.onCompletion(mMediaPlayer);
//                                        }
//                                    }
//                                })
//                        .setCancelable(false)
//                        .show();
            }
            return true;
        }
    };

    private final MediaPlayer.OnBufferingUpdateListener mBufferingUpdateListener =
        new MediaPlayer.OnBufferingUpdateListener() {
        @Override
		public void onBufferingUpdate(final MediaPlayer mp, final int percent) {
            mCurrentBufferPercentage = percent;
        }
    };

    /**
     * Register a callback to be invoked when the media file
     * is loaded and ready to go.
     *
     * @param l The callback that will be run
     */
    public void setOnPreparedListener(final MediaPlayer.OnPreparedListener l)
    {
        mOnPreparedListener = l;
    }

    /**
     * Register a callback to be invoked when the end of a media file
     * has been reached during playback.
     *
     * @param l The callback that will be run
     */
    public void setOnCompletionListener(final OnCompletionListener l)
    {
        mOnCompletionListener = l;
    }

    /**
     * Register a callback to be invoked when an error occurs
     * during playback or setup.  If no listener is specified,
     * or if the listener returned false, VideoView will inform
     * the user of any errors.
     *
     * @param l The callback that will be run
     */
    public void setOnErrorListener(final OnErrorListener l)
    {
        mOnErrorListener = l;
    }
   

    SurfaceHolder.Callback mSHCallback = new SurfaceHolder.Callback()
    {
        @Override
		public void surfaceChanged(final SurfaceHolder holder, final int format,
                                    final int w, final int h)
        {
           
            mSurfaceWidth = w;
            mSurfaceHeight = h;
            final boolean isValidState =  (mTargetState == STATE_PLAYING);
            final boolean hasValidSize = (mVideoWidth == w && mVideoHeight == h);
            if (mMediaPlayer != null && isValidState && hasValidSize) {
                if (mSeekWhenPrepared != 0) {
                    seekTo(mSeekWhenPrepared);
                }
                start();
            }
        }

        @Override
		public void surfaceCreated(final SurfaceHolder holder)
        {
            mSurfaceHolder = holder;
            openVideo();
        }

        @Override
		public void surfaceDestroyed(final SurfaceHolder holder)
        {
            // after we return from this we can't use the surface any more
            mSurfaceHolder = null;
            if (mMediaController != null) mMediaController.hide();
            release(true);
        }
    };

    /*
     * release the media player in any state
     */
    private void release(final boolean cleartargetstate) {
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mCurrentState = STATE_IDLE;
            if (cleartargetstate) {
                mTargetState  = STATE_IDLE;
            }
        }
    }

    @Override
    public boolean onTouchEvent(final MotionEvent ev) {
        if (isInPlaybackState() && mMediaController != null) {
            toggleMediaControlsVisiblity();
        }
        return false;
    }

    @Override
    public boolean onTrackballEvent(final MotionEvent ev) {
        if (isInPlaybackState() && mMediaController != null) {
            toggleMediaControlsVisiblity();
        }
        return false;
    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event)
    {
        final boolean isKeyCodeSupported = keyCode != KeyEvent.KEYCODE_BACK &&
                                     keyCode != KeyEvent.KEYCODE_VOLUME_UP &&
                                     keyCode != KeyEvent.KEYCODE_VOLUME_DOWN &&
                                     keyCode != KeyEvent.KEYCODE_VOLUME_MUTE &&
                                     keyCode != KeyEvent.KEYCODE_MENU &&
                                     keyCode != KeyEvent.KEYCODE_CALL &&
                                     keyCode != KeyEvent.KEYCODE_ENDCALL;
        if (isInPlaybackState() && isKeyCodeSupported && mMediaController != null) {
            if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK ||
                    keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                if (mMediaPlayer.isPlaying()) {
                    pause();
                    mMediaController.show();
                } else {
                    start();
                    mMediaController.hide();
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
                if (!mMediaPlayer.isPlaying()) {
                    start();
                    mMediaController.hide();
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP
                    || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
                if (mMediaPlayer.isPlaying()) {
                	pause();
                    mMediaController.show();
                }
                return true;
            } else {
                toggleMediaControlsVisiblity();
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    private void toggleMediaControlsVisiblity() {
        if (mMediaController.isShowing()) {
            mMediaController.hide();
        } else {
            mMediaController.show();
        }
    }

    @Override
	public void start() {
        if (isInPlaybackState()) {
            mMediaPlayer.start();
            mCurrentState = STATE_PLAYING;
        }
        mTargetState = STATE_PLAYING;
    }

    @Override
	public void pause() {
        if (isInPlaybackState()) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                mCurrentState = STATE_PAUSED;
            }
        }
        mTargetState = STATE_PAUSED;
    }

    public void suspend() {
        release(false);
    }

    public void resume() {
    	openVideo();
   }

   // cache duration as mDuration for faster access
    @Override
	public int getDuration() {
        if (isInPlaybackState()) {
            if (mDuration > 0) {
                return mDuration;
            }
            mDuration = mMediaPlayer.getDuration();
            return mDuration;
        }
        mDuration = -1;
        return mDuration;
    }

    @Override
	public int getCurrentPosition() {
        if (isInPlaybackState()) {
            return mMediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    @Override
	public void seekTo(final int msec) {
        if (isInPlaybackState()) {
            mMediaPlayer.seekTo(msec);
            mSeekWhenPrepared = 0;
        } else {
            
            if(msec == 0) {
                mSeekWhenPrepared = 1;
            }
            else {
                mSeekWhenPrepared = msec;
            }
            
        }
    }

    @Override
	public boolean isPlaying() {
        
    	boolean pb = isInPlaybackState();
    	boolean mpip;
    	
    	if(mMediaPlayer != null) {
    	    mpip = mMediaPlayer.isPlaying();
    	}
    	else {
    	    mpip = false;
            Log.d("AdsSDK", "1 backcheck mMediaPlayer = " + mMediaPlayer + ", isInPlaybackState = " + pb);
            if(Build.VERSION.SDK_INT == Build.VERSION_CODES.GINGERBREAD || Build.VERSION.SDK_INT == Build.VERSION_CODES.GINGERBREAD_MR1) {
                mpip = true;
                pb = true;
            }
    	}
    	
    	return pb && mpip;
//        return isInPlaybackState() && mMediaPlayer.isPlaying();
    }

    @Override
	public int getBufferPercentage() {
        if (mMediaPlayer != null) {
            return mCurrentBufferPercentage;
        }
        return 0;
    }

    private boolean isInPlaybackState() {
        return (mMediaPlayer != null &&
                mCurrentState != STATE_ERROR &&
                mCurrentState != STATE_IDLE &&
                mCurrentState != STATE_PREPARING);
    }

    @Override
	public boolean canPause() {
        return mCanPause;
    }

    @Override
	public boolean canSeekBackward() {
        return mCanSeekBack;
    }

    @Override
	public boolean canSeekForward() {
        return mCanSeekForward;
    }
    
    private int id = 0;
    
    @Override
    public int getAudioSessionId() {
        return ++id;
    }
}