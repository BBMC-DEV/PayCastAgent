package kr.co.bbmc.selforderutil;

import android.content.Context;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class ServerReqUrl {
    private static boolean LOG = false;

    public static String getServerSaveTokenUrl(StbOptionEnv model, Context c) {
        StringBuilder postString = new StringBuilder();

        String reqUrl = "";
        if (model.serverPort == 80)
        {
            reqUrl = String.valueOf("http://"+model.serverHost+"/dsg/agent/token");
        }
        else
        {
            reqUrl = String.valueOf("http://"+model.serverHost+":"+model.serverPort+"/dsg/agent/token");
        }
        // jason:serverssl: 서버 https 프로토콜 옵션(2017/10/12)
        String ssl = PropUtil.configValue(c.getString(R.string.serverSSLEnabled), c);
        if(ssl == null)
            return reqUrl;

        if (Boolean.valueOf(ssl))
        {
            reqUrl = reqUrl.replace("http://", "https://");
        }
        //-
        return reqUrl;
    }

    public static String getServerRcCommandUrl(StbOptionEnv model, Context c)
    {
        String reqUrl = "";
        if (model.serverPort == 80)
        {
            reqUrl = String.valueOf("http://"+model.serverHost+"/info/storecomplete");
        }
        else
        {
            reqUrl = String.valueOf("http://"+model.serverHost+":"+model.serverPort+"/info/storecomplete");
        }

        // jason:serverssl: 서버 https 프로토콜 옵션(2017/10/12)
        String ssl = PropUtil.configValue(c.getString(R.string.serverSSLEnabled), c);
        if(ssl == null)
            return reqUrl;

        if (Boolean.valueOf(ssl))
        {
            reqUrl = reqUrl.replace("http://", "https://");
        }
        //-
        return reqUrl;
    }
    public static String getServerSyncContentReportUrl(StbOptionEnv model, Context c)
    {
        String reqUrl = "";
        if (model.serverPort == 80)
        {
            reqUrl = String.valueOf("http://"+model.serverHost+"/info/dctntreport");
        }
        else
        {
            reqUrl = String.valueOf("http://"+model.serverHost+":"+model.serverPort+"/info/dctntreport");
        }

        String ssl = PropUtil.configValue(c.getString(R.string.serverSSLEnabled), c);
        if(ssl == null)
            return reqUrl;

        if (Boolean.valueOf(ssl))
        {
            reqUrl = reqUrl.replace("http://", "https://");
        }
        //-
        reqUrl += "?storeId="+model.storeId;
        Log.e("ServerReqUrl", "/info/dctntreport.... ");
        return reqUrl;
    }
    public static String reportServerSync(String completeY, String completeKY, StbOptionEnv model, PropUtil prop, Context c)
    {
        return reportServerSync(completeY, "|", completeKY, "|", model, prop, c);
    }

    public static String reportServerSync(String completeY, String completeN, String completeKY, String completeKN, StbOptionEnv model, PropUtil prop, Context c)
    {
        if (completeY.equals("|") && completeN.equals("|") && completeKY.equals("|") && completeKN.equals("|"))
        {
            return null;
        }
        List<String> macAddr = Utils.getMacAddress(c);

        StringBuilder postString = new StringBuilder();
        postString.append("storeId="+model.storeId+"&");
        Log.e("ServerReqUrl", "completeY="+completeY);

        if (!completeY.equals("|"))
        {
            postString.append("completeY="+completeY+"&");
        }

        if (!completeN.equals("|"))
        {
            postString.append("completeN="+completeN+"&");
        }

        if (!completeKY.equals("|"))
        {
            // jason:downloadkctntreport: 미완료된 스케줄 컨텐츠와 키오스크 컨텐츠가 함께 존재 시 보고 오류 수정(2012/11/28)
            postString.append("completeKY="+completeKY+"&");
        }

        if (!completeKN.equals("|"))
        {
            postString.append("completeKN="+completeKN);
        }

        String tmp = String.valueOf(postString);
        tmp = (tmp.endsWith("&")) ? tmp.substring(0, tmp.length() - 1) : tmp;

//        String ret = HttpResponseString(getServerSyncContentReportUrl(model, prop), tmp);
//        return !String.IsNullOrEmpty(ret) && ret.equals("Y");
        return tmp;
    }
    public static String getServerReportUrl(StbOptionEnv model, Context c)
    {
        String reqUrl = "";
        if (model.serverPort == 80)
        {
            reqUrl = String.valueOf("http://"+model.serverHost+"/mon/stbsttsreport");
        }
        else
        {
            reqUrl = String.valueOf("http://"+model.serverHost+":"+model.serverPort+"/mon/stbsttsreport");
        }

        // jason:serverssl: 서버 https 프로토콜 옵션(2017/10/12)
        String ssl = PropUtil.configValue(c.getString(R.string.serverSSLEnabled), c);

        if (Boolean.valueOf(ssl))
        {
            reqUrl = reqUrl.replace("http://", "https://");
        }
        //-
        return reqUrl;
    }
    public static String getServerPaymentInfoUrl(StbOptionEnv model, Context c)
    {
        String reqUrl = "";
        if (model.serverPort == 80)
        {
            reqUrl = String.valueOf("http://"+model.serverHost+"/info/printmenu");
        }
        else
        {
            reqUrl = String.valueOf("http://"+model.serverHost+":"+model.serverPort+"/info/printmenu");
        }

        // jason:serverssl: 서버 https 프로토콜 옵션(2017/10/12)
        String ssl = PropUtil.configValue(c.getString(R.string.serverSSLEnabled), c);

        if (Boolean.valueOf(ssl))
        {
            reqUrl = reqUrl.replace("http://", "https://");
        }
        //-
        return reqUrl;
    }
    public static String reportUrlServerMenuPrintKitchen(ArrayList<KioskOrderInfoForPrint> printList, StbOptionEnv model, Context c) throws UnsupportedEncodingException {
        String reqUrl = "";
        if (model.serverPort == 80)
        {
            reqUrl = String.valueOf("http://"+model.serverHost+"/info/printcomplete");
        }
        else
        {
            reqUrl = String.valueOf("http://"+model.serverHost+":"+model.serverPort+"/info/printcomplete");
        }

        // jason:serverssl: 서버 https 프로토콜 옵션(2017/10/12)
        String ssl = PropUtil.configValue(c.getString(R.string.serverSSLEnabled), c);

        if (Boolean.valueOf(ssl))
        {
            reqUrl = reqUrl.replace("http://", "https://");
        }
        //-
        reqUrl += "?storeId="+model.storeId+"&";

        Log.d("ServerReqUrl", "reqUrl="+reqUrl);

        return reqUrl;
    }
    public static String reportParamServerMenuPrintKitchen(ArrayList<KioskOrderInfoForPrint> printList, StbOptionEnv model, Context c) throws UnsupportedEncodingException {

        String postStringYes = "|";
        String postStringNo = "|";
        String postStringcCancel = "|";

        for(int i = 0; i<printList.size(); i++)
        {
            KioskOrderInfoForPrint list = printList.get(i);
            if(list.printOk)
            {
                postStringYes += (list.recommandId+"|");
            }
            else
                postStringNo +=(list.recommandId+"|");
            postStringcCancel+=(list.cancel+"|");
        }

        String param = "";
        if(!postStringYes.isEmpty())
        {
//            param = "completeY=" + postStringYes;
            param = "completeY=" + postStringYes;
        }
        if(!postStringNo.isEmpty()) {
//            param += ("&" +"completeN="+ postStringNo);
            param += ("&" +"completeN=" + postStringNo);
        }
        if(!postStringcCancel.isEmpty())
            param += ("&" +"cancelYN=" + postStringcCancel);
        if(LOG)
            Log.d("ServerReqUrl", "param="+param);
        printList.clear();
        printList = new ArrayList<>();
        return param;
    }

    public static String getServerChgInfoUrl(StbOptionEnv model, Context c)
    {
        String reqUrl = "";
        if (model.serverPort == 80)
        {
            reqUrl = String.valueOf("http://"+model.serverHost+"/info/storechgsync");
        }
        else
        {
            reqUrl = String.valueOf("http://"+model.serverHost+":"+model.serverPort+"/info/storechgsync");
        }

        // jason:serverssl: 서버 https 프로토콜 옵션(2017/10/12)
        String ssl = PropUtil.configValue(c.getString(R.string.serverSSLEnabled), c);

        if (Boolean.valueOf(ssl))
        {
            reqUrl = reqUrl.replace("http://", "https://");
        }
        //-
        return reqUrl;
    }
    public static String getServerStoreCompletResUrl(StbOptionEnv model, Context c)
    {
        String reqUrl = "";
        if (model.serverPort == 80)
        {
            reqUrl = String.valueOf("http://"+model.serverHost+"/info/storecomplete");
        }
        else
        {
            reqUrl = String.valueOf("http://"+model.serverHost+":"+model.serverPort+"/info/storecomplete");
        }

        // jason:serverssl: 서버 https 프로토콜 옵션(2017/10/12)
        String ssl = PropUtil.configValue(c.getString(R.string.serverSSLEnabled), c);

        if (Boolean.valueOf(ssl))
        {
            reqUrl = reqUrl.replace("http://", "https://");
        }
        //-
        return reqUrl;
    }

}
