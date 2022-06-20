package com.sk.mymassenger.chat.senders;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;


public class ImageSender extends Thread {

    private String userid;
    private String recieverid;
    private String msgimg;
   // private oTextDb finalDb;;
    private String time;
    private String msgid;
    private Context context;
    private String type;

    public ImageSender(String userid, String recieverid, String msgimg, String time, String msgid, Context context, String b) {
        this.userid = userid;
        this.recieverid = recieverid;
        this.msgimg = msgimg;

        this.time = time;
        this.msgid = msgid;
        this.context = context;
        type=b;
    }

    @Override
    public void run() {
        Uri image=Uri.parse( msgimg );
        Bitmap profile;
        Log.d( "TAG", "run: uri " +image);
        File file=null;
        if(type.equals( "png" ))
            file=new File( context.getFilesDir(),"sent/images/IMG"+msgid+".png" );
        else if(type.equals( "gif" ))
            file=new File( context.getFilesDir(),"sent/gifs/GIF"+msgid+".gif" );
        File dirblur=new File( context.getFilesDir(),"imagesThumb/");
        if(!dirblur.exists()){
            dirblur.mkdirs();
        }
        File fileBlur=new File( context.getFilesDir(),"imagesThumb/IMG"+msgid+".png"  );
        if(!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(!fileBlur.exists()){
            try {
                fileBlur.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Log.d( "TAG", "run: blur  " +fileBlur.getAbsolutePath());
        Log.d( "TAG", "run: file  " +file.getAbsolutePath());
        if (Build.VERSION.SDK_INT >= 29){
            // To handle deprication use
            try {
                ByteArrayOutputStream stream=new ByteArrayOutputStream(  );
                profile= ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.getContentResolver(),image ));
                if(type.equals( "png" )) {
                    int h = (int) (profile.getHeight() * (512.0 / profile.getWidth()));
                    profile = Bitmap.createScaledBitmap( profile, 512, h, true );
                    profile.compress( Bitmap.CompressFormat.PNG, 100, stream );
                    ByteArrayOutputStream streamBlur = new ByteArrayOutputStream();
                    int hb = (int) (profile.getHeight() * (8.0 / profile.getWidth()));
                    Bitmap profileimg = Bitmap.createScaledBitmap( profile, 8, hb, true );
                    profileimg.compress( Bitmap.CompressFormat.PNG, 100, streamBlur );
                    if(!fileBlur.exists()){
                        fileBlur.createNewFile();
                    }
                    FileOutputStream outblur=new FileOutputStream(  fileBlur );
                    outblur.write( streamBlur.toByteArray() );
                    Log.d( "TAG", "run: done"+streamBlur );



                    FileOutputStream out=new FileOutputStream(  file );
                    out.write( stream.toByteArray() );
                }else if(type.equals( "gif" )){
                    Files.copy(context.getContentResolver().openInputStream( image ),file.toPath());
                }




            } catch (IOException e) {
                e.printStackTrace();
            }
        } else{
            // Use older version
            try {

                ByteArrayOutputStream stream=new ByteArrayOutputStream(  );
                profile=MediaStore.Images.Media.getBitmap(context.getContentResolver(), image );
                if(type.equals( "png" )) {
                   // int h = (int) (profile.getHeight() * (512.0 / profile.getWidth()));
                   // profile = Bitmap.createScaledBitmap( profile, 512, h, true );
                    profile.compress( Bitmap.CompressFormat.PNG, 100, stream );
                    ByteArrayOutputStream streamBlur = new ByteArrayOutputStream();
                    int hb = (int) (profile.getHeight() * (8.0 / profile.getWidth()));
                    Bitmap profileimg = Bitmap.createScaledBitmap( profile, 8, hb, true );
                    profileimg.compress( Bitmap.CompressFormat.PNG, 100, streamBlur );
                    if(!fileBlur.exists()){
                        fileBlur.createNewFile();
                    }
                    FileOutputStream outblur=new FileOutputStream(  fileBlur );
                    outblur.write( streamBlur.toByteArray() );
                    Log.d( "TAG", "run: blurStream "+streamBlur );
                    if(!file.exists()){
                        try {
                            file.createNewFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }


                    FileOutputStream out=new FileOutputStream(  file );
                    out.write( stream.toByteArray() );
                }else if(type.equals( "gif" )){
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Files.copy(context.getContentResolver().openInputStream( image ),file.toPath());
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        String serverpath;
        String serverval;
      if(type.equals( "gif" )) {
          serverpath = "images/" + msgid + ".gif";
         serverval=msgid+".gif";
      }
      else {
          serverpath = "images/" + msgid + ".png";
          serverval=msgid+".png";
      }
        Constraints constraints=new Constraints.Builder().setRequiredNetworkType( NetworkType.CONNECTED ).build();
        OneTimeWorkRequest request=new OneTimeWorkRequest.Builder( FilesSender.class )
                .setConstraints( constraints )
                .setInputData( new Data.Builder().putString( "userid",userid )
                        .putString( "recieverid",recieverid )
                        .putString( "msgid",msgid )
                        .putString( "serverpath",serverpath )
                        .putString( "servervalue",serverval)
                        .putString( "file",file.getAbsolutePath() )
                        .putString( "fileBlur",fileBlur.getAbsolutePath() )
                        .putString( "type","img" )
                        .putString( "serverPathThumb","imagesThumb/"+msgid+".png" )
                        .putString( "time",time ).build())
                .build();
        WorkManager.getInstance(context  ).enqueue( request );




    }
}
