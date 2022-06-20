package com.sk.mymassenger.chat;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.sk.mymassenger.HomeActivity;
import com.sk.mymassenger.MessageForwarderActivity;
import com.sk.mymassenger.R;
import com.sk.mymassenger.chat.fetchers.FetchFiles;
import com.sk.mymassenger.chat.senders.ReactionSender;
import com.sk.mymassenger.data.MessageData;
import com.sk.mymassenger.databinding.FragmentChatBinding;
import com.sk.mymassenger.db.Database;
import com.sk.mymassenger.db.message.Message;
import com.sk.mymassenger.db.message.MessageDao;
import com.sk.mymassenger.dialog.SelectOptionDialog;
import com.sk.mymassenger.viewmodels.HomeViewModel;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class ChatFragmentAdapter extends RecyclerView.Adapter<ChatFragmentAdapter.MViewHolder>{
    private static final String TAG = "ChatFragmentAdapter";
    private final Handler handler;
    private boolean selectionMode;
        SelectOptionDialog dialog;

        public void setMsgs(List<Message> msg) {

            Log.d( "TAG", "ConversationActivityAdapter set: "+msg );
            this.msgs = msg;
            if(msgs!=null&&msg.size()>0) {
                ouser = msgs.get(0).getOUserId();
                if(dialog==null)
                    handler.post(()->
                    build(msgs.get(0)));
                count = msg.size();

            }else{
                count=0;
            }
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
        private final Context context;
        private final String userid;
        private ArrayList<Message> selectedList;
        private ArrayList<String> ids;


        private String ouser;
        private final HomeViewModel viewModel;
        private final FragmentChatBinding binding;
        public ChatFragmentAdapter(List<Message> msg, Context context, String userid,
                                   FragmentChatBinding binding, HomeViewModel viewModel, Handler handler) {
            this.viewModel=viewModel;
            msgs=msg;
            this.userid=userid;
            this.context=context;
            this.binding= binding;
            ids= new ArrayList<>();
            this.handler=handler;
            Log.d( "TAG", "ConversationActivityAdapter: "+msgs );
            if(msgs!=null){
                Log.d( "TAG", "ConversationActivityAdapter size: "+msgs.size() );
                count=msg.size();
                if(count>0) {
                    handler.post(()->
                    build(msg.get(0)));
                    ouser = msg.get(0).getOUserId();
                }


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

                Message msgData = msgs.get(position);
                holder.setPosition(position);
                if (selectionMode) {

                    String temp = msgs.get(position).getMsgId();
                if (ids.contains(temp)) {

                        holder.getRoot().setBackgroundColor(Color.parseColor("#34ae5aa4"));
                    } else {
                        holder.getRoot().setBackgroundColor(Color.parseColor("#00000000"));

                    }

                }
                TextView msg = holder.getMsg();
                System.out.println("onBindViewHolder");
                ImageView image = holder.getImage();
                System.out.println("TYPE OF POSITION " + position + " : " + msgData.getType());
                String status = msgData.getStatus();

                Log.d(TAG, "onBindViewHolder: " + msgData.getReact());
                String replyOf = msgData.getReplyOf();
                Chip replyCon = holder.getReplyCon();
            LinearLayout msgContainer = holder.getMsgCon();
            if (msgData.getType().equals(Database.Msg.TYPE_SENT)) {
                msgContainer.setBackground(ContextCompat.getDrawable(context, R.drawable.msg_back_light));

                CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) ((CoordinatorLayout) msgContainer.getParent()).getLayoutParams();
                lp.gravity = Gravity.END;
                ((CoordinatorLayout) msgContainer.getParent()).setLayoutParams(lp);
                ImageView statusImg=holder.getStatus();
                switch (status) {
                    case Database.Msg.STATUS_NOT_SENT:
                        statusImg.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_baseline_access_time_24));
                        break;
                    case Database.Msg.STATUS_SENT:
                       statusImg.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_baseline_check_24));

                        break;
                    case Database.Msg.STATUS_NOT_SEEN:
                        statusImg.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.double_tick));
                        break;
                    default:
                        statusImg.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_baseline_remove_red_eye_24));

                        break;
                }
                holder.getTimeCon().setTextColor(Color.DKGRAY);
                statusImg.setVisibility(View.VISIBLE);
            } else {

                msgContainer.setBackground(ContextCompat.getDrawable(context, R.drawable.msg_back_dark));

                CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) ((CoordinatorLayout) msgContainer.getParent()).getLayoutParams();

                lp.gravity = Gravity.START;
                ((CoordinatorLayout) msgContainer.getParent()).setLayoutParams(lp);
                holder.getTimeCon().setTextColor(Color.parseColor("#eaeaea"));

                holder.getStatus().setVisibility(View.GONE);
            }
                    align(msgData.getReact(), holder.getReact());
                    msg.setText(msgData.getMessage());
                    if (msgData.getType().equals(Database.Msg.TYPE_SENT)) {
                        replyCon.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#dfdfdf")));
                        replyCon.setTextColor(Color.DKGRAY);
                    } else {

                        replyCon.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#9d4994")));
                        replyCon.setTextColor(Color.parseColor("#eaeaea"));

                    }

                if (replyOf != null) {
                  new Thread(()->{ Message message = viewModel.getMessageDao().get(userid, msgData.getOUserId(), replyOf);
                  handler.post(()->{
                  if (message != null) {

                            holder.getReplyCon().setVisibility(View.VISIBLE);
                            if (message.getMediaType().equals(Database.Msg.MEDIA_IMG)) {
                                holder.getReplyCon().setCheckedIcon(Drawable.createFromPath(message.getMessage()));
                                holder.getReplyCon().setText(new File(message.getMessage()).getName());

                            } else {
                                holder.getReplyCon().setText(message.getMessage());
                            }
                            replyCon.setVisibility(View.VISIBLE);
                            if (message.getType().equals(Database.Msg.TYPE_SENT)) {
                                holder.getReplyCon().setChipIconTint(ColorStateList.valueOf(Color.parseColor("#eaeaea")));
                                holder.getReplyCon().setChipIconResource(R.drawable.you);
                            } else {
                                holder.getReplyCon().setChipIcon(null);
                            }
                            holder.getReplyCon().setOnClickListener(view -> new Thread(()->{
                                int index = Collections.binarySearch(msgs, message, (m1, m2) -> m1.getMsgId().compareToIgnoreCase(m2.getMsgId()));
                                Log.d(TAG, "onBindViewHolder: reply msg position " + index);
                                if (index >= 0) {
                                    handler.post(() ->
                                            binding.conversationActivityList.smoothScrollToPosition(index));
                                }
                            }).start());






                    } else {

                            replyCon.setVisibility(View.GONE);


                    }});
                  }).start();
                } else if (replyCon != null) {

                       replyCon.setVisibility(View.GONE);


                }
                if (msgData.getMediaType().equals(Database.Msg.MEDIA_IMG)) {

                    String path = msgData.getMessage();
                    System.out.println("image path" + Uri.parse(path).toString());

                       holder.getMsg().setVisibility(View.GONE);
                       holder.getFileMsg().setVisibility(View.GONE);
                       holder.getImage().setVisibility(View.VISIBLE);



                    View dbtn = holder.getDownload();
                    if (msgData.getType().equals(Database.Msg.TYPE_SENT)) {

                        Log.d(TAG, "onBindViewHolder: media status " + msgData.getMediaStatus());
                        if (msgData.getMediaStatus().equals(Database.Msg.MEDIA_STATUS_SENDING)) {

                            LiveData<Integer> liveData = viewModel.getLiveData((int) msgData.getTransferIndex());
                            image.setImageResource(R.drawable.ic_baseline_image_24);
                            ProgressBar progressBar = holder.getProgress();
                            handler.post(()->{
                                progressBar.setVisibility(View.VISIBLE);

                                ((View) dbtn.getParent()).setVisibility(View.VISIBLE);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    dbtn.setBackground(ContextCompat.getDrawable(context, R.drawable.ic_baseline_arrow_upward_24));
                                }

                                Log.d(TAG, "onBindViewHolder: Live data " + liveData);
                                if (liveData != null){
                                    liveData.observe((HomeActivity) context, integer -> {
                                        if(progressBar.isIndeterminate()){
                                            progressBar.setIndeterminate(false);
                                        }
                                        progressBar.setProgress(integer);
                                    });
                                }

                            });

                        } else {


                            handler.post(()->
                            ((View) dbtn.getParent()).setVisibility(View.GONE));
                            if (path != null) {
                                File imageFile = new File(context.getFilesDir(), path.substring(path.lastIndexOf("sent")));
                                System.out.println("Path in msg  :" + path + "\n" + path.substring(path.lastIndexOf("sent")));
                                Uri imguri = FileProvider.getUriForFile(context, "com.sk.mymassenger.fileProvider", imageFile);
                               handler.post(()->{
                                   if (path.endsWith("gif"))

                                       Glide.with(context).load(imguri).asGif().fitCenter().into(image);
                                   else
                                       Glide.with(context).load(imguri).fitCenter().into(image);
                               });

                            } else
                                    image.setImageResource(R.drawable.ic_baseline_image_24);
                                    image.setOnClickListener(view -> {
                                        Intent intent = new Intent(Intent.ACTION_VIEW);
                                        assert path != null;
                                        File imageFile = new File(context.getFilesDir(), path.substring(path.lastIndexOf("sent")));
                                        System.out.println("Path in msg  :" + path);
                                        Uri imguri = FileProvider.getUriForFile(context, "com.sk.mymassenger.fileProvider", imageFile);
                                        System.out.println("file Path :" + imageFile.getAbsolutePath());
                                        System.out.println("Uri Path :" + imguri.toString());
                                        intent.setDataAndType(imguri, "image/" + path.substring(path.lastIndexOf(".") + 1));
                                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                                        context.startActivity(Intent.createChooser(intent, "View Image"));
                                    });

                        }
                    } else {
                        if (msgData.getMediaStatus().equals(Database.Msg.MEDIA_STATUS_DOWNLOADING)) {


                            LiveData<Integer> liveData = viewModel.getLiveData((int) msgData.getTransferIndex());

                            ProgressBar progressBar = holder.getProgress();
                            handler.post(()->{
                                progressBar.setVisibility(View.VISIBLE);
                                ((View) dbtn.getParent()).setVisibility(View.VISIBLE);

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    dbtn.setForeground(ContextCompat.getDrawable(context, R.drawable.ic_baseline_arrow_downward_24));
                                }
                                Log.d(TAG, "onBindViewHolder: Live data " + liveData);
                                if (liveData != null) {
                                    liveData.observe((HomeActivity) context, progressBar::setProgress);
                                }
                            });


                        } else if (msgData.getMediaStatus().equals(Database.Msg.MEDIA_STATUS_NOT_DOWNLOADED)) {

                            ((View) dbtn.getParent()).setVisibility(View.VISIBLE);
                            if (path != null) {
                                File imageFile = new File(context.getFilesDir(), "imagesThumb/" + path.substring(path.lastIndexOf("/") + 1));
                                System.out.println("Path in msg  :" + path);
                                Uri imguri = FileProvider.getUriForFile(context, "com.sk.mymassenger.fileProvider", imageFile);
                                handler.post(()->{
                                    if (path.endsWith("gif"))
                                        Glide.with(context).load(imguri).asGif().into(image);
                                    else
                                        Glide.with(context).load(imguri).into(image);
                                });

                            } else

                                    image.setImageResource(R.drawable.ic_baseline_image_24);
                                    dbtn.setOnClickListener((v) -> {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            v.setForegroundTintList(ColorStateList.valueOf(Color.parseColor("#9d4994")));
                                        } else {
                                            v.setBackgroundColor(Color.parseColor("#9d4994"));
                                        }
                                        File imagedir;
                                        System.out.println("download path " + path);
                                        assert path != null;
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            dbtn.setForeground(context.getDrawable( R.drawable.ic_baseline_arrow_downward_24));
                                        }
                                        ProgressBar progressBar = holder.getProgress();
                                        progressBar.setVisibility(View.VISIBLE);
                                        if (path.endsWith("gif")) {
                                            imagedir = new File(context.getFilesDir(), "receive/gifs/");
                                        } else
                                            imagedir = new File(context.getFilesDir(), "receive/images/");
                                        if (!imagedir.exists()) {
                                            imagedir.mkdirs();
                                        }

                                        HomeViewModel.StatusPair index = viewModel.add();
                                        Constraints constraints = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
                                        WorkRequest workRequest = new OneTimeWorkRequest.Builder(FetchFiles.class).setInputData(
                                                new Data.Builder()
                                                        .putInt("liveDataIndex", index.getIndex())
                                                        .putString("file", new File(imagedir.getAbsolutePath(),
                                                                path.substring(path.lastIndexOf("/") + 1)).getAbsolutePath())
                                                        .putString("serverPath", "images/"
                                                                + path.substring(path.lastIndexOf("/") + 1))
                                                        .putInt("index", position).putString("msgid", msgs.get(position).getMsgId()).putString("muser", userid)
                                                        .putString("ouser", ouser).build()
                                        ).setConstraints(constraints).build();
                                        WorkManager.getInstance(context).enqueue(workRequest);
                                        index.getLiveData().observe((HomeActivity) context,
                                                progressBar::setProgress);
                                    });


                        } else {

                            ((View) dbtn.getParent()).setVisibility(View.GONE);
                            if (path != null) {
                                File imageFile = new File(context.getFilesDir(), path.substring(path.lastIndexOf("receive")));
                                System.out.println("Path in msg  :" + path);
                                Uri imguri = FileProvider.getUriForFile(context, "com.sk.mymassenger.fileProvider", imageFile);
                                handler.post(()->{if (path.endsWith("gif"))
                                    Glide.with(context).load(imguri).asGif().into(image);
                                else
                                    Glide.with(context).load(imguri).into(image);});

                            } else

                                    image.setImageResource(R.drawable.ic_baseline_image_24);
                                    image.setOnClickListener(view -> {
                                        Intent intent = new Intent(Intent.ACTION_VIEW);
                                        assert path != null;
                                        File imageFile = new File(context.getFilesDir(), path.substring(path.lastIndexOf("receive")));
                                        System.out.println("Path in msg  :" + path);
                                        Uri imguri = FileProvider.getUriForFile(context, "com.sk.mymassenger.fileProvider", imageFile);
                                        System.out.println("file Path :" + imageFile.getAbsolutePath());
                                        System.out.println("Uri Path :" + imguri.toString());
                                        System.out.println("Uri Path extention :" + path.substring(path.lastIndexOf(".") + 1));
                                        intent.setDataAndType(imguri, "image/" + path.substring(path.lastIndexOf(".")));
                                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                                        context.startActivity(Intent.createChooser(intent, "View Image"));
                                    });

                        }
                    }
                } else if (msgData.getMediaType().equals(Database.Msg.MEDIA_FILE)) {
                    LinearLayout filemsg = holder.getFileMsg();
                    String path = msgData.getMessage();

                        image.setVisibility(View.GONE);
                        image.setImageURI(null);
                        holder.getMsg().setVisibility(View.GONE);
                        filemsg.setVisibility(View.VISIBLE);
                        holder.getFilename().setText(new File(path).getName());



                    if (msgData.getType().equals(Database.Msg.TYPE_SENT)) {

                           holder.getFilename().setTextColor(Color.parseColor("#000000"));

                           if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                               holder.getRoot().findViewById(R.id.file_symb).setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#000000")));
                           }


                        View dbtn = holder.getDownload();

                        if (msgData.getMediaStatus().equals(Database.Msg.MEDIA_STATUS_SENDING)) {

                            ProgressBar progressBar = holder.getProgress();
                            LiveData<Integer> liveData = viewModel.getLiveData((int) msgData.getTransferIndex());

                               progressBar.setVisibility(View.VISIBLE);
                               ((View) dbtn.getParent()).setVisibility(View.VISIBLE);
                               if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                   dbtn.setForeground(ContextCompat.getDrawable(context, R.drawable.ic_baseline_arrow_upward_24));
                               }

                               Log.d(TAG, "onBindViewHolder: Live data " + liveData);
                               if (liveData != null) {
                                   liveData.observe((HomeActivity) context, integer -> {
                                       Log.d(TAG, "onBindViewHolder: downloaded " + integer + "%");
                                       progressBar.setProgress(integer);
                                   });
                               }



                        } else {

                                dbtn.setVisibility(View.GONE);

                                filemsg.setOnClickListener(view -> {

                                    File imageFile = new File(context.getFilesDir(), "receive/file/" + path.substring(path.lastIndexOf("/") + 1));
                                    System.out.println("Path in msg  :" + path);
                                    Uri imguri = FileProvider.getUriForFile(context, "com.sk.mymassenger.fileProvider", imageFile);
                                    System.out.println("file Path :" + imageFile.getAbsolutePath());
                                    System.out.println("Uri Path :" + imguri.toString());
                                    String type = context.getContentResolver().getType(imguri);
                                    System.out.println("Uri type :" + type);
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    intent.setData(imguri);
                                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                                    context.startActivity(Intent.createChooser(intent, "View Image"));
                                });

                        }
                    } else {



                        View dbtn = holder.getDownload();
                            holder.getFilename().setTextColor(Color.parseColor("#ffffff"));

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                holder.getRoot().findViewById(R.id.file_symb).setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#ffffff")));
                            }
                            if (msgData.getMediaStatus().equals(Database.Msg.MEDIA_STATUS_DOWNLOADING)) {

                                ((View) dbtn.getParent()).setVisibility(View.VISIBLE);
                                ProgressBar progressBar = holder.getProgress();
                                progressBar.setVisibility(View.VISIBLE);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    dbtn.setForeground(ContextCompat.getDrawable(context, R.drawable.ic_baseline_arrow_downward_24));
                                }

                                LiveData<Integer> liveData = viewModel.getLiveData((int) msgData.getTransferIndex());
                                Log.d(TAG, "onBindViewHolder: Live data " + liveData);
                                if (liveData != null) {
                                    liveData.observe((HomeActivity) context, integer -> {
                                        Log.d(TAG, "onBindViewHolder: downloaded " + integer + "%");
                                        progressBar.setProgress(integer);
                                    });
                                }

                            } else if (msgData.getMediaStatus().equals(Database.Msg.MEDIA_STATUS_NOT_DOWNLOADED)) {

                                ((View) dbtn.getParent()).setVisibility(View.VISIBLE);

                                dbtn.setOnClickListener((v) -> {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        v.setForegroundTintList(ColorStateList.valueOf(Color.parseColor("#9d4994")));
                                    }
                                    holder.getProgress().setVisibility(View.VISIBLE);
                                    File imagedir = new File(context.getFilesDir(), "receive/file/");
                                    if (!imagedir.exists()) {
                                        imagedir.mkdirs();
                                    }

                                    HomeViewModel.StatusPair index = viewModel.add();
                                    Constraints constraints = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
                                    WorkRequest workRequest = new OneTimeWorkRequest.Builder(FetchFiles.class)
                                            .setInputData(
                                                    new Data.Builder()
                                                            .putInt("liveDataIndex", index.getIndex())
                                                            .putString("file", new File(imagedir.getAbsolutePath(), path).getAbsolutePath()).putString("serverPath", "files/"
                                                            + path).putString("msgid", msgs.get(position).getMsgId()).putString("muser", userid)
                                                            .putString("ouser", ouser).build()
                                            ).setConstraints(constraints).build();
                                    WorkManager.getInstance(context).enqueue(workRequest);
                                    index.getLiveData().observe((HomeActivity) context, integer -> {

                                    });
                                });
                            } else {
                                ((View) dbtn.getParent()).setVisibility(View.GONE);

                                filemsg.setOnClickListener(view -> {

                                    File imageFile = new File(context.getFilesDir(), "receive/file/" + path.substring(path.lastIndexOf("/") + 1));
                                    System.out.println("Path in msg  :" + path);
                                    Uri imguri = FileProvider.getUriForFile(context, "com.sk.mymassenger.fileProvider", imageFile);
                                    System.out.println("file Path :" + imageFile.getAbsolutePath());
                                    System.out.println("Uri Path :" + imguri.toString());
                                    String type = context.getContentResolver().getType(imguri);
                                    System.out.println("Uri type :" + type);
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    intent.setDataAndType(imguri, type);
                                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                                    context.startActivity(Intent.createChooser(intent, "View Image"));
                                });
                            }


                    }
                } else {
                          ((View) holder.getDownload().getParent()).setVisibility(View.GONE);
                          holder.getFileMsg().setVisibility(View.GONE);
                          image.setImageBitmap(null);
                          image.setVisibility(View.GONE);
                          holder.getMsg().setVisibility(View.VISIBLE);
                          if (msgData.getType().equals(Database.Msg.TYPE_SENT)) {
                              msg.setTextColor(ColorStateList.valueOf(Color.parseColor("#000000")));

                          } else {
                              msg.setTextColor(ColorStateList.valueOf(Color.parseColor("#ffffff")));


                          }


                }
                Log.d(TAG, "onBindViewHolder: image visibility " + (image.getVisibility() == View.VISIBLE));

                Log.d(TAG, "onBindViewHolder: container visibility " + (msgContainer.getVisibility() == View.VISIBLE));
                SimpleDateFormat format = new SimpleDateFormat("hh:mm a");



                    holder.getTimeCon().setText(format.format(msgData.getTime()));


        /*holder.getRoot().setOnLongClickListener( view -> {
            ((ConversationActivity)context).startActionMode(new ActionMode.Callback() {
                @Override
                public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                    actionMode.getMenuInflater().inflate(R.menu.action_bar_menu,menu);
                    return true;
                }

                @Override
                public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                    return false;
                }

                @Override
                public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                    return false;
                }

                @Override
                public void onDestroyActionMode(ActionMode actionMode) {

                }
            });
            return true;
        } );

         */
                if (ids != null && ids.size() > 0) {
                    String temp = msgs.get(position).getMsgId();
                    if (ids.contains(temp)) {
                        holder.getRoot().setBackgroundColor(Color.parseColor("#34ae5aa4"));
                    } else {
                        handler.post(()->
                        holder.getRoot().setBackgroundColor(Color.parseColor("#00000000")));
                    }
                }
                handler.post(()->
                holder.getRoot().setOnClickListener(view -> {
                    Rect rect = new Rect();
                    view.getGlobalVisibleRect(rect);
                    try{
                        if(msgData.getMediaType().equals(Database.Msg.MEDIA_TEXT)||
                            msgData.getMediaStatus().equals(Database.Msg.MEDIA_STATUS_DOWNLOADED)){
                            dialog.setAlign(rect.left, rect.top);
                            dialog.setCancelable(true);
                            dialog.setMsgData(msgData);
                            dialog.show();
                        }
                    }catch (Exception throwble){
                        Log.d(TAG, "onBindViewHolder: "+throwble.getMessage());
                        dialog.setAlign(rect.left, rect.top);
                        dialog.setCancelable(true);
                        dialog.setMsgData(msgData);
                        dialog.show();
                    }



                }));



        }
        private void build(Message msgData){

            dialog=new SelectOptionDialog(context);
            dialog.setReactListener(view13 -> {
                dialog.getMsgData().setReact(((TextView) view13).getText().toString());
                new ReactionSender(dialog.getMsgData(), viewModel.getMessageDao()).start();


            });
            dialog.setReplyListener(view12 -> {
                Message msg=dialog.getMsgData();
                viewModel.setReplyTo(msg);
                binding.replyToContainer.setVisibility(View.VISIBLE);
                binding.replyTo.setText(msg.getMessage());
                binding.conversationEnterMsg.requestFocus();
                dialog.cancel();
            });
            dialog.setForwardListener(view1 -> {
                Intent intentf=new Intent( context, MessageForwarderActivity.class );
                intentf.putExtra( "mode","forward" );
                intentf.putStringArrayListExtra( "msgIdList",ids );
                context.startActivity(intentf);
                dialog.cancel();
            });


        }

        private void align(String react, Chip holderReact) {
            if(react!=null&&react.length()>0){
                holderReact.setVisibility(View.VISIBLE);
                holderReact.setText(react);
            }else{
                holderReact.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return count;
        }

        static class MViewHolder extends RecyclerView.ViewHolder {
            private final LinearLayout fileMsg;

            public TextView getTimeCon() {
                return timeCon;
            }

            private final TextView timeCon;

            public LinearLayout getMsgCon() {
                return msgCon;
            }

            private final LinearLayout msgCon;

            public Chip getReplyCon() {
                return replyCon;
            }

            private final Chip replyCon;
            private final Chip react;

            public ProgressBar getProgress() {
                return progress;
            }

            private final ProgressBar progress;

            public View getDownload() {
                return download;
            }

            private final View download;


            public int getMsgPosition() {
                return position;
            }

            public void setPosition(int position) {
                this.position = position;
            }

            private int position;

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

            public ImageView getStatus() {
                return status;
            }

            private final ImageView status;
            private final TextView msg;
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
                progress=itemView.findViewById(R.id.downloadStatus);
                react=itemView.findViewById(R.id.react);
                replyCon=itemView.findViewById(R.id.replyContainer);
                msgCon=itemView.findViewById(R.id.msgContainer);
                timeCon=itemView.findViewById(R.id.time);
            }

            public Chip getReact() {
                return  react;
            }

        }
        class DelMsg extends Thread{
            private static final String TAG = "DelMsg";
            private final ArrayList<Message> ids;
            private final Handler handler;

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
                handler.post(ChatFragmentAdapter.this::notifyDataSetChanged);
                selectedList.clear();

            }
        }
        static class ReactionSender extends Thread{
            private final Message message;
            private final MessageDao messageDao;
            private final String msg;

            public ReactionSender(Message message, MessageDao messageDao) {

                this.message = message;
                this.messageDao = messageDao;
                msg=message.getMessage();
                if(!message.getMediaType().equals(Database.Msg.MEDIA_TEXT))
                    message.setMessage(new File(msg).getName());
            }

            @Override
            public void run() {
                HashMap<String, Object> valueforserver = new HashMap<>();
                HashMap<String, Object> val = new HashMap<>();


                valueforserver.put(message.getMsgId(),new MessageData(message,false) );

                FirebaseFirestore.getInstance().collection( "Messages" )
                        .document( message.getMUserId()+ "to" +message.getOUserId() )
                        .set( valueforserver, SetOptions.merge() )
                        .addOnSuccessListener(aVoid -> new Thread(()-> {
                    HashMap<String, Object> status_info = new HashMap<>();
                    status_info.put(message.getOUserId(), Arrays.asList(message.getTime(), message.getStatus()));
                    valueforserver.clear();
                    val.clear();
                    val.put(message.getMsgId(), message.getTime());
                    valueforserver.put(message.getMUserId(), val);

                    FirebaseFirestore.getInstance().collection("Messages")
                            .document("to" + message.getOUserId())
                            .set(valueforserver, SetOptions.mergeFields(message.getMUserId()))
                            .addOnSuccessListener(aVoid1 -> new Thread(() -> {
                        message.setStatus(Database.Msg.STATUS_SENT);
                        message.setMessage(msg);
                        messageDao.update(message);
                    }).start());


                }).start());
            }
        }


}
