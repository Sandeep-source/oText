package com.sk.mymassenger;

import androidx.appcompat.app.AppCompatActivity;

import android.database.Cursor;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.sk.mymassenger.dialog.SearchUserDialog;
import com.sk.mymassenger.dialog.SetUpProfileDialog;
import com.sk.mymassenger.dialog.StatusUploadDialog;

public class MainActivity2 extends AppCompatActivity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

    }

    public void perform(View view) {


    /* //from user animation check
       ImageView imageView=findViewById(R.id.dot_up);
        AnimatedVectorDrawable drawable= (AnimatedVectorDrawable) imageView.getBackground();
        drawable.start();*/
        StatusUploadDialog dialog=new StatusUploadDialog(this);
        dialog.show();
        dialog.setCancelable(true);
    }

    public void show(View view) {
        SearchUserDialog searchUserDialog=new SearchUserDialog(this);
        searchUserDialog.create();
        searchUserDialog.setCancelable(true);
        searchUserDialog.show();
        searchUserDialog.start();
        Handler handler=new Handler(getMainLooper());
        handler.postDelayed(()->{
            searchUserDialog.setFetching();
            handler.postDelayed(()->{
                searchUserDialog.setSuccess();

            },4000);
        },4000);
    }

    public void showD(View view) {
        SetUpProfileDialog dialog=new SetUpProfileDialog(this);
        dialog.create();
        dialog.setCancelable(true);
        dialog.show();
    }
}