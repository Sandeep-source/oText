package com.sk.mymassenger.auth;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.navigation.NavController;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.sk.mymassenger.R;
import com.sk.mymassenger.data.ServerUserData;
import com.sk.mymassenger.db.Database;
import com.sk.mymassenger.db.OTextDb;
import com.sk.mymassenger.db.user.User;
import com.sk.mymassenger.dialog.SearchUserDialog;

import java.io.File;
import java.util.List;

public class UserDataFetcher {
    private static final String TAG = "UserDataFetcher";
    private final boolean start;
    FirebaseUser user;
    private String UserName;
    private SearchUserDialog dialog;
    private NavController navController;

    public UserDataFetcher(Context context, boolean b) {
        this.context = context;
        start=b;
        user= FirebaseAuth.getInstance().getCurrentUser();
    }

    private Context context;
    public String getUserData(Task<QuerySnapshot> task){
        if(task!=null) {
            List<DocumentSnapshot> documentSnapshots = task.getResult().getDocuments();
            if(documentSnapshots!=null&&documentSnapshots.size()>0)
            return getUserData(documentSnapshots.get(0));
        }
        return "";

    }
    public String getUserData(DocumentSnapshot snapshot){
        if(snapshot!=null){



                ServerUserData ref = snapshot.toObject(ServerUserData.class);
                if (ref != null) {

                    User userData=new User();

                    userData.setEmail( ref.getEmail() );
                    userData.setPhoneNo( ref.getPhone() );
                    userData.setUserMode( Database.User.MODE_PUBLIC );
                    userData.setUserId(ref.getUserId() );
                    userData.setMoodSince(ref.getMood().getSince());
                    userData.setMood(ref.getMood().getValue());
                    userData.setLastUpdated(ref.getChanged().getLastUpdated());
                    userData.setUserName( ref.getUserName());
                    if (user.getUid().equals(ref.getUserId())) {

                        userData.setType( Database.User.TYPE_SELF );
                    } else {

                        userData.setType(  Database.User.TYPE_FRIEND +"  OF "+user.getUid());
                    }

                    if (OTextDb.getInstance( context ).userDao().getUser( ref.getUserId() ) == null) {
                        OTextDb.getInstance( context ).userDao().insertAll( userData );
                        Log.d(TAG, "getUserData: inserted to local db");
                    }
                    File file = new File( context.getFilesDir(), "PFL" + ref.getUserId() + ".png" );
                    FirebaseStorage.getInstance().getReference( "profiles/" + ref.getUserId() + ".png" )
                            .getFile( file ).addOnSuccessListener( taskSnapshot -> new Thread(()->{
                       User userDat=OTextDb.getInstance(context).userDao().getUser(ref.getUserId());
                       userDat.setProfilePicture( file.getAbsolutePath() );

                          OTextDb.getInstance( context ).userDao().insertAll( userDat );
                       Log.d(TAG, "getUserData: inserted with image to local db");

                       if (start) {
                           dialog.setFetching();
                           new Handler(context.getMainLooper()).postDelayed(() -> {

                               Bundle intent=new Bundle();
                               intent.putString( "User",userDat.getUserId() );



                               if(dialog!=null){
                                   dialog.cancel();
                               }
                                       navController
                                               .navigate(R.id.action_contactFragment_to_chatFragment,intent);



                                   }, 4000
                           );

                       }else{
                           if(dialog!=null){
                               dialog.cancel();
                           }
                       }
                   }).start()
                    );

                } else {
                    if(dialog!=null){
                        dialog.cancel();
                    }
                    Toast.makeText( context.getApplicationContext(), "User not Found", Toast.LENGTH_SHORT ).show();
                }

        }else{
            if(dialog!=null){
                dialog.cancel();
            }
            Toast.makeText( context.getApplicationContext(),"User not Found",Toast.LENGTH_SHORT ).show();
        }
        if(dialog!=null){
            dialog.cancel();
        }
        return UserName;
    }

    public void setDialog(SearchUserDialog progress) {
        this.dialog=progress;
    }

    public void setNavHost(NavController navController) {
        this.navController=navController;
    }
}
