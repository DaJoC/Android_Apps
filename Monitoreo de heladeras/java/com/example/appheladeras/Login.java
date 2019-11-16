package com.example.appheladeras;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;

public class Login extends AppCompatActivity
{
    //Campos de la clase Login
    private EditText etUser;
    private EditText etPass;
    private static boolean existe;
    private static String botonVista;
    private static String[] cadena;
    private static final String PHP_FILE = "/nombre.php";
    TextView tvInfoWeb;
    private static String web_page="https://abcde.com.ar";
    private static FileOutputStream archivo;
    private static int statusCode;

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //Creación de variables
        existe = false;
        statusCode = 0;
        Log.i("onCREATE","LOGOUT: "+ botonVista);
        ActionBar actionBar = getSupportActionBar();
        String t="App Heladeras";
        if(actionBar != null)
            actionBar.setTitle(t);
        cadena = new String[3];
        String[] archivos = fileList();

        /*
         * Este ciclo for each busca entre los archivos existentes el que se denomine credencial.
         * En dicho archivo se almacenan el usuario y contraseña ingresados por el usuario
         * */
        for (String fileName : archivos) {
            if (fileName.equals("archivoConDatos"))
                existe = true;
        }

        /*
         * Se toma un valor proveninte de la actividad Vista y se lo guarda en la variable "botonVista".
         * Si no se recibe nada el valor de la variable "botonVista" es igual a "login"
         * */
        botonVista = getIntent().getStringExtra("CS");
        if(botonVista==null)
            botonVista="login";

        /*
         * Condición que que verifica que el archivo exista y es así que no se haya cerrado sesión
         * desde la activiad "Vista".
         * Cuando la condición es verdadera se inicia la actividad Vista y se finaliza la actividad Login.
         * */
        if(existe && botonVista.compareTo("logout")!=0) {
            startActivity(new Intent(getApplicationContext(),Vista.class));
            finish();
        }

        /*
         * Cuando la condición es falsa se espera al ingreso del usuario y contraseña. Una vez que se
         * haga clik en guardar se realiza una consulta a la base de datos para verificar los datos ingresados
         * */
        else {
            //setContentView(R.layout.activity_login);
            tvInfoWeb = findViewById(R.id.tvInfoWebConsulta);
            tvInfoWeb.setText(web_page);
            etUser = findViewById(R.id.etUser);
            etPass = findViewById(R.id.etPass);
            Button guardar = findViewById(R.id.btnSave);

            guardar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(etUser.getText().toString().length() == 0)
                        etUser.setError("Debe ingresar un usuario!");
                    else if(etPass.getText().toString().length() == 0)
                        etPass.setError("Debe ingresar una contraseña!");
                    else {
                        Log.d("LOGIN", "Status code de CambiarWeb: "+CambiarWeb.getStatusCode());
                        if(CambiarWeb.getStatusCode()==301) {
                            cadena[0]=CambiarWeb.getRedirection()+PHP_FILE;
                            Log.d("LOGIN", "301_cadena[0]: "+cadena[0]);
                        } else {
                            cadena[0]=web_page+PHP_FILE;
                            Log.d("LOGIN", "0_cadena[0]: "+cadena[0]);
                        }
                        try {
                            archivo = openFileOutput("archivoConDatos", Context.MODE_PRIVATE);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        cadena[1]=etUser.getText().toString();
                        cadena[2]=etPass.getText().toString();
                        consultaBDD();
                    }
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        startActivity(new Intent(getApplicationContext(),CambiarWeb.class));
        finish();
        return true;
    }

    //Funciones setters y getters
    public static Boolean getExiste(){return existe;}
    public static String getBotonVista(){return botonVista;}
    public static void setBotonVista(String bV){botonVista=bV;}
    public static void setWeb_page(String dir){ web_page=dir;}

    /*
     * Función que ejecuta una tarea asíncrona mediante la recepción de dos parámetros.
     * @Param String u es el usuario ingresado.
     * @Param String p es la contraseña ingresada.
     * @Return void.
     * */
    public void consultaBDD() {
        ConsultarDatos consultarDatos = new ConsultarDatos(Login.this);
        consultarDatos.execute();
    }

    /*
     * Clase privada, que hereda de la clase AsyncTask, destinada a realizar una tarea de forma asíncrona.
     * En esta clase se sobre-escriben dos métodos de la clase padre AsyncTask doInBackGround y onPostExecute.
     */
    private static class ConsultarDatos extends AsyncTask<Void, Void, String> {

        // Se declara una variable weakReference para tener una referencia que impida la fuga
        // de memoria.
        private WeakReference<Login> weakReference;
        ConsultarDatos(Login login) {
            weakReference = new WeakReference<>(login);
        }

        /*
         * Es el único método que se ejecuta en segundo plano. realiza la consulta a la base de datos
         * y devuelve un String que contendrá los datos obtenidos o un mensaje de error.
         * @Param urls es un vector String que se desconoce el tamaño, es la dirección del host.
         * @Retunr String es el valor producto de la consulta realizada.
         * */
        @Override
        protected String doInBackground(Void... urls) {
            //Se crean dos variables locales necesarias para la realizar la consulta.
            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                //Dirección url a la cual le realizaremos la consulta.
                URL url = new URL(cadena[0]);

                //Se abre la conexión httpURLConnection.
                HttpsURLConnection httpURLConnection = (HttpsURLConnection) url.openConnection();
                Log.d("LOGIN_CONEXIÓN", "Apertura de la conexión");
                httpURLConnection.setRequestProperty("Accept-Encoding", "identity");

                System.out.println("Orignal URL: " + httpURLConnection.getURL());

                /*
                 * setDoInput(true) es utilizado por el métddo POST para permitir el envío de datos
                 * a travez de la conexión
                 * */
                httpURLConnection.setDoInput(true);

                /*
                 * setDoOutput(true) es necesario para enviar una solicitu por metodo POST. Esta se
                 * realiza a través de la secuencia de conexión de salida.
                 * */
                httpURLConnection.setDoOutput(true);

                //Se define el método de envío de datos como POST.
                httpURLConnection.setRequestMethod("POST");

                /*
                 * A los valores pasados como parámetros se les asigna una clave y se las almacena
                 * una variable ContentValues que se conforma de la siguiente manera <Clave, Valor>.
                 * */
                ContentValues values = new ContentValues();
                values.put("Usuario", cadena[1]);
                values.put("Clave", cadena[2]);

                //Se crea el flujo de salida de la conección http.
                outputStream = httpURLConnection.getOutputStream();
                BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));

                /*
                 * Se le escribe los datos requeridos para la consulta previamente codificados por el
                 * método getQuery();
                 * */
                bufferedWriter.write(getQuery(values));

                //Se borra y se cierra el flujo de salida de la conexión http.
                bufferedWriter.flush();
                bufferedWriter.close();

                //Se obtiene el codigo de respusta de la conexión realizada.
                statusCode = httpURLConnection.getResponseCode();
                Log.d("LOGIN_CONEXIÓN", " The status code is " + statusCode);

                //Se crean un flujo de entrada.
                inputStream = new BufferedInputStream(httpURLConnection.getInputStream());
                //Se almacena la cantidad de caracteres que contiene la descarga.
                int len = httpURLConnection.getContentLength();
                Log.d("LOGIN_CONEXIÓN", "mensaje 3. len: " + len);
                //Se convierte el flujo de entrada en un String mediante el método readIt()
                String response = readIt(inputStream, len);
                Log.d("LOGIN_CONEXIÓN", "The response is " + response);
                //Se retorna los datos descargados en forma de String.
                return response;

            } catch (Exception e) {
                e.printStackTrace();
                return "Unable to retrieve web page. URL may be invalid.";
            } finally {
                try {
                    if (inputStream != null)
                        inputStream.close();
                    if (outputStream != null)
                        outputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /*
         * Este método se encarga de realizar las pruebas necesarias para validar los datos ingresados.
         * @Param String result: Valor obtenido del método doInbackground.
         * @Return void.
         * */
        @Override
        protected void onPostExecute(String result) {

            Login login = weakReference.get();
            if(login == null || login.isFinishing())
                return;

            // Se controla el usuario y la contraseña.
            Log.i("ON_POST_EXECUTE_LOGIN","ESTO ES RESULT: "+result);
            String e = "Error";
            int counter = 0;
            char c1,c2;
            for(int i=0; i<e.length(); i++) {
                c1 = e.charAt(i);
                c2 = result.charAt(i);
                if(c1==c2)
                    counter++;
            }
            if(counter==e.length()) {
                Log.i("USERyPASS","USUARIO O CONTRASEÑA INCORRECTOS");
                mostrarMensaje("Usario o contraseña incorrectos", login.getApplicationContext());
            }

            /*
             * Se controla que la conexión a la base de datos haya sido exitosa.
             * Si hubo un problema de conexión se muestra un mensaje de error y se vuelve a la actividad
             * MainActivity con el valor extra "error" que se indetifica con la etiqueta CONEXIÓN.
             * */
            else if(statusCode!=200) {
                Log.i("INTERNET","ACÁ HAY ERROR DE CONEXIÓN");
                mostrarMensaje("Verifique su conexión a internet", login.getApplicationContext());

                Intent intentMain = new Intent(login.getApplicationContext(), MainActivity.class);
                intentMain.putExtra("CONEXION","error");
                login.getApplicationContext().startActivity(intentMain);
                login.finish();
            }

            /*
             * Si la conexión fue exitosa y los datos son validados se escribe el archivo si este
             * no existe o se ha cerrado sesión en la clase Vista. Finalmente se inicia la activiadad
             * Vista y se cierra la activiada Login.
             * */
            else {
                Log.i("CONSULTA","SE MUESTRA LA CONSULTA");
                if(!getExiste()) {
                    escribirArchivo();
                    mostrarMensaje("Bienvenido!", login.getApplicationContext());
                }else if(getBotonVista().compareTo("logout")==0) {
                    escribirArchivo();
                    mostrarMensaje("Bienvenido!", login.getApplicationContext());
                    setBotonVista("login");
                }
                login.getApplicationContext().startActivity(new Intent(login.getApplicationContext(),Vista.class));
                login.finish();
            }
        }
    }

    /*
     * Función encargada de codificar los datos ingresados por el usario para realizar la consulta.
     * @Param values son los valore que se desean codificar para realizar la consulta.
     * @Return String es el valor ya codificado y listo para ser escrito en el flujo de salida de la
     * conexión.
     * */
    private static String getQuery(ContentValues values) throws UnsupportedEncodingException
    {
        //Variable necesaria para concatenar el String.
        StringBuilder result = new StringBuilder();
        //Variable que sirve para controlar que una acción se realice una única vez.
        boolean first = true;

        /*
         * Bucle for encargado de recorrer todos los valores. Para cada valor se lo codificará y se lo
         * agregará al StringBuilder.
         * */
        for (Map.Entry<String, Object> entry : values.valueSet())
        {
            if (first)
                first = false;
            else
                result.append("&");

            //Primero codifica y agrega la llave.
            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));

            //Agrega el caracter igual.
            result.append("=");

            //Por último codifica y agrega el valor.
            result.append(URLEncoder.encode(String.valueOf(entry.getValue()), "UTF-8"));
        }

        return result.toString();
    }

    /*
     * Función encargada de leer el imputStream producto de la conexión htpp y convertirlo en String.
     * @Param stream es el imputStream que se desea convertir.
     * @Param len es la cantidad de caracteres a leer.
     * @Return String imputStream ya convertido en String.
     * */
    public static String readIt(InputStream stream, int len) throws IOException {
        Reader reader;
        reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
        int i=0;
        char[] buffer = new char[len];

        while(i!=-1)
            i=reader.read(buffer);

        return new String(buffer);
    }

    /*
     * Función que se encarga de escribir los datos ingresado.
     * El contenido del archivo se crea a partir de los campos etUser y etPass
     * que se asignan en la tarea asíncrona "consltarDatos()".
     * Esta función se llamará una vez que los valores ingresados hayan sido validados.
     * @Param void.
     * @Return void.
     * */
    public static void escribirArchivo() {
        StringBuilder stringBuilder = new StringBuilder();

        for(String string : cadena) {
            stringBuilder.append(string);
            stringBuilder.append("\n");
        }

        try {
            archivo.write(stringBuilder.toString().getBytes());
            archivo.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Función que se encarga de mostrar un mensaje (Toast) que se pasa como parámetro.
     * @Param msj es el mensaje a mostrar en un toast.
     * @Return void.
     * */
    public static void mostrarMensaje(String msj, Context context) { Toast.makeText(context,msj,Toast.LENGTH_SHORT).show(); }
}