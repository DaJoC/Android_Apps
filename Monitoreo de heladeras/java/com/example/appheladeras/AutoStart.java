package com.example.appheladeras;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


/*
 * Clase autoStart cuya función es reescribir el método de la clase padre BroadcastReceiver
 * para que inicie el servicio "ServiceTemp" una vez iniciado Android.
 * */
public class AutoStart extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Se prueba que la acción no sea nula.
        if(intent.getAction() != null) {
            // Si dicha acción es el la  finalización del boot.
            if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED))
                // Inicio el servico "ServiceTemp".
                context.startService(new Intent(context,ServiceTemp.class));
        }
    }
}