package kr.co.bbmc.paycastagent;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import kr.co.bbmc.selforderutil.AuthKeyFile;
import kr.co.bbmc.selforderutil.FileUtils;
import kr.co.bbmc.selforderutil.NetworkUtil;
import kr.co.bbmc.selforderutil.PlayerCommand;
import kr.co.bbmc.selforderutil.ProductInfo;
import kr.co.bbmc.selforderutil.ServerReqUrl;
import kr.co.bbmc.selforderutil.SingCastPlayIntent;


public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";
    private AgentExternalVarApp mAgentExterVarApp;
    private static LocalSendBroadCastTask mBroadCastAsynTask = null;
    private boolean LOG  = false;

    public MyFirebaseMessagingService() {
        super();
//        mAgentExterVarApp = (AgentExternalVarApp) this.getApplication();
//        Context context = getApplicationContext();

        //AgentService.onSetFcmCommandTimer();
        /*  Persister   */
        //SettingEnvPersister.initPrefs(this);

        //sendFcmBroadCast();
/*
        if((mBroadCastAsynTask==null)||(mBroadCastAsynTask.getStatus()==AsyncTask.Status.FINISHED)||mBroadCastAsynTask.isCancelled()) {
            mBroadCastAsynTask = new LocalSendBroadCastTask();
            mBroadCastAsynTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
*/
        //AgentService.onSetFcmStart();
        //onReceiveFcmMessage();

    }
    private void onReceiveFcmMessage() {
//        AgentService.onSetFcmCommandTimer();

        PlayerCommand command = new PlayerCommand();
        Date currentTime = Calendar.getInstance().getTime();
        SimpleDateFormat simpleDateFormat =
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        command.executeDateTime = simpleDateFormat.format(currentTime);
        command.requestDateTime = simpleDateFormat.format(currentTime);
//                                command.command = getString(R.string.str_command_player_restart);
//        String tempCmd = getApplicationContext().getString(R.string.str_command_connect_server);
//        Log.d(TAG, "tempCmd="+tempCmd);
        //command.command = "Connect server command";

        Intent sendIntent = new Intent(SingCastPlayIntent.ACTION_SERVICE_COMMAND);
        Bundle b = new Bundle();
        b.putString("executeDateTime", command.executeDateTime);
        b.putString("requestDateTime", command.requestDateTime);
        b.putString("command", command.command);
        if((command.addInfo!=null)&&(!command.addInfo.isEmpty()))
            b.putString("addInfo", command.addInfo);
        if(LOG)
            Log.d(TAG, "sendBroadcast command = " + command.command);

        String log = String.format("onReceiveFcmMessage() SendBrooadCast command : %s", command.command);
        FileUtils.writeLog(log, "PayCastAgent");

        sendIntent.putExtras(b);


        this.sendBroadcast(sendIntent);

    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
/*
        Log.e(TAG, "From: " + remoteMessage.getFrom());
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            if (true) {
                scheduleJob();
            } else {

                handleNow();

            }
        }

        if (remoteMessage.getNotification() != null) {

            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());

            sendNotification(remoteMessage.getNotification().getBody());

        }
*/
        sendFcmBroadCast();
    }
    /**
     * Schedule a job using FirebaseJobDispatcher.
     */
    private void scheduleJob() {
        // [START dispatch_job]
/*
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(this));
        Job myJob = dispatcher.newJobBuilder()
                .setService(MyJobService.class)
                .setTag("my-job-tag")
                .build();
        dispatcher.schedule(myJob);
*/
        // [END dispatch_job]
    }
    private void handleNow() {

        if(LOG)
            Log.d(TAG, "Short lived task is done.");

    }

    @Override
    public void onNewToken(String s) {
        if(mAgentExterVarApp==null) {
            mAgentExterVarApp = (AgentExternalVarApp) getApplication();
        }

        mAgentExterVarApp.token = s;
        sendRegistrationToServer(s);
    }
    /**
     * Persist token to third-party servers.
     *
     * Modify this method to associate the user's FCM InstanceID token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token) {
        // TODO: Implement this method to send token to your app server.
        ProductInfo pInfo = AuthKeyFile.getProductInfo();
        if(pInfo!=null)
        {

            final String tokenSaveUrl = ServerReqUrl.getServerSaveTokenUrl(mAgentExterVarApp.mStbOpt, getApplicationContext());
            final String tokenParam = AuthKeyFile.getFcmTokenParam();
            new Thread()
            {
                public void run() {
                    if (!NetworkUtil.isConnected(getApplicationContext())) {
//                        sendShowMessage(R.string.Msg_InvalidStbStatusAlert);
                        if(LOG)
                            Log.e(TAG, "Msg_InvalidStbStatusAlert token");
                        return;
                    }

                    String response = NetworkUtil.HttpResponseString(tokenSaveUrl, tokenParam, getApplicationContext(), false);
                    if(LOG)
                        Log.d(TAG, "Token response = " + response);
                }
            }.start();

/*
            AuthKeyFile.onSetFcmToken(token);
            String serverUrl = AuthKeyFile.getAuthRegFCMTokenServer();
            String queryString = AuthKeyFile.getAuthTokenParam();
            String ssl = PropUtil.configValue(getApplicationContext().getString(kr.co.bbmc.selforderutil.R.string.serverSSLEnabled), getApplicationContext());

            if (Boolean.valueOf(ssl)) {
                serverUrl = serverUrl.replace("http://", "https://");
            }
            //-
            URI url = null;
            try {
                url = new URI(serverUrl + queryString);
//                url = new URI(reqUrl+encodedQueryString);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            String response = NetworkUtil.sendFCMTokenToAuthServer(serverUrl, queryString);
            if((response!=null)&&(!response.isEmpty()))
            {
                Log.d(TAG, "sendRegistrationToServer() response="+ response);
            }
*/
        }
    }
    private void sendNotification(String messageBody) {
/*
        Intent intent = new Intent(this, MainActivity.class);

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,

                PendingIntent.FLAG_ONE_SHOT);



//        String channelId = getString(R.string.default_notification_channel_id);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder =

                new NotificationCompat.Builder(this)

                        .setSmallIcon(R.mipmap.ic_launcher)

                        .setContentTitle("FCM Message")

                        .setContentText(messageBody)

                        .setAutoCancel(true)

                        .setSound(defaultSoundUri)

                        .setContentIntent(pendingIntent);
//        new NotificationCompat.Builder(this, channelId)



        NotificationManager notificationManager =

                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

*/
/*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            String channelName = getString(R.string.default_notification_channel_name);

//            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
            NotificationChannel channel = new NotificationChannel(NotificationManager.IMPORTANCE_HIGH);

            notificationManager.createNotificationChannel(channel);

        }
*/
/*
        notificationManager.notify(0, notificationBuilder.build());
*/

    }
    public  class LocalSendBroadCastTask extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... strings) {
            sendFcmBroadCast();
            return null;
        }
    }

    private void sendFcmBroadCast() {
        PlayerCommand command = new PlayerCommand();
        Date currentTime = Calendar.getInstance().getTime();
        SimpleDateFormat simpleDateFormat =
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        command.executeDateTime = simpleDateFormat.format(currentTime);
        command.requestDateTime = simpleDateFormat.format(currentTime);
//                                command.command = getString(R.string.str_command_player_restart);
        command.command = "Connect server command";

        //Intent bIntent = sendAgentCommand(command);

        Intent sendIntent = new Intent(SingCastPlayIntent.ACTION_SERVICE_COMMAND);
        Bundle b = new Bundle();
        b.putString("executeDateTime", command.executeDateTime);
        b.putString("requestDateTime", command.requestDateTime);
        b.putString("command", command.command);
        if(LOG)
            Log.d(TAG, "sendBroadcast command = " + command.command);
        sendIntent.putExtras(b);
        sendBroadcast(sendIntent);

    }

}
