package com.sk.mymassenger;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.sk.mymassenger.chat.senders.ImageSender;
import com.sk.mymassenger.chat.senders.PrepareMessage;
import com.sk.mymassenger.databinding.ActivityMessageForwarderBinding;
import com.sk.mymassenger.db.Database;
import com.sk.mymassenger.db.OTextDb;
import com.sk.mymassenger.db.block.Block;
import com.sk.mymassenger.db.message.Message;
import com.sk.mymassenger.db.recent.Recent;
import com.sk.mymassenger.db.user.User;

import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class MessageForwarderActivity extends AppCompatActivity {
    private static final String TAG = "ForwarderActivity";
    private FirebaseUser user= FirebaseAuth.getInstance().getCurrentUser();
    private String userid;

    private int density;
    private ArrayList<String> userIds;
    private ArrayList<String> msgIds;
    private String mode;
    private String type;
    private ActivityMessageForwarderBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        binding=ActivityMessageForwarderBinding.inflate(getLayoutInflater());
         if(user!=null) {
             userid = user.getUid();

             setContentView(binding.getRoot());
             binding.forwardList.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
             Log.d(TAG, "onCreate: type : " + getIntent().getType());
             Log.d(TAG, "onCreate: data : " + getIntent().getData());
             Log.d(TAG, "onCreate: data in parcelable : " + getIntent().getParcelableExtra(Intent.EXTRA_STREAM));

             msgIds = getIntent().getStringArrayListExtra("msgIdList");
             List<User> users = OTextDb.getInstance(getApplicationContext()).userDao().getAllUser(Database.User.TYPE_FRIEND + "  OF " + userid);
             mode = getIntent().getStringExtra("mode");
             String sender = getIntent().getStringExtra("sender");
             binding.forwardList.setAdapter(new ForwardAdapter(users));
             type = getIntent().getType();
             binding.forwardBtn.setOnClickListener((v) -> {
                 if (!(mode != null && mode.equals("forward")))
                     new SendMessage(type, "forward").start();
                 else {
                     new MessageForwarder(userIds, msgIds, userid, MessageForwarderActivity.this);
                     finishAndStart();

                 }
             });
             density = getResources().getDisplayMetrics().densityDpi;
         }else{
             startActivity(new Intent(this,Splash.class));
             finish();
         }
    }
    public void animateView() {
            if (binding.forwardBtn.getVisibility() == View.GONE) {
                binding.forwardBtn.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),
                        R.anim.scale_up));
                binding.forwardBtn.setVisibility(View.VISIBLE);
            } else {
                binding.forwardBtn.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),
                        R.anim.scale_down));
                binding.forwardBtn.setVisibility(View.GONE);

            }

    }
    private void finishAndStart(){
        Intent intent=new Intent(getApplicationContext(),HomeActivity.class);
        intent.putExtra("userMode", Database.Recent.MODE_PUBLIC);
        startActivity(intent);
        finishActivity(RESULT_OK);
    }

    public void forward(View view) {
    }

    private  class ForwardAdapter extends RecyclerView.Adapter<MViewHolder>{

        private List<User> users;
        private int count=0;
        public ForwardAdapter(List<User> data) {
            this.users=data;
            if(data!=null){
                count=data.size();
                userIds=new ArrayList<>(  );
            }
        }

        @NonNull
        @Override
        public MViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view= LayoutInflater.from( getApplicationContext() ).inflate(R.layout.conversation_layout,parent,false);

            return new MViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MViewHolder holder, int position) {
             User data=users.get( position );


            String phone=data.getPhoneNo();
            if(phone!=null&&phone.length()>0)
            holder.getMobile().setText( phone);
            else
                holder.getMobile().setText( data.getEmail());
            holder.getName().setText( data.getUserName());
            String uri=data.getProfilePicture();
            if(uri!=null)
                holder.getPic().setImageURI( Uri.parse( uri) );
            else
                holder.getPic().setImageURI( null );
            String id=users.get( position ).getUserId();
            View v=holder.getRoot();
            if(userIds.contains( id )) {
                v.setBackgroundColor( Color.parseColor( "#34ae5aa4" ) );
           }else{
                v.setBackgroundColor( Color.parseColor( "#00000000" ) );
           }
           v.setOnClickListener((vi)->{
                String idl=users.get( position ).getUserId();
                if(!userIds.contains( idl )) {
                    v.setBackgroundColor( Color.parseColor( "#34ae5aa4" ) );
                    if(userIds.size()==0){
                        animateView();
                    }
                    userIds.add(idl);

                }else{
                    v.setBackgroundColor( Color.parseColor( "#00000000" ) );
                    userIds.remove(idl);
                    if(userIds.size()<1){
                        animateView();
                    }
                }

            });
        }

        @Override
        public int getItemCount() {
            return count;
        }
    }

    private class MViewHolder extends RecyclerView.ViewHolder {
        private final ImageView pic;
        private final TextView name;
        private final TextView mobile;

        public View getRoot() {
            return root;
        }

        private final View root;

        public MViewHolder(@NonNull View itemView) {
            super( itemView );
            pic=itemView.findViewById( R.id.conversation_pic );
            mobile=itemView.findViewById( R.id.last_msg);
            name=itemView.findViewById( R.id.conversation_name);
            root=itemView;


        }

        public ImageView getPic() {
            return pic;
        }

        public TextView getName() {
            return name;
        }

        public TextView getMobile() {
            return mobile;
        }
    }

    private class SendMessage extends Thread{
        private final String mode;
        private String type;
        public SendMessage(String type,String mode){
            this.type=type;
            this.mode=mode;

        }

        @Override
        public void run() {

                for(String id : userIds){

                    DocumentReference doc=FirebaseFirestore.getInstance().collection( "Messages/" )
                            .document( userid+"to"+id+"messageidscount" );
                    doc.get().addOnCompleteListener( task -> {
                        DocumentSnapshot documentSnapshot=task.getResult();
                        long msg_ids_count;
                        if(documentSnapshot!=null&&documentSnapshot.exists()) {
                            msg_ids_count = documentSnapshot.getLong( "count" );
                        }else{
                            msg_ids_count=0;
                        }
                        String msgid = userid + "to" + id + msg_ids_count;
                        HashMap<String, Long> newcount = new HashMap<>();
                        newcount.put( "count", msg_ids_count + 1 );
                        doc.set( newcount, SetOptions.merge() );
                        String newmsgid = userid + "to" + id + msg_ids_count;
                        if(type.contains("image/*")) {

                            sendImage("png", msg_ids_count, id);
                        } else if(type.equals("text/plain")){
                            User rec=OTextDb.getInstance(getApplicationContext()).userDao().getUser(id);
                            Message message=new Message();
                            message.setMessage(getIntent().getStringExtra(Intent.EXTRA_TEXT) );
                            message.setType( Database.Msg.TYPE_SENT );
                            message.setOUserId( rec.getUserId() );
                            message.setMUserId( userid );
                            message.setMsgId( msgid);
                            message.setStatus( Database.Msg.STATUS_NOT_SENT );
                            message.setMsgMode( rec.getUserMode() );
                            message.setMediaType( Database.Msg.MEDIA_TEXT );
                            Block block=OTextDb.getInstance(getApplicationContext()).blockDao().getBlock(userid,rec.getUserId());
                            new PrepareMessage(message,block!=null,getApplicationContext()).start();
                           finishAndStart();
                        }
                    } ).addOnFailureListener( e -> {
                        e.printStackTrace();
                        Snackbar.make(binding.getRoot(),"Something went wrong",Snackbar.LENGTH_SHORT).show();
                    } );
                }


        }

        private void sendImage(String type, long msg_ids_count, String recieverid) {
            String path="";
            if(type.contains("gif")){
                path="sent/gifs/";
            }else{
                path="sent/images/";
            }
            Uri data=getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
            File directory=new File( getApplicationContext().getFilesDir(),path );
            if(!directory.exists()){
                directory.mkdirs();
            }
            String newmsgid=userid+"to"+recieverid+msg_ids_count;
            String prefix=type.equals( "gif" )?"GIF":"IMG";
            File file=new File( getFilesDir(),path+prefix+newmsgid+"."+type );
            System.out.println("dir path :"+ getFilesDir().getAbsolutePath() );
            User user=OTextDb.getInstance(getApplicationContext()).userDao().getUser(recieverid);
            addMsgData( file,newmsgid, Database.Msg.MEDIA_IMG,"You sent a picture ",-1, user);
            new ImageSender( userid, recieverid, data.toString(),new Timestamp( new Date().getTime() ).toString(),
                    newmsgid, getApplicationContext(),type ).start();
            finishAndStart();





        }

        private void addMsgData(File file, String newmsgid, String mediatype, String name, int index,User user){
            Timestamp time = new Timestamp( new Date().getTime() ) ;
            Message message=new Message();
            message.setMessage(file.getAbsolutePath()  );
            message.setType( Database.Msg.TYPE_SENT );
            message.setOUserId( user.getUserId() );
            message.setMUserId( userid );
            message.setMsgId( newmsgid );
            message.setTime( time.getTime() );
            message.setStatus( Database.Msg.STATUS_NOT_SENT );
            message.setMsgMode( user.getUserMode() );
            message.setTransferIndex(index);
            message.setMediaType( mediatype );
            message.setMediaStatus(Database.Msg.MEDIA_STATUS_SENDING);
            OTextDb.getInstance( getApplicationContext() ).messageDao().insertAll( message );

            Recent recent=new Recent();
            recent.setMsg( name);
            recent.setType( Database.Recent.TYPE_USER );
            recent.setLocalId(String.valueOf(user.getLocalId()));
            recent.setMUserId( userid );
            recent.setOUserId(user.getUserId());
            recent.setName( user.getUserName() );
            recent.setMsgId( newmsgid );
            recent.setRecentMode( user.getUserMode());
            recent.setMediaType(mediatype );
            recent.setTime(  time.getTime()  );
            recent.setStatus( Database.Recent.STATUS_SEEN );
            OTextDb.getInstance( getApplicationContext() ).recentDao().insert( recent );




        }


    }

}