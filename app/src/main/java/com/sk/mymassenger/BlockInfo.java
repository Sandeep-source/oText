package com.sk.mymassenger;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sk.mymassenger.db.Database;
import com.sk.mymassenger.db.OTextDb;
import com.sk.mymassenger.db.block.Block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class BlockInfo extends Worker {
    private final String userId;


    public BlockInfo(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super( context, workerParams );
        userId=getInputData().getString( "UserId" );
    }

    @NonNull
    @Override
    public Result doWork() {
        FirebaseFirestore.getInstance().document( "blockedUsers/blockList"+userId ).get().addOnSuccessListener( new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if(documentSnapshot!=null&&documentSnapshot.exists()) {
                    new Thread(()->{
                    Map<String, Object> data = documentSnapshot.getData();
                    ArrayList<String> set=new ArrayList<>(data.keySet());
                    if(set.size()>0) {
                        for (String id : set) {
                            Boolean by_me=((HashMap<String,Boolean>)data.get( id )).get( Database.Block.BY_ME );

                            Boolean by_other=((HashMap<String,Boolean>)data.get( id )).get( Database.Block.BY_OTHER );
                            if(by_me!=null&&by_me) {
                               /* Cursor cur = readable.query( Database.Block.TBL_BLOCK, null,
                                        Database.Block.MUSER_ID + " =? AND " + Database.Block.OUSER_ID + " = ? AND "+
                                                Database.Block.TYPE+" = ? "
                                        , new String[]{userId, id, Database.Block.BY_ME}, null, null, null );

                                */
                                Block cur=OTextDb.getInstance( getApplicationContext() ).blockDao().getBlockByMode( userId,id, Database.Block.BY_ME );
                                if (cur==null) {
                                   /* ContentValues values = new ContentValues();
                                    values.put( Database.Block.MUSER_ID, userId );
                                    values.put( Database.Block.OUSER_ID, id );
                                   values.put( Database.Block.TYPE, Database.Block.BY_ME );

                                    */
                                    Block block=new Block();
                                    block.setMUserId( userId );
                                    block.setOUserId( id );
                                    block.setType( Database.Block.BY_ME );
                                    OTextDb.getInstance( getApplicationContext() ).blockDao().insert( block );
                                  // writable.insert( Database.Block.TBL_BLOCK, null, values );
                                }
                            }else{
                               /* Cursor cur = readable.query( Database.Block.TBL_BLOCK, null,
                                        Database.Block.MUSER_ID + " =? AND " + Database.Block.OUSER_ID + " = ? AND "+
                                                Database.Block.TYPE+" = ? "
                                        , new String[]{userId, id, Database.Block.BY_ME}, null, null, null );

                                */
                                Block cur=OTextDb.getInstance( getApplicationContext() ).blockDao().getBlockByMode( userId,id, Database.Block.BY_ME );

                                if(cur!=null){
                                   /* writable.delete( Database.Block.TBL_BLOCK,
                                            Database.Block.MUSER_ID + " =? AND " + Database.Block.OUSER_ID + " = ? AND "+
                                                    Database.Block.TYPE+" = ? "
                                            , new String[]{userId, id, Database.Block.BY_ME});

                                    */

                                    OTextDb.getInstance( getApplicationContext() ).blockDao().delete( userId,id, Database.Block.BY_ME );
                                }

                            }
                            if(by_other!=null&&by_other){
                               /* Cursor cur = readable.query( Database.Block.TBL_BLOCK, null,
                                        Database.Block.MUSER_ID + " =? AND " + Database.Block.OUSER_ID + " = ? AND "+
                                                Database.Block.TYPE+" = ? "
                                        , new String[]{userId, id, Database.Block.BY_OTHER}, null, null, null );

                                */
                                Block cur=OTextDb.getInstance( getApplicationContext() ).blockDao().getBlockByMode( userId,id, Database.Block.BY_OTHER );

                                if (cur==null) {
                                   /* ContentValues values = new ContentValues();
                                    values.put( Database.Block.MUSER_ID, userId );
                                    values.put( Database.Block.OUSER_ID, id );
                                    values.put( Database.Block.TYPE, Database.Block.BY_OTHER );


                                    */

                                    Block block=new Block();
                                    block.setMUserId( userId );
                                    block.setOUserId( id );
                                    block.setType( Database.Block.BY_OTHER );
                                    OTextDb.getInstance( getApplicationContext() ).blockDao().insert( block );
                                   // writable.insert( Database.Block.TBL_BLOCK, null, values );
                                }
                            }else{
                               /* Cursor cur = readable.query( Database.Block.TBL_BLOCK, null,
                                        Database.Block.MUSER_ID + " =? AND " + Database.Block.OUSER_ID + " = ? AND "+
                                                Database.Block.TYPE+" = ? "
                                        , new String[]{userId, id, Database.Block.BY_OTHER}, null, null, null );


                                */
                                Block cur=OTextDb.getInstance( getApplicationContext() ).blockDao().getBlockByMode( userId,id, Database.Block.BY_OTHER );

                                if(cur!=null){
                                   /* writable.delete( Database.Block.TBL_BLOCK,
                                            Database.Block.MUSER_ID + " =? AND " + Database.Block.OUSER_ID + " = ? AND "+
                                                    Database.Block.TYPE+" = ? "
                                            , new String[]{userId, id, Database.Block.BY_OTHER});

                                    */

                                    OTextDb.getInstance( getApplicationContext() ).blockDao().delete(userId,id, Database.Block.BY_OTHER );
                                }

                            }
                        }
                    }else{
                        OTextDb.getInstance( getApplicationContext() ).blockDao().deleteAll();

                    }
                 }).start();
                }else{
                    new Thread(()->
                    OTextDb.getInstance(getApplicationContext()).blockDao().deleteAll()).start();
                }
            }
        } );
        return Result.success();
    }
    private String makeQ(int num){
        StringBuilder str= new StringBuilder( "(?" );
        while (num-->1){
            str.append( ",?" );
        }
        str.append( ")" );
        return str.toString();
    }
}
