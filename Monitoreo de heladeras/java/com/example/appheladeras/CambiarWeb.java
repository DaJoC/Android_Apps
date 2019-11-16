package com.example.appheladeras;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/*
 * Actividad CambiarWeb destinada a recibir una nueva dirección web, validarla y guardarla o no como
 * nueva dirección a consultar.
 * */
public class CambiarWeb extends AppCompatActivity {

    // Campos y constantes de la clase.
    private static final String PHP_FILE ="/nombre.php";
    private EditText etNuevaDir;
    private static int statusCode;
    private static String redirection;
    private Switch swProtocol;
    private TextView tvNamePr;
    private boolean prSafe;
    private static String directionToSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cambiar_web);

        // Inicialización de campos y variables.
        etNuevaDir = findViewById(R.id.etNewAddress);
        Button btnGuardar = findViewById(R.id.btnSaveAddress);
        swProtocol = findViewById(R.id.swProtocol);
        tvNamePr = findViewById(R.id.tvNamePr);
        prSafe=false;
        directionToSave="";
        ActionBar actionBar = getSupportActionBar();
        String t="App Heladeras";
        if(actionBar!=null) {
            actionBar.setTitle(t);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Acción que se genera una vez pulsado el botón btnGuardar.
        btnGuardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!etNuevaDir.getText().toString().equals("")) {
                    Log.i("guardar", "prueba :" +
                            probarTextoIngresado(etNuevaDir.getText().toString()));
                    // Se verifica que la dirección ingresada no se le anteponga "http://" ni "htpps://"
                    if (!probarTextoIngresado(etNuevaDir.getText().toString())) {
                        // Si la dirección es https. Indicado por la posición del switch.
                        if (prSafe) {
                            // Se le antepone el string "https://" a la dirección ingresada
                            // por el usuario.
                            directionToSave = "https://" + etNuevaDir.getText().toString();
                            // Verifica la conexión de la dirección ingresada ejecutando una
                            // nueva tarea asíncrona.
                            new test_web_page(CambiarWeb.this).execute(directionToSave + PHP_FILE);
                        } else {
                            // Si la dirección no es https entonces se le antepóne a la
                            // dirección ingresada "htpp://"
                            directionToSave = "http://" + etNuevaDir.getText().toString();
                            // Posteriormente se verifica la conexión de la dirección
                            // ingresada ejecutando una nueva tarea asíncrona.
                            new test_web_page(CambiarWeb.this).execute(directionToSave + PHP_FILE);
                        }
                    } else
                        // Si el usuario antepuso http o https a la dirección entonces se
                        // muestra este mensaje.
                        mostrarMensaje("Ingrese la dirección sin http://-https://", getApplicationContext());
                }else
                    mostrarMensaje("El campo está vacio", getApplicationContext());
            }
        });
    }

    /*
     * Función que se encarga de responder ante los cambios del switch.
     * */
    public void onSwitchClick(View view){
        String string ="https";
        // Si se activa el switch.
        if(swProtocol.isChecked()) {
            // Se actualiza la vista.
            tvNamePr.setText(string);
            // se pone en true la variable prSafe que se comprueba cuando se pulsa el botón guardar.
            prSafe=true;
            //si no se activa el switch, o se lo desactiva.
        } else {
            // Se actualiza la vista.
            string="htpp";
            tvNamePr.setText(string);
            // prSafe ahora es false.
            prSafe=false;
        }
    }

    @Override
    public void onBackPressed() {
        // Se llama a una función para volver a la actividad Login.
        vovlerAlLogin(getApplicationContext(), CambiarWeb.this);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Si se selecciona la flecha en la esquina superior izq.
        if(item.getItemId() == android.R.id.home)
            // Se vuelve a la actividad Login.
            vovlerAlLogin(getApplicationContext(), CambiarWeb.this);
        return super.onOptionsItemSelected(item);
    }

    /*
    * Clase asíncrona destinada a probar si la dirección ingresada es válida o no.
    * */
    private static class test_web_page extends AsyncTask<String,Void,String> {
        // Se declara una variable weakReference para tener una referencia que impida la fuga
        // de memoria.
        private WeakReference<CambiarWeb> weakReference;
        test_web_page(CambiarWeb context) { weakReference = new WeakReference<>(context); }

        @Override
        protected String doInBackground(String... strings) {
            // Si la conexión fue exitosa entonces se descarga y retorna el conetindo de la página.
            if(getCode(strings[0])==200) {
                Log.d("In_Background", "strings[0]: "+strings[0]);
                statusCode=200;
                return downloadURL(strings[0]);
            }
            // El código 301 indica que hubo una redirección por lo que hay que obtener la dirección
            // a la cual se redirige.
            else if(getCode(strings[0])==301){
                // la palabra clave a busca es href
                String e = "href=";
                String datos = downloadURL(strings[0]);
                int counter = 0, inicio=0;
                char c1,c2;
                StringBuilder dir = new StringBuilder();

                // Este for recorre el mesaje retornado.
                for(int i=0; i<datos.length(); i++) {
                    // Este for se encarga de encontrar que los caracteres "href=" para ubucar su
                    // posición.
                    for (int j=0; j < e.length(); j++) {
                        c1 = e.charAt(j);
                        c2 = datos.charAt(i+j);
                        if (c1 == c2)
                            counter++;
                    }
                    // Cuando se completó la comparación
                    if(counter==e.length()) {
                        // Se guarda el inicio en donde cominenza la dirección web de redirección.
                        // El indice se le incrementa una unidad ya que la dirección se presenta de
                        // la siguiente manera. Ej: "https://wwww.abcde.com.ar/algo"
                        inicio=i+(e.length()+1);
                        break;
                    } else
                        counter=0;
                }

                // Este for guarda la dirección
                for(int i=inicio; i<datos.length(); i++) {
                    if(datos.charAt(i)!=34)
                        dir.append(datos.charAt(i));
                    else
                        break;
                }
                Log.d("In_Background", " redireccionado a: "+dir.toString());
                int barras = 0;
                StringBuilder web_red = new StringBuilder();
                // Este for se encarga de que la la dirección quede como: https://wwww.abcde.com.ar
                for(int i=0; i<dir.toString().length();i++) {
                    if(dir.toString().charAt(i)== 47)
                        barras++;
                    if(barras==3)
                        break;
                    web_red.append(dir.toString().charAt(i));
                }

                // Se almacena la dirección a la cual se redirecciona.
                redirection = web_red.toString();
                Log.d("In_Background", " redirection: "+redirection);
                // Se almacena el valor del código de conexión
                statusCode = 301;
                // Se retorna el valor descargado de la página
                return downloadURL(dir.toString());
            }else
                return "Error";
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            // Se obtiene la referencia.
            CambiarWeb cambiarWeb = weakReference.get();
            if(cambiarWeb == null || cambiarWeb.isFinishing())
                return;


            // Si la descarga recibida de la página es "ok".
            if(s.equals("ok")) {
                // Guardo la dirección ingresada.
                Login.setWeb_page(directionToSave);
                // Se muestra un mesaje que indica que la dirección es aceptable.
                mostrarMensaje("Dirección guardada", cambiarWeb.getApplicationContext());
                // Retorno a la actividad Login.
                vovlerAlLogin(cambiarWeb.getApplicationContext(),cambiarWeb);
            }else
                // De lo contrario se meustra un mensaje de error.
                mostrarMensaje("ERROR!",cambiarWeb.getApplicationContext());
        }
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
     * Función que se encarga de mostrar un mensaje (Toast) que se pasa como parámetro.
     * @Param msj es el mensaje a mostrar en un toast.
     * @Return void.
     * */
    public static void mostrarMensaje(String msj, Context context) { Toast.makeText(context,msj,Toast.LENGTH_SHORT).show(); }

    /*
     * Función encargarda de realizar el cambio de actividad una vez invocada.
     * @Param void.
     * @Return void.
     * */
    public static void vovlerAlLogin(Context context, CambiarWeb cambiarWeb) {
        context.startActivity(new Intent(context,Login.class));
        cambiarWeb.finish();
    }

    /*
    * Función que se encarga de probar la dirección ingresada devolviendo el código de conexión como
    * respuesta.
    * @param String my_url es la dirección a testear.
    * @return int es el código obtenido de la conexión http realizada.
    * */
    public static int getCode(String my_url) {
        try {
            //Dirección url a la cual le realizaremos la consulta.
            URL url = new URL(my_url);

            //Se abre la conexión httpURLConnection.
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestProperty("Accept-Encoding", "identity");

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

            //Se obtiene el codigo de respusta de la conexión realizada.
            int statusCode = httpURLConnection.getResponseCode();
            Log.d("CONEXIÓN_GetCode", " The status code is " + statusCode);

            //Se retorna el valor del código de conexión.
            return statusCode;
        } catch (Exception e) {
            return 0;
        }
    }

    /*
    * Función encargada de gerar la conexión y obtener los datos de la pagina web consultada.
    * @param String my_url es la dirección de la página a la cual vamos a consultar.
    * @return String es el dato obtenido de la página.
    * */
    public static String downloadURL(String my_url) {

        //Se crean dos variables locales necesarias para realizar la consulta.
        InputStream inputStream = null;
        try {
            //Dirección url a la cual le realizaremos la consulta.
            URL url = new URL(my_url);

            //Se abre la conexión httpURLConnection.
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestProperty("Accept-Encoding", "identity");

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

            //Se obtiene el codigo de respusta de la conexión realizada.
            int statusCode = httpURLConnection.getResponseCode();
            Log.d("CONEXIÓN_downloadURL", " The status code is " + statusCode);

            //Se crean un flujo de entrada.
            inputStream = new BufferedInputStream(httpURLConnection.getInputStream());

            //Se almacena la cantidad de caracteres que contiene la descarga.
            int len = httpURLConnection.getContentLength();
            Log.i("CONEXIÓN_downloadURL", "Esta es la longitud de la respuesta: " + len);

            //Se convierte el flujo de entrada en un String mediante el método readIt()
            String response = readIt(inputStream, len);
            Log.i("CONEXIÓN_downloadURL", "Esta es la respuesta: " + response);

            //Se retorna los datos descargados en forma de String.
            return response;

        } catch (Exception e) {
            return "Error";
        } finally {
            try {
                if (inputStream != null)
                    inputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /*
    * Función encargada si el texto ingresado contine "http://" o "https://".
    * @param String string es la página ingresada.
    * @retun boolean es el valor retornado que indicará si la página contiene "http://" o "https://"
    * La razón por la cual se prueba el texto ingresado es porque se propuso el ingreso de caracteres
    * simples para el usuario. Entonces se propuso que solo se ingresara la dirección de la página
    * por Ejemplo: www.abcde.com y el código se encarga de anteponerle el protocolo http o https
    * según se seleccione.
    * */
    public boolean probarTextoIngresado(String string) {
        Log.i("ProbarTexto", "string a probar: " + string);
        String aux = "http://";
        int c = 0;
        for(int i=0; i<aux.length(); i++) {
            if(aux.charAt(i)==string.charAt(i))
                c++;
        }
        // Si las primeras letras coinciden con http://
        if(c==aux.length())
            // Entonces retorna true
            return true;

        aux="https://";
        c=0;
        for(int i=0; i<aux.length(); i++) {
            if(aux.charAt(i)==string.charAt(i))
                c++;
        }
        // Se retorna direcctamente si las primeras letras coinciden con https:// o no
        return c==aux.length();
    }

    //Funciones setters y getters
    public static int getStatusCode() { return statusCode; }
    public static String getRedirection() { return redirection; }
}