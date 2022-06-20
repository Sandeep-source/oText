package com.sk.mymassenger;

import android.Manifest;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.sk.mymassenger.db.OTextDb;
import com.sk.mymassenger.db.user.User;

import java.util.Arrays;
import java.util.List;

public class ContactActivity extends AppCompatActivity {

    FirebaseUser user;
    RecyclerView conatact_list;
    private AlertDialog progress;
    private EditText editText;
    private boolean empty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_contact );
       findViewById( R.id.contact_back_btn ).setOnClickListener( (view)-> super.onBackPressed() );
        conatact_list=findViewById( R.id.contact_list );
        editText= findViewById( R.id.search_contact);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            editText.clearFocus();
        }
        user= FirebaseAuth.getInstance().getCurrentUser();

        LinearLayoutManager layoutManager=new LinearLayoutManager( getApplicationContext() );

        conatact_list.setLayoutManager( layoutManager );
        if(ActivityCompat.checkSelfPermission( getApplicationContext(),Manifest.permission.READ_CONTACTS )== PackageManager.PERMISSION_GRANTED){
            new Handler( getMainLooper() ).post( ()->
            conatact_list.setAdapter( new ContactAdapter(  getContentResolver().query( ContactsContract.CommonDataKinds.Phone.CONTENT_URI,null,null,null,null ),
                    false)));


        }else{
            empty=true;
            conatact_list.setAdapter( new ContactAdapter(null,true) );
        }
        (editText).addTextChangedListener( new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String text= String.valueOf( charSequence );
                if(!empty) {
                    conatact_list.setAdapter( new ContactAdapter( getContentResolver().query( ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                                    ContactsContract.CommonDataKinds.Phone.PHOTO_URI}, ContactsContract.CommonDataKinds.Phone.NUMBER + " LIKE ? OR " +
                                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY + " LIKE ? ", new String[]{"%" + text + "%", "%" + text + "%"}, null ), false ) );


                }
                if(!text.contains( "\n" )){
                    return;
                }
                String temp=text.replaceAll( "[ \\s]","" );
                Log.d( "Check text ", "onTextChanged: "+temp );
                if(temp.length()<1){
                    return;
                }
                checkUserData( text );
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        } );


    }

    public void getUserData(Task<QuerySnapshot> task){
        if(task!=null) {
            List<DocumentSnapshot> list = task.getResult().getDocuments();
            if (list != null&&list.size()>0){
                DocumentSnapshot ref =list.get( 0 );
            if (ref != null) {
                new UserDataFetcher(getApplicationContext(),true).getUserData( task);
                progress.cancel();

            } else {
                progress.cancel();
                AlertDialog alertDialog=buildDialog();
                alertDialog.show();


            }
        }else{
                progress.cancel();
                AlertDialog alertDialog=buildDialog();

                    alertDialog.show();



            }
        }else{
            progress.cancel();
            AlertDialog alertDialog=buildDialog();
            alertDialog.show();


        }
    }
    private AlertDialog buildDialog(){
        return new AlertDialog.Builder( new ContextThemeWrapper(ContactActivity.this,R.style.AppTheme  ) )
                .setNegativeButton( "Cancel" ,null)
                .setCancelable( true ).setMessage( "This user is not on our platform. Invite him to join you" )
                .setPositiveButton( "Invite", (dialogInterface, i) -> {
                    Intent intent=new Intent(Intent.ACTION_SEND );
                    intent.putExtra( Intent.EXTRA_TEXT, "join me on oText Messanger. Download app from play store  https://www.oText.com");
                    intent.setType( "text/plain" );
                    startActivity( Intent.createChooser( intent,"Invite to oText" ) );

                } ).create();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult( requestCode, permissions, grantResults );
        System.out.println(requestCode+" "+ Arrays.toString( permissions ) );
        if(requestCode==10){
            if(grantResults[0]==0) {
                empty=false;
                new Handler( getMainLooper() ).post( ()-> conatact_list.setAdapter( new ContactAdapter( getContentResolver().query( ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null ), false ) ) );
                (editText).addTextChangedListener( new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        String text= String.valueOf( charSequence );
                        if(!empty)
                        conatact_list.setAdapter( new ContactAdapter(  getContentResolver().query( ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                                        ContactsContract.CommonDataKinds.Phone.PHOTO_URI}, ContactsContract.CommonDataKinds.Phone.NUMBER+" LIKE ? OR "+
                                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY+" LIKE ? ",new String[]{"%"+text+"%","%"+text+"%"},null ), false ));

                        String temp=text.replaceAll( "[ \\s]","" );
                        if(!text.contains( "\n" )){
                            return;
                        }
                        if(temp.length()<1){
                            return;
                        }
                        checkUserData( text );
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {

                    }
                } );

            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult( requestCode, resultCode, data );


    }

    private  class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.MViewholder>{
        private Cursor data;
        private int count=0;
        private boolean empty=false;
        public ContactAdapter(Cursor data, boolean empty) {
            if(!empty){
            this.data=data;
            if(data!=null){
                count=data.getCount();
            }}else {
                count=1;
                this.empty=empty;
            }
        }

        @NonNull
        @Override
        public MViewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if(empty){
               return new MViewholder( LayoutInflater.from( parent.getContext() ).inflate( R.layout.empty_conversation,parent,false ) );
            }
            View view= LayoutInflater.from( parent.getContext() ).inflate(R.layout.conversation_layout,parent,false);

            return new MViewholder( view );
        }

        @Override
        public void onBindViewHolder(@NonNull MViewholder holder, int position) {
            if(!empty) {
                if (!data.moveToPosition( position )) {
                    return;
                }

                    holder.getMobile().setText( data.getString( data.getColumnIndex( ContactsContract.CommonDataKinds.Phone.NUMBER ) ) );
                    holder.getName().setText( data.getString( data.getColumnIndex( ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY ) ) );
                    String uri = data.getString( data.getColumnIndex( ContactsContract.CommonDataKinds.Phone.PHOTO_URI ) );
                    if(uri!=null){
                        holder.getPic().setImageURI( Uri.parse( uri ) );
                    } else
                    holder.getPic().setImageURI( null );


                holder.getRoot().setOnClickListener( view -> {
                    String id = (String) ((TextView) view.findViewById( R.id.last_msg )).getText();

                    checkUserData( id );

                } );
            }else{
                holder.getContactMsg().setText( "Allow contacts to get suggestion to choose from" );
                holder.getAllow().setText( "Allow" );
                holder.getAllow().setOnClickListener( (v)->{
                    ActivityCompat.requestPermissions( ContactActivity.this,new String[]{Manifest.permission.READ_CONTACTS},10 );
                } );
            }
        }

        @Override
        public int getItemCount() {
            return count;
        }
         private class MViewholder extends RecyclerView.ViewHolder {
            private final ImageView pic;
            private final TextView name;
            private final TextView mobile;
             private final Button Allow;

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

            public MViewholder(@NonNull View itemView) {
                super( itemView );
                if(!empty) {
                    pic = itemView.findViewById( R.id.conversation_pic );
                    mobile = itemView.findViewById( R.id.last_msg );
                    name = itemView.findViewById( R.id.conversation_name );
                    root = itemView;
                    Allow=null;
                    contactMsg=null;
                }else{
                    pic =null;
                    mobile = null;
                    name = null;
                    root = null;
                    Allow=itemView.findViewById( R.id.start_new );
                    contactMsg=itemView.findViewById( R.id.msg_contact );                }


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
    }


    private void checkUserData(String data){
        {
            String  text=data.replaceAll( "[\\s]","" );

           /* Cursor tem=new oTextDb( getApplicationContext(),Database.O_TEXT_DB,null,Database.DB_VERSION ).getReadableDatabase()
                    .query( Database.User.TBL_USER,new String[]{Database.User.USER_ID, Database.User.USER_MODE},
                            Database.User.EMAIL+" = ? OR "+ Database.User.PHONE_NO+" = ?",
                            new String[]{text,text},null,null,null
                    );*/
            User tem= OTextDb.getInstance( getApplicationContext() ).userDao().getUserByContactDetails( text,text );
            if(tem!=null){
                String userid=tem.getUserId();
                String mode=tem.getUserMode();
                if(mode.equals( Database.User.MODE_PUBLIC )) {
                    Intent intent = new Intent( getApplicationContext(), ConversationActivity.class );
                    intent.putExtra( "User", userid );
                    startActivity( intent );
                }else{
                    Intent intent = new Intent( getApplicationContext(), PrivateActivity.class );
                    intent.putExtra( "start","start" );
                    intent.putExtra( "User", userid );
                    startActivity( intent );
                }
            }else{

                CollectionReference col = FirebaseFirestore.getInstance().collection( "users" );

                progress = new AlertDialog.Builder( new ContextThemeWrapper(ContactActivity.this,R.style.AppTheme) ).setView( R.layout.spinner ).create();
                progress.show();


                if (text.indexOf( "@" ) > 0) {
                    col.whereEqualTo( Database.Server.EMAIL, text ).get().addOnCompleteListener( this::getUserData );
                } else {

                    col.whereEqualTo( Database.Server.PHONE, text ).get()
                            .addOnCompleteListener( this::getUserData );
                }


            }
        }
    }

}