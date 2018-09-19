package com.ejemplo.simplecamera;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    //CAMERA_REQUEST es un código de solicitud, una guía para que lo identifiques al momento de recibir la respuesta
    private static final int CAMERA_REQUEST = 1888;
    private ImageView imgFoto;
    private Button btnTomarFoto;
    private Button btnCompartir;
    String mRutaFotoActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Se llaman a las view's del layout
        imgFoto = (ImageView) findViewById(R.id.imgFoto);
        btnTomarFoto = (Button) findViewById(R.id.btnTomarFoto);
        btnCompartir = (Button) findViewById(R.id.btnCompartir);
        //Se asigna un método setOnClickListener() para el botón TomarFoto
        btnTomarFoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PermisoEscrituraAlmacenamiento()) {
                    IntencionTomarFoto();
                } else {
                    requestStoragePermission();
                }
            }
        });
        //Se asigna un método setOnClickListener() para el botón Compartir
        btnCompartir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                compartirImagen();
            }
        });

    }

//    private void IntencionTomarFoto() {
//        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
//            startActivityForResult(takePictureIntent, CAMERA_REQUEST);
//        }
//    }

    private boolean PermisoEscrituraAlmacenamiento() {
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        boolean exito = false;
        if (result == PackageManager.PERMISSION_GRANTED)
            exito = true;
        return exito;
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
    }
    //Luego que el usuario responde (ACEPTAR O DENEGAR) el sistema INVOCA a este método
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 0) {
            //Si la respuesta fue cancelada el param "grantResults" es vacío
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, "Permiso ACEPTADO, ahora usted puede escribir el almacenamiento", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Permiso DENEGADO, usted no tiene acceso al almacenamiento", Toast.LENGTH_SHORT).show();

            }
        }
    }

    private void IntencionTomarFoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Existe una actividad de cámara para manejar la intención
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Crea un nuevo archivo con la foto

            File photoFile = null;
            try {
                photoFile = crearArchivoImagen();
                //Mas código....
            } catch (IOException ex) {
                // Error occurred while creating the File

            }
            // Si el archivo fue creado con éxito
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.ejemplo.simplecamera.provider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, CAMERA_REQUEST);
            }
        }
    }

    //Se recibe el resultado de la actividad lanzada en el intent luego de la toma de la foto
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        grabarFotoGaleria();
        mostrarFoto();

    }

    private File crearArchivoImagen() throws IOException {
        // Crea un archivo de imagen
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Grabar el archivo: path for use with ACTION_VIEW intents
        mRutaFotoActual = image.getAbsolutePath();
        return image;
    }

    private void grabarFotoGaleria() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);

        File f = new File(mRutaFotoActual);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);

    }

    private void mostrarFoto() {
        // Se obtiene las dimensiones del ImageView
        int targetW = imgFoto.getWidth();
        int targetH = imgFoto.getHeight();

        // Se obtiene las dimensiones del archivo BitMap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mRutaFotoActual, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determina cuánto escalar la imagen
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decodifica el archivo de imagen en un mapa de bits de tamaño para llenar la vista
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;
        //Se asigna el archivo imagen al ImageView
        Bitmap bitmap = BitmapFactory.decodeFile(mRutaFotoActual, bmOptions);
        imgFoto.setImageBitmap(bitmap);
    }

    private void compartirImagen()
    {
        Uri uri = Uri.parse(mRutaFotoActual);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/jpeg");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        startActivity(Intent.createChooser(intent, "Compartir Imagen"));
    }
}
