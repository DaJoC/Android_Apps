package com.example.appheladeras;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import androidx.core.app.NotificationCompat;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class ServiceTemp extends Service {
    //Campos de la clase ServiceTemp.
    private static final String LOG_TAG = "SERVICE_TEMP";
    private static final long HORA_EN_MILISEGUNDOS = 3600000, UN_MINUTO=60000;
    private static ConsultarDatos consultarDatos;
    private static String Ps="";
    private static String dato="";
    private static String notifyReg;
    private static String notifyTemp;
    private static String myURL="";
    private static boolean[] fechasTest, fechaTest2, tempTest, tempTest2;
    private static String[] parte;
    private static String contenido, fReal, Us="";
    private static int response;
    private static long marcaDeTiempo;
    private static long[] marcaDeTiempoTEMP, marcaDeTiempoFECHA;
    private static boolean entrar;
    private static boolean prendemeLaAlarmaT;
    private static boolean prendemeLaAlarmaF;
    private static boolean prendemeLaAlarmaC;
    private static boolean primerAlarmaC;
    private static boolean primerAlarmaT;
    private static boolean primerAlarmaF;
    private static boolean registrarFecha;
    private static boolean inicializarTempTest2;
    private static boolean inicializarFechaTest2;
    private static NotificationManager notificationManager;
    private static Bitmap bitmap;

    @Override
    public void onCreate() {
        super.onCreate();
        // Se puede dar el caso de que un usuario cierre sesión y luego reinicie el dispositivo.
        // De ser así el servicio intentará inciar pero el archivo no existirá.
        // Por lo tanto si esto último ocurre se debe detener el servicio.
        if(!MainActivity.existeArchivo("archivoConDatos",fileList())) {
            stopService(new Intent(getApplicationContext(),ServiceTemp.class));
        }
        // Lectura del usuario y contraseña escrita en el archivo para su utilización en la consulta.
        leerArchivos();
        //Inicialización de campos.
        parte = new String[60];
        Log.v(LOG_TAG, "EN onCreate");
        consultarDatos = new ConsultarDatos(ServiceTemp.this);
        consultarDatos.execute();
        response = 0;
        contenido = "";
        notifyReg=""; notifyTemp="";
        primerAlarmaC = true; primerAlarmaT = true; primerAlarmaF=true; registrarFecha = true;
        inicializarFechaTest2=true; inicializarTempTest2=true;
        notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.ic_launcher_background);

        /*
         * Importante fragmente que permite solicitar al usuario que cancele
         * la optimización de batería para que funcione normalmente en sengundo plano.
         * */
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if(pm != null) {
                if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                    intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setData(Uri.parse("package:" + packageName));
                    startActivity(intent);
                }
            }
        }
    }

    //Funciones setters y getters
    public static void setDato(String d){ dato = d; }
    public static String getDato(){ return dato; }
    public static void setEntrar(Boolean e){ entrar = e; }
    public static boolean getEntrar(){ return entrar; }
    public static String getUs(){ return Us; }
    public static String getPs(){ return Ps; }
    public static String getMyURL(){ return myURL; }
    public static void setParte(String [] p){ parte = p; }
    public static String[] getParte(){ return parte; }
    public static String getContenido(){ return contenido; }
    public static void setContenido(String c) { contenido = c; }
    public static void setfReal(String f){ fReal = f; }
    public static String getfReal(){ return fReal; }
    public static boolean[] getFechasTest() { return fechasTest; }
    public static boolean[] getTempTest() { return tempTest; }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(LOG_TAG, "EN onBind");
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(LOG_TAG, "En onDestroy");
        //entrar = false;
        consultarDatos.cancel(true);
    }

    // Clase encargada de realizar la consulta y evaluar los datos obtenidos de forma asíncrona.
    private static class ConsultarDatos extends AsyncTask<Void, Void, Void> {

        // Se declara una variable weakReference para tener una referencia que impida la fuga
        // de memoria.
        private WeakReference<ServiceTemp> weakReference;
        ConsultarDatos(ServiceTemp serviceTemp) {
            weakReference = new WeakReference<>(serviceTemp);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            setEntrar(true);
        }

        // Tarea en segundo plano es la que realiza la consulta y evalua los datos obtenidos.
        @Override
        protected Void doInBackground(Void... params) {
            Calendar c = Calendar.getInstance();
            SimpleDateFormat sdf_bis = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());

            sdf_bis.format(c.getTime());
            Log.i("SERVICE_TEMP","FECHA DE PRUEBA: "+sdf_bis.format(c.getTime()));

            int tiempoDeEspera=1; // 1 iteración de 15 segundos
            //Bucle principal
            while (getEntrar()) {
                try {
                    //Realiza la consulta al host.
                    setDato(downloadUrl(getMyURL(),getUs(),getPs()));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                prendemeLaAlarmaC = false;
                Log.i("PROGRESO BACKGROUND", "Esto response: "+response);
                /*
                 * Se prueba si la consulta a la BDD fue realizada de forma correcta (response==200)
                 * o si hubo algún error (response!=200).
                 * */
                if(response !=200) {
                    contenido = "error";
                    Log.i("PROGRESO BACKGROUND", "Esto contenido: "+contenido);
                    prendemeLaAlarmaC = true;
                    response = 0;
                    //setUpdate setea un valor en la variable refresh de la clase vista.
                    Vista.setUpdate(true);
                    //Se llama a la función onProgressUpdate.
                    publishProgress();
                }else {
                    Log.i("PROGRESO BACKGROUND", "Esto es dato" + getDato() + "\n");
                    /*
                     * El contenido de la consulta se almacena en el campo dato. Luego se le remplaza
                     * el simbolo <br> por el caracter nulo """ y se separa la cadena por arrobas "@".
                     * Cada fragmento se lo almacena en el vector campo Parte.
                     * */
                    String result = getDato().replace("<br>", ""), texto;
                    setParte(result.split("@"));
                    Log.i("PROGRESO BACKGROUND", "CREANDO EL STRING");

                    /*
                     * contenido es un campo en el cual queda almacenado los datos de la consulta ya
                     * acomodados para la presentación.
                     * */
                    contenido = "";

                    // Esta es la cantidad de filas que idican cuantos datos se deben tratar.
                    int cantidadDeDatos = (getParte().length) / 5;
                    int IndiceActual, IndiceNombre, IndiceFecha, indF = 0;

                    // array de strings que se encarga de almacenar las fechas de las raspby.
                    String[] fechas = new String[cantidadDeDatos];
                    StringBuilder stringBuilder = new StringBuilder();

                    /*
                     * En este for se arma el StringBuilder con los datos de temperaturas actuales
                     * que posteriormente se almacenará en el campo denominado contenido.
                     * */
                    for (int i = 0; i < getParte().length - 2; i++) {
                        if (i % 5 == 0) {
                            IndiceNombre = i;
                            IndiceActual = i + 1;
                            IndiceFecha = i + 4;
                            texto = " " + getParte()[IndiceNombre] + "\n";
                            stringBuilder.append(texto);
                            if (Double.parseDouble(getParte()[IndiceActual]) >= 10.00) {
                                texto ="   "+ getParte()[IndiceActual] + " °C       " +
                                        convertirFecha(getParte()[IndiceFecha]) + "\n";
                                stringBuilder.append(texto);
                            } else if(Double.parseDouble(getParte()[IndiceActual]) >= 0.00) {
                                texto ="   "+ getParte()[IndiceActual] + "   °C       " +
                                        convertirFecha(getParte()[IndiceFecha]) + "\n";
                                stringBuilder.append(texto);
                            }else {
                                texto ="   "+ getParte()[IndiceActual] + "  °C       " +
                                        convertirFecha(getParte()[IndiceFecha]) + "\n";
                                stringBuilder.append(texto);
                            }

                            fechas[indF] = getParte()[IndiceFecha];
                            indF++;
                        }
                    }
                    setContenido(stringBuilder.toString());
                    setfReal(getFechaActual());

                    //*****************************PRUEBA DE FECHAS*********************************
                    long milis = getFechaActualEnMilisegundo();
                    fechasTest = new boolean[fechas.length];

                    Date dateIn = new Date();
                    SimpleDateFormat sdff =
                            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.getDefault());
                    stringBuilder = new StringBuilder();

                    notifyReg="";
                    prendemeLaAlarmaF = false;
                    int index_name=0;

                    //Este for se encarga de evaluar si hay una fecha desactualizada. Se considera
                    //una como tal a aquella que se diferencie por lo menos 1 hora de la hora actual.
                    //Además si la fecha está desactualizada crea un string para la notificación
                    //posterioir.
                    for (int i = 0; i < fechas.length; i++) {
                        try {
                            dateIn = sdff.parse(fechas[i]);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        long F_milis=0;
                        if(dateIn!=null) { F_milis = dateIn.getTime(); }

                        Log.i("PROGRESO BACKGROUND", "fReal - fBdd[" + i + "]" + ": " +
                                (milis - F_milis));
                        if ((milis - F_milis) >= HORA_EN_MILISEGUNDOS) {
                            fechasTest[i] = true;
                            prendemeLaAlarmaF = true;

                            texto=getParte()[index_name]+": "+convertirFecha(fechas[i])+"\n";
                            stringBuilder.append(texto);
                        }
                        Log.i("PROGRESO BACKGROUND", "fechaTest[" + i + "]" + ": " +
                                fechasTest[i]);
                        index_name+=5;
                    }
                    notifyReg=stringBuilder.toString();
                    //******************************************************************************

                    Log.i("PROGRESO BACKGROUND", "ESTO ES PARTE.LENGHT: "
                            + getParte().length);
                    Log.i("PROGRESO BACKGROUND", "ESTO ES PARTE[0]: " + (getParte()[0]));
                    Log.i("PROGRESO BACKGROUND", "ESTO ES VALUES[0]: " + getDato());

                    //*****************************PRUEBA DE TEMPERATURAS***************************
                    /*
                     * Lógica similar a la prueba de fechas.
                     * */
                    stringBuilder = new StringBuilder();
                    index_name=0;
                    prendemeLaAlarmaT = false;
                    tempTest = new boolean[cantidadDeDatos];

                    int indice = 1, indT = 0;
                    while (indice < (getParte().length)) {
                        if (Double.parseDouble(getParte()[indice]) < Double.parseDouble(getParte()[indice + 1])
                                || Double.parseDouble(getParte()[indice]) > Double.parseDouble(getParte()[indice + 2])) {
                            prendemeLaAlarmaT = true;
                            tempTest[indT] = true;
                            texto=getParte()[index_name]+": "+getParte()[indice]+" °C\n";
                            stringBuilder.append(texto);
                        }
                        indice += 5;
                        index_name+=5;
                        indT++;
                    }
                    notifyTemp = stringBuilder.toString();
                    //******************************************************************************
                    response = 0;
                    Vista.setUpdate(true);

                    publishProgress();
                }
                try {
                    // tiempo de espera.
                    for(int q=0; q<tiempoDeEspera;q++) {
                        Thread.sleep(15000);// Stop 15s
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
            ServiceTemp serviceTemp = weakReference.get();
            if(serviceTemp == null)
                return;
            //*********************************ALERTA DE TEMPERATURA********************************
            Log.i("PROGRESO UPDATE","ESTO ES prendemeLaAlarma0: "+prendemeLaAlarmaT);
            // PrendemeLaAlarmaT controla si hay alguna temperatura fuera de rango.
            if(prendemeLaAlarmaT) {
                /*
                 * Esta condición es para que se ejecute una única vez. Solo se reestablece cuando
                 * las temperaturas vuelven a la normalidad.
                 * */
                if(inicializarTempTest2) {
                    //Marca de tiempo para cada heladera.
                    marcaDeTiempoTEMP = new long[tempTest.length];
                    tempTest2 = new boolean[tempTest.length];
                    inicializarTempTest2=false;
                }
                /*
                 * Este for va marcando cuales heladeras van teniendo temperaturas irregulares.
                 * */
                for(int i=0; i<tempTest.length; i++) {
                    if(tempTest[i] && (!tempTest2[i])) {
                        tempTest2[i]=true;
                        marcaDeTiempoTEMP[i]=getFechaActualEnMilisegundo();
                        Log.i("PROGRESO UPDATE","GRABO EN INDICE["+i+"]");
                        primerAlarmaT=true;
                    }
                }
                Log.i("PROGRESO UPDATE","primerAlarmaT: "+primerAlarmaT);
                if(primerAlarmaT) {
                    sonarAlerta("my_channel_id_00","alarmaTemp",
                            "TEMPERATURA FUERA DE RANGO EN: ",notifyTemp,
                            true,0,4,serviceTemp.getApplicationContext());
                    primerAlarmaT=false;
                }else {
                    for(int i=0; i<marcaDeTiempoTEMP.length; i++) {
                        if(tempTest2[i]) {
                            Log.i("PROGRESO UPDATE","EL RESULTADO DE LA RESTA["+i+"]: "
                                    +((getFechaActualEnMilisegundo())-(marcaDeTiempoTEMP[i])));
                            if ((getFechaActualEnMilisegundo() - marcaDeTiempoTEMP[i]) >= HORA_EN_MILISEGUNDOS) {
                                Log.i("PROGRESO UPDATE", "YA PASO UNA HORA. SONAR");
                                marcaDeTiempoTEMP[i] = getFechaActualEnMilisegundo();
                                //Antes de sonar la alerta se verifica si hay conexión.
                                if (!prendemeLaAlarmaC) {
                                    sonarAlerta("my_channel_id_00", "alarmaTemp",
                                            "TEMPERATURA FUERA DE RANGO EN: ", notifyTemp,
                                            true, 0, 4,serviceTemp.getApplicationContext());
                                }
                            }
                        }
                    }
                }
            } else {
                primerAlarmaT=true;
                inicializarTempTest2=true;
            }

            //**********************************ALERTA DE FECHAS************************************
            Log.i("PROGRESO UPDATE","ESTO ES prendemeLaAlarma1: "+prendemeLaAlarmaF);
            if(prendemeLaAlarmaF) {
                if(inicializarFechaTest2) {
                    marcaDeTiempoFECHA = new long[fechasTest.length];
                    fechaTest2 = new boolean[fechasTest.length];
                    inicializarFechaTest2=false;
                }
                for(int i=0; i<fechasTest.length; i++) {
                    if(fechasTest[i] && (!fechaTest2[i])) {
                        fechaTest2[i]=true;
                        marcaDeTiempoFECHA[i]=getFechaActualEnMilisegundo();
                        primerAlarmaF=true;
                    }
                }
                if(primerAlarmaF) {
                    sonarAlerta("my_channel_id_01", "alarmaFechas",
                            "ERROR DE REGISTRO EN: ",notifyReg,
                            true,1,4,serviceTemp.getApplicationContext());
                    primerAlarmaF=false;
                }else {
                    for(int i=0; i<marcaDeTiempoFECHA.length; i++) {
                        if(fechaTest2[i]) {
                            if ((getFechaActualEnMilisegundo() - marcaDeTiempoFECHA[i]) >= HORA_EN_MILISEGUNDOS) {
                                Log.i("PROGRESO UPDATE", "YA PASO UNA HORA. SONAR");
                                marcaDeTiempoFECHA[i] = getFechaActualEnMilisegundo();
                                //Antes de sonar la alerta se verifica si hay conexión.
                                if (!prendemeLaAlarmaC) {
                                    sonarAlerta("my_channel_id_01", "alarmaFechas",
                                            "ERROR DE REGISTRO EN: ", notifyReg,
                                            true, 1, 4,serviceTemp.getApplicationContext());
                                }
                            }
                        }
                    }
                }
            } else {
                primerAlarmaF=true;
                inicializarFechaTest2=true;
            }

            //*********************************ALERTA DE CONEXIÓN***********************************
            Log.i("PROGRESO UPDATE","ESTO ES prendemeLaAlarma2: "+prendemeLaAlarmaC);
            if(prendemeLaAlarmaC) {
                if(primerAlarmaC) {
                    if(registrarFecha) {
                        marcaDeTiempo = getFechaActualEnMilisegundo();
                        registrarFecha=false;
                    }
                    if(getFechaActualEnMilisegundo()-marcaDeTiempo>=UN_MINUTO) {
                        sonarAlerta("my_channel_id_02", "alarmaConexiónInicial",
                                "Notificación de alerta", "PROBLEMAS DE CONEXIÓN",
                                false, 2, 2,serviceTemp.getApplicationContext());
                        primerAlarmaC = false;
                        marcaDeTiempo = getFechaActualEnMilisegundo();
                    }
                }else {
                    if((getFechaActualEnMilisegundo()-marcaDeTiempo)>=HORA_EN_MILISEGUNDOS) {
                        marcaDeTiempo = getFechaActualEnMilisegundo();
                        sonarAlerta("my_channel_id_03","alarmaConexiónPosterior",
                                "PROBLEMAS DE CONEXIÓN","Lleva una hora o más sin conexión",
                                true,3,4,serviceTemp.getApplicationContext());
                    }
                }
            } else {
                registrarFecha = true;
                primerAlarmaC = true;
                notificationManager.cancel(2);
                notificationManager.cancel(3);
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            entrar = false;
        }
    }

    public static void detenerAsyncTask() {
        int cont=0;
        Log.i("DETENER_ASYNCTASK", "---------- isCancelled: " + consultarDatos.isCancelled());
        while (!consultarDatos.isCancelled()) {
            consultarDatos.cancel(true);
            cont++;
        }
        Log.i("DETENER_ASYNCTASK", "Esto es cont: " + cont);
        consultarDatos.onCancelled();
    }

    /*
     * Función que se encarga de emitir las notificaciones correspondiente.
     * @Param channelID es el id del canal necesario para android 8 en adelante.
     * @Param nombre es el nombre de la alerta.
     * @Param titulo es como se titula la notificación.
     * @Param texto muestra las temperaturas o fehcas incorrectas asociadas con el nombre de la
     * heladera.
     * @Param sonido controla si la alarma es silenciosa.
     * @Param id identifica a cada notificación.
     * @Param importancia necesario para android 8 en adelante. Establece si la alarma sonará o no
     * de acuerdo a la importancia asignada.
     * */
    public static void sonarAlerta(String channelID, CharSequence nombre, String titulo, String texto,
                                   boolean sonido, int id, int importancia,Context context) {
        NotificationCompat.Builder mBuilder;

        int icono = R.drawable.ic_action_add_alert;
        Intent i=new Intent(context, Vista.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                0, i, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder = new NotificationCompat.Builder(context, "");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelID, nombre, importancia);
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            notificationManager.createNotificationChannel(channel);

            mBuilder = new NotificationCompat.Builder(context, channelID);
        }

        mBuilder.setSmallIcon(icono)
                .setLargeIcon(bitmap)
                .setTicker("AppHeladeras")
                .setContentTitle(titulo)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(texto))
                .setAutoCancel(true)
                .setWhen(System.currentTimeMillis())
                .setContentInfo("Info")
                .setContentIntent(pendingIntent);
        if(sonido) {
            mBuilder.setDefaults(Notification.DEFAULT_ALL);//Sonido de alerta por defecto
        }
        notificationManager.notify(/*notification id*/id, mBuilder.build());
    }

    /*
     * Función que realiza la conexión http con el host pasado por argumento.
     * @Param myurl es la dirección del host a la cual se desea consultar.
     * @Return String es el contenido producto de la consulta.
     * */
    private static String downloadUrl(String dir, String user, String pass) {
        Log.i("URL",""+dir);
        dir = dir.replace(" ","%20");

        //Se crean dos variables locales necesarias para la realizar la consulta.
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {

            //Dirección url a la cual le realizaremos la consulta.
            URL url = new URL(dir);

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

            /*
             * A los valores pasados como parámetros se les asigna una clave y se las almacena
             * una variable ContentValues que se conforma de la siguiente manera <Clave, Valor>.
             * */
            ContentValues contentValues = new ContentValues();
            contentValues.put("Usuario", user);
            contentValues.put("Clave", pass);

            //Se crea el flujo de salida de la conección http.
            outputStream = httpURLConnection.getOutputStream();
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));

            /*
             * Se le escribe los datos requeridos para la consulta previamente codificados por el
             * método getQuery();
             * */
            bufferedWriter.write(getQuery(contentValues));

            //Se borra y se cierra el buffer de escritura.
            bufferedWriter.flush();
            bufferedWriter.close();

            //Se obtiene el código (valor entero) de respusta de la conexión realizada.
            response = httpURLConnection.getResponseCode();
            Log.d("CONEXIÓN", " El código de estado es: " + response);

            //Si es 200 quiere decir que la conexión fue exitosa
            if (response == 200) {

                //Se crean un flujo de entrada.
                inputStream = new BufferedInputStream(httpURLConnection.getInputStream());

                //Se almacena la cantidad de caracteres que contiene la descarga.
                int len = httpURLConnection.getContentLength();

                //Se convierte el flujo de entrada en un String mediante el método readIt()
                String response = readIt(inputStream, len);
                Log.d("CONEXIÓN", "The response is: " + response);

                //Se retorna los datos descargados en forma de String.
                return response;

            } else {
                return "Unable to retrieve web page. URL may be invalid.";
            }

        } catch (Exception e) {
            return "Error en la conexión";
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
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
                if(cont == 0){myURL = line;}
                else if(cont == 1){Us = line;}
                else {Ps = line;}
                cont++;
            }

            Log.i("LEER_ARCHIVO","URL: " + myURL);
            Log.i("LEER_ARCHIVO","User: " + Us);
            Log.i("LEER_ARCHIVO","Pass: " + Ps);
            fileInputStream.close();
            inputStreamReader.close();
            bufferedReader.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     * Función que convierte el formato de una fecha ingresada como parámetro.
     * @Param fecha es la fecha que se desea cambiar el formato.
     * @Return String es la fecha con el formato deseado.
     * */
    public static String convertirFecha(String fecha) {
        String[] f = fecha.split(" ");
        String[] diaMesYear = f[0].split("-");
        return diaMesYear[2]+"-"+diaMesYear[1]+"-"+diaMesYear[0]+" "+f[1];
    }

    /*
     * Función que se encargad e devolver la fecha actual.
     * @Param void.
     * @Return String fecha actual con un formato predefinido.
     * */
    public static String getFechaActual() {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss",Locale.getDefault());

        return sdf.format(c.getTime());
    }

    /*
     * Función encargada de obtener la fecha actual en milisegundos.
     * @Param void.
     * @Return long la fecha actual en milisegundos.
     * */
    public static long getFechaActualEnMilisegundo() {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss",Locale.getDefault());
        Date date = new Date();

        final String strDate = sdf.format(c.getTime());

        try {
            date = sdf.parse(strDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        //La función getTime retorna un valor de tipo long
        //que equivale a la fecha actual en milisegundos
        if(date!=null)
            return date.getTime();
        else
            return 0;
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


        //Bucle for encargado de recorrer todos los valores. Para cada valor se lo codificará y se lo
        //agregará al StringBuilder.
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
}