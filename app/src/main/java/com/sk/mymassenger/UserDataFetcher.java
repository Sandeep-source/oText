package com.sk.mymassenger;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.sk.mymassenger.db.OTextDb;
import com.sk.mymassenger.db.user.User;

import java.io.File;
import java.util.List;

public class UserDataFetcher {
    private final boolean start;
    FirebaseUser user;
    private String UserName;

    public UserDataFetcher(Context context, boolean b) {
        this.context = context;
        start=b;
        user= FirebaseAuth.getInstance().getCurrentUser();
    }

    private Context context;
    public String getUserData(Task<QuerySnapshot> task){
        if(task!=null){
            List<DocumentSnapshot> ldoc=task.getResult().getDocuments();
            if(ldoc!=null&&ldoc.size()>0) {
                DocumentSnapshot ref = ldoc.get( 0 );
                if (ref != null) {
                    String User_id = ref.getString( Database.Server.USER_ID );
                    UserName = ref.getString( Database.Server.USER_NAME );
                    String Email = ref.getString( Database.Server.EMAIL );
                    String Phone = ref.getString( Database.Server.PHONE );
                    String uri = ref.getString( Database.Server.PROFILE );
                    User userData=new User();
                    userData.setEmail( Email );
                    userData.setPhoneNo( Phone );
                    userData.setUserMode( Database.User.MODE_PUBLIC );
                    userData.setUserId( User_id );
                    userData.setUserName( UserName );
                    userData.setType( Database.User.TYPE_SELF );
                   /* ContentValues contentValues = new ContentValues();
                    contentValues.put( Database.User.USER_ID, User_id );
                    contentValues.put( Database.User.USER_NAME, UserName );
                    contentValues.put( Database.User.EMAIL, Email );
                    contentValues.put( Database.User.PHONE_NO, Phone );
                    contentValues.put( Database.User.USER_MODE, Database.User.MODE_PUBLIC );
                    Intent intent = new Intent( context, ConversationActivity.class );
                    intent.putExtra( "User", User_id );
                    oTextDb db = new oTextDb( context.getApplicationContext(), Database.O_TEXT_DB, null, Database.DB_VERSION );

                    */
                    if (OTextDb.getInstance( context ).userDao().getUser( User_id ) == null) {
                        OTextDb.getInstance( context ).userDao().insertAll( userData );
                        Toast.makeText( context.getApplicationContext(), "inserted to local db", Toast.LENGTH_SHORT ).show();
                       /* Intent intentbroad = new Intent( Database.DB_DATA_CHANGED );
                        intentbroad.putExtra( "User", User_id );
                        intent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
                        context.sendBroadcast( intentbroad );

                        */


                    }
                    File file = new File( context.getFilesDir(), "PFL" + User_id + ".png" );
                    FirebaseStorage.getInstance().getReference( "profiles/" + User_id + ".png" ).getFile( file ).addOnSuccessListener( taskSnapshot -> {
                       // contentValues.put( Database.Server.PROFILE, file.getAbsolutePath() );
                        userData.setProfilePicture( file.getAbsolutePath() );
                        if (user.getUid() == User_id) {
                           // contentValues.put( Database.User.TYPE, Database.User.TYPE_SELF );
                            userData.setType( Database.User.TYPE_SELF );
                        } else {
                            //contentValues.put( Database.User.TYPE, Database.User.TYPE_FRIEND +"  OF "+user.getUid());
                            userData.setType(  Database.User.TYPE_FRIEND +"  OF "+user.getUid());
                        }
                        //if (OTextDb.getInstance( context ).userDao().getUser( User_id ) == null) {
                            //db.insert( Database.User.TBL_USER, contentValues );
                            OTextDb.getInstance( context ).userDao().insertAll( userData );
                            Toast.makeText( context.getApplicationContext(), "inserted with image to local db", Toast.LENGTH_SHORT ).show();
                           /* Intent intentbroad = new Intent( Database.DB_DATA_CHANGED );
                            intentbroad.putExtra( "User", User_id );
                            intent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
                            context.sendBroadcast( intentbroad );

                            */


                       // } else {
                           // db.getWritableDatabase().update( Database.User.TBL_USER, contentValues, Database.User.USER_ID + " = ?", new String[]{User_id} );
                           // Toast.makeText( context.getApplicationContext(), "updated with image to local db", Toast.LENGTH_SHORT ).show();

                           // OTextDb.getInstance( context ).userDao().update( userData );
                       // }
                        if (start) {
                            Intent intent=new Intent(context,ConversationActivity.class);
                            intent.putExtra( "User",User_id );
                            intent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
                            context.startActivity( intent );
                        }
                    } );

                } else {
                    Toast.makeText( context.getApplicationContext(), "User not Found", Toast.LENGTH_SHORT ).show();
                }
            }
        }else{
            Toast.makeText( context.getApplicationContext(),"User not Found",Toast.LENGTH_SHORT ).show();
        }
        return UserName;
    }
}
