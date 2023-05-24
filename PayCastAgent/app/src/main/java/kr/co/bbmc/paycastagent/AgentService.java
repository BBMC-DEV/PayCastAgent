package kr.co.bbmc.paycastagent;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.iid.FirebaseInstanceId;
import com.sewoo.jpos.command.ESCPOSConst;
import com.sewoo.port.android.WiFiPort;
import com.sewoo.request.android.RequestHandler;

import org.json.JSONException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import kr.co.bbmc.selforderutil.AuthKeyFile;
import kr.co.bbmc.selforderutil.CatagoryObject;
import kr.co.bbmc.selforderutil.CommandAsynTask;
import kr.co.bbmc.selforderutil.CommandObject;
import kr.co.bbmc.selforderutil.DownFileInfo;
import kr.co.bbmc.selforderutil.FileUtils;
import kr.co.bbmc.selforderutil.FtpUploadThread;
import kr.co.bbmc.selforderutil.KioskOrderInfoForPrint;
import kr.co.bbmc.selforderutil.MenuCatagoryObject;
import kr.co.bbmc.selforderutil.MenuObject;
import kr.co.bbmc.selforderutil.NetworkUtil;
import kr.co.bbmc.selforderutil.OptUtil;
import kr.co.bbmc.selforderutil.PlayerCommand;
import kr.co.bbmc.selforderutil.PlayerOptionEnv;
import kr.co.bbmc.selforderutil.ProductInfo;
import kr.co.bbmc.selforderutil.PropUtil;
import kr.co.bbmc.selforderutil.RecvedCommand;
import kr.co.bbmc.selforderutil.ServerReqUrl;
import kr.co.bbmc.selforderutil.SingCastPlayIntent;
import kr.co.bbmc.selforderutil.StbOptionEnv;
import kr.co.bbmc.selforderutil.UdpServerThread;
import kr.co.bbmc.selforderutil.Utils;
import kr.co.bbmc.selforderutil.XmlOptionParser;

import static com.sewoo.jpos.command.ESCPOSConst.LK_FAIL;
import static com.sewoo.jpos.command.ESCPOSConst.LK_SUCCESS;
import static com.sewoo.jpos.command.ESCPOSConst.STS_COVEROPEN;
import static com.sewoo.jpos.command.ESCPOSConst.STS_NORMAL;
import static com.sewoo.jpos.command.ESCPOSConst.STS_PAPEREMPTY;
import static com.sewoo.jpos.command.ESCPOSConst.STS_PAPERNEAREMPTY;
import static kr.co.bbmc.selforderutil.NetworkUtil.pingHost;


public class AgentService extends Service {
    private static boolean LOG = false;
    private static boolean JUBANG = true;
    private int MAX_RETRY_COUNT =2;


    public static int ALARM_CHECK_INTERVAL = 2 * 60 * 1000;
    private static final String TAG = "AgentService";
    private static String MOBILE_PRINT_IP = "192.168.0.217";    //유선
    //private static String MOBILE_PRINT_IP = "192.168.0.192";  //무선
    private static int MAX_AUTH_RETRY_NUM = 5;
    private static int AUTH_TIME_INTERVAL = 100000;
    private static int MENU_PRINT_CHK_TIMER = 2000;
    private static int MAX_PRINTER_STATUS_CHK_INTERVAL = 4000;
    private static int MAX_FCM_TIMER = 60*1000;   //1 min
    private static int SERVER_PING_TIMER = 60000;   //1 min

    //https://test.signcast.co.kr:8281/info/printmenu?storeId=7&catId=2158232002
    private AgentExternalVarApp mAgentExterVarApp;
    private XmlOptionParser mXmlOptUtil;
    private PlayerOptionEnv mPlayerOpt;
    private ServerReqUrl mServrReq;

    /*  Timer   */
    private static Timer mMonitorTimer;
    private static Timer mCommandTimer;
    private commandTimerTask commandTimerTask;
    private static Timer mFileUploadTimer;
    private static Timer mRecCommandFrPlayerTimer;
    private static Timer mMenuPrintChkTimer;
    private static Timer mPrintChkTimer;
    private static Timer mFcmTimer;
    private static Timer mFcmChkTimer;

    private static Timer mPeriodicTimer;
    private PerodiTimerTask mPeriodicTimerTask;

    /*  Async Task  */
    private static CommandAsynTask mCommandAsynTask = null;

    private boolean isDownloading = false;  //download 진행 중
    private boolean isUploading = false; //upload 진행 중

    private List<String> uploadList = new ArrayList<String>();
    private List<String> newuploadList = new ArrayList<String>();
    private List<CommandObject> commandList = new ArrayList<CommandObject>();
    private List<CommandObject> newcommandList = new ArrayList<CommandObject>();
    private List<DownFileInfo> downloadList = new ArrayList<DownFileInfo>();
    private List<RecvedCommand> mRecievedCmdList = new ArrayList<RecvedCommand>();

    private UdpServerThread udpServerThread;
    private FtpUploadThread mFtpUploadThread;
    private static boolean isAdditionalReportRequired = true;

    /*  Mobile printer */
    private Thread hThread;
    private WiFiPort wifiPort;
    private Vector<String> ipAddrVector;
    private MenuCatagoryObject mMenuObject;
    private CatagoryObject mCatagoryObject;
    private MenuObject mMenuItemObject;

//    private ProductInfo productKey;
    private boolean mAuthVaild = false;
    private Timer mAuthTimer = null;  //auth timer
    private AuthTimerTask authTimerTask = null;
//    private int mAuthRetry = 0;  //auth retry
    private ArrayList<String> mAuthList = new ArrayList<String>();
//    private int mMaxRetryNum = 0;
    private ConnectTask mPrintConnTask = null;
    private boolean mPrintConnSuccess = false;
    private int mPrintConnRetry = 0;

    private MenuPrintCheckAsynTask mMenuPrintCheckAsynTask;
    private AsyncTask mMonitorAsync;
    private BroadcastReceiver mServerConnectReceiver;
    private static boolean mFcmReceived =false;
    private static boolean mOldFcmReceived = false;

    private static monitorReportTask mMonitorReportTask;
    private static fcmCommandTask mFcmCommandTask;
    private static menuPrintCheckTask mMenuPrtTask;
    private static printerCheckTask mPrintChkTask;

    private BroadcastReceiver mAgentCmdReceiver;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        scheduleAlarm();
        mAgentExterVarApp = (AgentExternalVarApp) getApplication();
        mAgentExterVarApp.installHandler();
        mAgentExterVarApp.propUtilInit();
        mAgentExterVarApp.mStbOpt = new StbOptionEnv();
        mAgentExterVarApp.mStbOpt.init();
        mAgentExterVarApp.setContext(getApplicationContext());

        mPlayerOpt = new PlayerOptionEnv();
        mPlayerOpt.init();

        /*  Persister   */
        SettingEnvPersister.initPrefs(this);

        mXmlOptUtil = new XmlOptionParser();

        mMenuObject = new MenuCatagoryObject();

        File bbmcDefault = new File(FileUtils.BBMC_DEFAULT_DIR);
        if (!bbmcDefault.exists()) {
            bbmcDefault.mkdir();
        }
        bbmcDefault = new File(FileUtils.BBMC_PAYCAST_DIRECTORY);
        if (!bbmcDefault.exists())
            bbmcDefault.mkdir();

        bbmcDefault = new File(FileUtils.BBMC_PAYCAST_DATA_DIRECTORY);
        if (!bbmcDefault.exists())
            bbmcDefault.mkdir();

        bbmcDefault = new File(FileUtils.BBMC_PAYCAST_MENU_DIRECTORY);
        if (!bbmcDefault.exists())
            bbmcDefault.mkdir();

        bbmcDefault = new File(FileUtils.BBMC_TEMP_DIRECTORY);
        if (!bbmcDefault.exists())
            bbmcDefault.mkdir();
        else {
            String list[] = FileUtils.getPlayerUpdateVersion(); //delete *player*.apk
            if ((list != null) && (list.length > 0)) {
                for (String fn : list) {
                    File f = new File(FileUtils.BBMC_TEMP_DIRECTORY + fn);
                    if (f.exists())
                        f.delete();
                }
            }

        }
        File dir = FileUtils.makeDirectory(FileUtils.BBMC_PAYCAST_DIRECTORY);

        // 'PayCastAgentOpt.xml' 파일이 초기화되는 현상 떄문에 임시 비활성화
        //File f = FileUtils.makeAgentOptionFile(dir, FileUtils.BBMC_PAYCAST_DATA_DIRECTORY + FileUtils.getFilename(FileUtils.PayCastAgent), getApplication(), mAgentExterVarApp.mStbOpt);
        OptUtil.ReadOptions(FileUtils.PayCastAgent, true, FileUtils.BBMC_PAYCAST_DATA_DIRECTORY + FileUtils.getFilename(FileUtils.PayCastAgent), getApplicationContext());
        mAgentExterVarApp.mStbOpt = mXmlOptUtil.parseAgentOptionXML(FileUtils.BBMC_PAYCAST_DATA_DIRECTORY + FileUtils.getFilename(FileUtils.PayCastAgent), mAgentExterVarApp.mStbOpt, getApplicationContext());
        if(LOG)
            Log.e(TAG, "onCreate() mainPrtEnable="+mAgentExterVarApp.mStbOpt.mainPrtEnable);
        onReadPlayerOption();
        mAgentExterVarApp.mStbOpt.menuName = mPlayerOpt.optionDefaultMenuFile;

        if((mAgentExterVarApp.mStbOpt.menuName!=null)&&(!mAgentExterVarApp.mStbOpt.menuName.isEmpty())) {
            try {
                mMenuObject = parseMenuObject(FileUtils.BBMC_PAYCAST_MENU_DIRECTORY + mAgentExterVarApp.mStbOpt.menuName);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        mAgentExterVarApp.setMenuObject(mMenuObject);

        ArrayList sellerInfolist = new ArrayList();

        onUpdatePrinterStoreInfo(sellerInfolist);
        mAgentExterVarApp.setSellerInfo(sellerInfolist);

        startTimerService();

//        productKey = new ProductInfo(getApplicationContext());

        mAuthTimer = new Timer();
        authTimerTask = new AuthTimerTask();
        mAuthTimer.schedule(authTimerTask, 2000);
        mAuthList = FileUtils.searchByFilefilter(FileUtils.BBMC_PAYCAST_DATA_DIRECTORY, "deviceid", "txt");


        mAgentExterVarApp.token = FirebaseInstanceId.getInstance().getToken();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SingCastPlayIntent.ACTION_SERVICE_COMMAND);

        mAgentCmdReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(LOG)
                    Log.d(TAG, "broadcast receiver() action="+intent.getAction()+" action="+SingCastPlayIntent.ACTION_SERVICE_COMMAND);
                if (intent.getAction().equalsIgnoreCase(SingCastPlayIntent.ACTION_SERVICE_COMMAND)) {
                    Bundle b = intent.getExtras();
                    if(LOG)
                        Log.e(TAG, "broadcast receiver() 1");
                    if (b != null) {
                        PlayerCommand c = new PlayerCommand();
                        SimpleDateFormat simpleDateFormat =
                                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                        c.command = b.getString("command");
                        c.requestDateTime = b.getString("requestDateTime");
                        c.executeDateTime = b.getString("executeDateTime");
                        if(LOG)
                            Log.e(TAG, "broadcast receiver() 2 cmd="+c.command);
                        if (c.command.equals(getString(R.string.str_command_connect_server))) {
                            if(LOG)
                                Log.d(TAG, "connect server");
                            onSetFcmCommandTimer();
                        }
                        else if (c.command.equals(getString(R.string.paycast_agent_info_update))) {
                            mAgentExterVarApp.mStbOpt = mXmlOptUtil.parseAgentOptionXML(FileUtils.BBMC_PAYCAST_DATA_DIRECTORY + FileUtils.getFilename(FileUtils.PayCastAgent), mAgentExterVarApp.mStbOpt, getApplicationContext());
                            mAgentExterVarApp.mStbOpt.menuName = mPlayerOpt.optionDefaultMenuFile;
                            onSetFcmCommandTimer();
                        }
                    }
                }
            }
        };
        registerReceiver(mAgentCmdReceiver, intentFilter);

/*
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SingCastPlayIntent.ACTION_SERVICE_COMMAND);

        mServerConnectReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "ACTION_SERVICE_COMMAND received");
                if(mFcmTimer==null) {
                    mFcmTimer = new Timer("mFcmTimer");
                    mFcmTimer.schedule(new fcmCommandTask(), 0, MAX_FCM_TIMER);

                    mMonitorTimer = new Timer("mMonitorTimer");

                    mMonitorTimer.scheduleAtFixedRate(new monitorReportTask(), 0, mAgentExterVarApp.mStbOpt.monitorMins * 10000);

                    mMenuPrintChkTimer = new Timer("mMenuPrintChkTimer");

                    mMenuPrintChkTimer.scheduleAtFixedRate(new menuPrintCheckTask(), 0, MENU_PRINT_CHK_TIMER);

                    mCommandTimer = new Timer("mCommandTimer");
                    mCommandTimer.scheduleAtFixedRate(new commandTimerTask(), 0, 5000);
                }

            }
        };
        registerReceiver(mServerConnectReceiver, intentFilter);
*/

//test를 위한 코드 입니다.--->
/*
        ESCPSample sample = new ESCPSample();
        int prtStatus = LK_FAIL;

        if(sample!=null) {
            try {
                prtStatus = sample.customSample1(null, mAgentExterVarApp.getSellerInfo());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //printer fail
        if(prtStatus!=ESCPOSConst.LK_STS_NORMAL) {
            Log.e("ESCPSample", "1 prtStatus=" + prtStatus);
            String msg = "";
            if ((ESCPOSConst.LK_STS_BATTERY_LOW & prtStatus) > 0)
                msg = "Battery Low\r\n";
            if ((ESCPOSConst.LK_STS_COVER_OPEN & prtStatus) > 0)
                msg = msg + "Cover Open\r\n";
            if ((ESCPOSConst.LK_STS_MSR_READ & prtStatus) > 0)
                msg = msg + "MSR Read status\r\n";
            if ((ESCPOSConst.LK_STS_PAPER_EMPTY & prtStatus) > 0)
                msg = msg + "Paper Empty\r\n";

            Log.e("ESCPSample", "msg=" + msg);
        }
*/
//<------test를 위한 코드 입니다.
    }

    // WiFi Connection method.
    private void wifiConn() {
/*
        if(LOG)
            Log.e(TAG, " wifiConn() 1");
        if(mPrintConnTask==null) {
            if(LOG)
                Log.e(TAG, " wifiConn() 2");
            if(mPrintChkTimer==null) {
                if(LOG)
                    Log.e(TAG, " wifiConn() 3");
                mPrintChkTimer = new Timer();
                if(LOG)
                    Log.e(TAG, " wifiConn() 4");
                mPrintChkTask = new printerCheckTask();
                if(LOG)
                    Log.e(TAG, " wifiConn() 5");
//                mPrintChkTimer.schedule(mPrintChkTask, MAX_PRINTER_STATUS_CHK_INTERVAL);
                mPrintChkTimer.scheduleAtFixedRate(mPrintChkTask, 0, MAX_PRINTER_STATUS_CHK_INTERVAL);

            }
        }
*/
//        new connTask().execute(ipAddr);
    }

    protected void startTimerService() {
        /*  AGENT LOG Delete */
        FileUtils.removeFile(FileUtils.BBMC_LOG_DIR + "*.log");

        /*  DEBUG Delete */
        FileUtils.removeFile(FileUtils.BBMC_DEBUG_DIR + "*.log");

        /*  Report jpg Delete */
        FileUtils.removeFile(FileUtils.BBMC_REPORT_DIR + "*.jpg");
/*
        mMonitorTimer = new Timer("mMonitorTimer");

        mMonitorTimer.scheduleAtFixedRate(new monitorReportTask(), 0, mAgentExterVarApp.mStbOpt.monitorMins * 10000);

        mMenuPrintChkTimer =new Timer("mMenuPrintChkTimer");

        mMenuPrintChkTimer.scheduleAtFixedRate(new menuPrintCheckTask(), 0, MENU_PRINT_CHK_TIMER);

        mCommandTimer = new Timer("mCommandTimer");
        mCommandTimer.scheduleAtFixedRate(new commandTimerTask(), 0, 5000);
*/

/*
        mFileUploadTimer = new Timer("mFileUploadTimer");
        mFileUploadTimer.scheduleAtFixedRate(new fileUploadTimerTask(), 0, 5000);
        mRecCommandFrPlayerTimer = new Timer("mRecCommandFrPlayerTimer");
        mRecCommandFrPlayerTimer.scheduleAtFixedRate(new recCommandFrPlayerTimerTask(), 0, 5000);
*/
        mCommandTimer = new Timer("mCommandTimer");
        commandTimerTask = new  commandTimerTask();
        mCommandTimer.scheduleAtFixedRate(commandTimerTask, 0, 5000);

        mFcmReceived =true;
        mOldFcmReceived = false;

        //mFcmChkTimer = new Timer("mFcmChkTimer");


        //mFcmChkTimer.schedule(new fcmCheckTask(), 0, 1000);
//        mFcmChkTimer.scheduleAtFixedRate(new fcmCheckTask(), 0, 1000);

        mServrReq = new ServerReqUrl();
        onCheckMemory();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mPeriodicTimer!=null)
        {
            mPeriodicTimer.cancel();
            mPeriodicTimerTask.cancel();
        }

        if(mCommandTimer!=null) {
            mCommandTimer.cancel();
            commandTimerTask.cancel();
        }
        if(mAuthTimer!=null)
        {
            mAuthTimer.cancel();
            authTimerTask.cancel();
        }
    }

    public void scheduleAlarm() {
        Intent intent = new Intent(getApplicationContext(), ScAlarmReceiver.class);
        final PendingIntent pIntent = PendingIntent.getBroadcast(this, ScAlarmReceiver.REQUEST_CODE,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        long firstMillis = System.currentTimeMillis(); // alarm is set right away
        AlarmManager alarm = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, firstMillis,
                ALARM_CHECK_INTERVAL, pIntent);
    }

    public void onCancelscheduleAlarm() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent myIntent = new Intent(getApplicationContext(), ScAlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                getApplicationContext(), ScAlarmReceiver.REQUEST_CODE, myIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
    }
    private class fcmCommandTask extends TimerTask {
        @Override
        public void run() {
            //Log.e(TAG, "fcmCommandTask() mFcmReceived isDownloading="+isDownloading+" mFcmReceived="+mFcmReceived+" mOldFcmReceived="+mOldFcmReceived);
                if (isDownloading != true)
                {
                    if (mMonitorTimer != null) {
                        if(LOG)
                            Log.e(TAG, "fcmCommandTask() monitorTimer cancel");
                        mMonitorTimer.cancel();
                        mMonitorTimer = null;
                        if(mMonitorReportTask!=null)
                            mMonitorReportTask.cancel();
                        mMonitorReportTask = null;
                        if ((mMonitorAsync != null) && (!mMonitorAsync.isCancelled()) && !mMonitorAsync.getStatus().equals((AsyncTask.Status.FINISHED)))
                            mMonitorAsync.cancel(true);
                    }
                    if (mMenuPrintChkTimer != null) {
                        if(LOG)
                            Log.e(TAG, "fcmCommandTask() mMenuPrintChkTimer cancel");
                        mMenuPrintChkTimer.cancel();
                        mMenuPrintChkTimer = null;
                        if(mMenuPrtTask!=null)
                            mMenuPrtTask.cancel();
                        mMenuPrtTask = null;
                        if(mMenuPrintCheckAsynTask!=null) {
                            mMenuPrintCheckAsynTask.cancel(true);
                            mMenuPrintCheckAsynTask = null;
                        }

                    }
                    if(mPrintChkTimer != null)
                    {
                        if(wifiPort.isConnected()) {
                            if(LOG)
                                Log.e(TAG, "fcmCommandTask() wifiDisConn()..");
                            try {
                                wifiDisConn();
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                        mPrintChkTimer.cancel();
                        mPrintChkTimer = null;
                        if(mPrintChkTask!=null)
                            mPrintChkTask.cancel();
                        mPrintChkTask = null;
                        mAgentExterVarApp.setPrinterSample(null);
                        if(mPrintConnTask!=null)
                            mPrintConnTask.cancel(true);
                        mPrintConnTask = null;

                    }
                    if (mFcmReceived == mOldFcmReceived) {
                        mFcmReceived = false;
                        mOldFcmReceived = false;
                    }
                    mFcmTimer = null;
                    mFcmCommandTask = null;
                } else {
                    {
                        String log = String.format("fcmCommandTask() isDownloading=true");
                        FileUtils.writeLog(log, "PayCastAgent");
                    }
                    if (mFcmTimer != null) {
                        mFcmCommandTask = new fcmCommandTask();
                        mFcmTimer.schedule(mFcmCommandTask, MAX_FCM_TIMER);
                        String log = String.format("fcmCommandTask() mFcmTimer set");
                        FileUtils.writeLog(log, "PayCastAgent");
                    }
                }
        }
    }
    static int count = 0;
    private class menuPrintCheckTask extends TimerTask {
        @Override
        public void run() {
            if(LOG)
                Log.d(TAG, "1 menuPrintCheckTask() isDownloading=" + isDownloading);
            if (isDownloading != true) {
                {
                    if((mMenuPrintCheckAsynTask==null)||(mMenuPrintCheckAsynTask.isCancelled())||mMenuPrintCheckAsynTask.getStatus().equals((AsyncTask.Status.FINISHED))) {
                        mMenuPrintCheckAsynTask = new MenuPrintCheckAsynTask();
                        mMenuPrintCheckAsynTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
                        {
                            String log = String.format("menuPrintCheckTask() mMenuPrintCheckAsynTask() setting");
                            FileUtils.writeLog(log, "PayCastAgent");
                        }

                    }
                }
            }
            else
            {
                String log = String.format("menuPrintCheckTask() isDownloading=true");
                FileUtils.writeLog(log, "PayCastAgent");
            }
        }
    }
    private class menuPrintCheckTask_old extends TimerTask {
        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            mMenuPrintChkTimer.cancel();
            mMenuPrintChkTimer = new Timer("mMenuPrintChkTimer");

            mMenuPrintChkTimer.scheduleAtFixedRate(new menuPrintCheckTask(), 0,  5000);
            if(LOG)
                Log.d(TAG, "menuPrintCheckTask() finalize() menuPrintCheckTask_old SET");
        }

        @Override
        public void run() {
            if(LOG)
                Log.d(TAG, "2 menuPrintCheckTask() isDownloading=" + isDownloading);
            if (isDownloading != true) {
                MenuPrintCheckAsynTask menuPrintCheckAsynTask = new MenuPrintCheckAsynTask();
                menuPrintCheckAsynTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");

            }
        }
    }
    private boolean mFcmCheckRun = false;
    private class fcmCheckTask extends TimerTask {
        @Override
        public void run() {
            mFcmCheckRun = true;
            //Log.d(TAG, " fcmCheckTask() 1 mFcmReceived=" + mFcmReceived+" mOldFcmReceived="+mOldFcmReceived);
            if(mOldFcmReceived!=mFcmReceived) {
                //Log.e(TAG, " fcmCheckTask() 2 mFcmReceived=" + mFcmReceived+" mOldFcmReceived="+mOldFcmReceived);
                if (mFcmReceived) {

                    //Log.e(TAG, " fcmCheckTask() 2-1 mFcmReceived=" + mFcmReceived+" mOldFcmReceived="+mOldFcmReceived);
                    onSetFcmCommandTimer(); //Timer set

                    PlayerCommand command = new PlayerCommand();
                    Date currentTime = Calendar.getInstance().getTime();
                    SimpleDateFormat simpleDateFormat =
                            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    command.executeDateTime = simpleDateFormat.format(currentTime);
                    command.requestDateTime = simpleDateFormat.format(currentTime);
//                                command.command = getString(R.string.str_command_player_restart);
                    command.command = getString(R.string.str_command_connect_server);

                    Intent bIntent = sendAgentCommand(command);
                    sendBroadcast(bIntent);
                }
            }
            mOldFcmReceived = mFcmReceived;
            mFcmCheckRun = false;
            //Log.e(TAG, " fcmCheckTask() 3 mFcmReceived=" + mFcmReceived+" mOldFcmReceived="+mOldFcmReceived);
        }
    }
    private class monitorReportTask extends TimerTask {

        @Override
        public void run() {
//            Log.d(TAG, "1 monitorReportTask() isDownloading=" + isDownloading);
            if (isDownloading != true) {
                if((mMonitorAsync==null)||(mMonitorAsync.isCancelled())||(mMonitorAsync.getStatus().equals(AsyncTask.Status.FINISHED)||mMonitorAsync.isCancelled())) {
                    mMonitorAsync = new MonitorAsynTask();
                    mMonitorAsync.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
                    {
                        String log = String.format("monitorReportTask() mMonitorAsync() setting");
                        FileUtils.writeLog(log, "PayCastAgent");
                    }
                }
            }
            else
            {
                String log = String.format("monitorReportTask() mMonitorAsync() isDownloading=true");
                FileUtils.writeLog(log, "PayCastAgent");
            }

        }
    }
    private boolean restartFlag = false;

    private class printerCheckTask extends TimerTask {

        @Override
        public void run() {
            if(LOG)
                Log.d(TAG, "LAN CONNECT printerCheckTask=" + mPrintConnSuccess);
            {
               if (mPrintConnTask == null) {
                    mPrintConnTask = (ConnectTask) new ConnectTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mAgentExterVarApp.mStbOpt.mainPrtip);
                }
                else {
                    mPrintConnRetry++;
                    if (mPrintConnTask!=null) {
                        mPrintConnTask.onProgressUpdate();

                        if (mPrintConnSuccess==false) {
                            if((mPrintConnTask!=null)&&mPrintConnTask.getStatus().equals(AsyncTask.Status.FINISHED))
                            {
                                mPrintConnTask = (ConnectTask) new ConnectTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mAgentExterVarApp.mStbOpt.mainPrtip);
                            }
                        }

                    }
                    }
            }

        }
    }


    private class commandTimerTask extends TimerTask {

        @Override
        public void run() {
            if ((mCommandAsynTask == null)||(mCommandAsynTask.isCancelled())||mCommandAsynTask.getStatus().equals(AsyncTask.Status.FINISHED)) {
                mCommandAsynTask = new CommandAsynTask();
                mCommandAsynTask.setApplication( mAgentExterVarApp.mStbOpt, getApplication(), new CommandAsynTask.onExecuteCommandListener() {
                    @Override
                    public String exeCommand(CommandObject ci) {
                        return executeCommand(ci);
                    }

                    @Override
                    public ArrayList<CommandObject> getNewCommandList() {
                        if(mAgentExterVarApp.newcommandList==null)
                            mAgentExterVarApp.newcommandList = new ArrayList<CommandObject>();
                        return mAgentExterVarApp.newcommandList;
                    }
                });
                mCommandAsynTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                {
                    String log = String.format("mCommandAsynTask() mCommandAsynTask setting");
                    FileUtils.writeLog(log, "PayCastAgent");
                }

            }

        }
    }
    private class PerodiTimerTask extends TimerTask {

        @Override
        public void run() {
            onSetFcmCommandTimer();
        }
    }

    class MenuPrintCheckAsynTask extends AsyncTask {
        public boolean isRun = false;
        @Override
        protected Object doInBackground(Object[] objects) {
            String reqUrl = null;
            String infoStr = null;
            isRun=true;
            if (!NetworkUtil.isConnected(getApplicationContext())) {
                Utils.LOG(getString(R.string.Msg_InvalidStbStatusAlert));
                return null;
            }
            if((NetworkUtil.getConnectivityStatus(getApplicationContext())==1)&&(NetworkUtil.getWifiRssi(getApplicationContext())<(-70)))
            {
                PlayerCommand command = new PlayerCommand();
                Date currentTime = Calendar.getInstance().getTime();
                SimpleDateFormat simpleDateFormat =
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                command.executeDateTime = simpleDateFormat.format(currentTime);
                command.requestDateTime = simpleDateFormat.format(currentTime);
                command.command = getString(R.string.paycast_did_network_instability);
                Intent bIntent = sendAgentCommand(command);
                sendBroadcast(bIntent);
                return null;

            }
            else
            {
                PlayerCommand command = new PlayerCommand();
                Date currentTime = Calendar.getInstance().getTime();
                SimpleDateFormat simpleDateFormat =
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                command.executeDateTime = simpleDateFormat.format(currentTime);
                command.requestDateTime = simpleDateFormat.format(currentTime);
                command.command = getString(R.string.paycast_did_network_stability);
                Intent bIntent = sendAgentCommand(command);
                sendBroadcast(bIntent);
            }

            reqUrl = ServerReqUrl.getServerPaymentInfoUrl(mAgentExterVarApp.mStbOpt, getApplicationContext());
//            infoStr = "?storeId=" + mAgentExterVarApp.mStbOpt.storeId+"&catId="+ mAgentExterVarApp.mStbOpt.storeCatId;
            infoStr = "?storeId=" + mAgentExterVarApp.mStbOpt.storeId+"&deviceId="+ mAgentExterVarApp.mStbOpt.deviceId;
            URI url = null;
            String tempString = reqUrl + infoStr;
            if(LOG)
                Log.d(TAG, "1 tempString=" + tempString);
            String res = NetworkUtil.onGetOrderPrintStringFrServer(tempString);
            if(LOG)
                Log.d(TAG, "onGetOrderPrintStringFrServer() 1 RES=" + res);
            if ((res != null)&&(!res.isEmpty())) {
                    ArrayList<KioskOrderInfoForPrint> printArrayList = mXmlOptUtil.onParseOrderMenuPrintJson(res, mAgentExterVarApp.ISVERSION);
                    if((printArrayList==null)||(printArrayList.size()==0))
                        return null;
                    mAgentExterVarApp.setPrintList(printArrayList);
                    if((mPrintConnTask==null)||(mPrintConnTask.getStatus().equals(Status.FINISHED))||mPrintConnTask.isCancelled()) {
                        mPrintConnTask = (ConnectTask) new ConnectTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mAgentExterVarApp.mStbOpt.mainPrtip);
                        int count = 0;
                        while(mPrintConnSuccess!=true)
                        {
                            count++;
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            if(count>20)
                                break;
                        }
                        if(mPrintConnSuccess==false) {
                            mAgentExterVarApp.setPrintList(null);
                            return null;
                        }

                    }
//                ParseXML(res);
            }
            else
                return null;
            ArrayList<KioskOrderInfoForPrint> printArrayList = mAgentExterVarApp.getPrintList();
            if(printArrayList==null)
                return null;
            String urlStr = "";
            String param = "";

            ESCPSample sample = mAgentExterVarApp.getPrinterSample();

            int prtStatus = LK_FAIL;

            if(sample!=null) {
                try {
                    prtStatus = sample.customSample1(printArrayList, mAgentExterVarApp.getSellerInfo(), mAgentExterVarApp);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            //printer fail
            if(prtStatus!=STS_NORMAL)
            {
                if(LOG)
                    Log.e("ESCPSample", "1 prtStatus="+prtStatus);
                String msg = "";
                if((STS_COVEROPEN& prtStatus) > 0)
                    msg = msg + "Cover Open\r\n";
                if((STS_PAPERNEAREMPTY & prtStatus) > 0)
                    msg = msg + "Paper near Empty\r\n";
                if((STS_PAPEREMPTY & prtStatus) > 0)
                    msg = msg + "Paper Empty\r\n";

                if(LOG)
                    Log.e("ESCPSample", "msg="+msg);



                for(int i = 0; i<printArrayList.size(); i++)
                {
                    KioskOrderInfoForPrint printItem = printArrayList.get(i);
                    printItem.printOk = false;
                }
                mOldFcmReceived=false;
                mFcmReceived = true;
                return null;
            }

            try {
                urlStr = ServerReqUrl.reportUrlServerMenuPrintKitchen(printArrayList, mAgentExterVarApp.mStbOpt, getApplicationContext());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            try {
                param = ServerReqUrl.reportParamServerMenuPrintKitchen(printArrayList, mAgentExterVarApp.mStbOpt, getApplicationContext());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            String response = NetworkUtil.HttpResponseString(urlStr, param, mAgentExterVarApp.mPropUtil);
            if(LOG)
                Log.d(TAG, "MenuPrintCheckAsynTask onPostExecute()  response=" + response);
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            mMenuPrintCheckAsynTask = null;
            isRun = false;
            if (wifiPort.isConnected()) {
                try {
                    wifiDisConn();
                    mPrintConnSuccess = false;
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            else
                mPrintConnSuccess = false;


/*
            if(JUBANG)
            {
                if(wifiPort!=null) {
                    if (wifiPort.isConnected()) {
                        try {
                            wifiDisConn();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
*/
        }

    }
    private int monRetryCount = -1;
    class MonitorAsynTask extends AsyncTask {
        @Override
        protected Object doInBackground(Object[] objects) {
            String reqUrl = null;
            String infoStr = null;
            Log.d(TAG, "1-1 monitor onGetOrderPrintStringFrServer() atEnabled=" + mAgentExterVarApp.mStbOpt.atEnable);

            if (!NetworkUtil.isConnected(getApplicationContext())) {
                Utils.LOG(getString(R.string.Msg_InvalidStbStatusAlert));
                return null;
            }
            else if((NetworkUtil.getConnectivityStatus(getApplicationContext())==1)&&(NetworkUtil.getWifiRssi(getApplicationContext())<(-70)))
            {
                PlayerCommand command = new PlayerCommand();
                Date currentTime = Calendar.getInstance().getTime();
                SimpleDateFormat simpleDateFormat =
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                command.executeDateTime = simpleDateFormat.format(currentTime);
                command.requestDateTime = simpleDateFormat.format(currentTime);
                command.command = getString(R.string.paycast_did_network_instability);
                Intent bIntent = sendAgentCommand(command);
                sendBroadcast(bIntent);
                return null;

            }
            else
            {
                PlayerCommand command = new PlayerCommand();
                Date currentTime = Calendar.getInstance().getTime();
                SimpleDateFormat simpleDateFormat =
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                command.executeDateTime = simpleDateFormat.format(currentTime);
                command.requestDateTime = simpleDateFormat.format(currentTime);
                command.command = getString(R.string.paycast_did_network_stability);
                Intent bIntent = sendAgentCommand(command);
                sendBroadcast(bIntent);
            }

            reqUrl = ServerReqUrl.getServerChgInfoUrl(mAgentExterVarApp.mStbOpt, getApplicationContext());
//            infoStr = String.valueOf("?storeId=" + mAgentExterVarApp.getMenuObject().storeId);
            infoStr = String.format("?storeId=%s&deviceId=%s" , mAgentExterVarApp.mStbOpt.storeId,mAgentExterVarApp.mStbOpt.deviceId);
            URI url = null;
            String tempString = reqUrl + infoStr;
            if(LOG)
                Log.d(TAG, "1 tempString=" + tempString);
            String res = NetworkUtil.onGetStoreChgSync(tempString);
            if(LOG)
                Log.d(TAG, "monitor onGetOrderPrintStringFrServer() 1 RES=" + res);
            if ((res != null)&&(!res.isEmpty())) {
                Log.d(TAG, "1 monitor onGetOrderPrintStringFrServer() atEnabled=" + mAgentExterVarApp.mStbOpt.atEnable);
                ParseXML(res);
                Log.d(TAG, "2 monitor onGetOrderPrintStringFrServer() atEnabled=" + mAgentExterVarApp.mStbOpt.atEnable);
            }
            else
            {
                monRetryCount++;
                if(mAuthRetry>=MAX_RETRY_COUNT)
                {
                    PlayerCommand command = new PlayerCommand();
                    Date currentTime = Calendar.getInstance().getTime();
                    SimpleDateFormat simpleDateFormat =
                            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    command.executeDateTime = simpleDateFormat.format(currentTime);
                    command.requestDateTime = simpleDateFormat.format(currentTime);
                    command.command = getString(R.string.paycast_did_network_instability);
                    Intent bIntent = sendAgentCommand(command);
                    sendBroadcast(bIntent);
                    monRetryCount=-1;

                }

            }
            return null;
        }
    }

    public boolean ParseXML(String fls) {
        CommandObject cmd = null;
        boolean addCmd = true;

        if((fls==null)||(fls.isEmpty()))
            return false;
        try {

            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            InputStream is = new ByteArrayInputStream(fls.getBytes());
            if(LOG) {
                if (is == null)
                    Log.e(TAG, "InputStream... null!!!!");
            }
            if (is == null)
                return false;
            parser.setInput(is, null);
            int eventType = parser.getEventType();
            StbOptionEnv tempStbOpt = new StbOptionEnv();

            copyStbOption(mAgentExterVarApp.mStbOpt, tempStbOpt);
            boolean parErr = false;

            while (eventType != XmlPullParser.END_DOCUMENT) {

                switch (eventType) {
                    case XmlPullParser.START_TAG: {
                        String name = parser.getName();
                        if(LOG)
                            Log.d(TAG, "START_TAG.name=" + name);
                        if (name.equals(getResources().getString(R.string.server))) {

                            String ref = parser.getAttributeValue(null, "ref");
                            //Log.d(TAG, "ref:" + ref);
                            for (int i = 0; i < parser.getAttributeCount(); i++) {
                                String attrName = parser.getAttributeName(i);
                                if(LOG)
                                    Log.d(TAG, "mainPrtEnable parser.getAttributeName[" + i + "]" + parser.getAttributeName(i));
                                if (attrName.equals(getResources().getString(R.string.ftpActiveMode))) {
                                    tempStbOpt.ftpActiveMode = Boolean.valueOf(parser.getAttributeValue(i));
                                    //Log.d(TAG, "FtpActiveMode:" + mStbOpt.ftpActiveMode);
                                } else if (attrName.equals(getResources().getString(R.string.monitorMins))) {
                                    int monitorMin = Integer.valueOf(parser.getAttributeValue(i));
                                    if((monitorMin > 0)&&(monitorMin<=3))
                                        tempStbOpt.monitorMins = monitorMin;
                                    else
                                        parErr = true;
                                    //Log.d(TAG, "MonitorMins:" + mStbOpt.monitorMins);
                                } else if (attrName.equals(getResources().getString(R.string.playerStart))) {
                                    tempStbOpt.playerStart = Boolean.parseBoolean(parser.getAttributeValue(i));
                                    //Log.d(TAG, "PlayerStart:" + mStbOpt.playerStart);
                                } else if (attrName.equals(getResources().getString(R.string.ftpHost))) {
                                    String ftphost = parser.getAttributeValue(i);
                                    if(ftphost!=null&&!ftphost.isEmpty())
                                        tempStbOpt.ftpHost = ftphost;
                                    else
                                        parErr = true;
                                    //Log.d(TAG, "Server.FtpHost:" + mStbOpt.ftpHost);
                                } else if (attrName.equals(getResources().getString(R.string.ftpPassword))) {
                                    String ftppw = parser.getAttributeValue(i);
                                    if((ftppw!=null)&&(!ftppw.isEmpty()))
                                        tempStbOpt.ftpPassword = ftppw;
                                    else
                                        parErr = true;
                                    //Log.d(TAG, "Server.FtpPassword:" + mStbOpt.ftpPassword);
                                } else if (attrName.equals(getResources().getString(R.string.ftpPort))) {
                                    int ftpport = Integer.valueOf(parser.getAttributeValue(i));
                                    if(ftpport > 0)
                                        tempStbOpt.ftpPort = ftpport;
                                    else
                                        parErr = true;
                                    //Log.d(TAG, "Server.FtpPort:" + mStbOpt.ftpPort);
                                } else if (attrName.equals(getResources().getString(R.string.ftpUser))) {
                                    String ftpUser = parser.getAttributeValue(i);
                                    if((ftpUser!=null)&&(!ftpUser.isEmpty()))
                                        tempStbOpt.ftpUser = ftpUser;
                                    else
                                        parErr = true;
                                } else if (attrName.equals(getResources().getString(R.string.serverHost))) {
                                    String serverHost = parser.getAttributeValue(i);
                                    if((serverHost!=null)&&(!serverHost.isEmpty()))
                                        tempStbOpt.serverHost = serverHost;
                                    else
                                        parErr = true;
                                    //Log.d(TAG, "Server.ServerHost:" + mStbOpt.serverHost);
                                } else if (attrName.equals(getResources().getString(R.string.serverPort))) {
                                    int serverPort = Integer.valueOf(parser.getAttributeValue(i));
                                    if(serverPort > 0)
                                        tempStbOpt.serverPort = serverPort;
                                    else
                                        parErr = true;
                                    //Log.d(TAG, "Server.ServerPort:" + mStbOpt.serverPort);
                                } else if (attrName.equals(getResources().getString(R.string.serverUkid))) {
                                    String serverUkid = parser.getAttributeValue(i);
                                    if((serverUkid!=null)&&(!serverUkid.isEmpty()))
                                        tempStbOpt.serverUkid = serverUkid;
                                    else
                                        parErr = true;
                                    //Log.d(TAG, "Server.ServerUkid:" + mStbOpt.serverUkid);
                                } else if (attrName.equals(getResources().getString(R.string.stbId))) {
                                    int stbId = Integer.valueOf(parser.getAttributeValue(i));

                                    if(stbId > 0)
                                        tempStbOpt.stbId = stbId;
                                    if (tempStbOpt.stbId > 0)
                                    {
                                        if(tempStbOpt.stbStatus == 0)
                                            tempStbOpt.stbStatus = 5;
                                    }
                                    else
                                        parErr = true;

                                    //Log.d(TAG, "Server.StbId:" + mStbOpt.stbId);
                                } else if (attrName.equals(getResources().getString(R.string.stbName))) {
                                    String stbName = parser.getAttributeValue(i);
                                    if((stbName!=null)&&(!stbName.isEmpty()))
                                        tempStbOpt.stbName = stbName;
                                    else
                                        parErr = true;
                                    //Log.d(TAG, "Server.StbName:" + mStbOpt.stbName);
                                } else if (attrName.equals(getResources().getString(R.string.stbServiceType))) {
                                    String stbServiceType = parser.getAttributeValue(i);
                                    if((stbServiceType!=null)&&!stbServiceType.isEmpty())
                                        tempStbOpt.stbServiceType = stbServiceType;
                                    else
                                        parErr = true;
                                    //Log.d(TAG, "Server.StbServiceType:" + mStbOpt.stbServiceType);
                                } else if (attrName.equals(getResources().getString(R.string.stbUdpPort))) {
                                    int stbUdpPort = Integer.valueOf(parser.getAttributeValue(i));
                                    if(stbUdpPort > 0)
                                        tempStbOpt.stbUdpPort = stbUdpPort;
                                    else
                                        parErr = true;
                                    //Log.d(TAG, "Server.StbUdpPort:" + mStbOpt.stbUdpPort);
                                } else if (attrName.equals(getResources().getString(R.string.store_name))) {
                                    String storeName = parser.getAttributeValue(i);
                                    if((storeName!=null)&&(!storeName.isEmpty()))
                                        tempStbOpt.storeName = storeName;
                                    //else
                                    //    parErr = true;

                                    //Log.d(TAG, "Server.StbUdpPort:" + mStbOpt.stbUdpPort);
                                } else if (attrName.equals(getResources().getString(R.string.store_addr))) {
                                    String storeAddr = parser.getAttributeValue(i);
                                    if((storeAddr!=null)&&(!storeAddr.isEmpty()))
                                        tempStbOpt.storeAddr = storeAddr;
                                    //else
                                    //    parErr = true;
                                    //Log.d(TAG, "Server.StbUdpPort:" + mStbOpt.stbUdpPort);
                                } else if (attrName.equals(getResources().getString(R.string.business_num))) {
                                    String storeBusinessNum = parser.getAttributeValue(i);
                                    if((storeBusinessNum!=null)&&(!storeBusinessNum.isEmpty()))
                                        tempStbOpt.storeBusinessNum = storeBusinessNum;
                                    //else
                                    //    parErr = true;
                                    //Log.d(TAG, "Server.StbUdpPort:" + mStbOpt.stbUdpPort);
                                } else if (attrName.equals(getResources().getString(R.string.store_tel))) {
                                    String storeTel = parser.getAttributeValue(i);
                                    if((storeTel!=null)&&(!storeTel.isEmpty()))
                                        tempStbOpt.storeTel = storeTel;
                                    //else
                                    //    parErr = true;
                                    //Log.d(TAG, "Server.StbUdpPort:" + mStbOpt.stbUdpPort);
                                } else if (attrName.equals(getResources().getString(R.string.merchant_num))) {
                                    String storeMerchantNum = parser.getAttributeValue(i);
                                    if((storeMerchantNum!=null)&&(!storeMerchantNum.isEmpty()))
                                        tempStbOpt.storeMerchantNum = storeMerchantNum;
                                    //else
                                    //    parErr = true;
                                    //Log.d(TAG, "Server.StbUdpPort:" + mStbOpt.stbUdpPort);
                                } else if (attrName.equals(getResources().getString(R.string.store_catid))) {
                                    String storeCatId = parser.getAttributeValue(i);
                                    if((storeCatId!=null)&&(!storeCatId.isEmpty()))
                                        tempStbOpt.storeCatId = storeCatId;
                                    //else
                                    //parErr = true;
                                    //Log.d(TAG, "Server.StbUdpPort:" + mStbOpt.stbUdpPort);
                                } else if (attrName.equals(getResources().getString(R.string.represent))) {
                                    String storeRepresent = parser.getAttributeValue(i);
                                    if((storeRepresent!=null)&&(!storeRepresent.isEmpty()))
                                        tempStbOpt.storeRepresent = storeRepresent;
                                    //else
                                    //    parErr = true;
                                    //Log.d(TAG, "Server.storeRepresent:" + mAgentExterVarApp.mStbOpt.storeRepresent);
                                } else if (attrName.equals(getResources().getString(R.string.store_id))) {
                                    String storeId = parser.getAttributeValue(i);
                                    if((storeId!=null)&&(!storeId.isEmpty()))
                                        tempStbOpt.storeId = storeId;
                                    else
                                        parErr = true;
                                    //Log.d(TAG, "Server.storeId:" + mAgentExterVarApp.mStbOpt.storeId);
                                } else if (attrName.equals(getResources().getString(R.string.store_operating_time))) {
                                    String operatingTime = parser.getAttributeValue(i);
                                    if((operatingTime!=null)&&(!operatingTime.isEmpty()))
                                        tempStbOpt.operatingTime = operatingTime;
                                    //else
                                    //    parErr = true;
                                } else if (attrName.equals(getResources().getString(R.string.store_introduction_msg))) {
                                    String introMsg = parser.getAttributeValue(i);
                                    if((introMsg!=null)&&(!introMsg.isEmpty()))
                                        tempStbOpt.introMsg = introMsg;
                                    //else
                                    //    parErr = true;
                                } else if (attrName.equals(getResources().getString(R.string.server_device_id))) {
                                    String deviceId = parser.getAttributeValue(i);
                                    if((deviceId!=null)&&(!deviceId.isEmpty()))
                                        tempStbOpt.deviceId = deviceId;
                                    else
                                        parErr = true;
                                } else if (attrName.equals(getResources().getString(R.string.main_print_enable))) {
                                    String mainPrtEnable = parser.getAttributeValue(i);
                                    if((mainPrtEnable!=null)&&(!mainPrtEnable.isEmpty())) {
                                        tempStbOpt.mainPrtEnable = mainPrtEnable;
                                        if(LOG)
                                            Log.d(TAG, "ParseXML mainPrtEnable="+tempStbOpt.mainPrtEnable);
                                    }
                                    else
                                        parErr = true;
                                } else if (attrName.equals(getResources().getString(R.string.main_print_ip))) {
                                    String mainPrtip = parser.getAttributeValue(i);
                                    if((mainPrtip!=null)&&(!mainPrtip.isEmpty()))
                                        tempStbOpt.mainPrtip = mainPrtip;
                                    else
                                        parErr = true;
                                } else if (attrName.equals(getResources().getString(R.string.ko_enabled))) {
                                    String enable = parser.getAttributeValue(i);
                                    if((enable!=null)&&(!enable.isEmpty()))
                                        tempStbOpt.koEnable = enable;
                                    else
                                        parErr = true;
                                } else if (attrName.equals(getResources().getString(R.string.at_enabled))) {
                                    String enable = parser.getAttributeValue(i);
                                    Log.d(TAG, "Line 1206 atEnabled="+enable);
                                    if((enable!=null)&&(!enable.isEmpty()))
                                        tempStbOpt.atEnable = enable;
                                    else
                                        parErr = true;
                                } else if (attrName.equals(getResources().getString(R.string.openType))) {
                                    String enable = parser.getAttributeValue(i);
                                    Log.d(TAG, "Line 1216 openType="+enable);
                                    if((enable!=null)&&(!enable.isEmpty()))
                                        tempStbOpt.openType = enable;
                                    else
                                        parErr = true;
                                }

                                if(parErr)
                                {
                                    if(LOG)
                                        Log.e(TAG, "attrName="+attrName+" value="+parser.getAttributeValue(i));
                                }
                            }
                            if(parErr == false) {
								if(LOG)
                                	Log.d(TAG, "Line 1220 atEnabled="+mAgentExterVarApp.mStbOpt.atEnable);
                                if(onCheckStbOpt(mAgentExterVarApp.mStbOpt, tempStbOpt)) {
                                    mAgentExterVarApp.mStbOpt = tempStbOpt;
                                    FileUtils.updateFile(FileUtils.BBMC_PAYCAST_DATA_DIRECTORY + FileUtils.AGENT_OPT_FILE, getApplication(), mAgentExterVarApp.mStbOpt);
                                }
                            }
//                            onAgentRestart();
                        }
                        if (name.equals("command")) {

//                            String ref = parser.getAttributeValue(null, "ref");
                            if(LOG) {
                                Log.d(TAG, "count:" + parser.getAttributeCount());
                                Log.d(TAG, "Text:" + parser.getText());
                            }
                            if (parser.getAttributeCount() == 0) {
                                if (mAgentExterVarApp.mStbOpt.stbStatus == 2)
                                    isAdditionalReportRequired = false;
                                cmd = null;
                            } else
                                cmd = new CommandObject();
                            for (int i = 0; i < parser.getAttributeCount(); i++) {
                                String attrName = parser.getAttributeName(i);
                                Log.d(TAG, "L1244 atEnable="+mAgentExterVarApp.mStbOpt.atEnable);
                                if(LOG)
                                    Log.d(TAG, "parser.getAttributeName[" + i + "]" + parser.getAttributeName(i));
                                if (attrName.equals("rcCommandId")) {
                                    cmd.rcCommandid = parser.getAttributeValue(i);
                                } else if (attrName.equals("command")) {
                                    cmd.command = parser.getAttributeValue(i);
                                } else if (attrName.equals("execTime")) {
                                    cmd.execTime = parser.getAttributeValue(i);
                                } else if (attrName.equals("koEnabled")) {
                                    cmd.koEnabled = parser.getAttributeValue(i);
                                } else if (attrName.equals("atEnabled")) {
                                    cmd.atEnabled = parser.getAttributeValue(i);
                                } else if (attrName.equals("openType")) {
                                    cmd.openType = parser.getAttributeValue(i);
                                } else if (attrName.equals("CDATA")) {
                                    cmd.execTime = parser.getAttributeValue(i);
                                }
                                Log.d(TAG, "L1260 atEnable="+mAgentExterVarApp.mStbOpt.atEnable+" openType="+cmd.openType);

                            }

                            for (CommandObject c : commandList) {
                                if ((cmd.rcCommandid != null) && !cmd.command.isEmpty()) {
                                    if (c.rcCommandid == cmd.rcCommandid) {
                                        addCmd = false;
                                        break;
                                    }
                                } else {
                                    addCmd = false;
                                    break;
                                }
                            }
                            if (cmd == null) {
                                addCmd = false;
                            }
                        }

                    }
                    break;
                    case XmlPullParser.TEXT:
                        String text = parser.getText();
                        if (LOG)
                            Log.d(TAG, "TEXT = " + text);
                        if ((text != null) && !text.isEmpty()) {
                            if (cmd != null)
                                cmd.prameter = text;
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        String name = parser.getName();
                        if (LOG)
                            Log.d(TAG, "END_TAG.name=" + name + "");
                        if (name.equals("command")) {
                            if (cmd != null) {
                                if (addCmd) {
/*
                                    if (mCommandAsynTask.newcommandList == null)
                                        mCommandAsynTask.newcommandList = new ArrayList<>();
                                    mCommandAsynTask.newcommandList.add(cmd);
                                    Log.e(TAG, "mCommandAsynTask.newcommandList=" + mCommandAsynTask.newcommandList.size());
*/
                                    if (mAgentExterVarApp.newcommandList == null)
                                        mAgentExterVarApp.newcommandList = new ArrayList<>();
                                    boolean newAddcmd = true;
                                    if(mAgentExterVarApp.newcommandList.size()>0)
                                    {
                                        for(int c = 0; c<mAgentExterVarApp.newcommandList.size(); c++)
                                        {
                                            CommandObject cmdObj = mAgentExterVarApp.newcommandList.get(c);
                                            if(cmdObj.command.equalsIgnoreCase(cmd.command))
                                            {
                                                if(cmdObj.rcCommandid.equalsIgnoreCase(cmd.rcCommandid)) {
                                                    newAddcmd = false;
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                    if(newAddcmd) {
                                        mAgentExterVarApp.newcommandList.add(cmd);
                                        //Log.e(TAG, "mCommandAsynTask.newcommandList=" + mAgentExterVarApp.newcommandList.size());
                                        Utils.LOG("--------------------");
                                        Utils.LOG(getString(R.string.Log_CmdAdditionalCommand) + " #: " + cmd.rcCommandid);
                                        Utils.LOG(getString(R.string.Log_CmdCommandName) + ": " + cmd.command);
                                        Utils.LOG(getString(R.string.Log_CmdExecTime) + ": " + cmd.execTime);
                                        Utils.LOG(getString(R.string.Log_CmdParameter) + ": " + cmd.prameter);
                                        Utils.LOG(getString(R.string.Log_CmdTotalCommandCount) + ": " + mAgentExterVarApp.newcommandList.size());
                                        Utils.LOG("--------------------");
                                    }
                                }
                            }
                        }
                        break;
                    default:
                        Log.d(TAG, "EVENT TYPE = " + eventType);
                        break;
                }
//                Log.d(TAG, "parser.NEXT="+parser.nextToken());
                eventType = parser.next();

            }


        } catch (Exception e) {
            Log.d(TAG, "2 Error in ParseXML()", e);
            return false;

        }
        return true;

    }

    private boolean onCheckStbOpt(StbOptionEnv stbOpt, StbOptionEnv tempStbOpt) {
        boolean result = false;

        if(tempStbOpt == null)
        {
            return result;
        }
        if(tempStbOpt.monitorMins <= 0)
        {
            return result;
        }

        if(tempStbOpt.stbId <= 0)
        {
            return result;
        }
        if((tempStbOpt.stbName==null)||(tempStbOpt.stbName.isEmpty()))
        {
            return result;
        }
        if(tempStbOpt.stbUdpPort <= 0)
        {
            return result;
        }
        if(tempStbOpt.serverPort<= 0)
        {
            return result;
        }
        if((tempStbOpt.stbServiceType==null)||(tempStbOpt.stbServiceType.isEmpty()))
        {
            return result;
        }
        if((tempStbOpt.serverHost==null)||(tempStbOpt.serverHost.isEmpty()))
        {
            return result;
        }
        if((tempStbOpt.serverUkid==null)||(tempStbOpt.serverUkid.isEmpty()))
        {
            return result;
        }

        if((tempStbOpt.ftpHost==null)||(tempStbOpt.ftpHost.isEmpty()))
        {
            return result;
        }
        if(tempStbOpt.ftpPort<=0)
        {
            return result;
        }
        if((tempStbOpt.ftpUser==null)||(tempStbOpt.ftpUser.isEmpty()))
        {
            return result;
        }
        if((tempStbOpt.ftpPassword==null)||(tempStbOpt.ftpPassword.isEmpty()))
        {
            return result;
        }
/*
        if((tempStbOpt.scheduleName==null)||(tempStbOpt.scheduleName.isEmpty()))
        {
            return result;
        }

 */
        if(tempStbOpt.stbStatus<0) //0: 미확인, 2 : 장비꺼짐, 3:모니터 꺼짐, 4:플레이어 꺼짐, 5:스케줄 미지정, 6: 정상방송
        {
            return result;
        }

        if((tempStbOpt.menuName==null)||(tempStbOpt.menuName.isEmpty()))
        {
            return result;
        }
/*
        if((tempStbOpt.storeName==null)||(tempStbOpt.storeName.isEmpty())) //매장명
        {
            return result;
        }
        if((tempStbOpt.storeAddr==null)||(tempStbOpt.storeAddr.isEmpty())) //매장 주소
        {
            return result;
        }

 */
        if((tempStbOpt.storeBusinessNum==null)||(tempStbOpt.storeBusinessNum.isEmpty())) //매장사업자 번호
        {
            return result;
        }
/*
        if((tempStbOpt.storeTel==null)||(tempStbOpt.storeTel.isEmpty())) //매장 전화번호
        {
            return result;
        }

        if((tempStbOpt.storeMerchantNum==null)||(tempStbOpt.storeMerchantNum.isEmpty())) //매장 가맹점 번호
        {
            return result;
        }
        if((tempStbOpt.storeCatId==null)||(tempStbOpt.storeCatId.isEmpty()))       //kiosk 카드 단말기 cat id
        {
            return result;
        }
        if((tempStbOpt.storeRepresent==null)||(tempStbOpt.storeRepresent.isEmpty()))    //매장 대표자 명
        {
            return result;
        }

 */
        if((tempStbOpt.storeId==null)||(tempStbOpt.storeId.isEmpty()))    //매장 번호
        {
            return result;
        }
        if((tempStbOpt.deviceId==null)||(tempStbOpt.deviceId.isEmpty()))     //deviceId;
        {
            return result;
        }
        if((tempStbOpt.operatingTime==null)||(tempStbOpt.operatingTime.isEmpty()))    //매장 영업시간
        {
            return result;
        }

/*
        if((tempStbOpt.introMsg==null)||(tempStbOpt.introMsg.isEmpty()))         //매장소개글
        {
            return result;
        }

 */
        if((tempStbOpt.mainPrtEnable==null)||(tempStbOpt.mainPrtEnable.isEmpty()))     //true :main print kiosk, false : no main print kiosk
        {
            return result;
        }
        if((tempStbOpt.mainPrtip==null)||(tempStbOpt.mainPrtip.isEmpty()))     //sewoo printer 초기 ip
        {
            return result;
        }
        if((tempStbOpt.koEnable==null)||(tempStbOpt.koEnable.isEmpty()))
        {
            return result;
        }
        if((tempStbOpt.atEnable==null)||(tempStbOpt.atEnable.isEmpty()))
        {
            return result;
        }
        if((tempStbOpt.openType==null)||(tempStbOpt.openType.isEmpty()))   //o (영업중), c (영업종료)
        {
            return result;
        }
/*
        if(tempStbOpt.playerStart==null)
        {
            return result;
        }
        if(tempStbOpt.ftpActiveMode==null)
        {
            return result;
        }
*/
        if(stbOpt.monitorMins != tempStbOpt.monitorMins)
        {
            result =true;
        }

        if(stbOpt.stbId!=tempStbOpt.stbId )
        {
            result =true;
        }
        if(!stbOpt.stbName.equals(tempStbOpt.stbName))
        {
            result =true;
        }
        if(stbOpt.stbUdpPort!=tempStbOpt.stbUdpPort)
        {
            result =true;
        }
        if(stbOpt.serverPort!= tempStbOpt.serverPort)
        {
            result =true;
        }
        if(!stbOpt.stbServiceType.equals(tempStbOpt.stbServiceType))
        {
            result =true;
        }
        if(!stbOpt.serverHost.equals(tempStbOpt.serverHost))
        {
            result =true;
        }
        if(!stbOpt.serverUkid.equals(tempStbOpt.serverUkid))
        {
            result =true;
        }

        if(stbOpt.ftpHost.equals(tempStbOpt.ftpHost))
        {
            result =true;
        }
        if(stbOpt.ftpPort!= tempStbOpt.ftpPort)
        {
            result =true;
        }
        if(!stbOpt.ftpUser.equals(tempStbOpt.ftpUser))
        {
            result =true;
        }
        if(!stbOpt.ftpPassword.equals(tempStbOpt.ftpPassword))
        {
            result =true;
        }

        if((tempStbOpt.scheduleName!=null)&&(stbOpt.scheduleName!=null)) {
            if (!stbOpt.scheduleName.equals(tempStbOpt.scheduleName)) {
                result = true;
            }
        }
        else if((tempStbOpt.scheduleName==null)||(stbOpt.scheduleName==null)) {
            if((tempStbOpt.scheduleName==null)&&(stbOpt.scheduleName!=null))
                result = true;
            else if ((stbOpt.scheduleName==null)&&(tempStbOpt.scheduleName!=null)) {
                result = true;
            }
        }
        if(stbOpt.stbStatus!=tempStbOpt.stbStatus) //0: 미확인, 2 : 장비꺼짐, 3:모니터 꺼짐, 4:플레이어 꺼짐, 5:스케줄 미지정, 6: 정상방송
        {
            result =true;
        }

        if(!stbOpt.menuName.equals(tempStbOpt.menuName))
        {
            result =true;
        }
        if(!stbOpt.storeName.equals(tempStbOpt.storeName)) //매장명
        {
            result =true;
        }

        //매장 주소
        if((tempStbOpt.storeAddr!=null)&&(stbOpt.storeAddr!=null)) {
            if (!stbOpt.storeAddr.equals(tempStbOpt.storeAddr)) {
                result = true;
            }
        }
        else if((tempStbOpt.storeAddr==null)||(stbOpt.storeAddr==null)) {
            if((tempStbOpt.storeAddr==null)&&(stbOpt.storeAddr!=null))
                result = true;
            else if ((stbOpt.storeAddr==null)&&(tempStbOpt.storeAddr!=null)) {
                result = true;
            }
        }

        if(!stbOpt.storeBusinessNum.equals(tempStbOpt.storeBusinessNum)) //매장사업자 번호
        {
            result =true;
        }
        //매장 전화번호
        if((tempStbOpt.storeTel!=null)&&(stbOpt.storeTel!=null)) {
            if (!stbOpt.storeTel.equals(tempStbOpt.storeTel)) {
                result = true;
            }
        }
        else if((tempStbOpt.storeTel==null)||(stbOpt.storeTel==null)) {
            if((tempStbOpt.storeTel==null)&&(stbOpt.storeTel!=null))
                result = true;
            else if ((stbOpt.storeTel==null)&&(tempStbOpt.storeTel!=null)) {
                result = true;
            }
        }

        //매장 가맹점 번호
        if((tempStbOpt.storeMerchantNum!=null)&&(stbOpt.storeMerchantNum!=null)) {
            if (!stbOpt.storeMerchantNum.equals(tempStbOpt.storeMerchantNum)) {
                result = true;
            }
        }
        else if((tempStbOpt.storeMerchantNum==null)||(stbOpt.storeMerchantNum==null)) {
            if((tempStbOpt.storeMerchantNum==null)&&(stbOpt.storeMerchantNum!=null))
                result = true;
            else if ((stbOpt.storeMerchantNum==null)&&(tempStbOpt.storeMerchantNum!=null)) {
                result = true;
            }
        }

        //kiosk 카드 단말기 cat id
        if((tempStbOpt.storeCatId!=null)&&(stbOpt.storeCatId!=null)) {
            if (!stbOpt.storeCatId.equals(tempStbOpt.storeCatId)) {
                result = true;
            }
        }
        else if((tempStbOpt.storeCatId==null)||(stbOpt.storeCatId==null)) {
            if((tempStbOpt.storeCatId==null)&&(stbOpt.storeCatId!=null))
                result = true;
            else if ((stbOpt.storeCatId==null)&&(tempStbOpt.storeCatId!=null)) {
                result = true;
            }
        }

        //매장 대표자 명
        if((tempStbOpt.storeRepresent!=null)&&(stbOpt.storeRepresent!=null)) {
            if (!stbOpt.storeRepresent.equals(tempStbOpt.storeRepresent)) {
                result = true;
            }
        }
        else if((tempStbOpt.storeRepresent==null)||(stbOpt.storeRepresent==null)) {
            if((tempStbOpt.storeRepresent==null)&&(stbOpt.storeRepresent!=null))
                result = true;
            else if ((stbOpt.storeRepresent==null)&&(tempStbOpt.storeRepresent!=null)) {
                result = true;
            }
        }
        if(!stbOpt.storeId.equals(tempStbOpt.storeId))    //매장 번호
        {
            result =true;
        }
        if(!stbOpt.deviceId.equals(tempStbOpt.deviceId))     //deviceId;
        {
            result =true;
        }
        if(!stbOpt.operatingTime.equals(tempStbOpt.operatingTime))    //매장 영업시간
        {
            result =true;
        }
        //매장소개글
        if((tempStbOpt.introMsg!=null)&&(stbOpt.introMsg!=null)) {
            if (!stbOpt.introMsg.equals(tempStbOpt.introMsg)) {
                result = true;
            }
        }
        else if((tempStbOpt.introMsg==null)||(stbOpt.introMsg==null)) {
            if((tempStbOpt.introMsg==null)&&(stbOpt.introMsg!=null))
                result = true;
            else if ((stbOpt.introMsg==null)&&(tempStbOpt.introMsg!=null)) {
                result = true;
            }
        }
        if(!stbOpt.mainPrtEnable.equals(tempStbOpt.mainPrtEnable))     //true :main print kiosk, false : no main print kiosk
        {
            result =true;
        }
        if(!stbOpt.mainPrtip.equals(tempStbOpt.mainPrtip))     //sewoo printer 초기 ip
        {
            result =true;
        }
        if(!stbOpt.koEnable.equals(tempStbOpt.koEnable))
        {
            result =true;
        }
        if(!stbOpt.atEnable.equals(tempStbOpt.atEnable))
        {
            result =true;
        }
        if(!stbOpt.openType.equals(tempStbOpt.openType))   //o (영업중), c (영업종료)
        {
            result =true;
        }
        return true;
    }

    private class fileUploadTimerTask extends TimerTask {
        @Override
        public boolean cancel() {
            if(LOG)
                Log.d(TAG, "fileUploadTimerTask() cancel...");
            return super.cancel();
        }

        @Override
        public void run() {
//            Log.d(TAG, "fileUploadTimerTask() ");
            if (newuploadList.size() > 0) {
                for (String newUpload : newuploadList) {
                    if (uploadList == null)
                        uploadList = new ArrayList<>();
                    uploadList.add(newUpload);
                }
                newuploadList.clear();
                newuploadList = new ArrayList<>();
            }
            if (uploadList.size() > 0) {
                for (String upload : uploadList) {
                    if(LOG)
                        Log.d(TAG, "fileUploadTimerTask() isUploading=" + isUploading);
                    if (isUploading == false) {
                        Utils.LOG(getString(R.string.Log_UploadFile) + ": " + getString(R.string.Log_Success));
                        mFtpUploadThread = new FtpUploadThread();
                        if (mFtpUploadThread.isAlive() == false) {
                            mFtpUploadThread.setApplication(getApplication(), mAgentExterVarApp.mStbOpt, mAgentExterVarApp.uploadList);
                            mFtpUploadThread.start();
                        }
                    }
                }
            }


        }
    }

    private class recCommandFrPlayerTimerTask extends TimerTask {
        @Override
        public boolean cancel() {
//            Log.d(TAG, "commandTimerTask() cancel...");
            return super.cancel();
        }

        @Override
        public void run() {
            List<RecvedCommand> delCommandList = new ArrayList<RecvedCommand>();
            if (mRecievedCmdList.size() > 0) {
                for (RecvedCommand c : mRecievedCmdList) {
/*
                    String result = executeSetSchedule(c.command, c.param1, false);
                    if (result.equals("S")) {
                        //removeCommandByUkid(PlayerCommand.CommandUkid.UserRequest);


                        delCommandList.add(c);
                    }
*/
                    ;
                }
            }
            for (RecvedCommand c : delCommandList) {
                mRecievedCmdList.remove(c);
            }
        }
    }

    // WiFi Connection Task.
    class ConnectTask extends AsyncTask<String, Void, Integer> {
//        private final ProgressDialog dialog = new ProgressDialog(WiFiConnectMenu.this);
        boolean connStatus = false;

        @Override
        protected void onProgressUpdate(Void... values) {
            ESCPSample sample = mAgentExterVarApp.getPrinterSample();

            if((sample==null)||!NetworkUtil.isConnected(getApplicationContext())) {
                super.onProgressUpdate(values);
                return;
            }

            try {
                int printStatus = sample.posPtr.printerSts();   //posPtr.printerCheck() function 과 같이 쓰면 error 남.
                //Log.e(TAG, "LAN CONNECT...onProgressUpdate()= "+wifiPort.isConnected()+"2 posPtr="+printStatus);
                if(!connStatus)
                {
                    if(wifiPort.isConnected())
                    {
                        mOldFcmReceived=false;
                        mFcmReceived = true;
                    }
                }
                connStatus = wifiPort.isConnected();
                if(printStatus==LK_FAIL) {
                    mPrintConnSuccess = false;
                    restartFlag = false;

                    if(wifiPort.isConnected())
                        wifiDisConn();
                }
            } catch (IOException e) {
                e.printStackTrace();
                restartFlag = false;

            } catch (InterruptedException e) {
                e.printStackTrace();
                restartFlag = false;
            }
            super.onProgressUpdate(values);
        }

        @Override
        protected Integer doInBackground(String... params) {
            Integer retVal = null;
            if(LOG)
                Log.d(TAG, "LAN CONNECT...1 doInBackground() param="+params[0]);
            if(!NetworkUtil.isConnected(getApplicationContext())) {
                if(wifiPort!=null) {
                    try {
                        wifiPort.disconnect();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                return retVal;
            }
            try {
                int result = pingHost(params[0]);
                if(LOG)
                    Log.e(TAG, "LAN CONNECT... PING RESULT="+result);
                if(result == 0)
                {
                    if(mAgentExterVarApp.getPrinterSample()==null)
                        mAgentExterVarApp.setPrinterSample(new ESCPSample());

                    try {
                        // ip
                        if(!wifiPort.isConnected()) {
                            Log.e(TAG, "wifiPort.connect() params="+params[0]);
                            wifiPort.connect(params[0]);

                        }
                        retVal = new Integer(0);
                        if(LOG)
                            Log.d(TAG, "LAN CONNECT...3 doInBackground() retVal="+retVal);
                    } catch (IOException e) {
                        if(LOG) {
                            Log.d(TAG, "LAN CONNECT...2 doInBackground()");
                            Log.e(TAG, e.getMessage(), e);
                        }
                        mPrintConnSuccess = false;
                        restartFlag = false;
                        mAgentExterVarApp.setPrinterSample(null);
                        return -1;
                    }
                }
                else
                    retVal = new Integer(-1);

            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return retVal;
        }

        @Override
        protected void onPostExecute(Integer result) {
            if(result==null)
            {
                if(LOG)
                    Log.d(TAG, "LAN CONNECT...fail result null");
                mPrintConnSuccess = false;
            }
            else {
                if (result.intValue() == 0) {
                    if(LOG)
                        Log.d(TAG, "LAN CONNECT...OK");
                    RequestHandler rh = new RequestHandler();
                    hThread = new Thread(rh);
                    hThread.start();
                    mPrintConnSuccess = true;

                } else {
                    if(LOG)
                        Log.d(TAG, "LAN CONNECT...fail");
                    mPrintConnSuccess = false;
                    try {
                        wifiDisConn();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }
            restartFlag = false;
            super.onPostExecute(result);
        }

    }

    // WiFi Disconnection method.
    private void wifiDisConn() throws IOException, InterruptedException {
        if(LOG) {
            Log.d(TAG, "LAN CONNECT...wifiDisConn");
        }
        wifiPort.disconnect();
        if(hThread!=null)
            hThread.interrupt();
    }

    @Override
    public void onRebind(Intent intent) {
        if(LOG)
            Log.d(TAG, "LAN CONNECT...onRebind");
        if (!wifiPort.isConnected()) {
            wifiConn();
            mOldFcmReceived=false;
            mFcmReceived = true;
        }
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if(LOG)
            Log.d(TAG, "LAN CONNECT...onUnbind");
        if (wifiPort.isConnected()) {
            try {
                wifiDisConn();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return super.onUnbind(intent);
    }

    public MenuCatagoryObject parseMenuObject(String fileName) throws FileNotFoundException {
        int areaId = -1;
        int pageId = -1;
        int frameId = -1;
        int contentId = -1;

        File f = new File(fileName);
        if (!f.exists())
            return mMenuObject;
        FileInputStream fis = new FileInputStream(fileName);
        MenuCatagoryObject menuCatagoryObject = mMenuObject;
        if(fis==null)
            return mMenuObject;

        int count = 0;
        //Log.e(TAG, "parseMenuObject()START=========================");
        try {

            XmlPullParserFactory parserFactory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = parserFactory.newPullParser();
            int no = -1;
            String name = null;

            parser.setInput(fis, null);
            //          parser.setInput(str, null) ;

            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_DOCUMENT) {
                    // XML 데이터 시작
                } else if (eventType == XmlPullParser.START_TAG) {
                    String startTag = parser.getName();

//                    Log.d(TAG, "startTag = " + startTag+"start_tag count="+count);
                    if (startTag.equals("Store")) {
                        menuCatagoryObject = new MenuCatagoryObject();
                        mMenuObject = mXmlOptUtil.getStoreCatagory(parser, menuCatagoryObject);
                        mMenuObject.catagoryObjectList = new ArrayList<>();
                    } else if (startTag.equals("Catagory")) {
                        CatagoryObject catagoryObject = new CatagoryObject();
                        mCatagoryObject = mXmlOptUtil.getMenuCatagory(parser, catagoryObject);
                        mCatagoryObject.menuObjectList = new ArrayList<>();
//                        mMenuObject.catagoryObjectList.add(mCatagoryObject);
                    } else if (startTag.equals("Menu")) {
                        MenuObject menuObject = new MenuObject();
                        mMenuItemObject = mXmlOptUtil.getMenuItem(parser, menuObject);
//                        mCatagoryObject.menuObjectList.add(menuObject);

                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    String endTag = parser.getName();
                    switch (endTag) {
                        case "Store":
                            break;
                        case "Catagory":
                            mMenuObject.catagoryObjectList.add(mCatagoryObject);
                            if(LOG)
                                Log.d(TAG, "mMenuObject.catagoryObjectList.size() = " + mMenuObject.catagoryObjectList.size());
                            break;
                        case "Menu":
                            mCatagoryObject.menuObjectList.add(mMenuItemObject);
                            if(LOG)
                                Log.d(TAG, "mCatagoryObject.menuObjectList.size() = " + mCatagoryObject.menuObjectList.size());
                            break;
                    }
                }
                eventType = parser.next();
            }

            if (no == -1 || name == null) {
                // ERROR : XML is invalid.
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Log.e(TAG, "parseMenuObject()END=========================");
        return mMenuObject;
    }

    private void onReadPlayerOption() {

        File dir = FileUtils.makeDirectory(FileUtils.BBMC_PAYCAST_DATA_DIRECTORY);

        //Player option data
        FileUtils.makePlayerOptionFile(dir, FileUtils.BBMC_PAYCAST_DATA_DIRECTORY + FileUtils.getFilename(FileUtils.PayCastPlayer), getApplication(), mPlayerOpt);
        //(int optType, boolean doInitOption, String pathFilename, Context c)
        OptUtil.ReadOptions(FileUtils.PayCastPlayer, true, FileUtils.BBMC_PAYCAST_DATA_DIRECTORY + FileUtils.getFilename(FileUtils.PayCastPlayer), getApplicationContext());
        mPlayerOpt = mXmlOptUtil.parsePlayerOptionXML(FileUtils.BBMC_PAYCAST_DATA_DIRECTORY + FileUtils.getFilename(FileUtils.PayCastPlayer), mPlayerOpt, getApplicationContext());
/*
        try {
            SettingPersister.setPlayerOptionEnv(mPlayerOpt);
        } catch (JSONException e) {
            e.printStackTrace();
        }
*/
    }

    private void onAgentRestart() {
        if(LOG)
            Log.d(TAG, "AGENT_RESTART....");
        Intent mStartActivity = new Intent(getApplicationContext(), AgentActivity.class);
        int mPendingIntentId = 600001;
        PendingIntent mPendingIntent = PendingIntent.getActivity(getApplicationContext(), mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager mgr = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 3000, mPendingIntent);
        android.os.Process.killProcess(android.os.Process.myPid());
        stopSelf();

    }

    private String executeCommand(CommandObject ci) {
        String result = "N";
        String responseStr = "";

        if (ci != null) {

            switch (ci.command) {
                case "StoreInfoChg.bbmc":
                {
                    String log = String.format("executeCommand() ci.command=%s", ci.command);
                    FileUtils.writeLog(log, "PayCastAgent");
                }
                    if(LOG)
                        Log.e(TAG, "StoreInfoChg.bbmc");
                Log.d(TAG, "1 StoreInfoChg.bbmc atEnabled="+mAgentExterVarApp.mStbOpt.atEnable);
                    responseStr = NetworkUtil.getStoreInfoChgFromServer(ci.rcCommandid, getApplicationContext(), mAgentExterVarApp.mStbOpt);
                    if ((responseStr!=null)&&(!responseStr.isEmpty())) {
                        ParseXML(responseStr);
                        result = "Y";
                        ArrayList sellerInfolist;

                        sellerInfolist = mAgentExterVarApp.getSellerInfo();
                        sellerInfolist.clear();
                        sellerInfolist = new ArrayList();

                        onUpdatePrinterStoreInfo(sellerInfolist);
                        mAgentExterVarApp.setSellerInfo(sellerInfolist);
                        Log.d(TAG, "2 StoreInfoChg.bbmc atEnabled="+mAgentExterVarApp.mStbOpt.atEnable);
/*
                        try {
                            FileUtils.updateFile(FileUtils.BBMC_PAYCAST_DATA_DIRECTORY + FileUtils.AGENT_OPT_FILE, getApplication(), mAgentExterVarApp.mStbOpt);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
*/
                        PlayerCommand command = new PlayerCommand();
                        Date currentTime = Calendar.getInstance().getTime();
                        SimpleDateFormat simpleDateFormat =
                                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        command.executeDateTime = simpleDateFormat.format(currentTime);
                        command.requestDateTime = simpleDateFormat.format(currentTime);
//                                command.command = getString(R.string.str_command_player_restart);
                        command.command = getString(R.string.str_command_store_info_change);

                        Intent bIntent = sendAgentCommand(command);
                        sendBroadcast(bIntent);
                    }
                    else if((responseStr==null)||(responseStr.isEmpty()))
                    {
                        return "";
                    }
                    break;
                case "MenuInfoChg.bbmc": {
                    {
                        String log = String.format("executeCommand() ci.command=%s", ci.command);
                        FileUtils.writeLog(log, "PayCastAgent");
                    }
                    if (LOG)
                        Log.e(TAG, "cmd=" + ci.command);

                    PlayerCommand command = new PlayerCommand();
                    Date currentTime = Calendar.getInstance().getTime();
                    SimpleDateFormat simpleDateFormat =
                            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    command.executeDateTime = simpleDateFormat.format(currentTime);
                    command.requestDateTime = simpleDateFormat.format(currentTime);
                    command.command = getString(R.string.str_command_connect_server);

                    Intent bIntent = sendAgentCommand(command);
                    sendBroadcast(bIntent);
                }
                    break;
                case "KioskEnabled.bbmc" : {
                    {
                        String log = String.format("executeCommand() ci.command=%s", ci.command);
                        FileUtils.writeLog(log, "PayCastAgent");
                    }
                    if (LOG)
                        Log.e(TAG, "cmd=" + ci.command);
                    mAgentExterVarApp.mStbOpt.koEnable = ci.koEnabled;
                    mAgentExterVarApp.mStbOpt.atEnable = ci.atEnabled;
                    mAgentExterVarApp.mStbOpt.openType = ci.openType;
                    Log.d(TAG, "Line 1780 atEnabled="+mAgentExterVarApp.mStbOpt.atEnable+" openType="+mAgentExterVarApp.mStbOpt.openType);
                    try {
                        FileUtils.updateFile(FileUtils.BBMC_PAYCAST_DATA_DIRECTORY + FileUtils.AGENT_OPT_FILE, getApplication(), mAgentExterVarApp.mStbOpt);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                    PlayerCommand command = new PlayerCommand();
                    Date currentTime = Calendar.getInstance().getTime();
                    SimpleDateFormat simpleDateFormat =
                            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    command.executeDateTime = simpleDateFormat.format(currentTime);
                    command.requestDateTime = simpleDateFormat.format(currentTime);
                    command.command = getString(R.string.paycast_agent_file_update);

                    Intent bIntent = sendAgentCommand(command);
                    sendBroadcast(bIntent);
                    Log.d(TAG, "1 KioskEnabled.bbmc atEnabled="+mAgentExterVarApp.mStbOpt.atEnable);
                    result = "Y";

                }
                    break;

                default:
                    Log.d(TAG, "1 default : "+ci.command+" atEnabled="+mAgentExterVarApp.mStbOpt.atEnable);
                    break;

            }
        }
        return result;
    }
    private int mAuthRetry = -1;

    private class AuthTimerTask extends TimerTask {
        @Override
        public void run() {
            mAuthTimer.cancel();
            if(!NetworkUtil.isConnected(getApplicationContext()))
            {
                if(LOG)
                    Log.e(TAG, " NETWORK NOT CONNECT");
                mAuthTimer = new Timer();
                mAuthTimer.schedule(new AuthTimerTask(), 2000);
                return;
            }
            if((NetworkUtil.getConnectivityStatus(getApplicationContext())==1)&&(NetworkUtil.getWifiRssi(getApplicationContext())<(-70)))
            {
                PlayerCommand command = new PlayerCommand();
                Date currentTime = Calendar.getInstance().getTime();
                SimpleDateFormat simpleDateFormat =
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                command.executeDateTime = simpleDateFormat.format(currentTime);
                command.requestDateTime = simpleDateFormat.format(currentTime);
                command.command = getString(R.string.paycast_did_network_instability);
                Intent bIntent = sendAgentCommand(command);
                sendBroadcast(bIntent);
                return;

            }
            else
            {
                PlayerCommand command = new PlayerCommand();
                Date currentTime = Calendar.getInstance().getTime();
                SimpleDateFormat simpleDateFormat =
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                command.executeDateTime = simpleDateFormat.format(currentTime);
                command.requestDateTime = simpleDateFormat.format(currentTime);
                command.command = getString(R.string.paycast_did_network_stability);
                Intent bIntent = sendAgentCommand(command);
                sendBroadcast(bIntent);
            }

            boolean deviceFile = false;
            if(mAuthList.size()> 1) {
                for(int i = 0; i < mAuthList.size(); i++) {
                    deviceFile = AuthKeyFile.readKeyFile(getApplicationContext(), mAuthList.get(i));
                    if(deviceFile)
                        break;
                }
            }
            else {
                deviceFile = AuthKeyFile.readKeyFile(getApplicationContext(), FileUtils.BBMC_PAYCAST_DATA_DIRECTORY+"Deviceid.txt");
            }
            if(deviceFile == false)
            {
                PlayerCommand command = new PlayerCommand();
                Date currentTime = Calendar.getInstance().getTime();
                SimpleDateFormat simpleDateFormat =
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                command.executeDateTime = simpleDateFormat.format(currentTime);
                command.requestDateTime = simpleDateFormat.format(currentTime);
//                                command.command = getString(R.string.str_command_player_restart);
                command.command = getString(R.string.str_command_player_not_exist_deviceid_file);

                final Intent bIntent = sendPlayerCommand(command, "", "", "");
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        sendBroadcast(bIntent);
                    }

                }, 3000);
            }
            /*  FCM Token registration  */

            if((mAgentExterVarApp.token!=null)&&(!mAgentExterVarApp.token.isEmpty())) {
                AuthKeyFile.onSetFcmToken(mAgentExterVarApp.token);
                sendRegistrationToServer(mAgentExterVarApp.token);
            }

            final String authServer = AuthKeyFile.getAuthValidationServer();
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

            String param = AuthKeyFile.getAuthKeyParam(getApplicationContext(), getString(R.string.majorVersion));
            final String finalParam = param;

            new Thread() {
                public void run() {
                    if(!NetworkUtil.isConnected(getApplicationContext()))
                    {
//                        sendShowMessage(R.string.Msg_InvalidStbStatusAlert);
                        if(LOG)
                            Log.e(TAG, "Msg_InvalidStbStatusAlert");
                        return;
                    }
                    if((NetworkUtil.getConnectivityStatus(getApplicationContext())==1)&&(NetworkUtil.getWifiRssi(getApplicationContext())<(-70)))
                    {
                        PlayerCommand command = new PlayerCommand();
                        Date currentTime = Calendar.getInstance().getTime();
                        SimpleDateFormat simpleDateFormat =
                                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        command.executeDateTime = simpleDateFormat.format(currentTime);
                        command.requestDateTime = simpleDateFormat.format(currentTime);
                        command.command = getString(R.string.paycast_did_network_instability);
                        Intent bIntent = sendAgentCommand(command);
                        sendBroadcast(bIntent);
                        return;

                    }
                    else
                    {
                        PlayerCommand command = new PlayerCommand();
                        Date currentTime = Calendar.getInstance().getTime();
                        SimpleDateFormat simpleDateFormat =
                                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        command.executeDateTime = simpleDateFormat.format(currentTime);
                        command.requestDateTime = simpleDateFormat.format(currentTime);
                        command.command = getString(R.string.paycast_did_network_stability);
                        Intent bIntent = sendAgentCommand(command);
                        sendBroadcast(bIntent);
                    }

                    String response = NetworkUtil.HttpResponseString(authServer, finalParam, getApplicationContext(), false);
                    if(LOG)
                        Log.d(TAG, "AUTH response = " + response);

                    // 사전 인증 시의 결과는 다음과 같이 성공 flag | AuthKey | UserName 으로 전달됨
                    // S:?|AAAABBBBCCCC|Username
                    if((response!=null)&&(!response.isEmpty())) {
                        PlayerCommand command = new PlayerCommand();
                        Date currentTime = Calendar.getInstance().getTime();
                        SimpleDateFormat simpleDateFormat =
                                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        command.executeDateTime = simpleDateFormat.format(currentTime);
                        command.requestDateTime = simpleDateFormat.format(currentTime);
                        command.command = getString(R.string.paycast_did_network_stability);
                        Intent bIntent = sendAgentCommand(command);
                        sendBroadcast(bIntent);

                        mAuthRetry = -1;

                        if (response.startsWith("S:")) {
                            ArrayList<String> tokens = new ArrayList<>();
                            //= response.split("|" );

                            while (response.length() > 0) {
                                int index = response.indexOf("|");
                                String s = new String();
                                if (index <= 0) {
                                    if (response.length() > 0) {
                                        index = 0;
                                        s = response;
                                        tokens.add(s);
                                    }
                                    break;
                                } else
                                    s = response.substring(0, index);
                                String temp = response.substring(index + 1, response.length());
                                response = temp;
                                tokens.add(s);
                                if (temp.length() <= 0)
                                    break;
                            }
                            ProductInfo productKey = AuthKeyFile.getProductInfo();
                            if (AuthKeyFile.writeKeyFile(productKey.getAuthDeviceId(), tokens.get(2), tokens.get(1),
                                    productKey.getAuthMacAddress(), Integer.parseInt(getString(R.string.majorVersion)), tokens.get(0).substring(2))) {
                                mAuthVaild = true;
                                if(LOG)
                                    Log.d(TAG, "Msg_ProductRegCompleteAlert");

                            } else {
                                mAuthVaild = false;
                                if(LOG)
                                    Log.d(TAG, "Msg_WrongAuthUrl ");
                                //                            sendShowMessage(R.string.Msg_WrongAuthUrl);
                            }
                        } else if (response.equals("Y")) {
                            AuthKeyFile.writeKeyFile("?", "?", "?", "?", 2, "?");
                            mAuthVaild = false;
                        } else if (response.equals("N")) {
                            if(LOG)
                                Log.d(TAG, "Auth ok");
                            mAuthVaild = true;
                        } else {
                            mAuthVaild = false;
                            if(LOG)
                                Log.d(TAG, "Msg_WrongAuthUrl ");
                        }
                    }
                    else if((response==null)||(response.isEmpty()))
                    {
                        if(mAuthRetry>=MAX_RETRY_COUNT)
                        {
                            PlayerCommand command = new PlayerCommand();
                            Date currentTime = Calendar.getInstance().getTime();
                            SimpleDateFormat simpleDateFormat =
                                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            command.executeDateTime = simpleDateFormat.format(currentTime);
                            command.requestDateTime = simpleDateFormat.format(currentTime);
                            command.command = getString(R.string.paycast_did_network_instability);
                            Intent bIntent = sendAgentCommand(command);
                            sendBroadcast(bIntent);
                            mAuthRetry=-1;

                        }
                        mAuthRetry++;
                        mAuthTimer.cancel();
                        mAuthTimer = new Timer();
                        mAuthTimer.schedule(new AuthTimerTask(), 2000);
                        return;
                    }
                    if (mAuthVaild == false) {
//                        if((mAuthRetry>mMaxRetryNum))
                        {
                            PlayerCommand command = new PlayerCommand();
                            Date currentTime = Calendar.getInstance().getTime();
                            SimpleDateFormat simpleDateFormat =
                                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            command.executeDateTime = simpleDateFormat.format(currentTime);
                            command.requestDateTime = simpleDateFormat.format(currentTime);
//                                command.command = getString(R.string.str_command_player_restart);
                            command.command = getString(R.string.str_command_player_auth_fail);

                            final Intent bIntent = sendPlayerCommand(command, "", "", "");
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    sendBroadcast(bIntent);
//                                    mAuthRetry = 0;
                                }

                            }, 3000);
                        }
                        if (mAuthTimer != null)
                            mAuthTimer.cancel();
                        mAuthTimer = new Timer();
                        mAuthTimer.schedule(new AuthTimerTask(), AUTH_TIME_INTERVAL);
                    } else {
                        if (mAuthTimer != null)
                            mAuthTimer.cancel();


                        mAgentExterVarApp.mStbOpt.deviceId = AuthKeyFile.getProductInfo().getAuthDeviceId();

                        PlayerCommand command = new PlayerCommand();
                        Date currentTime = Calendar.getInstance().getTime();
                        SimpleDateFormat simpleDateFormat =
                                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        command.executeDateTime = simpleDateFormat.format(currentTime);
                        command.requestDateTime = simpleDateFormat.format(currentTime);
//                                command.command = getString(R.string.str_command_player_restart);
                        command.command = getString(R.string.str_command_player_deviceid);
                        command.addInfo = mAgentExterVarApp.mStbOpt.deviceId;

                        String stbRes = NetworkUtil.getNetworkInfoFromServer(getApplicationContext(), mAgentExterVarApp.mStbOpt);
                        if ((stbRes != null) && (!stbRes.isEmpty())) {
                            ParseXML(stbRes);
                        }
                        Log.d(TAG, "Line 2077 atEnabled="+mAgentExterVarApp.mStbOpt.atEnable);

                        try {
                            FileUtils.updateFile(FileUtils.BBMC_PAYCAST_DATA_DIRECTORY + FileUtils.AGENT_OPT_FILE, getApplication(), mAgentExterVarApp.mStbOpt);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                        Log.e(TAG, "mAgentExterVarApp.mStbOpt.koEnabled="+mAgentExterVarApp.mStbOpt.koEnable+" atEnable="+mAgentExterVarApp.mStbOpt.atEnable+" openType="+mAgentExterVarApp.mStbOpt.openType);
                        final Intent bIntent = sendPlayerCommand(command, mAgentExterVarApp.mStbOpt.koEnable, mAgentExterVarApp.mStbOpt.atEnable, mAgentExterVarApp.mStbOpt.openType);


                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                sendBroadcast(bIntent);
//                                    mAuthRetry = 0;
                            }

                        }, 100);
/*
                        String stbRes = NetworkUtil.getNetworkInfoFromServer(getApplicationContext(), mAgentExterVarApp.mStbOpt);
                        if ((stbRes != null) && (!stbRes.isEmpty())) {
                            ParseXML(stbRes);
                        }
*/
                        if(mPeriodicTimer!=null) {
                            mPeriodicTimer.cancel();
                            mPeriodicTimerTask.cancel();
                        }
                        mPeriodicTimer = new Timer();
                        mPeriodicTimerTask = new PerodiTimerTask();
                        mPeriodicTimer.scheduleAtFixedRate(mPeriodicTimerTask, 0, 5000);   //5sec 마다 체크
                    }
                }
            }.start();

        }
    }
    public void onSetFcmCommandTimer()
    {
        if(LOG)
            Log.e(TAG, " onSetFcmCommandTimer() mFcmTimer="+mFcmTimer);

        if(mFcmTimer==null) {
            mMonitorTimer = new Timer("mMonitorTimer");
            if(mMonitorTimer!=null) {
                if(mMonitorReportTask==null) {
                    mMonitorReportTask = new monitorReportTask();
                }
                mMonitorTimer.scheduleAtFixedRate(mMonitorReportTask, 0, 30000);
            }

            if(JUBANG) {
                if(LOG)
                    Log.e(TAG, " fcmCheckTask() JUBANG onSetFcmCommandTimer");
                if(wifiPort==null) {
                    wifiPort = WiFiPort.getInstance();
                    if(wifiPort!=null) {
                        wifiConn();
                        if(LOG)
                            Log.e(TAG, " fcmCheckTask() 1 wifiConn()");
                    }
                }
                else
                {
                    if(!wifiPort.isConnected()) {
                        if(LOG)
                            Log.e(TAG, " fcmCheckTask() 2 wifiConn()");
                        wifiConn();
                    }
                }
            }

            if(LOG)
                Log.e(TAG, "onSetFcmCommandTimer() mainPrtEnable="+mAgentExterVarApp.mStbOpt.mainPrtEnable+" atEnabled="+mAgentExterVarApp.mStbOpt.atEnable+" openType="+mAgentExterVarApp.mStbOpt.openType);
            if(mAgentExterVarApp.mStbOpt.mainPrtEnable.equalsIgnoreCase("true"))
            {
                if (mMenuPrintChkTimer != null)
                    mMenuPrintChkTimer.cancel();
                mMenuPrintChkTimer = new Timer("mMenuPrintChkTimer");
                if (mMenuPrintChkTimer != null) {
                    if (mMenuPrtTask == null)
                        mMenuPrtTask = new menuPrintCheckTask();
                    mMenuPrintChkTimer.scheduleAtFixedRate(mMenuPrtTask, 0, MENU_PRINT_CHK_TIMER);
                }
            }
            else
            {
                if (mMenuPrintChkTimer != null) {
                    mMenuPrintChkTimer.cancel();
                    if(mMenuPrtTask!=null)
                        mMenuPrtTask.cancel();
                }
            }
            if(LOG)
                Log.e(TAG, " fcmCheckTask() 1 onSetFcmCommandTimer");
            mFcmTimer = new Timer("mFcmTimer");
            if(mFcmTimer!=null) {
                if(mFcmCommandTask==null)
                    mFcmCommandTask = new fcmCommandTask();
                mFcmTimer.schedule(mFcmCommandTask, MAX_FCM_TIMER);
            }

        }
        else {

            if(LOG)
                Log.e(TAG, " fcmCheckTask() 2 onSetFcmCommandTimer");
        }

    }
    public static void onSetFcmStart()
    {
        //onSetFcmCommandTimer();
/*
        if(mFcmReceived)
            mOldFcmReceived = false;
        mFcmReceived = true;
*/
        if(SettingEnvPersister.getSettingFcmMsgReceived()==false)
            SettingEnvPersister.setSettingFcmMsgReived(true);
        if(LOG)
            Log.d(TAG, "onSetFcmCommandTimer()");
    }

    public static Intent sendPlayerCommand(final PlayerCommand c, String enable, String atenable, String openType) {
        Intent sendIntent = new Intent(SingCastPlayIntent.ACTION_PLAYER_COMMAND);
        Bundle b = new Bundle();
        b.putString("executeDateTime", c.executeDateTime);
        b.putString("requestDateTime", c.requestDateTime);
        b.putString("command", c.command);
        if((c.addInfo!=null)&&(!c.addInfo.isEmpty())) {
            b.putString("addInfo", c.addInfo);
            if((enable!=null)&&(!enable.isEmpty()))
                b.putString("koEnabled", enable);
            if((atenable!=null)&&(!atenable.isEmpty()))
                b.putString("atEnabled", atenable);
            if((openType!=null)&&(!openType.isEmpty()))
                b.putString("openType", openType);
        }
        if(LOG)
            Log.d(TAG, "sendBroadcast command = " + c.command);
        sendIntent.putExtras(b);
//        sendBroadcast(sendIntent);
        return sendIntent;
    }
    public static Intent sendAgentCommand(final PlayerCommand c) {
        Intent sendIntent = new Intent(SingCastPlayIntent.ACTION_SERVICE_COMMAND);
        Bundle b = new Bundle();
        b.putString("executeDateTime", c.executeDateTime);
        b.putString("requestDateTime", c.requestDateTime);
        b.putString("command", c.command);
        if(LOG) {
            Log.d(TAG, "sendBroadcast command = " + c.command);
            String log = String.format("SEndBrooadCast command : %s", c.command);
            FileUtils.writeLog(log, "PayCastAgent");
        }
        sendIntent.putExtras(b);
//        sendBroadcast(sendIntent);
        return sendIntent;
    }
    private void sendRegistrationToServer(String token) {
        // TODO: Implement this method to send token to your app server.
        ProductInfo pInfo = AuthKeyFile.getProductInfo();
        if(pInfo!=null)
        {
            String serverUrl = AuthKeyFile.getAuthRegFCMTokenServer();
            String queryString = AuthKeyFile.getAuthTokenParam();
            String ssl = PropUtil.configValue(getApplicationContext().getString(kr.co.bbmc.selforderutil.R.string.serverSSLEnabled), getApplicationContext());

            String response = NetworkUtil.sendFCMTokenToAuthServer(serverUrl, queryString);
            if((response!=null)&&(!response.isEmpty()))
            {
                if(LOG)
                    Log.d(TAG, "sendRegistrationToServer() response="+ response);
            }
        }
    }
    private void onUpdatePrinterStoreInfo(ArrayList<String> list) {
        if (mAgentExterVarApp.mStbOpt.storeName != null)
            list.add(new String("상호: " + mAgentExterVarApp.mStbOpt.storeName));
        if (mAgentExterVarApp.mStbOpt.storeBusinessNum != null)
            list.add(new String("사업자번호: " + mAgentExterVarApp.mStbOpt.storeBusinessNum));
        if (mAgentExterVarApp.mStbOpt.storeRepresent != null)
            list.add(new String("대표자: " + mAgentExterVarApp.mStbOpt.storeRepresent));
        if (mAgentExterVarApp.mStbOpt.storeTel != null)
            list.add(new String("전화: " + mAgentExterVarApp.mStbOpt.storeTel));
        if (mAgentExterVarApp.mStbOpt.storeAddr != null)
            list.add(new String("주 소: " + mAgentExterVarApp.mStbOpt.storeAddr));
/*
        if (mAgentExterVarApp.mStbOpt.koEnable != null)
            list.add(new String("koEnabled" + mAgentExterVarApp.mStbOpt.koEnable));
        if (mAgentExterVarApp.mStbOpt.atEnable != null)
            list.add(new String("atEnabled" + mAgentExterVarApp.mStbOpt.atEnable));
*/
    }

    private void copyStbOption(StbOptionEnv source, StbOptionEnv dest)
    {
        dest.playerStart = source.playerStart;
        dest.monitorMins = source.monitorMins;

        dest.stbId=source.stbId;
        dest.stbName=source.stbName;
        dest.stbUdpPort=source.stbUdpPort;
        dest.serverPort= source.serverPort;
        dest.stbServiceType=source.stbServiceType;
        dest.serverHost=source.serverHost;
        dest.serverUkid=source.serverUkid;

        dest.ftpActiveMode = source.ftpActiveMode;
        dest.ftpHost=source.ftpHost;
        dest.ftpPort =source.ftpPort;
        dest.ftpUser=source.ftpUser;
        dest.ftpPassword=source.ftpPassword;

        dest.scheduleName=source.scheduleName;
        dest.stbStatus = source.stbStatus;       //0: 미확인, 2 : 장비꺼짐, 3:모니터 꺼짐, 4:플레이어 꺼짐, 5:스케줄 미지정, 6: 정상방송

        dest.menuName= source.menuName;
        dest.storeName= source.storeName;    //매장명
        dest.storeAddr= source.storeAddr;        //매장 주소
        dest.storeBusinessNum= source.storeBusinessNum; //매장사업자 번호
        dest.storeTel= source.storeTel;         //매장 전화번호
        dest.storeMerchantNum= source.storeMerchantNum; //매장 가맹점 번호
        dest.storeCatId= source.storeCatId;       //kiosk 카드 단말기 cat id
        dest.storeRepresent= source.storeRepresent;    //매장 대표자 명
        dest.storeId= source.storeId;    //매장 번호
        dest.deviceId= source.deviceId;     //deviceId;
        dest.operatingTime= source.operatingTime;    //매장 영업시간
        dest.introMsg= source.introMsg;         //매장소개글
        dest.mainPrtEnable= source.mainPrtEnable;         //main print enable?
        dest.mainPrtip= source.mainPrtip;         //main print ip
        dest.koEnable= source.koEnable;         //매장
        dest.atEnable= source.atEnable;         //알림톡
        dest.openType= source.openType;         // O (영업중) / C (영업종료)
		if(LOG)
		{
        	Log.d(TAG, "copyStbOption() dest.atEnable="+dest.atEnable+" openType="+dest.openType);
        	Log.d(TAG, "copyStbOption() dest.koEnable="+dest.koEnable);
		}
    }
    private boolean onCheckMemory() {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        double availableMegs = mi.availMem / 1048576L;
        double totalMegs = mi.totalMem / 1048576L;

        float percentAvaillong = (float)(((float)(availableMegs / totalMegs))*100);
        if(LOG)
        {
            Log.d(TAG, "onCheckMemory() percentAvaillong=" + percentAvaillong+" availableMegs="+availableMegs+ " totalMegs="+totalMegs);
            Utils.DebugAuto("");
            Utils.DebugAuto("onCheckMemory() : Percent="+percentAvaillong+" availMem="+availableMegs+" totalMem="+totalMegs);
        }
        if((100-percentAvaillong) > 80) {
            Utils.DebugAuto("");
            Utils.DebugAuto(String.format("사용가능 메모리 부족\r\n   사용 가능 메모리 : %.0f MB \r\n" ,availableMegs ));
            Utils.DebugAuto(String.format("   전체 메모리 : %.0f MB \r\n" ,totalMegs ));
            FileUtils.deleteDebugFile();
            //return false;
        }

        return true;
    }

}