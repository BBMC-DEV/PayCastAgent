package kr.co.bbmc.paycastagent;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import kr.co.bbmc.selforderutil.CommandObject;
import kr.co.bbmc.selforderutil.DownFileInfo;
import kr.co.bbmc.selforderutil.FileUtils;
import kr.co.bbmc.selforderutil.KioskOrderInfoForPrint;
import kr.co.bbmc.selforderutil.MenuCatagoryObject;
import kr.co.bbmc.selforderutil.PaymentInfoData;
import kr.co.bbmc.selforderutil.PropUtil;
import kr.co.bbmc.selforderutil.StbOptionEnv;

public class AgentExternalVarApp extends Application implements Thread.UncaughtExceptionHandler{
//    private List<NameValuePair> mTransTempFiles = new ArrayList<NameValuePair>();
    public static String TAG =	"AgentExternalVarApp";
    public static int ISVERSION = 2;
    public List<String> uploadList = new ArrayList<String>();
    public StbOptionEnv mStbOpt;
    public ArrayList<CommandObject> newcommandList = new ArrayList<CommandObject>();
    public ArrayList<CommandObject> commandList = new ArrayList<CommandObject>();
    public List<DownFileInfo> downloadList = new ArrayList<DownFileInfo>();
    public PropUtil mPropUtil= null;
    private PaymentInfoData paymentInfoData = new PaymentInfoData();
    private MenuCatagoryObject mStoreMenuInfo;
    private ArrayList<String> mOrderList = null;
    public String token = "";

    private ArrayList mSellerInfo = new ArrayList();
    private ArrayList<KioskOrderInfoForPrint> mPrintList = new ArrayList();
    private ESCPSample printerSample =null;
    private Context mAppContext;

    /**
     * Storage for the original default crash handler.
     */
    private Thread.UncaughtExceptionHandler defaultHandler;

    /**
     * Installs a new exception handler.
     */
    public static void installHandler() {
        if (!(Thread.getDefaultUncaughtExceptionHandler() instanceof AgentExternalVarApp)) {
            Thread.setDefaultUncaughtExceptionHandler(new AgentExternalVarApp());
        }
    }
    public AgentExternalVarApp() {
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }


    public boolean isAdditionalReportRequired = false;
/*
    public List<NameValuePair> getListTransTempFiles(){
        if(mTransTempFiles==null)
            mTransTempFiles = new ArrayList<NameValuePair>();
        return mTransTempFiles;
    }
*/
    public void propUtilInit()
    {
        if(mPropUtil==null)
            mPropUtil = new PropUtil();
        mPropUtil.init(getApplicationContext());

    }
    public PaymentInfoData getPaymentInfoData()
    {
        return paymentInfoData;
    }
    public void setPaymentInfoData(PaymentInfoData pd) {
        paymentInfoData = pd;
    }
    public void setMenuObject(MenuCatagoryObject storemenu) {
        mStoreMenuInfo = storemenu;
    }
    public MenuCatagoryObject getMenuObject() {
        return mStoreMenuInfo;
    }
    public ArrayList<String> getOrderList() {
        return mOrderList;
    }
    public void setSellerInfo(ArrayList<String> list) {
        mSellerInfo = list;
    }
    public ArrayList getSellerInfo() {

        if(mSellerInfo.size()<=0)   //test 용
        {
            String s = new String("BBMC Corp.");
            mSellerInfo.add(s);
            s = new String("서울시 서초구 남부순환로337가길 33 CMC 빌딩 302");
            mSellerInfo.add(s);
            s = new String("Tel:(02)833-8615");
            mSellerInfo.add(s);
        }
        return mSellerInfo;
    }
    public void setPrintList(ArrayList<KioskOrderInfoForPrint> list) {
        mPrintList = list;
    }
    public ArrayList<KioskOrderInfoForPrint> getPrintList() {
        return mPrintList;
    }
    public void setPrinterSample(ESCPSample ps)
    {
        printerSample = ps;
    }
    public ESCPSample getPrinterSample()
    {
        return printerSample;
    }
    public void setContext(Context c)
    {
        mAppContext = c;
    }
    public Context getContext()
    {
        return mAppContext;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable e) {

        String errlog = String.format("Exception: %s\n%s", e.toString(), getStackTrace(e));
        // Place a breakpoint here to catch application crashes
        FileUtils.writeDebug(errlog, "PayCastAgent");

        Log.wtf(TAG, errlog);

        //android.os.Process.killProcess(android.os.Process.myPid());
        // Call the default handler
        defaultHandler.uncaughtException(thread, e);
    }
    /**
     * Convert an exception into a printable stack trace.
     * @param e the exception to convert
     * @return the stack trace
     */
    private String getStackTrace(Throwable e) {
        final Writer sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);

        e.printStackTrace(pw);
        String stacktrace = sw.toString();
        pw.close();

        return stacktrace;
    }

}
