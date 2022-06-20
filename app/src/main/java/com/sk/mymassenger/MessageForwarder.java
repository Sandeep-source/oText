package com.sk.mymassenger;

import android.content.Context;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.sk.mymassenger.chat.senders.ImageSender;
import com.sk.mymassenger.chat.senders.PrepareMessage;
import com.sk.mymassenger.db.Database;
import com.sk.mymassenger.db.OTextDb;
import com.sk.mymassenger.db.block.Block;
import com.sk.mymassenger.db.message.Message;
import com.sk.mymassenger.db.recent.Recent;
import com.sk.mymassenger.db.user.User;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class MessageForwarder {
    private ArrayList<String> ids,msgids;

    public MessageForwarder(ArrayList<String> ids, ArrayList<String> msgids, String userid, Context context) {
        this.ids = ids;
        this.msgids = msgids;
        this.userid = userid;
        this.context = context;
    }

    private  String userid;
    private Context context;



    public void doWork() {
        for(String id : ids){

                DocumentReference doc = FirebaseFirestore.getInstance().collection("Messages/")
                        .document(userid + "to" + id + "messageidscount");
                doc.get().addOnCompleteListener(task -> {
                    DocumentSnapshot documentSnapshot = task.getResult();
                    long msg_ids_count;
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        msg_ids_count = documentSnapshot.getLong("count");
                    } else {
                        msg_ids_count = 0;
                    }
                    for (String msgId :
                            msgids) {
                        Message messageToForward = OTextDb.getInstance(context).messageDao().getMessage(userid, msgId);

                        String msgid = userid + "to" + id + msg_ids_count;

                        if (messageToForward.getType().equals(Database.Msg.MEDIA_IMG)) {

                            sendImage(messageToForward.getType(), msgid, id);
                        } else if (messageToForward.getType().equals(Database.Msg.MEDIA_TEXT)) {
                            User rec = OTextDb.getInstance(context).userDao().getUser(id);
                            Message message1 = new Message();
                            message1.setMessage(messageToForward.getMessage());
                            message1.setType(Database.Msg.TYPE_SENT);
                            message1.setOUserId(rec.getUserId());
                            message1.setMUserId(userid);
                            message1.setMsgId(msgid);
                            message1.setStatus(Database.Msg.STATUS_NOT_SENT);
                            message1.setMsgMode(rec.getUserMode());
                            message1.setMediaType(Database.Msg.MEDIA_TEXT);
                            Block block = OTextDb.getInstance(context).blockDao().getBlock(userid, rec.getUserId());
                            new PrepareMessage(message1, block != null, context).start();
                        }
                        msg_ids_count++;
                    }
                    HashMap<String, Long> newcount = new HashMap<>();
                    newcount.put("count", msg_ids_count + msgids.size());
                    doc.set(newcount, SetOptions.merge());
                }).addOnFailureListener(e -> {

                });
            }

    }
    private void sendImage(String type, String msgId, String recieverid) {
        String path="";
        if(type.contains("gif")){
            path="sent/gifs/";
        }else{
            path="sent/images/";
        }
        File directory=new File( context.getFilesDir(),path );
        if(!directory.exists()){
            directory.mkdirs();
        }
        String prefix=type.equals( "gif" )?"GIF":"IMG";
        File file=new File( context.getFilesDir(),path+prefix+msgId+"."+type );
        System.out.println("dir path :"+ context.getFilesDir().getAbsolutePath() );
        User user=OTextDb.getInstance(context).userDao().getUser(recieverid);
        long time=new Date().getTime();
        addMsgData( file,msgId, Database.Msg.MEDIA_IMG,"You sent a picture ",-1, user,time);
        new ImageSender( userid, recieverid, file.getAbsolutePath(),String.valueOf(time),
                msgId, context,type ).start();




    }

    private void addMsgData(File file, String newmsgid, String mediatype, String name, int index,User user,long time){

        Message message=new Message();
        message.setMessage(file.getAbsolutePath()  );
        message.setType( Database.Msg.TYPE_SENT );
        message.setOUserId( user.getUserId() );
        message.setMUserId( userid );
        message.setMsgId( newmsgid );
        message.setTime( time );
        message.setStatus( Database.Msg.STATUS_NOT_SENT );
        message.setMsgMode( user.getUserMode() );
        message.setTransferIndex(index);
        message.setMediaType( mediatype );
        message.setMediaStatus(Database.Msg.MEDIA_STATUS_SENDING);
        OTextDb.getInstance( context ).messageDao().insertAll( message );

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
        recent.setTime(  time );
        recent.setStatus( Database.Recent.STATUS_SEEN );
        OTextDb.getInstance( context ).recentDao().insert( recent );




    }


}
