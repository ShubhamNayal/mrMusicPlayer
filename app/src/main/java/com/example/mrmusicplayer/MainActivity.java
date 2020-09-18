package com.example.mrmusicplayer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.net.URI;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
private boolean checkPermisson=false;
    Uri uri;//uniform resource identifier
    String songName,songUrl;
    ListView listView;
    ArrayList<String>arrayListSongName=new ArrayList<>();
    ArrayList<String>arrayListSongUrl=new ArrayList<>();
    ArrayAdapter<String>arrayAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView=findViewById(R.id.listView);
        retriveSong();
    }

    private void retriveSong() {
        DatabaseReference databaseReference=FirebaseDatabase.getInstance().getReference("Songs");

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                for(DataSnapshot ds: snapshot.getChildren()){
                    Song songObj=ds.getValue(Song.class);
                    arrayListSongName.add(songObj.getSongName());
                    arrayListSongUrl.add(songObj.getSongUrl());


                }
                arrayAdapter=new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_list_item_1,arrayListSongName);
                listView.setAdapter(arrayAdapter);


            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.custom_menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId()==R.id.nav_upload){
            if(validatePermission()){
                pickSong();
            }

        }
        return super.onOptionsItemSelected(item);
    }

    private void pickSong() {
        Intent upload_intent=new Intent();
        upload_intent.setType("audio/*");
        upload_intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(upload_intent,1);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode==1){
            if(requestCode==RESULT_OK){
             uri=data.getData();

                Cursor mcursor=getApplicationContext().getContentResolver().query(uri,null,null,null,null);
                int indexName=mcursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                mcursor.moveToFirst();
                songName=mcursor.getString(indexName);
                mcursor.close();

                uploadSongToFireBase();



            }
        }


    }

    private void uploadSongToFireBase() {
        StorageReference storageReference= FirebaseStorage.getInstance().getReference()
                .child("Songs").child(uri.getLastPathSegment());

        final ProgressDialog progressDialog=new ProgressDialog(this);
        progressDialog.show();

        storageReference.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot)
            {
                Task<Uri>uriTask=taskSnapshot.getStorage().getDownloadUrl();
                while (!uriTask.isComplete());
                Uri UrlSong=uriTask.getResult();

                songUrl=songUrl.toString();

                uploadDetailsToDatabase();
                progressDialog.dismiss();




            }


        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e)
            {
                Toast.makeText(MainActivity.this,e.getMessage(),Toast.LENGTH_LONG).show();
                progressDialog.dismiss();
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>()
        {
            @Override
            public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                double progress=(100*snapshot.getBytesTransferred())/snapshot.getTotalByteCount();
                int currentProgress=(int)progress;
                progressDialog.setMessage("Uploaded: "+currentProgress+"%");
            }
        });



    }
    private void uploadDetailsToDatabase()
    {
        Song songObj=new Song(songName,songUrl);

        FirebaseDatabase.getInstance().getReference("Songs")
                .push().setValue(songObj).addOnCompleteListener(new OnCompleteListener<Void>()
        {
            @Override
            public void onComplete(@NonNull Task<Void> task)
            {
                if(task.isSuccessful())
                {Toast.makeText(MainActivity.this,"Song Uploaded",Toast.LENGTH_LONG).show();

                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this,"Song Uploaded",Toast.LENGTH_LONG).show();
            }
        });


    }

    private boolean validatePermission(){
        Dexter.withActivity(MainActivity.this)
                .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                        checkPermisson=true;
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                    checkPermisson=false;
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                    permissionToken.continuePermissionRequest();
                    }
                }).check();
            return checkPermisson;
    }

}