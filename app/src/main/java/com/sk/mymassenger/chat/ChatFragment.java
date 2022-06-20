package com.sk.mymassenger.chat;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.transition.MaterialContainerTransform;
import com.google.android.material.transition.MaterialElevationScale;
import com.google.android.material.transition.MaterialSharedAxis;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.sk.mymassenger.db.Database;
import com.sk.mymassenger.DeleteMessage;
import com.sk.mymassenger.ImageActivity;
import com.sk.mymassenger.chat.senders.ImageSender;
import com.sk.mymassenger.OnlineUsers;
import com.sk.mymassenger.chat.senders.PrepareFileToSend;
import com.sk.mymassenger.chat.senders.PrepareMessage;
import com.sk.mymassenger.R;
import com.sk.mymassenger.databinding.FragmentChatBinding;
import com.sk.mymassenger.db.OTextDb;
import com.sk.mymassenger.db.message.Message;
import com.sk.mymassenger.db.recent.Recent;
import com.sk.mymassenger.viewmodels.HomeViewModel;

import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;


//TODO Last changes made on load message data to filter changed messages
// and update only when required
public class ChatFragment extends Fragment {
    private static final String TAG = "ChatFragment";
    private static final int RC_CHAT_SETTING = 0;
    private static final int GET_FILE = 1;
    private static final int GET_IMAGE = 2;
    private FragmentChatBinding binding;
    private HomeViewModel viewModel;
    private Handler handler;
    private Long msg_ids_count;
    private DocumentReference doc;
    private boolean isRunning=true;
    private ViewTreeObserver.OnGlobalLayoutListener globalLayoutListener;
    private String realTimeDbUrl;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.X,true));
        setExitTransition(new MaterialSharedAxis(MaterialSharedAxis.X,true));
        realTimeDbUrl=getString(R.string.FIREBASE_REALTIME_DB_PATH);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
       binding=FragmentChatBinding.inflate(inflater);
       viewModel=new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        String receiverId=getArguments().getString( "User" );
        FirebaseUser user= FirebaseAuth.getInstance().getCurrentUser();

        handler=new Handler( requireActivity().getMainLooper() );
        init(user, receiverId,new Handler(getActivity().getMainLooper()));
        viewModel.getLiveReceiver(receiverId).observe(getViewLifecycleOwner(),(u)->{
            if(u!=null)
                new loadMessageData(new Handler(requireActivity().getMainLooper()),null).start();

        });
        binding.backConversationActivity.setOnClickListener( (v)-> requireActivity().onBackPressed() );
        LinearLayoutManager layoutManager=new LinearLayoutManager( requireContext());
        layoutManager.setStackFromEnd( true );
        binding.conversationActivityList.setLayoutManager( layoutManager );




        binding.replyToContainer.setEndIconOnClickListener(view -> {
            binding.replyTo.setText(null);
            binding.replyToContainer.setVisibility(View.GONE);
            viewModel.setReplyTo(null);
        });
        binding.topBar.inflateMenu(R.menu.conversation_menu);
        binding.topBar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.profile_info) {
                Bundle intent = new Bundle();
                intent.putString("Reciever", receiverId);
                FragmentNavigator.Extras extras=new FragmentNavigator.Extras.Builder()
                        .addSharedElement(binding.conversationProfileImg,"profile")
                        .build();
                NavHostFragment.findNavController(ChatFragment.this)
                        .navigate(R.id.action_chatFragment_to_profileOptions,
                                intent/*,null,extras*/);
            }
            return false;
        });
        viewModel.getBlockDao().getBlockLiveData(user.getUid(), receiverId)
                .observe(getViewLifecycleOwner(),(b)->{
                    if(b!=null){
                        viewModel.can=false;
                        if(b.getType().equals(Database.Block.BY_ME)){
                            viewModel.byme=true;
                        }else{
                            viewModel.byme=false;
                        }
                    }else{
                        viewModel.can=false;
                        viewModel.byme=false;
                    }
                });
        startPostponedEnterTransition();
        return binding.getRoot();
    }

    private void init(FirebaseUser user, String receiverId,Handler handler) {
        new Thread(()-> {
            viewModel.getUser();
            viewModel.getReceiver(receiverId);
            String name=viewModel.getReceiver(null).getUserName();



            String mail=viewModel.receiver.getEmail();
            String phone=viewModel.receiver.getPhoneNo();
            String info_id = mail != null && mail.length() > 0 ? mail : phone;
            String path=viewModel.receiver.getProfilePicture();
            if(path!=null&&path.length()>5) {
                File imageFile = new File( requireContext().getFilesDir(), path.substring( path.lastIndexOf( "/" ) + 1 ) );
                Uri imguri = FileProvider.getUriForFile( requireContext(), "com.sk.mymassenger.fileProvider", imageFile );

                handler.post( ()-> {
                   Glide.with( requireContext() ).load( imguri )
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .centerCrop().into( binding.conversationProfileImg );
                });
            }
            handler.post(()->{
                binding.conversationActivityName.setText(name);
                binding.conversationActivityList.setAnimation(
                        AnimationUtils.loadAnimation(requireContext(),R.anim.slide_down_and_fade_in));

                binding.idInfo.setText( info_id );
            });
            viewModel.getRecentDao().updateStatus( user.getUid(),receiverId, Database.Recent.STATUS_SEEN );
            doc= FirebaseFirestore.getInstance().collection( "Messages" )
                    .document( viewModel.getUser().getUserId()+"to"+receiverId+"messageidscount" );
            doc.get().addOnCompleteListener( task -> {
                DocumentSnapshot documentSnapshot=task.getResult();
                if(documentSnapshot!=null&&documentSnapshot.exists()) {
                    //noinspection ConstantConditions
                    msg_ids_count = documentSnapshot.getLong( "count" );
                }else{
                    msg_ids_count= 0L;
                }
            } ).addOnFailureListener( e -> {

            } );
            HashMap<String,Object> status_info=new HashMap<>(  );


            System.out.println( "user : "+viewModel.getUser().getUserId() );
            status_info.put(viewModel.getUser().getUserId(), Arrays.asList( new Timestamp( new Date(  ).getTime() ).toString(),Database.Msg.STATUS_SEEN ) );
            FirebaseFirestore.getInstance().collection( "Messages" ).document( "info"+viewModel.receiver.getUserId())
                    .set(status_info, SetOptions.mergeFields(viewModel.getUser().getUserId()));


          //TODO(2) maybe listeners are called on main thread so handler just increasing burden
            binding.conversationSendBtnFile.setOnClickListener( (v)->{

                Intent getImage=new Intent(Intent.ACTION_GET_CONTENT);
                getImage.setType( "*/*" );
              handler.post(()->{
                  animate();
                  startActivityForResult( getImage,GET_FILE );
              });
            } );

            binding.conversationSendBtnOnlineImg.setOnClickListener(view -> {
            //TODO(1) suspected of error
                NavHostFragment.findNavController(ChatFragment.this)
                        .navigate(R.id.action_chatFragment_to_imageBoard);
                handler.post(this::animate);

            });

            binding.conversationSendBtnImg.setOnClickListener( (v)->{
                Intent getImage=new Intent(Intent.ACTION_GET_CONTENT);
                getImage.setType( "image/*" );
                handler.post(()->{
                    animate();
                    startActivityForResult( getImage,GET_IMAGE );
                });

            } );
            binding.extraOption.setOnClickListener( (v)->animate() );
            doc.addSnapshotListener( (value, error) -> {
                if(value!=null&&value.exists())
                    msg_ids_count=value.getLong("count");
            } );



        }).start();

    }

    private void animate(){
        if(binding.conversationSendBtnImg.getVisibility()==View.GONE) {
            binding.conversationSendBtnImg.startAnimation(AnimationUtils.loadAnimation(requireContext(),R.anim.up_open));
            binding.conversationSendBtnFile.startAnimation(AnimationUtils.loadAnimation(requireContext(),R.anim.up_open));
            binding.conversationSendBtnOnlineImg.startAnimation(AnimationUtils.loadAnimation(requireContext(),R.anim.up_open));
            binding.extraOption.startAnimation(AnimationUtils.loadAnimation(requireContext(),R.anim.rotate));
            binding.extraOption.setRotation(45);
            binding.conversationSendBtnOnlineImg.setVisibility(View.VISIBLE);
            binding.conversationSendBtnFile.setVisibility(View.VISIBLE);
            binding.conversationSendBtnImg.setVisibility(View.VISIBLE);
        }else{
            binding.conversationSendBtnImg.startAnimation(AnimationUtils.loadAnimation(requireContext(),R.anim.up_close));
            binding.conversationSendBtnFile.startAnimation(AnimationUtils.loadAnimation(requireContext(),R.anim.up_close));
            binding.extraOption.startAnimation(AnimationUtils.loadAnimation(requireContext(),R.anim.rotate));
            binding.conversationSendBtnOnlineImg.startAnimation(AnimationUtils.loadAnimation(requireContext(),R.anim.up_close));
            binding.extraOption.setRotation(0);
            binding.conversationSendBtnOnlineImg.setVisibility(View.GONE);
            binding.conversationSendBtnFile.setVisibility(View.GONE);
            binding.conversationSendBtnImg.setVisibility(View.GONE);
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        binding.conversationProfileImg.setOnClickListener(view -> {
            Intent intent=new Intent( requireContext(), ImageActivity.class );
            intent.putExtra("userId",viewModel.receiver.getUserId());
            intent.setData(Uri.parse(viewModel.receiver.getUserId()));
            startActivity( intent );
        });
        new Thread(()->{

            HashMap<String,Object> mapo=new HashMap<>(  );
            mapo.put( "status","online" );
            mapo.put( "for",viewModel.receiver.getUserId());
            mapo.put( "last seen",new Date( ).getTime() );
            FirebaseDatabase.getInstance(realTimeDbUrl)
                    .getReference( "Users/"+viewModel.getUser().getUserId()).setValue(mapo);

            FirebaseDatabase.getInstance().setPersistenceEnabled(true);

            DatabaseReference ref=FirebaseDatabase.getInstance(realTimeDbUrl)
                    .getReference( "Users/"+viewModel.getReceiver(null).getUserId());
            ref.keepSynced( true );
            ref.addValueEventListener( new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    if(snapshot.exists())
                        if(isRunning)
                            new OnlineUsers( handler,binding.idInfo,
                                    binding.onlineInfoImg,requireActivity(),snapshot,viewModel.userId).start();

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    error.toException().printStackTrace();
                }
            } );


        }).start();
        binding.conversationEnterMsg.setOnGifAttach((uri)-> viewModel.sendImage( uri,"gif","sent/gifs/" ,msg_ids_count,"null"));
        new Thread(()->{
        globalLayoutListener=() -> {
            new Thread(()->{
                Rect r=new Rect(  );
                binding.getRoot().getWindowVisibleDisplayFrame( r );
                int height=binding.getRoot().getRootView().getHeight();
                HashMap<String,Object> map=new HashMap<>(  );
                if((height-r.bottom)>(height*0.15)){
                    handler.post(()->{ binding.extraOption.setVisibility(View.GONE);});

                    map.put( "status","typing..." );
                    map.put( "for",viewModel.receiver.getUserId());
                    map.put( "last seen",new Date( ).getTime() );
                    FirebaseDatabase.getInstance(realTimeDbUrl)
                            .getReference( "Users/"+viewModel.getUser().getUserId() ).setValue(map);


                }else{

                    handler.post(()->{ binding.extraOption.setVisibility(View.VISIBLE);});
                    map.put( "status","online" );
                    map.put( "last seen",new Date( ).getTime() );
                    FirebaseDatabase.getInstance(realTimeDbUrl)
                            .getReference( "Users/"+viewModel.getUser().getUserId() ).setValue(map);
                }
            }).start();

        };
            binding.getRoot().getViewTreeObserver().addOnGlobalLayoutListener(globalLayoutListener);
        }).start();


        binding.conversationSendBtn.setOnClickListener( (view)->{
            binding.replyToContainer.setVisibility(View.GONE);
            Log.d(TAG, "onSending : { replyTo in Binding: "+binding.replyTo.getText().toString()+", reply in viewModel : "+viewModel.getReplyTo());

            new Thread(()->{
                if(viewModel.can) {
                    String msg = Objects.requireNonNull(binding.conversationEnterMsg.getText()).toString();
                   handler.post(()->{ binding.conversationEnterMsg.setText( "" );});
                    HashMap<String, Long> newCount = new HashMap<>();
                    newCount.put( "count", msg_ids_count + 1 );
                    doc.set( newCount, SetOptions.merge() );
                    String newMsgId = viewModel.getUser().getUserId() + "to" + viewModel.receiver.getUserId() + msg_ids_count;
                    msg = msg.trim();
                    if (msg.length() == 0)
                        return;
                    Message message=new Message();
                    message.setMessage(msg );

                    if(viewModel.getReplyTo()!=null)
                        message.setReplyOf(viewModel.getReplyTo().getMsgId());

                    message.setType( Database.Msg.TYPE_SENT );
                    message.setOUserId( viewModel.receiver.getUserId());
                    message.setMUserId( viewModel.userId );
                    message.setMsgId( newMsgId);
                    message.setStatus( Database.Msg.STATUS_NOT_SENT );
                    message.setMsgMode( viewModel.receiver.getUserMode() );
                    message.setMediaType( Database.Msg.MEDIA_TEXT );

                    if(viewModel.prepareMessage==null) {
                        viewModel.prepareMessage = new PrepareMessage(message,false,requireContext());
                    }else{
                        Log.d(TAG, "onResume: can "+viewModel.can);
                        viewModel.prepareMessage.setBlocked( !viewModel.can );
                        viewModel.prepareMessage.getMessage().setMsgMode( viewModel.receiver.getUserMode()  );
                        viewModel.prepareMessage.getMessage().setMessage( msg );
                        viewModel.prepareMessage.getMessage().setMsgId( newMsgId );
                        viewModel.prepareMessage.setReplyOf(viewModel.getReplyTo()==null?null:viewModel.getReplyTo().getMsgId());
                        handler.post(()->binding.replyTo.setText(null));
                        viewModel.setReplyTo(null);
                    }
                    viewModel.prepareMessage.start();
                }else if(viewModel.byme){
                    handler.post(()->{
                    new AlertDialog.Builder( new ContextThemeWrapper(requireActivity(),R.style.AppTheme  ) )
                            .setNegativeButton( "Cancle" ,null)
                            .setCancelable( true ).setMessage( "Unblock User first to send messages." )
                            .setPositiveButton( "Unblock", (dialogInterface, i) -> {
                                HashMap<String, Object> blockValue = new HashMap<>();
                                blockValue.put( viewModel.receiver.getUserId(), FieldValue.delete() );
                                new Thread(()->
                                FirebaseFirestore.getInstance().collection( "blockedUsers" ).document( "blockList" + viewModel.getUser().getUserId() )
                                        .set( blockValue, SetOptions.merge() ).addOnSuccessListener( aVoid -> {
                                            new Thread(()-> {
                                                blockValue.clear();
                                                blockValue.put(viewModel.getUser().getUserId(), FieldValue.delete());
                                                FirebaseFirestore.getInstance().collection("blockedUsers").document("blockList" + viewModel.receiver.getUserId()).set(blockValue, SetOptions.merge()).addOnSuccessListener(aVoid1 -> {

                                                    new Thread(() -> {
                                                        OTextDb.getInstance(requireContext()).blockDao().delete(viewModel.getUser().getUserId(), viewModel.receiver.getUserId(), Database.Block.BY_ME);
                                                        viewModel.byme = false;
                                                        viewModel.can = true;
                                                    }).start();
                                                }).addOnFailureListener(e -> {

                                                });
                                            }).start();
                                } ).addOnFailureListener( e -> {

                                } )).start();

                            })
                            .create().show();
                    });
                }else{
                    String msg = Objects.requireNonNull(binding.conversationEnterMsg.getText()).toString();
                   handler.post(()->{ binding.conversationEnterMsg.setText( "" );});
                    HashMap<String, Long> newcount = new HashMap<>();
                    newcount.put( "count", msg_ids_count + 1 );
                    doc.set( newcount, SetOptions.merge() );
                    String newmsgid = viewModel.userId + "to" + viewModel.receiver.getUserId() + msg_ids_count;
                    msg = msg.trim();
                    if (msg.length() == 0)
                        return;
                    Message message=new Message();
                    message.setMessage(msg );
                    message.setType( Database.Msg.TYPE_SENT );
                    message.setOUserId( viewModel.receiver.getUserId() );
                    message.setMUserId( viewModel.userId);
                    message.setMsgId( newmsgid );
                    message.setStatus( Database.Msg.STATUS_NOT_SENT );
                    message.setMsgMode( viewModel.receiver.getUserMode() );
                    message.setMediaType( Database.Msg.MEDIA_TEXT );
                    if(viewModel.prepareMessage==null) {
                        viewModel.prepareMessage = new PrepareMessage(message,true,requireContext());

                    }else{
                        Log.d(TAG, "onResume: can "+viewModel.can);
                        viewModel.prepareMessage.setBlocked( !viewModel.can );
                        viewModel.prepareMessage.getMessage().setMessage( msg );
                        viewModel.prepareMessage.getMessage().setMsgId( newmsgid );
                        viewModel.prepareMessage.setReplyOf(viewModel.getReplyTo()==null?null:viewModel.getReplyTo().getMsgId());
                        viewModel.setReplyTo(null);
                        handler.post(()->binding.replyTo.setText(null));
                    }
                    viewModel.prepareMessage.start();

                }
            }).start();

        } );

        Log.d( TAG, "onResume: pre get()" );


    }

    @Override
    public void onStop() {
        super.onStop();
        binding.getRoot().getRootView().getViewTreeObserver().removeOnGlobalLayoutListener(globalLayoutListener);

    }

    @Override
    public void onDestroy() {

        super.onDestroy();
        isRunning=false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult( requestCode, resultCode, data );
        if(requestCode==GET_IMAGE){
            if(resultCode== Activity.RESULT_OK){

                    if (data != null) {
                       new Thread(()->viewModel.sendImage(data.getData(), "png", "sent/images/",msg_ids_count,"kjk")).start();
                    }


            }
        }else if(requestCode==GET_FILE){
            if(resultCode==Activity.RESULT_OK){
                handler.post( ()-> {
                    if (data != null) {
                        viewModel.sendFile(data.getData(),msg_ids_count);
                    }
                });

            }
        }
    }


    private void sendFile(Uri data) {
        HashMap<String,Long> newcount=new HashMap<>(  );
        newcount.put("count",msg_ids_count+1);
        doc.set(newcount,SetOptions.merge());
        Cursor cur=requireContext().getContentResolver().query( data,null,null,null,null,null );
        assert cur != null;
        cur.moveToFirst();
        String name=cur.getString( cur.getColumnIndex( OpenableColumns.DISPLAY_NAME ) );
        String newmsgid=viewModel.userId+"to"+viewModel.getReceiver(null).getUserId()+msg_ids_count;
        File directory=new File( requireContext().getFilesDir(),"sent/file/" );
        if(!directory.exists()){
            directory.mkdirs();
        }
        File file=new File( requireContext().getFilesDir(),"sent/file/"+name );
        System.out.println("dir path :"+ requireContext().getFilesDir().getAbsolutePath() );
        HomeViewModel.StatusPair pair=viewModel.add();
        addMsgData( file,newmsgid , Database.Msg.MEDIA_FILE,name, pair.getIndex());
        new PrepareFileToSend( viewModel.userId, viewModel.getReceiver(null).getUserId(), data, new Timestamp( new Date().getTime() ).toString(),
                newmsgid,name, requireContext()).start();

        cur.close();


    }

    private void sendImage(Uri data, String type,String path) {
        HashMap<String,Long> newcount=new HashMap<>(  );
        newcount.put("count",msg_ids_count+1);
        doc.set(newcount,SetOptions.merge());
        File directory=new File( requireContext().getFilesDir(),path );
        if(!directory.exists()){
            directory.mkdirs();
        }
        String newmsgid=viewModel.userId+"to"+viewModel.getReceiver(null).getUserId()+msg_ids_count;
        String prefix=type.equals( "gif" )?"GIF":"IMG";
        HomeViewModel.StatusPair pair=viewModel.add();
        File file=new File( requireContext().getFilesDir(),path+prefix+newmsgid+"."+type );
        System.out.println("dir path :"+ requireContext().getFilesDir().getAbsolutePath() );
        addMsgData( file,newmsgid, Database.Msg.MEDIA_IMG,"You sent a picture ",pair.getIndex());
        new ImageSender(viewModel.userId, viewModel.receiver.getUserId(), data.toString(),new Timestamp( new Date().getTime() ).toString(),
                newmsgid, requireContext(),type ).start();




    }


    private void addMsgData(File file, String newmsgid, String mediatype, String name, int index){
        Timestamp time = new Timestamp( new Date().getTime() ) ;
        Message message=new Message();
        message.setMessage(file.getAbsolutePath()  );
        message.setType( Database.Msg.TYPE_SENT );
        message.setOUserId( viewModel.receiver.getUserId() );
        message.setMUserId( viewModel.userId );
        message.setMsgId( newmsgid );
        message.setTime( time.getTime() );
        message.setStatus( Database.Msg.STATUS_NOT_SENT );
        message.setMsgMode( viewModel.getReceiver(null).getUserMode() );
        message.setTransferIndex(index);
        message.setMediaType( mediatype );
        message.setMediaStatus(Database.Msg.MEDIA_STATUS_SENDING);
        OTextDb.getInstance( requireContext() ).messageDao().insertAll( message );

        Recent recent=new Recent();
        recent.setMsg( name);
        recent.setType( Database.Recent.TYPE_USER );
        recent.setLocalId(String.valueOf(viewModel.getReceiver(null).getLocalId()));
        recent.setMUserId( viewModel.userId );
        recent.setOUserId( viewModel.receiver.getUserMode() );
        recent.setName( viewModel.receiver.getUserName() );
        recent.setMsgId( newmsgid );
        recent.setRecentMode( viewModel.receiver.getUserMode() );
        recent.setMediaType(mediatype );
        recent.setTime(  time.getTime()  );
        recent.setStatus( Database.Recent.STATUS_SEEN );
        OTextDb.getInstance( requireContext() ).recentDao().insert( recent );




    }


    class loadMessageData extends Thread{
        private  Handler handler;
        private List<Message> messageList;
        public loadMessageData(Handler handler,List<Message> list){
            this.handler=handler;
            this.messageList=list;

        }

        @Override
        public void run() {

            if(messageList==null) {
                LiveData<List<Message>> listLiveData = viewModel.getMessageDao()
                        .getAll( viewModel.userId,viewModel.receiver.getUserId(),
                                viewModel.receiver.getUserMode());
                messageList=listLiveData.getValue();


                handler.post( ()-> listLiveData.observe( getViewLifecycleOwner(), messages -> {
                    Log.d( TAG, "run: " +messages);

                    new Thread(()-> {

                        int prein = 0;
                        List<Message> lst=viewModel.chatAdapter.getMsgs();
                       int size=-1;
                        if (lst!= null) {
                            prein = (viewModel.chatAdapter.getMsgs().size() - 1);
                            size=prein+1;

                        }
                        int pos=((LinearLayoutManager)binding.conversationActivityList.getLayoutManager()).findLastVisibleItemPosition();
                        viewModel.chatAdapter.setMsgs(messages);

                        boolean same=true;
                        int newSize=messages.size();

                        if(messages!=null&&size==newSize){
                            for(int i=0;i<newSize;i++){
                                Message newMsg=messages.get(i);
                                Message pre=lst.get(i);
                                if(!newMsg.getMsgId().equals(pre.getMsgId())){
                                    same=false;
                                    int finalI = i;
                                    handler.post(()->{
                                        viewModel.chatAdapter.notifyItemRangeChanged(finalI,newSize-finalI);
                                    });
                                    break;
                                }else if(!newMsg.toString().equals(pre.toString())){
                                    int finalI1 = i;
                                    handler.post(()->
                                        viewModel.chatAdapter.notifyItemChanged(finalI1)
                                    );
                                }else{
                                    handler.post(()->
                                            viewModel.chatAdapter.notifyDataSetChanged()
                                    );
                                }
                            }
                        }else {

                            int finalPrein = prein;
                            handler.post(() -> {
                                viewModel.chatAdapter.notifyDataSetChanged();
                                 if (pos == finalPrein && messages != null)
                                    binding.conversationActivityList.scrollToPosition(messages.size() - 1);
                            });
                        }



                        HashMap<String, Object> info = new HashMap<>();
                        info.put(viewModel.userId, Arrays.asList(new Timestamp(new Date().getTime()).toString(), Database.Msg.STATUS_SEEN));
                        FirebaseFirestore.getInstance().collection("Messages").document("info" + viewModel.receiver.getUserId())
                                .set(info, SetOptions.mergeFields(viewModel.userId));

                    }).start();



                } ));
            }
            handler.post( ()->{
                viewModel.chatAdapter=new ChatFragmentAdapter(messageList,requireContext(),viewModel.userId,binding,viewModel,handler);
                binding.conversationActivityList.setAdapter(viewModel.chatAdapter );
            } );
            ItemTouchHelper.SimpleCallback callback=new ItemTouchHelper.SimpleCallback(0,ItemTouchHelper.LEFT|ItemTouchHelper.RIGHT) {
                @Override
                public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                    return false;
                }

                @Override
                public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                    int position=((ChatFragmentAdapter.MViewHolder)viewHolder).getMsgPosition();
                    Message replyTo=viewModel.chatAdapter.getMsgs().get(position);
                    viewModel.chatAdapter.notifyItemChanged(position);
                    viewModel.setReplyTo(replyTo);
                    binding.replyTo.setText(replyTo.getMessage());
                    binding.replyToContainer.setVisibility(View.VISIBLE);
                    Toast.makeText(requireContext(),replyTo.getMessage(),Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                    super.onChildDraw(c, recyclerView, viewHolder, dX/4, dY/4, actionState, isCurrentlyActive);
                }
            };
            handler.post(()-> new ItemTouchHelper(callback).attachToRecyclerView(binding.conversationActivityList));

        }
    }


}
