package com.sk.mymassenger.customui;

import android.app.usage.UsageEvents;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.os.BuildCompat;
import androidx.core.view.inputmethod.EditorInfoCompat;
import androidx.core.view.inputmethod.InputConnectionCompat;
import androidx.core.view.inputmethod.InputContentInfoCompat;

import com.sk.mymassenger.R;

public   class MEditText extends androidx.appcompat.widget.AppCompatEditText {

    public MEditText(@NonNull Context context) {
        super( context ,null,android.R.attr.editTextStyle);
        int density=getResources().getDisplayMetrics().densityDpi;
        int one=density/160;
        setMinHeight( 40*one );
        setMaxHeight( 200*one );
        setHint( "Type your Message..." );
        setHintTextColor( getResources().getColor (R.color.lightText) );
        setBackgroundColor( getResources().getColor ( R.color.light ));
        setTextColor(  getResources().getColor (R.color.textColor ));
        setPadding( 5*one,5*one,50*one,5*one );

        CoordinatorLayout.LayoutParams params= new CoordinatorLayout.LayoutParams(CoordinatorLayout.LayoutParams.MATCH_PARENT,
                CoordinatorLayout.LayoutParams.WRAP_CONTENT);
        params.gravity= Gravity.BOTTOM;
        setLayoutParams( params );
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setTranslationZ( 15*one );
        }


    }

    public MEditText(@NonNull Context context, @Nullable AttributeSet attrs) {
        super( context, attrs ,android.R.attr.editTextStyle);
    }

    public MEditText(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super( context, attrs, defStyleAttr );
    }
    private OnGifAttach onGifAttach;
    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        final InputConnection inputConnection= super.onCreateInputConnection( outAttrs );
        EditorInfoCompat.setContentMimeTypes( outAttrs,new String[]{"image/gif"} );
        InputConnectionCompat.OnCommitContentListener call=new InputConnectionCompat.OnCommitContentListener() {
            @Override
            public boolean onCommitContent(InputContentInfoCompat inputContentInfo, int flags, Bundle opts) {
                if(BuildCompat.isAtLeastNMR1()&&(flags&InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION)!=0){
                    try {
                        inputContentInfo.requestPermission();
                        Uri uri=inputContentInfo.getContentUri();
                        if(uri!=null){
                            if(onGifAttach!=null){
                                onGifAttach.send(uri);
                            }
                        }
                    }catch (Exception ex){
                        return false;
                    }
                }
                return  true;
            }
        };
        return InputConnectionCompat.createWrapper( inputConnection,outAttrs,call );
    }
    public  void setOnGifAttach(OnGifAttach attach){
        onGifAttach=attach;
    }
    public interface OnGifAttach{
        public void send(Uri uri);
    }

}