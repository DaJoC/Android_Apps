package com.example.appheladeras;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class Vista extends AppCompatActivity {

    //Campos de la clase Vista
    private static String LOG_TAG = "VISTA";
    private TextView pantalla, fechaAct;
    private static boolean refresh, btnUpD;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vista);

        //Creación de variables
        Log.v(LOG_TAG, "EN onCreate");
        pantalla = findViewById(R.id.tvSecreen);
        final SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipe);
        fechaAct = findViewById(R.id.tvFecha);
        refresh = false; btnUpD = false;

        //Hilo principal que se encarga de mantener los datos actualizados en pantalla.
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        //Cada un sengundo se actuliza la pantalla.
                        Thread.sleep(1000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //Este if cotrola que si se pulsó el borón actualizar
                                if(btnUpD) {
                                    /*
                                     * Ya que la clase vista debe esperar hasta que se procesecen los
                                     * datos antes de mostrarlo la variable refresh controlará esta
                                     * condición dejando unicamente que se invoque el método mostrar()
                                     * una vez que el servicio haya terminado de procesar los datos.
                                     * */
                                    if(refresh) {
                                        Log.i("HILO_VISTA","Ingreso para mostrar");
                                        mostrar();
                                        btnUpD = false;
                                    }
                                } else {
                                    /*
                                     * Cuando no se presiona el botón actualizar se muestra los valores
                                     * actuales en pantalla.
                                     * Las condiciones sirven unicamente al inicio de la aplicación ya
                                     * que luego de la primera consulta siempre habrá un valor anterior.
                                     * */
                                    if(ServiceTemp.getContenido()!=null && ServiceTemp.getFechasTest()!=null
                                            && ServiceTemp.getfReal()!=null && ServiceTemp.getParte()!=null
                                            && ServiceTemp.getTempTest()!=null) {
                                        mostrar();
                                    }
                                }
                            }
                        });
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        //Se inicia el hilo que actualiza los valores en pantalla.
        t.start();

        //Acción que se realiza cuandose desliza hacia abajo para actualizar.
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                //Variable btnUpD en true indicando que se pulsó el botón actualizar.
                btnUpD = true;

                // Se detiene la tarea asíncrona.
                ServiceTemp.detenerAsyncTask();

                //Se detiene el servicio.
                stopService(new Intent(getApplicationContext(),ServiceTemp.class));

                //Se inicia el servicio nuevamente para que actualice el valor de los datos
                startService(new Intent(getApplicationContext(),ServiceTemp.class));

                swipeRefreshLayout.setRefreshing(false);
            }
        });

        /*
         * Esta condición está para que los datos se guarden cuando se cambia de pantalla vertical a
         * horizontal y veceversa.
         * */
        if(savedInstanceState != null) {
            String lectura = "Última lectura: " + savedInstanceState.getString("St_fR");
            fechaAct.setText(lectura);
            String datoDeTempGuardados = savedInstanceState.getString("St_Contenido");
            if(datoDeTempGuardados!=null)
                pantalla.setText(coloreameElString(datoDeTempGuardados), TextView.BufferType.SPANNABLE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    //Sobre-escritura del método onOptionsItemSelected() para crear el menu de opciones.
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        showAlertDialog();
        return true;
    }

    //Setter de la clase Vista.
    public static void setUpdate(boolean update){refresh=update;}

    @Override
    protected void onStart() {
        super.onStart();
        startService(new Intent(getApplicationContext(),ServiceTemp.class));
        Log.v(LOG_TAG, "EN onStart ");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.v(LOG_TAG, "EN onStop ");
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("St_fR",ServiceTemp.getfReal());
        outState.putString("St_Contenido",ServiceTemp.getContenido());
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        savedInstanceState.getString("St_fR");
        savedInstanceState.getString("St_Contenido");
    }

    /*
     * Función que se encarga de setear en los textViews de la aplicación los datos almacenados en
     * las variables del servicio.
     * @Param void
     * @Return void
     * */
    public void mostrar() {

        // Si hubo problemas con la conexión http se manda un msj de error y se retorna.
        if(ServiceTemp.getContenido().compareTo("error")==0) {
            fechaAct.setText("");
            String t = "Unable to resolve:\n" + ServiceTemp.getMyURL() +
                    "\nNo address associated with hostname.\n";
            pantalla.setText(t);
            refresh = false;
            return;
        }
        refresh = false;

        // Si la conexión http fue exitosa se muestra el usuario.
        String textToSet = "Usuario: "+ServiceTemp.getUs();
        if(getSupportActionBar()!=null)
            getSupportActionBar().setTitle(textToSet);

        //Se muestra en pantalla el contenido remarcando las temperaturas y las fechas de las raspbys.
        pantalla.setText(coloreameElString(ServiceTemp.getContenido()), TextView.BufferType.SPANNABLE);

        //Se muestra la fecha y hora en que se realizó la consulta
        String lastRead = "Última lectura: \n" + ServiceTemp.getfReal();
        fechaAct.setText(lastRead);
    }

    /*
     * Función encargada de remarcar las temperaturas y fehcas de las raspby de cada heladera.
     * @Param dato es un String creado en el servicio con los datos obtenidos de la consulta.
     * @Return SpannableString es el valor ya resaltado.
     * */
    public SpannableString coloreameElString(String datos) {
        int marcas=0;
        int[] cuenta = new int[datos.length()+5];
        int j=0;


        // Este for recorre todos los caracteres del parametro dato. Cuando encuentra un salto de
        // linea se almacena la posición para uso posterior.
        for(int i=0; i<datos.length()-5; i++) {
            marcas++;
            if(datos.charAt(i) == '\n') {
                cuenta[j] = marcas;
                j++;
            }
        }
        Log.v("MOSTRAR", "cuenta.length: "+cuenta.length);
        int indice = 1;
        int indeceAux = 0;
        SpannableString spantext = new SpannableString(datos);

        // Bucle principal de la función donde se realiza el resaltado del texto.
        for (int i=0; i<cuenta.length;i++) {

            // Condición que controla que la porción a colorear sean las temperaturas y fehcas.
            // Eso se hace ubicando todas las posiciones pares y sin contar el cero.
            if(cuenta[i]!=0 && i%2==0) {

                // Este if toma como control el vector booleano del servicio.
                // Si el valor actual es true entonces quiere decir que se debe
                // resaltar en rojo de lo contrario se resaltará en verde.
                if(ServiceTemp.getTempTest()[indeceAux]) {
                    if(Double.parseDouble(ServiceTemp.getParte()[indice])<10.00) {
                        if(Double.parseDouble(ServiceTemp.getParte()[indice])<0.00) {
                            spantext.setSpan(new BackgroundColorSpan(Color.rgb(0,150,255)), cuenta[i], cuenta[i] + 9, 0);
                        }else {
                            spantext.setSpan(new BackgroundColorSpan(Color.RED), cuenta[i]+3, cuenta[i] + 12, 0);
                        }
                    }else {
                        spantext.setSpan(new BackgroundColorSpan(Color.RED),cuenta[i]+3, cuenta[i]+11, 0);
                    }
                }else {
                    if(Double.parseDouble(ServiceTemp.getParte()[indice])<10.00) {
                        spantext.setSpan(new BackgroundColorSpan(Color.GREEN), cuenta[i]+3, cuenta[i] + 12, 0);
                    }else {
                        spantext.setSpan(new BackgroundColorSpan(Color.GREEN), cuenta[i]+3, cuenta[i] + 11, 0);
                    }
                }
                // Misma lógica solo que se utiliza como condición el vector fechaTest.
                if(ServiceTemp.getFechasTest()[indeceAux]) {
                    if(Double.parseDouble(ServiceTemp.getParte()[indice])<10.00) {
                        spantext.setSpan(new BackgroundColorSpan(Color.RED), cuenta[i] + 19, cuenta[i] + 38, 0);
                    }else {
                        spantext.setSpan(new BackgroundColorSpan(Color.RED), cuenta[i] + 18, cuenta[i] + 38, 0);
                    }
                } else {
                    if(Double.parseDouble(ServiceTemp.getParte()[indice])<10.00) {
                        spantext.setSpan(new BackgroundColorSpan(Color.GREEN), cuenta[i] + 19, cuenta[i] + 38, 0);
                    } else {
                        spantext.setSpan(new BackgroundColorSpan(Color.GREEN), cuenta[i] + 18, cuenta[i] + 38, 0);
                    }
                }
                indice+=5;
                indeceAux++;
            }
        }
        return spantext;
    }

    /*
     * Función encargada de mostrar una mensaje de alerta una vez que se ejecuta una acción.
     * Requiere una confirmación para seguir avanzando.
     * @Param void
     * @Return void
     * */
    private void showAlertDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle("Cerrar sesión");
        builder.setTitle("¿Está seguro de que quiere cerrar sesión?");
        builder.setPositiveButton("SI", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ServiceTemp.detenerAsyncTask();
                stopService(new Intent(getApplicationContext(),ServiceTemp.class));
                deleteFile("archivoConDatos");
                startActivity(new Intent(getApplicationContext(),Login.class));
                finish();
            }
        });
        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.create().show();
    }
}