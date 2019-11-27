package com.example.delsa.fragment;


import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.delsa.POJO.Bencana;
import com.example.delsa.R;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import static android.app.Activity.RESULT_OK;

/**
 * A simple {@link Fragment} subclass.
 */
public class TambahFragment extends Fragment implements View.OnClickListener {

    private Button btnTambahBencana;
    private ImageView ivFotoBencana;
    private EditText etJudulBencana, etAlamatBencana, etDeskripsiBencana;
    private Spinner spnKategoriBencana;
    private Dialog dialog;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    private byte[] dataFoto;
    private FirebaseDatabase firebaseDatabase;
    private FirebaseAuth auth;
    private StorageReference photoDataDiriRef;
    private DatabaseReference accountReference;
    private String judulBencana, alamatBencana, deskripsiBencana, kategoriBencana;

    public TambahFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_tambah, container, false);
        btnTambahBencana = view.findViewById(R.id.btn_tambahBencana);
        ivFotoBencana = view.findViewById(R.id.iv_fotoBencana);
        etJudulBencana = view.findViewById(R.id.et_judulBencana);
        etAlamatBencana = view.findViewById(R.id.et_alamatBencana);
        etDeskripsiBencana = view.findViewById(R.id.et_deskripsiBencana);
        spnKategoriBencana = view.findViewById(R.id.spn_kategoriBencana);
        btnTambahBencana.setOnClickListener(this);
        ivFotoBencana.setOnClickListener(this);

        auth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        accountReference = FirebaseDatabase.getInstance().getReference().child("Users").child(auth.getUid());
        return view;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_tambahBencana:
                openDialogTambahBencana();
                break;
            case R.id.btn_cekkembalitambahbencana:
                dialog.dismiss();
                break;
            case R.id.btn_yatambahbencana:
                dialog.dismiss();
                tambahBencana();
                break;
            case R.id.iv_fotoBencana:
                dispatchTakePictureIntent();
                break;
        }
    }

    private void tambahBencana() {
        judulBencana = etJudulBencana.getText().toString();
        alamatBencana = etAlamatBencana.getText().toString();
        deskripsiBencana = etDeskripsiBencana.getText().toString();
        kategoriBencana = spnKategoriBencana.getSelectedItem().toString();
        if (judulBencana.isEmpty() && alamatBencana.isEmpty() && deskripsiBencana.isEmpty() &&
                spnKategoriBencana.getSelectedItemPosition() == 0) {
            Toast.makeText(getContext(), "Lengkapi data diatas", Toast.LENGTH_SHORT).show();
        } else {
            storePhotoIdentity(dataFoto);
        }
    }

    private void openDialogTambahBencana() {
        dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.dialog_konfirmasi_bencana);
        Button btn_cekkembalitambahbencana = dialog.findViewById(R.id.btn_cekkembalitambahbencana);
        Button btn_yatambahbencana = dialog.findViewById(R.id.btn_yatambahbencana);
        btn_cekkembalitambahbencana.setOnClickListener(this);
        btn_yatambahbencana.setOnClickListener(this);
        dialog.show();
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            ivFotoBencana.setImageBitmap(imageBitmap);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            dataFoto = baos.toByteArray();

            btnTambahBencana.setEnabled(true);
        }
    }

    private String getKey(){
        String key = FirebaseDatabase.getInstance().getReference().child("Bencana").push().getKey();
        return key;
    }

    private void storePhotoIdentity(byte[] dataFoto) {
        String uid = auth.getUid();

        photoDataDiriRef = FirebaseStorage.getInstance().getReference().child("images").child("photo_bencana").child(getKey() + ".jpg");
        UploadTask uploadTask = photoDataDiriRef.putBytes(dataFoto);

        Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }

                // Continue with the task to get the download URL
                return photoDataDiriRef.getDownloadUrl();
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful()) {
                    Uri downloadUri = task.getResult();
                    String url_photo = downloadUri.toString();

                    tambahBencanaKeDatabase(judulBencana, alamatBencana, deskripsiBencana, kategoriBencana, url_photo);

                }
            }
        });
    }

    private void tambahBencanaKeDatabase(String judulBencana, String alamatBencana, String deskripsiBencana, String kategoriBencana, String fotobencana) {
        DatabaseReference myRef = firebaseDatabase.getReference("Bencana").child(getKey());
        Bencana bencana = new Bencana(kategoriBencana, judulBencana, alamatBencana, deskripsiBencana, fotobencana);
        myRef.setValue(bencana);
    }

    @Override
    public void onStart() {
        final Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.dialog_peringatan_tambah_bencana);
        Button dialogButton = dialog.findViewById(R.id.btn_sayamengerti);
        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
        super.onStart();
    }
}
