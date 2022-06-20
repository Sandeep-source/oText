package com.sk.mymassenger.chat.senders;

import android.content.Context;
import android.net.Uri;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class PrepareFileToSend extends Thread {
    private final String userid;
    private final String recieverid;
    private final Uri msgfile;
    private final String time;
    private final String msgid;
    private final String filename;
    private final Context context;

    public PrepareFileToSend(String userid, String recieverid, Uri msgfile, String time, String msgid, String filename, Context context) {
        this.userid = userid;
        this.recieverid = recieverid;
        this.msgfile = msgfile;
        this.time = time;
        this.msgid = msgid;
        this.filename = filename;
        this.context = context;
    }

    @Override
    public void run() {

        File file=new File( context.getFilesDir(),"sent/file/"+filename );
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }




            try {
                InputStream inputStream = context.getContentResolver().openInputStream( msgfile );
                byte[] size = new byte[0];
                if (inputStream != null) {
                    size = new byte[inputStream.available()];
                }
                FileOutputStream out = new FileOutputStream( file );
                int n;
                n=inputStream.read( size );
                System.out.println( "Read : "+n );
                out.write( size );
                inputStream.close();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }


        String serverpath="files/"+filename;
        Constraints constraints=new Constraints.Builder().setRequiredNetworkType( NetworkType.CONNECTED ).build();
        OneTimeWorkRequest request=new OneTimeWorkRequest.Builder( FilesSender.class ).setConstraints( constraints )
                .setInputData( new Data.Builder().putString( "userid",userid ).putString( "recieverid",recieverid ).putString( "msgid",msgid )
                        .putString( "serverpath",serverpath ).putString( "servervalue",filename).putString( "file",file.getAbsolutePath() )
                        .putString( "fileBlur","").putString( "type","file" )
                        .putString( "time",time ).build()).build();
        WorkManager.getInstance(context  ).enqueue( request );
    }
}
