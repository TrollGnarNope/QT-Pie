package com.veigar.questtracker;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.veigar.questtracker.services.receiver.CopyErrorReceiver;
import com.veigar.questtracker.data.HelpCenterRepository;

import java.io.PrintWriter;
import java.io.StringWriter;

public class VeigarApplication extends Application {
    private Context context;
    private Thread.UncaughtExceptionHandler defaultHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler();

        // Set a custom handler to catch uncaught exceptions
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(@NonNull Thread thread, @NonNull Throwable ex) {
                // Create a notification to inform the user about the error
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                String channelId = "error_channel";

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    NotificationChannel channel = new NotificationChannel(channelId, "Error Notifications", NotificationManager.IMPORTANCE_HIGH);
                    notificationManager.createNotificationChannel(channel);
                }

                // Get the stack trace as a string
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                ex.printStackTrace(pw);
                String stackTrace = sw.toString();
                
                // Save crash log to HelpCenterRepository for bug reports
                HelpCenterRepository.INSTANCE.saveCrashLog(context, stackTrace);

                // Create an intent to copy the error to the clipboard
                Intent copyIntent = new Intent(context, CopyErrorReceiver.class);
                copyIntent.putExtra("error_details", stackTrace);
                PendingIntent copyPendingIntent = PendingIntent.getBroadcast(context, 0, copyIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                        .setSmallIcon(android.R.drawable.ic_dialog_alert)
                        .setContentTitle("An error occurred")
                        .setContentText("An unexpected error happened during runtime.")
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText("An unexpected error happened during runtime. Tap to copy details."))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .addAction(android.R.drawable.ic_menu_save, "Copy Error", copyPendingIntent); // Replace with a suitable icon

                notificationManager.notify(5, builder.build());

                // Call the original default handler
                if (defaultHandler != null) {
                    defaultHandler.uncaughtException(thread, ex);
                }
            }
        });
    }
}
