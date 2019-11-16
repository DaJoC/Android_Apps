package com.example.appheladeras;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {
    private String[] strings;
    private static final int TAM = 3;

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActionBar actionBar = getSupportActionBar();

        //Creación de variables
        Button inicio = findViewById(R.id.btnConfig);
        TextView tvMsjError = findViewById(R.id.tvError);
        boolean existe = false;
        String textToSet = "Iniciar Sesion";
        inicio.setText(textToSet);
        textToSet="App Heladeras";
        if(actionBar != null)
            actionBar.setTitle(textToSet);
        strings = new String[TAM];

        /*
         * Se toma un valor proveninte de la actividad Login y se lo guarda en la variable "conexión".
         * Si no se recibe nada la variable "conexión" es igual a "desconocido"
         * */
        String conexion = getIntent().getStringExtra("CONEXION");
        if(conexion==null)
            conexion="desconocido";


        // Esta condición verifica si el valor recibido refelja, o  no, un problema con internet.
        // Además se prueba la conexión a internet en el momento.
        if(conexion.equals("error") || !probarConexion()) {
            // De haber algun error se introduce un texto que refleja una serie de instrucciones
            // para solucionar el problema.
            String msjErr = "Problemas de conexión.\n " +
                    "Cerrar la aplicación, deshabiltar WiFi\n" +
                    "Ir a: Ajustes->Almacenamiento\n" +
                    "->Memoria Interna->Aplicaciones\n->App_Hel\n" +
                    "Hacer click en 'borrar datos'\n" +
                    "Esperar 10 segundos, habilitar WiFi.\n " + "Abrir la aplicación." +
                    "\n\n\n\nNota: En algunos dispositivos 'Ajustes' aparece como 'Configuración'\n";
            tvMsjError.setText(msjErr);
        }

        // Se comprueba si el archivo existe.
        if (existeArchivo("archivoConDatos",fileList())) {
            // Si existe entonces se lo lee y se prueba que valores guarda.
            leerArchivos();
            // Si alguno de ellos es nulo
            if (strings[0] == null || strings[1] == null || strings[2] == null) {
                existe = false;
                // Se borra el archivo
                deleteFile("archivoConDatos");

                Log.i("MAIN", "string[0]: " + strings[0]);
                Log.i("MAIN", "string[1]: " + strings[1]);
                Log.i("MAIN", "string[2]: " + strings[2]);
                Log.i("MAIN", "Se acaba de eliminar el archivo y se dentendrá el servicio.");
            } else
                existe = true;
        }

        /*
         * Si no hay ningún problema con internet y el archivo "credencial" existe
         * se inicia la actividad Login y se finaliza la actividad MainActivity
         * */
        if(existe && (!conexion.equals("error") && probarConexion())) {
            startActivity(new Intent(getApplicationContext(),Login.class));
            finish();
        }

        /*
        * Acción que se realiza cuando se presiona el botón inicio
        * En caso de que sea la primera vez que se ingrese
        * a la aplicación se hará uso de este botón
        * */
        inicio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (probarConexion()) {
                    startActivity(new Intent(getApplicationContext(), Login.class));
                    finish();
                } else
                    Login.mostrarMensaje("¡Verifique su conexión a internet!", getApplicationContext());
            }
        });
    }

    /*
     * Función encargada de leer el archivo con el usuario y contraseña y almacenarlos en los campos
     * correspondientes.
     * @Param void.
     * @Return void.
     * */
    public void leerArchivos() {
        try {
            FileInputStream fileInputStream = getApplicationContext().openFileInput("archivoConDatos");
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, StandardCharsets.UTF_8);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            String line;
            int cont = 0;
            while((line = bufferedReader.readLine()) != null) {
                if(cont == 0)
                    strings[cont] = line;
                else if(cont == 1)
                    strings[cont] = line;
                else
                    strings[cont] = line;
                cont++;
            }

            fileInputStream.close();
            inputStreamReader.close();
            bufferedReader.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean probarConexion(){
        ConnectivityManager cm =
                (ConnectivityManager)getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        if(cm!=null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null &&
                    activeNetwork.isConnectedOrConnecting();
        } else
            return false;
    }

    public static boolean existeArchivo(String nombreArchivo, String[] listaDeArchivos){
        /*
         * Este ciclo for each busca entre los archivos existentes el que se denomine archivoConDatos.
         * En dicho archivo se almacenan el usuario y contraseña ingresados por el usuario
         * */
        for (String fileName : listaDeArchivos) {
            if (fileName.equals(nombreArchivo)) {
                return true;
            }
        }
        return false;
    }
}