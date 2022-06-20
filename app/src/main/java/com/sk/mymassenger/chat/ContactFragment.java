package com.sk.mymassenger.chat;

import static android.view.View.GONE;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.transition.MaterialSharedAxis;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import com.google.firebase.storage.FirebaseStorage;
import com.sk.mymassenger.data.ServerUserData;
import com.sk.mymassenger.db.Database;
import com.sk.mymassenger.PrivateActivity;
import com.sk.mymassenger.R;
import com.sk.mymassenger.auth.UserDataFetcher;
import com.sk.mymassenger.databinding.FragmentContactBinding;
import com.sk.mymassenger.db.OTextDb;
import com.sk.mymassenger.db.user.User;
import com.sk.mymassenger.dialog.SearchUserDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ContactFragment extends Fragment {
    private static final String TAG = "ContactFragment";
    FirebaseUser user;
    private SearchUserDialog progress;

    private boolean empty;
    FragmentContactBinding binding;
    private GlobalUserAdapter adapter;
    Handler handler;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler=new Handler(getActivity().getMainLooper());
        setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.X,true));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding=FragmentContactBinding.inflate(getLayoutInflater());
        binding.contactBackBtn.setOnClickListener( (view)-> requireActivity().onBackPressed() );
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            binding.searchContact.clearFocus();
        }
        user= FirebaseAuth.getInstance().getCurrentUser();
        adapter=new GlobalUserAdapter(requireContext());
        binding.globalUsers.setAdapter(adapter);

        loadUserData(handler,"");
        setChangeListener();


        binding.searchContact.setOnEditorActionListener((textView, i, keyEvent) -> {
            String text=textView.getText().toString();
            String temp=text.replaceAll( "[ \\s]","" );
            Log.d( "Check text ", "onTextChanged: "+temp );
            if(temp.length()<1){
                return false;
            }


            new Thread(()->{ checkUserData( text );}).start();
            return true;
        });


        return binding.getRoot();
    }

    public void setChangeListener(){
        (binding.searchContact).addTextChangedListener( new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String text= String.valueOf( charSequence );
                String temp=text.replaceAll( "[ \\s]","" );

                if(temp.length()<1){
                    if(adapter!=null){
                        adapter=new GlobalUserAdapter(adapter.getServerUserData(),adapter.getUsers(),requireContext());
                    }else{
                        adapter=new GlobalUserAdapter(null,null,requireContext());

                    } return;
                }

                binding.progressBar.setVisibility(View.VISIBLE);
                if(text!=null&&text.length()>0)
                    loadUserData(handler,text);
                if(!text.contains( "\n" )){
                    return;
                }
                checkUserData( text );


            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        } );

    }

    private void loadUserData(Handler handler, String searchKey) {
        new Thread(()->{
            boolean cancel = false;
            if(searchKey.length()>3) {
                FirebaseFirestore.getInstance().collection("users")
                        .whereGreaterThan("email", searchKey)
                        .whereLessThan("email", searchKey + '\uf8ff')
                        .get().addOnSuccessListener(queryDocumentSnapshots -> {
                            new Thread(() ->{
                                List<ServerUserData> serverUserData=queryDocumentSnapshots.toObjects(ServerUserData.class);
                               /** List<String> userIds=adapter.getIds();

                                for(ServerUserData s : serverUserData){
                                    if(userIds.contains(s.getUserId())){
                                        serverUserData.remove(s);
                                    }
                                }
                                adapter.setServerUserData(serverUserData);
                                */
                               adapter=new GlobalUserAdapter(serverUserData,adapter.getUsers(),requireContext());
                                handler.post(()->{

                                        binding.progressBar.setVisibility(GONE);
                                        binding.globalUsers.setAdapter(adapter);
                            });
                            }).start();
                        }).addOnFailureListener(e -> binding.progressBar.setVisibility(GONE));
            }else{
                cancel=true;
            }
            List<User> userList= OTextDb.getInstance(requireContext()).userDao().searchUser(searchKey, user.getUid());

            boolean finalCancel = cancel;
            handler.post(()->{
                if(adapter==null) {
                    adapter = new GlobalUserAdapter(null,userList, requireContext());
                    binding.globalUsers.setAdapter(adapter);
                }else{
                    adapter=new GlobalUserAdapter(adapter.getServerUserData(),userList,requireContext());

                    binding.globalUsers.setAdapter(adapter);

                }
                if(finalCancel){
                    binding.progressBar.setVisibility(GONE);
                }

            });
        }).start();
    }

    public void getUserData(Task<QuerySnapshot> task){
        new Thread(()->{if(task!=null) {
            List<DocumentSnapshot> list = task.getResult().getDocuments();
            if (list != null&&list.size()>0){
                DocumentSnapshot ref =list.get( 0 );
                if (ref != null) {
                    progress.setFetching();
                    UserDataFetcher fetcher=new UserDataFetcher(requireContext(),true);
                    fetcher.setNavHost(NavHostFragment.findNavController(ContactFragment.this));
                    fetcher.setDialog(progress);
                    fetcher.getUserData( task);


                } else {
                    progress.setFailed((v)-> open());


                }
            }else{
                progress.setFailed((v)-> open());



            }
        }else{
            progress.setFailed((view -> open()));


        }
        }).start();
    }


    private void open(){
        Intent intent=new Intent(Intent.ACTION_SEND );
        intent.putExtra( Intent.EXTRA_TEXT, "join me on oText Messenger. Download app from play store  https://chat.oText.home/");
        intent.setType( "text/plain" );
        startActivity( Intent.createChooser( intent,"Invite to oText" ) );
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult( requestCode, permissions, grantResults );
        System.out.println(requestCode+" "+ Arrays.toString( permissions ) );
        if(requestCode==10){
            if(ActivityCompat.checkSelfPermission( requireContext(),Manifest.permission.READ_CONTACTS )== PackageManager.PERMISSION_GRANTED) {
                empty=false;
                setChangeListener();
            }
        }
    }



  private void checkUserData(String data){
        {
            String  text=data.replaceAll( "[\\s]","" );


            User tem= OTextDb.getInstance( requireContext() ).userDao().getUserByContactDetails( text,text );
            if(tem!=null){
                String userid=tem.getUserId();
                String mode=tem.getUserMode();
                if(mode.equals( Database.User.MODE_PUBLIC )) {
                    Bundle intent=new Bundle();
                    intent.putString( "User",tem.getUserId());
                    handler.post(()-> NavHostFragment.findNavController(ContactFragment.this)
                            .navigate(R.id.action_contactFragment_to_chatFragment,intent));


                }else{
                    Intent intent = new Intent( requireContext(), PrivateActivity.class );
                    intent.putExtra( "start","start" );
                    intent.putExtra( "User", userid );
                    startActivity( intent );
                }
            }else{

                CollectionReference col = FirebaseFirestore.getInstance().collection( "users" );
                handler.post(()->{
                    progress = new SearchUserDialog(requireContext(),handler);
                    progress.create();
                    progress.show();
                    progress.start();

                });



                if (text.indexOf( "@" ) > 0) {
                    col.whereEqualTo( Database.Server.EMAIL, text ).get().addOnCompleteListener(
                            this::getUserData );
                } else {

                    col.whereEqualTo( Database.Server.PHONE, text ).get()
                            .addOnCompleteListener( this::getUserData );
                }


            }
        }
    }

    private class GlobalUserAdapter extends RecyclerView.Adapter<GlobalUserAdapter.ViewHolder> {
        private final Context context;
        private List<ServerUserData> serverUserData;
        private List<User> userData;


        public GlobalUserAdapter(Context requireContext) {
            this.context=requireContext;
            if(users==null)
            this.users=new ArrayList();

        }

        public GlobalUserAdapter(List<ServerUserData> serverUserData, List<User> users, Context requireContext) {
            this.context=requireContext;
            this.userData=users;
            this.serverUserData=serverUserData;
            this.users=new ArrayList();
            if (serverUserData!=null)
                this.users.addAll(serverUserData);
            if(userData!=null)
                this.users.addAll(userData);
            handler.post(this::notifyDataSetChanged);

        }



        private List users;



        @NonNull
        @Override
        public GlobalUserAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new GlobalUserAdapter.ViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.conversation_layout,parent,false));
        }

        @Override
        public void onBindViewHolder(@NonNull GlobalUserAdapter.ViewHolder holder, int position) {
              Object object;
            if (position < users.size() - 1) {
                object = users.get(position);
            }else {
               object=users.get(users.size()-1);
            }
            if(object==null)return;
                if (object instanceof ServerUserData) {
                    ServerUserData user = (ServerUserData) object;
                    holder.getName().setText(user.getUserName());
                    FirebaseStorage.getInstance().getReference("profile/"+user.getProfile())
                            .getDownloadUrl().addOnSuccessListener(uri -> Glide.with(context).load(uri)
                                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                                    .fitCenter()
                                    .skipMemoryCache(true)
                                    .into(holder.getPic()));

                    if (user.getEmail() == null) {
                        holder.getMobile().setText(user.getPhone());
                    } else {
                        holder.getMobile().setText(user.getEmail());
                    }
                    holder.getFrom().setText("oText User");
                }  else if (object instanceof User) {
                    User user = (User) object;
                    holder.getName().setText(user.getUserName());
                    Glide.with(context).load(user.getProfilePicture())
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .fitCenter()
                            .skipMemoryCache(true)
                            .into(holder.getPic());
                    if (user.getEmail() == null) {
                        holder.getMobile().setText(user.getPhoneNo());
                    } else {
                        holder.getMobile().setText(user.getEmail());
                    }
                    holder.getFrom().setText("Friend");


                }
                holder.getRoot().setOnClickListener((v) -> {
                    String id = (String) ((TextView) v.findViewById(R.id.last_msg)).getText();

                    new Thread(() -> checkUserData(id)).start();
                });

        }



        @Override
        public int getItemCount() {
            if(users==null)
                return 0;

            return users.size();
        }




        public List<User> getUsers() {
            return userData;

        }


        public List<ServerUserData> getServerUserData() {
            return serverUserData;
        }

        class ViewHolder extends RecyclerView.ViewHolder{
            private final ImageView pic;
            private final TextView name;
            private final TextView mobile;
            private final Button Allow;
            private final TextView from;

            public Button getAllow() {
                return Allow;
            }

            public TextView getContactMsg() {
                return contactMsg;
            }

            private final TextView contactMsg;

            public View getRoot() {
                return root;
            }

            private final View root;

            public ViewHolder(@NonNull View itemView) {
                super( itemView );

                pic = itemView.findViewById(R.id.conversation_pic);
                mobile = itemView.findViewById(R.id.last_msg);
                name = itemView.findViewById(R.id.conversation_name);
                from = itemView.findViewById(R.id.last_msg_date);
                root = itemView;
                Allow = null;
                contactMsg = null;
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

            public TextView getFrom() {
                return from;
            }
        }
    }

}
