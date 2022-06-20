package com.sk.mymassenger;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.bumptech.glide.Glide;
import com.sk.mymassenger.db.message.Message;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

class ConversationActivityAdapter extends RecyclerView.Adapter<ConversationActivityAdapter.MViewHolder> {
    private boolean selectionMode;

    public void setMsgs(List<Message> msg) {

        Log.d( "TAG", "ConversationActivityAdapter set: "+msg );
        this.msgs = msg;
        count=msg.size();
    }
    public void add(Message data){
        msgs.add( data);
    }
    public void add(int position,Message data){
        msgs.set( position,data );
        count++;
    }
    public void remove(int pos){
        msgs.remove( pos );
        count=msgs.size();
    }

    public List<Message> getMsgs() {
        return msgs;

    }

    private List<Message> msgs;
    private  int count=0;
    private Context context;
    private  String userid;
    private ArrayList<Message> selectedList;
    private ArrayList<String> ids;
    private  View option;

    private String ouser;
    private int dpi;
    public ConversationActivityAdapter(List<Message> msg, Context context, String userid, View option) {
        msgs=msg;
        dpi=context.getResources().getDisplayMetrics().densityDpi;
        this.userid=userid;
        this.context=context;
        this.option=option;
        ids=new ArrayList<String>();
        Log.d( "TAG", "ConversationActivityAdapter: "+msgs );
        if(msgs!=null){
            Log.d( "TAG", "ConversationActivityAdapter size: "+msgs.size() );
            count=msg.size();
            if(count>0) {

                ouser = msg.get(0).getOUserId();
            }

            option.findViewById( R.id.opt_delete ).setOnClickListener( (vi)->{
                selectionMode=false;
                Log.d( "Adapter del ", "ConversationActivityAdapter: "+selectedList.size() );
                Intent intent=new Intent(context,DeleteMessage.class);
                intent.putStringArrayListExtra( "ids",ids );
                intent.putExtra( "mode", Database.Msg.TBL_MSG);
                intent.putExtra( Database.Msg.MUSER_ID,userid);
                intent.putExtra( Database.Msg.OUSER_ID,ouser);
                context.startService(intent);
                //new DelMsg(new Handler( context.getMainLooper() ),selectedList).start();
                ids.clear();

                option.setVisibility( View.GONE );
            } );

            option.findViewById( R.id.opt_forward ).setOnClickListener( (vi)->{
                Intent intent=new Intent( context,MessageForwarderActivity.class );
                intent.putExtra( "mode","forward" );
                intent.putStringArrayListExtra( "msgidlist",ids );
                context.startActivity(intent);

                option.setVisibility( View.GONE );
            } );
        }

    }


    @NonNull
    @Override
    public MViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        System.out.println( "onCreateViewHolder" );
        View view= LayoutInflater.from(context).inflate(R.layout.msg_box ,parent,false);

        return new MViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MViewHolder holder, int position) {
        Message msgData=msgs.get( position );
        if(selectionMode){

            Message temp=msgs.get( position ) ;
            if(selectedList.contains(temp )) {

                holder.getRoot().setBackgroundColor( Color.parseColor( "#34ae5aa4" ) );
             }else{
                holder.getRoot().setBackgroundColor( Color.parseColor( "#00000000" ) );

            }
        }
        TextView msg=holder.getMsg();
        System.out.println( "onBindViewHolder" );
        ImageView image=holder.getImage();
        msg.setText( msgData.getMessage() );
        System.out.println( "TYPE OF POSITION "+position+" : " +msgData.getType());
        String status=msgData.getStatus();
        View root =holder.getRoot();
       if(msgData.getMediaType().equals( Database.Msg.MEDIA_IMG )){

            holder.getFileMsg().setVisibility( View.GONE );
            String path=msgData.getMessage();
            System.out.println( "image path"+Uri.parse( path   ).toString() );

            image.setVisibility( View.VISIBLE );
            msg.setVisibility( View.GONE );


            if(msgData.getType().equals(Database.Msg.TYPE_SENT)){
                image.setBackground( ContextCompat.getDrawable(context,R.drawable.msg_back_light) );
                CoordinatorLayout.LayoutParams Ilp=new CoordinatorLayout.LayoutParams( image.getLayoutParams() );
                Ilp.bottomMargin=15;

                Ilp.gravity= Gravity.END;
                image.setLayoutParams( Ilp );
                holder.getStatus().setVisibility( View.VISIBLE );
                switch (status) {
                    case Database.Msg.STATUS_NOT_SENT:
                        holder.getStatus().setBackground( ContextCompat.getDrawable( context, R.drawable.ic_baseline_access_time_24 ) );
                        break;
                    case Database.Msg.STATUS_SENT:
                        holder.getStatus().setBackground( ContextCompat.getDrawable( context, R.drawable.ic_baseline_check_24 ) );

                        break;
                    case Database.Msg.STATUS_NOT_SEEN:
                        holder.getStatus().setBackground( ContextCompat.getDrawable( context, R.drawable.not_seen ) );
                        break;
                    default:
                        holder.getStatus().setBackground( ContextCompat.getDrawable( context, R.drawable.ic_baseline_remove_red_eye_24 ) );

                        break;
                }
                View dbtn=holder.getDownload();
                ((View)dbtn.getParent()).setVisibility( View.GONE );
                if(path!=null) {
                    File imageFile = new File( context.getFilesDir(),path.substring(path.lastIndexOf( "sent" )  ));
                    System.out.println( "Path in msg  :" + path +"\n"+path.substring(path.lastIndexOf( "sent" )  ));
                    Uri imguri = FileProvider.getUriForFile( context, "com.sk.mymassenger.fileProvider", imageFile );
                    if(path.endsWith( "gif" ))
                        Glide.with( context ).asGif().load( imguri ).fitCenter().into( image );
                    else
                        Glide.with( context ).load( imguri ).fitCenter().into( image );
                }
                else
                    image.setImageResource(R.drawable.ic_baseline_image_24);
                image.setOnClickListener( view -> {
                    Intent intent=new Intent( Intent.ACTION_VIEW);
                    File imageFile= new File( context.getFilesDir(),path.substring(path.lastIndexOf( "sent" )  ));
                    System.out.println("Path in msg  :"+path);
                    Uri imguri=FileProvider.getUriForFile( context,"com.sk.mymassenger.fileProvider",imageFile);
                    System.out.println("file Path :"+imageFile.getAbsolutePath());
                    System.out.println("Uri Path :"+imguri.toString());
                    intent.setDataAndType(imguri ,"image/"+path.substring( path.lastIndexOf( "." )+1 ));
                    intent.addFlags( Intent.FLAG_GRANT_READ_URI_PERMISSION |Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                    context.startActivity( Intent.createChooser( intent,"View Image" ) );
                } );
            }else{
                View dbtn=holder.getDownload();
                if(msgData.getMediaStatus().equals( Database.Msg.MEDIA_STATUS_NOT_DOWNLOADED )){

                    ((View)dbtn.getParent()).setVisibility( View.VISIBLE );
                    if(path!=null) {
                        File imageFile = new File( context.getFilesDir(), "imagesThumb/" + path.substring( path.lastIndexOf( "/" ) + 1 ) );
                        System.out.println( "Path in msg  :" + path );
                        Uri imguri = FileProvider.getUriForFile( context, "com.sk.mymassenger.fileProvider", imageFile );
                        if(path.endsWith( "gif" ))
                            Glide.with( context ).asGif().load( imguri ).into( image );
                        else
                            Glide.with( context ).load( imguri ).into( image );
                    }
                    else
                        image.setImageResource(R.drawable.ic_baseline_image_24);
                    dbtn.setOnClickListener( (v)->{
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            v.setForegroundTintList( ColorStateList.valueOf( Color.parseColor( "#9d4994" )) );
                        }else{
                            v.setBackgroundColor( Color.parseColor( "#9d4994" ) );
                        }
                        File imagedir= null;
                        String downloadpath=null;
                        System.out.println( "download path "+path );
                        if(path.endsWith( "gif" )) {
                            imagedir = new File( context.getFilesDir(), "receive/gifs/" );
                        }
                        else  imagedir=new File( context.getFilesDir(), "receive/images/");
                        if(!imagedir.exists()){
                            imagedir.mkdirs();
                        }


                        Constraints constraints=new Constraints.Builder().setRequiredNetworkType( NetworkType.CONNECTED ).build();
                        WorkRequest workRequest=new OneTimeWorkRequest.Builder( FetchFiles.class ).setInputData(
                                new Data.Builder()
                                        .putString( "file",new File(imagedir.getAbsolutePath(),path).getAbsolutePath())
                                        .putString("serverPath","images/"
                                        + msgs.get( position ).getMessage())
                                        .putInt( "index",position ).putString( "msgid",msgs.get( position ).getMsgId() ).putString( "muser",userid )
                                        .putString( "ouser",ouser ).build()
                        ).setConstraints( constraints ).build();
                        WorkManager.getInstance( context ).enqueue( workRequest );
                    } );

                }else{

                    ((View)dbtn.getParent()).setVisibility( View.GONE );
                    if(path!=null) {
                        File imageFile = new File( context.getFilesDir(),  path.substring( path.lastIndexOf( "receive" )  ) );
                        System.out.println( "Path in msg  :" + path );
                        Uri imguri = FileProvider.getUriForFile( context, "com.sk.mymassenger.fileProvider", imageFile );
                        if(path.endsWith( "gif" ))
                            Glide.with( context ).asGif().load( imguri ).fitCenter().into( image );
                        else
                            Glide.with( context ).load( imguri ).fitCenter().into( image );
                    }
                    else
                        image.setImageResource(R.drawable.ic_baseline_image_24);
                    image.setOnClickListener( view -> {
                        Intent intent=new Intent( Intent.ACTION_VIEW);
                        File imageFile= new File( context.getFilesDir(),path.substring( path.lastIndexOf( "receive") ) );
                        System.out.println("Path in msg  :"+path);
                        Uri imguri=FileProvider.getUriForFile( context,"com.sk.mymassenger.fileProvider",imageFile);
                        System.out.println("file Path :"+imageFile.getAbsolutePath());
                        System.out.println("Uri Path :"+imguri.toString());
                        System.out.println("Uri Path extention :"+path.substring( path.lastIndexOf( "." )+1 ));
                        intent.setDataAndType(imguri ,"image/"+path.substring( path.lastIndexOf( "." ) ) );
                        intent.addFlags( Intent.FLAG_GRANT_READ_URI_PERMISSION |Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                        context.startActivity( Intent.createChooser( intent,"View Image" ) );
                    } );
                }
                image.setBackground(  ContextCompat.getDrawable(context,R.drawable.msg_back_dark));
                CoordinatorLayout.LayoutParams Ilp=new CoordinatorLayout.LayoutParams( image.getLayoutParams() );
                Ilp.bottomMargin=3;

                Ilp.gravity= Gravity.START;
                image.setLayoutParams( Ilp );

                holder.getStatus().setVisibility( View.GONE );
            }
        }else if(msgData.getMediaType().equals( Database.Msg.MEDIA_FILE )){
            LinearLayout filemsg= holder.getFileMsg();
            msg.setVisibility( View.GONE );
            String path=msgData.getMessage();
           image.setVisibility( View.GONE );
           image.setImageURI( null );
            msg.setVisibility( View.GONE );
            filemsg.setVisibility( View.VISIBLE );
            holder.getFilename().setText(  new File(path).getName());


           if(msgData.getType().equals(Database.Msg.TYPE_SENT)){
               holder.getFilename().setTextColor( Color.parseColor( "#000000" ) );

               if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                   holder.getRoot().findViewById( R.id.file_symb ).setBackgroundTintList( ColorStateList.valueOf( Color.parseColor( "#000000" ))  );
               }
                View dbtn=holder.getDownload();
                dbtn.setVisibility( View.GONE );
                filemsg.setBackground( ContextCompat.getDrawable(context,R.drawable.msg_back_light) );
                CoordinatorLayout.LayoutParams Ilp=new CoordinatorLayout.LayoutParams( image.getLayoutParams() );
                Ilp.bottomMargin=15;

                Ilp.gravity= Gravity.END;
                filemsg.setLayoutParams( Ilp );
                holder.getStatus().setVisibility( View.VISIBLE );
                switch (status) {
                    case Database.Msg.STATUS_NOT_SENT:
                        holder.getStatus().setBackground( ContextCompat.getDrawable( context, R.drawable.ic_baseline_access_time_24 ) );
                        break;
                    case Database.Msg.STATUS_SENT:
                        holder.getStatus().setBackground( ContextCompat.getDrawable( context, R.drawable.ic_baseline_check_24 ) );

                        break;
                    case Database.Msg.STATUS_NOT_SEEN:
                        holder.getStatus().setBackground( ContextCompat.getDrawable( context, R.drawable.not_seen ) );
                        break;
                    default:
                        holder.getStatus().setBackground( ContextCompat.getDrawable( context, R.drawable.ic_baseline_remove_red_eye_24 ) );

                        break;
                }
                dbtn.setVisibility( View.GONE );

                filemsg.setOnClickListener( view -> {

                    File imageFile= new File( context.getFilesDir(),"receive/file/"+path.substring( path.lastIndexOf( "/")+1 ) );
                    System.out.println("Path in msg  :"+path);
                    Uri imguri=FileProvider.getUriForFile( context,"com.sk.mymassenger.fileProvider",imageFile);
                    System.out.println("file Path :"+imageFile.getAbsolutePath());
                    System.out.println("Uri Path :"+imguri.toString());
                    String type=context.getContentResolver().getType( imguri );
                    System.out.println("Uri type :"+type);
                    Intent intent=new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType( imguri,type );
                    intent.addFlags( Intent.FLAG_GRANT_READ_URI_PERMISSION |Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    context.startActivity( Intent.createChooser( intent,"View Image" ) );
                } );
            }else{
               holder.getFilename().setTextColor( Color.parseColor( "#ffffff" ) );

               if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                   holder.getRoot().findViewById( R.id.file_symb ).setBackgroundTintList( ColorStateList.valueOf( Color.parseColor( "#ffffff" ))  );
               }
                View dbtn=holder.getDownload();
                if(msgData.getMediaStatus().equals( Database.Msg.MEDIA_STATUS_NOT_DOWNLOADED )){

                    ((View)dbtn.getParent()).setVisibility( View.VISIBLE );

                    dbtn.setOnClickListener( (v)->{
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            v.setForegroundTintList( ColorStateList.valueOf( Color.parseColor( "#9d4994" )));
                        }
                        File imagedir= new File( context.getFilesDir(), "receive/file/");
                        if(!imagedir.exists()){
                            imagedir.mkdirs();
                        }

                        Constraints constraints=new Constraints.Builder().setRequiredNetworkType( NetworkType.CONNECTED ).build();
                        WorkRequest workRequest=new OneTimeWorkRequest.Builder( FetchFiles.class ).setInputData(
                                new Data.Builder().putString( "file",new File( imagedir.getAbsolutePath(), path ).getAbsolutePath()).putString("serverPath","files/"
                                        + path).putString( "msgid",ids.get( position ) ).putString( "muser",userid )
                                        .putString( "ouser",ouser ).build()
                        ).setConstraints( constraints ).build();
                        WorkManager.getInstance( context ).enqueue( workRequest );
                    } );
                }else {
                    ((View)dbtn.getParent()).setVisibility( View.GONE );

                    filemsg.setOnClickListener( view -> {

                        File imageFile= new File( context.getFilesDir(),"receive/file/"+path.substring( path.lastIndexOf( "/")+1 ) );
                        System.out.println("Path in msg  :"+path);
                        Uri imguri=FileProvider.getUriForFile( context,"com.sk.mymassenger.fileProvider",imageFile);
                        System.out.println("file Path :"+imageFile.getAbsolutePath());
                        System.out.println("Uri Path :"+imguri.toString());
                        String type=context.getContentResolver().getType( imguri );
                        System.out.println("Uri type :"+type);
                        Intent intent=new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType( imguri,type );
                        intent.addFlags( Intent.FLAG_GRANT_READ_URI_PERMISSION |Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        context.startActivity( Intent.createChooser( intent,"View Image" ) );
                    } );
                }
                filemsg.setBackground(  ContextCompat.getDrawable(context,R.drawable.msg_back_dark));
                CoordinatorLayout.LayoutParams Ilp=new CoordinatorLayout.LayoutParams( image.getLayoutParams() );
                Ilp.bottomMargin=3;

                Ilp.gravity= Gravity.START;
                filemsg.setLayoutParams( Ilp );

                holder.getStatus().setVisibility( View.GONE );
            }
        }else{
           ((View)holder.getDownload().getParent()).setVisibility( View.GONE );
            holder.getFileMsg().setVisibility( View.GONE );
            image.setImageBitmap( null );
            image.setVisibility( View.GONE );
            msg.setVisibility( View.VISIBLE );
            if(msgData.getType().equals(Database.Msg.TYPE_SENT)){
                msg.setTextColor( ColorStateList.valueOf( Color.parseColor("#000000") ) );
                msg.setBackground( ContextCompat.getDrawable(context,R.drawable.msg_back_light) );

                CoordinatorLayout.LayoutParams lp=new CoordinatorLayout.LayoutParams( msg.getLayoutParams() );
                lp.bottomMargin=15;
                lp.gravity= Gravity.END;
                msg.setLayoutParams( lp );
                holder.getStatus().setVisibility( View.VISIBLE );
                switch (status) {
                    case Database.Msg.STATUS_NOT_SENT:
                        holder.getStatus().setBackground( ContextCompat.getDrawable( context, R.drawable.ic_baseline_access_time_24 ) );
                        break;
                    case Database.Msg.STATUS_SENT:
                        holder.getStatus().setBackground( ContextCompat.getDrawable( context, R.drawable.ic_baseline_check_24 ) );

                        break;
                    case Database.Msg.STATUS_NOT_SEEN:
                        holder.getStatus().setBackground( ContextCompat.getDrawable( context, R.drawable.not_seen ) );
                        break;
                    default:
                        holder.getStatus().setBackground( ContextCompat.getDrawable( context, R.drawable.ic_baseline_remove_red_eye_24 ) );

                        break;
                }


            }else{
            /*
            if(msgs.getString( 6 ).equals( Database.Msg.STATUS_NOT_SEEN )){
                ContentValues value=new ContentValues(  );
                value.put( Database.Msg.STATUS, Database.Msg.STATUS_SEEN );
                db.update( Database.Msg.TBL_MSG,value, Database.Msg.MSG_ID+" = ? AND "+ Database.Msg.MUSER_ID+" = ?",
                        new String[]{msgs.getString( 0 ),userid});

                Constraints constraints=new Constraints.Builder().setRequiredNetworkType( NetworkType.CONNECTED ).build();
                WorkRequest work= new OneTimeWorkRequest.Builder(MsgState.class).setConstraints( constraints )
                        .addTag("MessageServiceO")
                        .setInputData(
                                new Data.Builder().putString("UserId",msgs.getString( 3 )).putString("Myid",msgs.getString( 2 ))
                                        .putString( "time",new Timestamp( new Date().getTime() ).toString() )
                                        .putBoolean( "get",true ).build()
                        ).build();
                WorkManager.getInstance(context ).enqueue( work);

            }*/
                msg.setTextColor( ColorStateList.valueOf( Color.parseColor("#ffffff") ) );
                msg.setBackground(  ContextCompat.getDrawable(context,R.drawable.msg_back_dark));

                CoordinatorLayout.LayoutParams lp=new CoordinatorLayout.LayoutParams( msg.getLayoutParams() );
                lp.bottomMargin=3;
                lp.gravity= Gravity.START;
                msg.setLayoutParams( lp );


                holder.getStatus().setVisibility( View.GONE );

            }
        }

        holder.getRoot().setOnLongClickListener( view -> {
            holder.getRoot().setBackgroundColor(Color.parseColor( "#34ae5aa4" )  );
            if(selectedList==null){
                selectedList= new ArrayList<>();

            }
            if(ids==null){
                ids=new ArrayList<>(  );
            }
            selectedList.add(msgs.get( position ));
            ids.add( msgs.get( position ).getMsgId() );
            selectionMode=true;
            option.setVisibility( View.VISIBLE );
            return true;
        } );
       if(selectedList!=null) {
           Message temp = msgs.get( position );
           if (selectedList.contains( temp )) {
               holder.getRoot().setBackgroundColor( Color.parseColor( "#34ae5aa4" ) );
           } else {
               holder.getRoot().setBackgroundColor( Color.parseColor( "#00000000" ) );
           }
       }
        holder.getRoot().setOnClickListener( view -> {
            if(selectionMode) {
               Message temp1=msgs.get( position ) ;
                if(selectedList.contains(temp1 )) {

                        holder.getRoot().setBackgroundColor( Color.parseColor( "#00000000" ) );
                        selectedList.remove( temp1 );
                        ids.remove( temp1.getMsgId() );

                }else{
                    holder.getRoot().setBackgroundColor( Color.parseColor( "#34ae5aa4" ) );
                    selectedList.add( temp1 );
                    ids.add( temp1.getMsgId() );
                }
                if(selectedList.size()<1){
                    selectionMode=false;
                    option.setVisibility( View.GONE );
                }

            }

        } );



    }

    @Override
    public int getItemCount() {
        return count;
    }

    public class MViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout fileMsg;

        public View getDownload() {
            return download;
        }

        private final View download;

        public LinearLayout getFileMsg() {
            return fileMsg;
        }

        public TextView getFilename() {
            return filename;
        }

        private final TextView filename;

        public View getRoot() {
            return root;
        }

        private final View root;

        public ImageView getImage() {
            return image;
        }

        private final ImageView image;

        public TextView getStatus() {
            return status;
        }

        private final TextView status;
        private TextView msg;
        public TextView getMsg() {
            return msg;
        }


        public MViewHolder(@NonNull View itemView) {
            super( itemView );
            msg=itemView.findViewById( R.id.msg );
            status=itemView.findViewById( R.id.sent_status);
            image=itemView.findViewById( R.id.imagemsg );
            root=itemView;
            fileMsg=itemView.findViewById( R.id.filemsg );
            filename=itemView.findViewById( R.id.filename );
            download=itemView.findViewById( R.id.download_btn );
        }
    }
    class DelMsg extends Thread{
        private static final String TAG = "DelMsg";
        private ArrayList<Message> ids;
        private Handler handler;

        public DelMsg(Handler handler, ArrayList<Message> ids){
            this.ids=ids;
            this.handler=handler;
        }

        @Override
        public void run() {

            for(Message msg: ids){
                int index=msgs.indexOf(msg);
                Log.d( TAG, "run: "+index );
                remove(index );

            }
            handler.post( ()->{
                notifyDataSetChanged();
            } );
            selectedList.clear();

        }
    }
}