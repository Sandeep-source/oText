package com.sk.mymassenger;

import android.content.Context;
import android.net.Uri;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class PrepareFileToSend extends Thread {
    private String userid;
    private String recieverid;
    private Uri msgfile;
    private oTextDb finalDb;;
    private String time;
    private String msgid;
    private String filename;
    private Context context;

    public PrepareFileToSend(String userid, String recieverid, Uri msgfile, String time, String msgid, String filename, Context context) {
        this.userid = userid;
        this.recieverid = recieverid;
        this.msgfile = msgfile;
        this.finalDb = finalDb;
        this.time = time;
        this.msgid = msgid;
        this.filename = filename;
        this.context = context;
    }

    @Override
    public void run() {

        File file=new File( context.getFilesDir(),"sent/file/"+filename );
      //  File inputFile=new File( msgfile );
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
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }



        String serverpath="files/"+filename;
        Constraints constraints=new Constraints.Builder().setRequiredNetworkType( NetworkType.CONNECTED ).build();
        OneTimeWorkRequest request=new OneTimeWorkRequest.Builder( FilesSender.class ).setConstraints( constraints )
                .setInputData( new Data.Builder().putString( "userid",userid ).putString( "recieverid",recieverid ).putString( "msgid",msgid )
                        .putString( "serverpath",serverpath ).putString( "servervalue",filename+"(file)(/file)").putString( "file",file.getAbsolutePath() )
                        .putString( "fileBlur","").putString( "type","file" )
                        .putString( "time",time ).build()).build();
        WorkManager.getInstance(context  ).enqueue( request );
    }
}
