package kr.co.bbmc.paycastagent;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.util.Log;

import com.sewoo.jpos.command.ESCPOS;
import com.sewoo.jpos.command.ESCPOSConst;
import com.sewoo.jpos.printer.ESCPOSImage;
import com.sewoo.jpos.printer.ESCPOSPrinter;
import com.sewoo.jpos.printer.LKPrint;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import kr.co.bbmc.selforderutil.KioskOrderInfoForPrint;

import static com.sewoo.jpos.command.ESCPOSConst.LK_SUCCESS;
import static com.sewoo.jpos.command.ESCPOSConst.STS_NORMAL;

public class ESCPSample {
    public ESCPOSPrinter posPtr;
    private ESCPOSImage posImg;
    private final char ESC = ESCPOS.ESC;
    private final char LF = ESCPOS.LF;
    private boolean LOG  = false;
    int nLineWidth = 384;

    public ESCPSample()
    {
//        posPtr = new ESCPOSPrinter();
		posPtr = new ESCPOSPrinter("EUC-KR"); // Korean.
//		posPtr = new ESCPOSPrinter("BIG5"); // Big5
        posImg = new ESCPOSImage();
    }

    public void receipt() throws UnsupportedEncodingException
    {
        posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + ESC + "|2C" + "Receipt" + LF + LF);
        posPtr.printNormal(ESC + "|rA" + ESC + "|bC" + "TEL (123)-456-7890" + LF);
        posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + "Thank you for coming to our shop!" + LF + LF);

        posPtr.printNormal(ESC + "|cA" +"Chicken                   $10.00" + LF);
        posPtr.printNormal(ESC + "|cA" +"Hamburger                 $20.00" + LF);
        posPtr.printNormal(ESC + "|cA" +"Pizza                     $30.00" + LF);
        posPtr.printNormal(ESC + "|cA" +"Lemons                    $40.00" + LF);
        posPtr.printNormal(ESC + "|cA" +"Drink                     $50.00" + LF + LF);
        posPtr.printNormal(ESC + "|cA" +"Excluded tax             $150.00" + LF);

        posPtr.printNormal( ESC + "|cA" +ESC + "|uC" + "Tax(5%)                    $7.50" + LF);
        posPtr.printNormal( ESC + "|cA" +ESC + "|bC" + ESC + "|2C" + "Total   $157.50" + LF + LF);
        posPtr.printNormal( ESC + "|cA" +ESC + "|bC" + "Payment                  $200.00" + LF);
        posPtr.printNormal( ESC + "|cA" +ESC + "|bC" + "Change                    $42.50" + LF);
        posPtr.lineFeed(5);
        posPtr.cutPaper();
    }

    public int sample() throws UnsupportedEncodingException
    {
        int sts;
        int iSize = 0;

        sts = posPtr.printerCheck();
        if(sts != ESCPOSConst.LK_STS_NORMAL)
        {
            return sts;
        }

        sts = posPtr.status();
        if(sts != ESCPOSConst.LK_STS_NORMAL)
        {
            return sts;
        }

        posPtr.setAsync(false);

        receipt();
        try {
            iSize = posImg.printBitmap("//sdcard//temp//test//sign.bmp");
            if(iSize > 0)
            {
                byte [] iImageBuffer = new byte[iSize];
                int iLen = posImg.getImage(iImageBuffer);
                if(iLen > 0)
                {
                    posPtr.sendByte(iImageBuffer);
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + ESC + "|4C" + "Thank you" + LF);
//    	posPtr.printNormal("測試");
        posPtr.lineFeed(3);
        return 0;
    }

    public int barcode2d() throws UnsupportedEncodingException
    {
        String data = "ABCDEFGHIJKLMN";
        int sts;

        sts = posPtr.printerCheck();
        if(sts != ESCPOSConst.LK_STS_NORMAL)
        {
            return sts;
        }

        sts = posPtr.status();
        if(sts != ESCPOSConst.LK_STS_NORMAL)
        {
            return sts;
        }

        posPtr.setAsync(false);

        posPtr.printString("PDF417\r\n");
        posPtr.printPDF417(data, data.length(), 0, 10, ESCPOSConst.LK_ALIGNMENT_CENTER);
        posPtr.printPDF417(data, data.length(), 4, 3, ESCPOSConst.LK_ALIGNMENT_CENTER);
        posPtr.printString("QRCode\r\n");
        posPtr.printQRCode(data, data.length(), 3, ESCPOSConst.LK_QRCODE_EC_LEVEL_L, ESCPOSConst.LK_ALIGNMENT_CENTER);

        posPtr.printString("DDDD\r\n");
//        receipt();
        posPtr.printBarCode("1234567890", LKPrint.LK_BCS_Code39, 40, 2, LKPrint.LK_ALIGNMENT_CENTER, LKPrint.LK_HRI_TEXT_BELOW);
//        posPtr.printNormal(ESC + "|cA" + ESC + "|4C" + ESC + "|bC" + "Thank you" + LF);

        posPtr.lineFeed(4);
//	    posPtr.cutPaper();
        return 0;
    }
    public int barcodesample() throws UnsupportedEncodingException
    {
        int sts;

        sts = posPtr.printerCheck();
        if(sts != ESCPOSConst.LK_STS_NORMAL)
        {
            return sts;
        }

        sts = posPtr.status();
        if(sts != ESCPOSConst.LK_STS_NORMAL)
        {
            return sts;
        }

        posPtr.setAsync(false);

        posPtr.printBarCode("1234567890", LKPrint.LK_BCS_Code39, 40, 2, LKPrint.LK_ALIGNMENT_CENTER, LKPrint.LK_HRI_TEXT_BELOW);
        posPtr.printBarCode("0123498765", LKPrint.LK_BCS_Code93, 40, 2, LKPrint.LK_ALIGNMENT_CENTER, LKPrint.LK_HRI_TEXT_BELOW);
        posPtr.printBarCode("0987654321", LKPrint.LK_BCS_ITF, 40, 2, LKPrint.LK_ALIGNMENT_CENTER, LKPrint.LK_HRI_TEXT_BELOW);
        posPtr.printBarCode("{ACODE 128", LKPrint.LK_BCS_Code128, 40, 2, LKPrint.LK_ALIGNMENT_CENTER, LKPrint.LK_HRI_TEXT_BELOW);
        posPtr.printBarCode("{BCode 128", LKPrint.LK_BCS_Code128, 40, 2, LKPrint.LK_ALIGNMENT_CENTER, LKPrint.LK_HRI_TEXT_BELOW);
        posPtr.printBarCode("{C12345", LKPrint.LK_BCS_Code128, 40, 2, LKPrint.LK_ALIGNMENT_CENTER, LKPrint.LK_HRI_TEXT_BELOW);
        posPtr.printBarCode("A1029384756A", LKPrint.LK_BCS_Codabar, 40, 2, LKPrint.LK_ALIGNMENT_CENTER, LKPrint.LK_HRI_TEXT_BELOW);
        posPtr.printNormal(ESC + "|cA" + ESC + "|4C" + ESC + "|bC" + "Thank you" + LF);
        posPtr.lineFeed(3);
        return 0;
    }

    public int imageTest() throws IOException
    {
        int sts;

        sts = posPtr.printerCheck();
        if(sts != ESCPOSConst.LK_STS_NORMAL)
        {
            return sts;
        }

        sts = posPtr.status();
        if(sts != ESCPOSConst.LK_STS_NORMAL)
        {
            return sts;
        }

        posPtr.setAsync(false);

        posPtr.printBitmap("//sdcard//temp//test//logo_s.jpg", LKPrint.LK_ALIGNMENT_CENTER);
        posPtr.printBitmap("//sdcard//temp//test//sample_2.jpg", LKPrint.LK_ALIGNMENT_CENTER);
        posPtr.printBitmap("//sdcard//temp//test//sample_3.jpg", LKPrint.LK_ALIGNMENT_LEFT);
        posPtr.printBitmap("//sdcard//temp//test//sample_4.jpg", LKPrint.LK_ALIGNMENT_RIGHT);

        Bitmap _bitmap = BitmapFactory.decodeFile("//sdcard//temp//test//logo_s.jpg");
        posPtr.printBitmap(_bitmap, LKPrint.LK_ALIGNMENT_CENTER);
        posPtr.printBitmap(_bitmap, LKPrint.LK_ALIGNMENT_CENTER, 0, 1);

        posPtr.printBitmap(_bitmap, LKPrint.LK_ALIGNMENT_LEFT, 1);
        posPtr.printBitmap(_bitmap, LKPrint.LK_ALIGNMENT_LEFT, 2);
        posPtr.printBitmap(_bitmap, LKPrint.LK_ALIGNMENT_LEFT, 3);

        posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + ESC + "|4C" + "Thank you" + LF);

        posPtr.lineFeed(3);
        return 0;
    }

    public int imageTest1() throws IOException
    {
        int sts;

        sts = posPtr.printerCheck();
        if(sts != ESCPOSConst.LK_STS_NORMAL)
        {
            return sts;
        }

        sts = posPtr.status();
        if(sts != ESCPOSConst.LK_STS_NORMAL)
        {
            return sts;
        }

        posPtr.setAsync(false);

        posPtr.printBitmap("//sdcard//temp//test//1512301638.png", LKPrint.LK_ALIGNMENT_CENTER);
        posPtr.lineFeed(3);
        return 0;
    }

    public int invoice() throws UnsupportedEncodingException
    {
/*
    	posPtr.setCharSet("UTF-8");

		// Setting PageMode
		posPtr.setPageMode(true);
    	// 180 DPI or 203 DPI
		// 180 DPI - 7 dot per 1mm
    	// 203 DPI - 8 dot per 1mm
    	posPtr.setDPI(203);
    	// Print direction.
		posPtr.setPrintDirection(ESCPOSConst.DIRECTION_LEFT_RIGHT);
    	// 399 dot x 630 dot.
		posPtr.setPrintingArea(399, 630);

    	// Data
    	// Medium Text (20, 20)
    	posPtr.setAbsoluteVertical(20);
    	posPtr.setAbsoluteHorizontal(20);
	    posPtr.printText("丟並乾亂佔佪亙", LKPrint.LK_ALIGNMENT_LEFT, LKPrint.LK_FNT_DEFAULT, LKPrint.LK_TXT_2WIDTH | LKPrint.LK_TXT_2HEIGHT);

    	// Large Text
	    posPtr.setAbsoluteVertical(90);
    	posPtr.setAbsoluteHorizontal(20);
	    posPtr.printText("伋伕佇佈", LKPrint.LK_ALIGNMENT_LEFT, LKPrint.LK_FNT_DEFAULT, LKPrint.LK_TXT_3WIDTH | LKPrint.LK_TXT_3HEIGHT);

	    // Must be Off Unicode when print Alphabet or print barcode.
	    posPtr.setCharSet("Big5");

	    posPtr.setAbsoluteVertical(190);
    	posPtr.setAbsoluteHorizontal(20);
	    posPtr.printText("ABCDE", LKPrint.LK_ALIGNMENT_LEFT, LKPrint.LK_FNT_DEFAULT, LKPrint.LK_TXT_3WIDTH | LKPrint.LK_TXT_3HEIGHT);

	    posPtr.setCharSet("UTF-8");

    	// Small Text
	    posPtr.setAbsoluteVertical(300);
    	posPtr.setAbsoluteHorizontal(20);
	    posPtr.printText("壓壘壙壚壞壟壢壩壯壺", LKPrint.LK_ALIGNMENT_LEFT, LKPrint.LK_FNT_DEFAULT, LKPrint.LK_TXT_1WIDTH | LKPrint.LK_TXT_1HEIGHT);

	    // Must be Off Unicode when print Alphabet or print barcode.
    	posPtr.setCharSet("Big5");

    	// 1D Barcode
    	posPtr.setAbsoluteVertical(380);
    	posPtr.setAbsoluteHorizontal(0);
    	posPtr.printBarCode("0123456789012345678901", ESCPOSConst.LK_BCS_Code39, 40, 1, ESCPOSConst.LK_ALIGNMENT_CENTER, ESCPOSConst.LK_HRI_TEXT_NONE);
//    	    	posPtr.printBarCode("0123498765", ESCPOSConst.LK_BCS_Code93, 40, 2, ESCPOSConst.LK_ALIGNMENT_CENTER, ESCPOSConst.LK_HRI_TEXT_NONE);

    	// QRCODE
    	String data = "12345678901234567890123456789012345678901234567890123456789012345678901234567890";
    	posPtr.setAbsoluteVertical(450);
    	posPtr.setAbsoluteHorizontal(40);
    	posPtr.printQRCode(data, data.length(), 3, ESCPOSConst.LK_QRCODE_EC_LEVEL_L, ESCPOSConst.LK_ALIGNMENT_CENTER);
    	posPtr.setAbsoluteVertical(450);
    	posPtr.setAbsoluteHorizontal(240);
    	posPtr.printQRCode(data, data.length(), 3, ESCPOSConst.LK_QRCODE_EC_LEVEL_L, ESCPOSConst.LK_ALIGNMENT_CENTER);

    	// Data
	    posPtr.printPageModeData();
    	posPtr.setPageMode(false);
    	posPtr.lineFeed(4);
    	return 0;
*/
        int sts;

        sts = posPtr.printerCheck();
        if(sts != ESCPOSConst.LK_STS_NORMAL)
        {
            return sts;
        }

        sts = posPtr.status();
        if(sts != ESCPOSConst.LK_STS_NORMAL)
        {
            return sts;
        }

        posPtr.setAsync(false);

        posPtr.setCharSet("Big5");

        // Setting PageMode
        posPtr.setPageMode(true);
        // 180 DPI or 203 DPI
        // 180 DPI - 7 dot per 1mm
        // 203 DPI - 8 dot per 1mm
        posPtr.setDPI(203);
        // Print direction.
        posPtr.setPrintDirection(ESCPOSConst.DIRECTION_LEFT_RIGHT);
        // 399 dot x 630 dot.
        posPtr.setPrintingArea(399, 730); // al

        // Data
        // Medium Text (20, 20)
        posPtr.setAbsoluteVertical(20);
        posPtr.setAbsoluteHorizontal(20);
        posPtr.printText("丟並乾亂佔佪亙", LKPrint.LK_ALIGNMENT_LEFT, LKPrint.LK_FNT_DEFAULT, LKPrint.LK_TXT_2WIDTH | LKPrint.LK_TXT_2HEIGHT);

        // Large Text
        posPtr.setAbsoluteVertical(90);
        posPtr.setAbsoluteHorizontal(20);
        posPtr.printText("伋伕佇佈", LKPrint.LK_ALIGNMENT_LEFT, LKPrint.LK_FNT_DEFAULT, LKPrint.LK_TXT_3WIDTH | LKPrint.LK_TXT_3HEIGHT);

        // Must be Off Unicode when print Alphabet or print barcode.
        posPtr.setCharSet("Big5");

        posPtr.setAbsoluteVertical(190);
        posPtr.setAbsoluteHorizontal(20);
        posPtr.printText("ABCDE", LKPrint.LK_ALIGNMENT_LEFT, LKPrint.LK_FNT_DEFAULT, LKPrint.LK_TXT_3WIDTH | LKPrint.LK_TXT_3HEIGHT);

        posPtr.setCharSet("Big5");

        // Small Text
        posPtr.setAbsoluteVertical(300);
        posPtr.setAbsoluteHorizontal(20);
        posPtr.printText("壓壘壙壚壞壟壢壩壯壺桌號菇", LKPrint.LK_ALIGNMENT_LEFT, LKPrint.LK_FNT_DEFAULT, LKPrint.LK_TXT_1WIDTH | LKPrint.LK_TXT_1HEIGHT);

        // Must be Off Unicode when print Alphabet or print barcode.
        //posPtr.setCharSet("Big5");

        // 1D Barcode
        posPtr.setAbsoluteVertical(380);
        posPtr.setAbsoluteHorizontal(0);
        posPtr.printBarCode("0123456789012345678901", ESCPOSConst.LK_BCS_Code39, 40, 1, ESCPOSConst.LK_ALIGNMENT_CENTER, ESCPOSConst.LK_HRI_TEXT_NONE);
//    	    	posPtr.printBarCode("0123498765", ESCPOSConst.LK_BCS_Code93, 40, 2, ESCPOSConst.LK_ALIGNMENT_CENTER, ESCPOSConst.LK_HRI_TEXT_NONE);

        // QRCODE
        String data = "中華民國萬萬稅1234567890123456789012345678901234567890123456789012345678";
        posPtr.setAbsoluteVertical(450);
        posPtr.setAbsoluteHorizontal(40);
        posPtr.printQRCode(data, data.length(), 5, ESCPOSConst.LK_QRCODE_EC_LEVEL_L, ESCPOSConst.LK_ALIGNMENT_CENTER);
        posPtr.setAbsoluteVertical(450);
        posPtr.setAbsoluteHorizontal(240);
        posPtr.printQRCode(data, data.length(), 3, ESCPOSConst.LK_QRCODE_EC_LEVEL_L, ESCPOSConst.LK_ALIGNMENT_CENTER);

        // Data
        posPtr.printPageModeData();
        posPtr.setPageMode(false);
        posPtr.lineFeed(4);
        return 0;

    }

    public int printDataMatrix() throws UnsupportedEncodingException
    {
        // DataMatrix
        int sts;

        sts = posPtr.printerCheck();
        if(sts != ESCPOSConst.LK_STS_NORMAL)
        {
            return sts;
        }

        sts = posPtr.status();
        if(sts != ESCPOSConst.LK_STS_NORMAL)
        {
            return sts;
        }

        posPtr.setAsync(false);

        String data = "1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        posPtr.printDataMatrix(data, data.length(), 6, ESCPOSConst.LK_ALIGNMENT_CENTER);

        posPtr.lineFeed(4);
        return 0;
    }

    public int printAndroidFont() throws UnsupportedEncodingException
    {
        //int nLineWidth = 384;
        String data = "Receipt";
//    	String data = "영수증";
        Typeface typeface = null;

        try
        {
            int sts;

            sts = posPtr.printerCheck();
            if(sts != ESCPOSConst.LK_STS_NORMAL)
            {
                return sts;
            }

            sts = posPtr.status();
            if(sts != ESCPOSConst.LK_STS_NORMAL)
            {
                return sts;
            }

            posPtr.setAsync(false);

            posPtr.printAndroidFont(data, nLineWidth, 100, ESCPOSConst.LK_ALIGNMENT_CENTER);
            posPtr.lineFeed(2);
            posPtr.printAndroidFont("Left Alignment", nLineWidth, 24, ESCPOSConst.LK_ALIGNMENT_LEFT);
            posPtr.printAndroidFont("Center Alignment", nLineWidth, 24, ESCPOSConst.LK_ALIGNMENT_CENTER);
            posPtr.printAndroidFont("Right Alignment", nLineWidth, 24, ESCPOSConst.LK_ALIGNMENT_RIGHT);

            posPtr.lineFeed(2);
            posPtr.printAndroidFont(Typeface.SANS_SERIF, "SANS_SERIF : 1234iwIW", nLineWidth, 24, ESCPOSConst.LK_ALIGNMENT_LEFT);
            posPtr.printAndroidFont(Typeface.SERIF, "SERIF : 1234iwIW", nLineWidth, 24, ESCPOSConst.LK_ALIGNMENT_LEFT);
            posPtr.printAndroidFont(typeface.MONOSPACE, "MONOSPACE : 1234iwIW", nLineWidth, 24, ESCPOSConst.LK_ALIGNMENT_LEFT);

            posPtr.lineFeed(2);
            posPtr.printAndroidFont(Typeface.SANS_SERIF, "SANS : 1234iwIW", nLineWidth, 24, ESCPOSConst.LK_ALIGNMENT_LEFT);
            posPtr.printAndroidFont(Typeface.SANS_SERIF, true, "SANS BOLD : 1234iwIW", nLineWidth, 24, ESCPOSConst.LK_ALIGNMENT_LEFT);
            posPtr.printAndroidFont(Typeface.SANS_SERIF, true, false, "SANS BOLD : 1234iwIW", nLineWidth, 24, ESCPOSConst.LK_ALIGNMENT_LEFT);
            posPtr.printAndroidFont(Typeface.SANS_SERIF, false, true, "SANS ITALIC : 1234iwIW", nLineWidth, 24, ESCPOSConst.LK_ALIGNMENT_LEFT);
            posPtr.printAndroidFont(Typeface.SANS_SERIF, true, true, "SANS BOLD ITALIC : 1234iwIW", nLineWidth, 24, ESCPOSConst.LK_ALIGNMENT_LEFT);
            posPtr.printAndroidFont(Typeface.SANS_SERIF, true, true, true, "SANS B/I/U : 1234iwIW", nLineWidth, 24, ESCPOSConst.LK_ALIGNMENT_LEFT);

            posPtr.lineFeed(2);
            posPtr.printAndroidFont(Typeface.SERIF, "SERIF : 1234iwIW", nLineWidth, 24, ESCPOSConst.LK_ALIGNMENT_LEFT);
            posPtr.printAndroidFont(Typeface.SERIF, true, "SERIF BOLD : 1234iwIW", nLineWidth, 24, ESCPOSConst.LK_ALIGNMENT_LEFT);
            posPtr.printAndroidFont(Typeface.SERIF, true, false, "SERIF BOLD : 1234iwIW", nLineWidth, 24, ESCPOSConst.LK_ALIGNMENT_LEFT);
            posPtr.printAndroidFont(Typeface.SERIF, false, true, "SERIF ITALIC : 1234iwIW", nLineWidth, 24, ESCPOSConst.LK_ALIGNMENT_LEFT);
            posPtr.printAndroidFont(Typeface.SERIF, true, true, "SERIF BOLD ITALIC : 1234iwIW", nLineWidth, 24, ESCPOSConst.LK_ALIGNMENT_LEFT);
            posPtr.printAndroidFont(Typeface.SERIF, true, true, true, "SERIF B/I/U : 1234iwIW", nLineWidth, 24, ESCPOSConst.LK_ALIGNMENT_LEFT);

            posPtr.lineFeed(2);
            posPtr.printAndroidFont(Typeface.MONOSPACE, "MONOSPACE : 1234iwIW", nLineWidth, 24, ESCPOSConst.LK_ALIGNMENT_LEFT);
            posPtr.printAndroidFont(Typeface.MONOSPACE, true, "MONO BOLD : 1234iwIW", nLineWidth, 24, ESCPOSConst.LK_ALIGNMENT_LEFT);
            posPtr.printAndroidFont(Typeface.MONOSPACE, true, false, "MONO BOLD : 1234iwIW", nLineWidth, 24, ESCPOSConst.LK_ALIGNMENT_LEFT);
            posPtr.printAndroidFont(Typeface.MONOSPACE, false, true, "MONO ITALIC : 1234iwIW", nLineWidth, 24, ESCPOSConst.LK_ALIGNMENT_LEFT);
            posPtr.printAndroidFont(Typeface.MONOSPACE, true, true, "MONO BOLD ITALIC: 1234iwIW", nLineWidth, 24, ESCPOSConst.LK_ALIGNMENT_LEFT);
            posPtr.printAndroidFont(Typeface.MONOSPACE, true, true, true, "MONO B/I/U: 1234iwIW", nLineWidth, 24, ESCPOSConst.LK_ALIGNMENT_LEFT);

            posPtr.lineFeed(4);
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return 0;
    }

    public int printMultilingualFont() throws UnsupportedEncodingException
    {
        int nLineWidth = 384;
        String Koreandata = "영수증";
        String Turkishdata = "Turkish(İ,Ş,Ğ)";
        String Russiandata = "Получение";
        String Arabicdata = "الإيصال";
        String Greekdata = "Παραλαβή";
        String Japanesedata = "領収書";
        String GB2312data = "收据";
        String BIG5data = "收據";

        try
        {
            int sts;

            sts = posPtr.printerCheck();
            if(sts != ESCPOSConst.LK_STS_NORMAL)
            {
                return sts;
            }

            sts = posPtr.status();
            if(sts != ESCPOSConst.LK_STS_NORMAL)
            {
                return sts;
            }

            posPtr.setAsync(false);

            posPtr.printAndroidFont("Korean Font", nLineWidth, 24, ESCPOSConst.LK_ALIGNMENT_LEFT);
            // Korean 100-dot size font in android device.
            posPtr.printAndroidFont(Koreandata, nLineWidth, 100, ESCPOSConst.LK_ALIGNMENT_CENTER);

            posPtr.printAndroidFont("Turkish Font", nLineWidth, 24, ESCPOSConst.LK_ALIGNMENT_LEFT);
            // Turkish 50-dot size font in android device.
            posPtr.printAndroidFont(Turkishdata, nLineWidth, 50, ESCPOSConst.LK_ALIGNMENT_CENTER);

            posPtr.printAndroidFont("Russian Font", 384, 24, ESCPOSConst.LK_ALIGNMENT_LEFT);
            // Russian 60-dot size font in android device.
            posPtr.printAndroidFont(Russiandata, nLineWidth, 60, ESCPOSConst.LK_ALIGNMENT_CENTER);

            posPtr.printAndroidFont("Arabic Font", nLineWidth, 24, ESCPOSConst.LK_ALIGNMENT_LEFT);
            // Arabic 100-dot size font in android device.
            posPtr.printAndroidFont(Arabicdata, nLineWidth, 100, ESCPOSConst.LK_ALIGNMENT_CENTER);

            posPtr.printAndroidFont("Greek Font", nLineWidth, 24, ESCPOSConst.LK_ALIGNMENT_LEFT);
            // Greek 60-dot size font in android device.
            posPtr.printAndroidFont(Greekdata, nLineWidth, 60, ESCPOSConst.LK_ALIGNMENT_CENTER);

            posPtr.printAndroidFont("Japanese Font", nLineWidth, 24, ESCPOSConst.LK_ALIGNMENT_LEFT);
            // Japanese 100-dot size font in android device.
            posPtr.printAndroidFont(Japanesedata, nLineWidth, 100, ESCPOSConst.LK_ALIGNMENT_CENTER);

            posPtr.printAndroidFont("GB2312 Font", nLineWidth, 24, ESCPOSConst.LK_ALIGNMENT_LEFT);
            // GB2312 100-dot size font in android device.
            posPtr.printAndroidFont(GB2312data, nLineWidth, 100, ESCPOSConst.LK_ALIGNMENT_CENTER);

            posPtr.printAndroidFont("BIG5 Font", nLineWidth, 24, ESCPOSConst.LK_ALIGNMENT_LEFT);
            // BIG5 100-dot size font in android device.
            posPtr.printAndroidFont(BIG5data, nLineWidth, 100, ESCPOSConst.LK_ALIGNMENT_CENTER);

            posPtr.lineFeed(4);
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return 0;
    }
    //(ArrayList list, String tprice, ArrayList sellerInfo, PaymentInfoData payInfo, int orderNum) throws IOException
    @SuppressLint("NewApi")
    public int customSample1(ArrayList<KioskOrderInfoForPrint> printList, ArrayList sellerInfo, AgentExternalVarApp varApp)throws IOException
    {
//        int nLineWidth = 384;
        int max_char = 39;
        DecimalFormat myFormatter = new DecimalFormat("###,###");

        int sts = -1;

//        sts = posPtr.printerStatus();
//        sts = posPtr.status();


        try {
             sts = posPtr.printerSts(); ////posPtr.printerCheck() function 과 같이 쓰면 error 남.
            if(LOG)
                Log.e("ESCPSample", "1customSample1() sts="+sts);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if(sts != STS_NORMAL)
        {
            return sts;
        }


        if(LOG) {
            Log.e("ESCPSample", "1 sts=" + sts);
            Log.e("ESCPSample", "2 sts=" + sts);
        }
        //123456789012345678901234567890123456789012345678901234567890
        String spaceStr = "                                                            ";
        for(int i = 0; i<printList.size(); i++) {
            KioskOrderInfoForPrint orderlist = printList.get(i);
            if(orderlist.orderType.equalsIgnoreCase("I"))
            {
                ArrayList <String> storeInfoList = new ArrayList();
                String storeInfoName = String.format("주문매장:%s", varApp.mStbOpt.storeName);
                String storeInfoTel = String.format("매장전화:%s", varApp.mStbOpt.storeTel);
                storeInfoList.add(storeInfoName);
                storeInfoList.add(storeInfoTel);
                onRefillOrderPrintForStore(orderlist, sellerInfo, varApp.mStbOpt.deviceId );
                //onDeliveryOrderPrintForCustom(orderlist, storeInfoList, varApp.mStbOpt.deviceId );
            }
            else if(orderlist.orderType.equalsIgnoreCase("D"))
            {
                ArrayList <String> storeInfoList = new ArrayList();
                String storeInfoName = String.format("주문매장:%s", varApp.mStbOpt.storeName);
                String storeInfoTel = String.format("매장전화:%s", varApp.mStbOpt.storeTel);
                storeInfoList.add(storeInfoName);
                storeInfoList.add(storeInfoTel);
                onDeliveryOrderPrintForStore(orderlist, varApp.mStbOpt.deviceId );
                onDeliveryOrderPrintForCustom(orderlist, storeInfoList, varApp.mStbOpt.deviceId );
            }
            else if(orderlist.orderType.equalsIgnoreCase("P"))
            {
                ArrayList <String> storeInfoList = new ArrayList();
                String storeInfoName = String.format("주문매장:%s", varApp.mStbOpt.storeName);
                String storeInfoTel = String.format("매장전화:%s", varApp.mStbOpt.storeTel);
                storeInfoList.add(storeInfoName);
                storeInfoList.add(storeInfoTel);
                onPackingOrderPrintForStore(orderlist, varApp.mStbOpt.deviceId );
                onPackingOrderPrintForCustom(orderlist, storeInfoList, varApp.mStbOpt.deviceId );
/*
                onDeliveryOrderPrintForStore(orderlist, varApp.mStbOpt.deviceId );
                onDeliveryOrderPrintForCustom(orderlist, storeInfoList, varApp.mStbOpt.deviceId );
*/
            }
            else {
                String formattedStringtotalPrice = myFormatter.format(Float.valueOf(orderlist.goodsAmt));
                SimpleDateFormat sdf = null;
                sdf = new SimpleDateFormat("yyyyMMddHHmmss");

                Date tradingDate = null;
                try {
//                tradingDate = sdf.parse("20181126125740");
                    tradingDate = sdf.parse(orderlist.orderDate);
                } catch (ParseException ex) {
                    Log.v("Exception", ex.getLocalizedMessage());
                    Log.e("ESCPSample", "Exception() tradingDate=" + tradingDate);
                }
                sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                if (orderlist.cancel.equalsIgnoreCase("Y"))
                    posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + ESC + "|2C" + "[ 취소 주문서 ]" + LF + LF);
                else
                    posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + ESC + "|2C" + "[ 주문서 ]" + LF + LF);

                if (sellerInfo != null && sellerInfo.size() > 0) {
                    String s = (String) sellerInfo.get(0);
                    posPtr.printNormal(ESC + "|lA" + s + LF);
                } else {
                    if (LOG)
                        Log.e("ESCPSample", "Error sellerInfo.size()=" + sellerInfo.size());
                }
                if (tradingDate != null)
                    posPtr.printNormal(ESC + "|lA" + String.format("주문일시: %s", sdf.format(tradingDate)) + LF + LF);
                else
                    posPtr.printNormal(ESC + "|lA" + String.format("주문일시: %s", "날짜오류") + LF + LF);
                if ((orderlist.orderNumber != null) && (!orderlist.orderNumber.isEmpty()))
                    posPtr.printNormal(ESC + "|1F" + "주문번호           " + ESC + "|cA" + ESC + "|bC" + ESC + "|rA" + ESC + "|4C" + orderlist.orderNumber + LF);
                else
                    posPtr.printNormal(ESC + "|1F" + "주문번호           " + ESC + "|cA" + ESC + "|bC" + ESC + "|rA" + ESC + "|4C" + "주문번호 오류" + LF);
                if((orderlist.reservTime!=null)&&(!orderlist.reservTime.isEmpty()))
                {
                    posPtr.printNormal(ESC + "|lA" +  LF);
                    posPtr.printNormal(ESC + "|1F" + "예약시간         " + ESC + "|cA" + ESC + "|bC" + ESC + "|rA" + ESC + "|4C" + orderlist.reservTime + LF);
                }
                ArrayList<KioskOrderInfoForPrint.OrderMenuItem> menuItems = orderlist.menuItems;
                if (menuItems.size() > 0) {
                    KioskOrderInfoForPrint.OrderMenuItem items = (KioskOrderInfoForPrint.OrderMenuItem) menuItems.get(0);
                    if (items.orderPackage) {
                        posPtr.printNormal(ESC + "|lA" +  LF);
                        posPtr.printNormal(ESC + "|1F" + "주문유형           " + ESC + "|cA" + ESC + "|bC" + ESC + "|rA" + ESC + "|4C" + "포장" + LF);
                    }
                }
                posPtr.printNormal(ESC + "|lA" + ESC + "|bC" + "------------------------------------------" + LF);
                posPtr.printNormal(ESC + "|lA" + ESC + "|bC" + "  품목                              수량  " + LF);
                posPtr.printNormal(ESC + "|lA" + ESC + "|bC" + "------------------------------------------" + LF);


                for (int j = 0; j < menuItems.size(); j++) {
                    KioskOrderInfoForPrint.OrderMenuItem items = (KioskOrderInfoForPrint.OrderMenuItem) menuItems.get(j);
                    if ((items.productName != null) && (!items.productName.isEmpty())) {
                        //int menulen = items.productName.getBytes("euc-kr").length;
                        //int countlen = items.orderCount.getBytes("euc-kr").length;
                        //String testStr = items.productName + spaceStr.substring(0, max_char - menulen - countlen);

                        if(items.productName.length()>max_char)
                            posPtr.printNormal(ESC + "|lA" + items.productName.substring(0, max_char) + LF);
                        else
                            posPtr.printNormal(ESC + "|lA" + items.productName + LF);
                    }
                    if ((items.menuOptItems != null) && (items.menuOptItems.size() > 0)) {
                        String tempKind = "";
                        for (int optCount = 0; optCount < items.menuOptItems.size(); optCount++) {
                            KioskOrderInfoForPrint.OrderMenuItem.MenuOptionItem optItem = items.menuOptItems.get(optCount);
                            if (tempKind.isEmpty())
                                tempKind = optItem.optKind;
                            if (!optItem.optKind.equalsIgnoreCase(tempKind)) {
//                            posPtr.printNormal(ESC + "|cA" +ESC + "|bC" +" " + LF );
                                tempKind = optItem.optKind;
                            }
                            posPtr.printNormal(ESC + "|lA" + " -" + optItem.optName + LF);
                        }

                    }
                    posPtr.printNormal(ESC + "|lA" + spaceStr.substring(0, max_char - items.orderCount.length())+items.orderCount + LF);
                    posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + " " + LF);

                }
//            posPtr.printNormal(ESC + "|cA"+" " + LF);

                {
                    int pricelen = formattedStringtotalPrice.getBytes("euc-kr").length;
                    //123456789012345678901234567890123456789012345678901234567890
                    String priceStr = " 총액:";
                    int spacelen = 0;
                    try {
                        spacelen = max_char - pricelen - priceStr.getBytes("euc-kr").length - 2;
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    if(spacelen<0)
                        spacelen = 0;

                    String printStr = spaceStr.substring(0, spacelen);

                    posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + "------------------------------------------" + LF);
                    posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + priceStr + printStr + formattedStringtotalPrice + "원" + LF);
                    posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + "------------------------------------------" + LF );
                }
                //<- 주문 정보
                try {
                    posPtr.printNormal(ESC + "|lA" + "매장 요청 메시지" + LF+LF);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                try {
                    if((orderlist.storeMsg!=null)&&(!orderlist.storeMsg.isEmpty()))
                        posPtr.printNormal(ESC + "|1F" + ESC + "|lA" + ESC + "|bC" + String.format("%s", orderlist.storeMsg) + LF);
                    else
                        posPtr.printNormal(ESC + "|1F" + ESC + "|lA" + ESC + "|bC" + LF);

                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                try {
                    posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + "------------------------------------------" + LF );
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                posPtr.printNormal(ESC + "|lA" + "이용해 주셔서 감사합니다." + LF);
                posPtr.printNormal(ESC + "|lA" + "기기번호 : " + varApp.mStbOpt.deviceId + LF);
//            posPtr.printNormal(ESC + "|lA"+"device id : "+varApp.mStbOpt.deviceId + LF);

                //        posPtr.lineFeed(3);
                // POSPrinter Only.
                posPtr.printNormal(ESC + "|fP");
                posPtr.cutPaper();

                orderlist.printOk = true;
            }
        }
        return STS_NORMAL;

    }
    private int onRefillOrderPrintForStore(KioskOrderInfoForPrint order, ArrayList<String> sellerInfo, String deviceId) {
        int max_char = 40;
        String spaceStr = "                                                             ";

        DecimalFormat myFormatter = new DecimalFormat("###,###");
        String formattedStringtotalPrice = myFormatter.format(Float.valueOf(order.goodsAmt));
        SimpleDateFormat sdf = null;
        sdf = new SimpleDateFormat("yyyyMMddHHmmss");

        Date tradingDate = null;
        try {
            tradingDate = sdf.parse(order.orderDate);
        } catch (ParseException ex) {
            Log.v("Exception", ex.getLocalizedMessage());
            Log.e("ESCPSample", "Exception() tradingDate=" + tradingDate);
        }
        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        try {
            posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + ESC + "|2C" + "[ 주문서 ]" + LF + LF);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        if (sellerInfo != null && sellerInfo.size() > 0) {
            String s = (String) sellerInfo.get(0);
            try {
                posPtr.printNormal(ESC + "|lA" + s + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else {
            if (LOG)
                Log.e("ESCPSample", "Error sellerInfo.size()=" + sellerInfo.size());
        }
        if (tradingDate != null) {
            try {
                posPtr.printNormal(ESC + "|lA" + String.format("주문일시: %s", sdf.format(tradingDate)) + LF + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        else {
            try {
                posPtr.printNormal(ESC + "|lA" + String.format("주문일시: %s", "날짜오류") + LF + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        if ((order.orderNumber != null) && (!order.orderNumber.isEmpty())) {
            try {
                posPtr.printNormal(ESC + "|1F" + "주문번호           " + ESC + "|cA" + ESC + "|bC" + ESC + "|rA" + ESC + "|4C" + order.orderNumber + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        else {
            try {
                posPtr.printNormal(ESC + "|1F" + "주문번호           " + ESC + "|cA" + ESC + "|bC" + ESC + "|rA" + ESC + "|4C" + "주문번호 오류" + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        if((order.reservTime!=null)&&(!order.reservTime.isEmpty()))
        {
            try {
                posPtr.printNormal(ESC + "|lA" +  LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            try {
                posPtr.printNormal(ESC + "|1F" + "예약시간         " + ESC + "|cA" + ESC + "|bC" + ESC + "|rA" + ESC + "|4C" + order.reservTime + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        ArrayList<KioskOrderInfoForPrint.OrderMenuItem> menuItems = order.menuItems;
        if (menuItems.size() > 0) {
            KioskOrderInfoForPrint.OrderMenuItem items = (KioskOrderInfoForPrint.OrderMenuItem) menuItems.get(0);

            try {
                posPtr.printNormal(ESC + "|1F" + "주문유형          " + ESC + "|cA" + ESC + "|bC" + ESC + "|rA" + ESC + "|4C" + "리필" + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        try {
            posPtr.printNormal(ESC + "|lA" + ESC + "|bC" + "------------------------------------------" + LF);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            posPtr.printNormal(ESC + "|lA" + ESC + "|bC" + "  품목                              수량  " + LF);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            posPtr.printNormal(ESC + "|lA" + ESC + "|bC" + "------------------------------------------" + LF);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }


        for (int j = 0; j < menuItems.size(); j++) {
            KioskOrderInfoForPrint.OrderMenuItem items = (KioskOrderInfoForPrint.OrderMenuItem) menuItems.get(j);
            if ((items.productName != null) && (!items.productName.isEmpty())) {
                int menulen = 0;
                try {
                    menulen = items.productName.getBytes("euc-kr").length;
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                if(menulen > max_char) {
                    try {
                        posPtr.printNormal(ESC + "|lA" + items.productName.substring(0, max_char)+ LF);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    try {
                        posPtr.printNormal(ESC + "|lA" + items.productName+ LF);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
                if ((items.menuOptItems != null) && (items.menuOptItems.size() > 0)) {
                    String tempKind = "";
                    for (int optCount = 0; optCount < items.menuOptItems.size(); optCount++) {
                        KioskOrderInfoForPrint.OrderMenuItem.MenuOptionItem optItem = items.menuOptItems.get(optCount);
                        if (tempKind.isEmpty())
                            tempKind = optItem.optKind;
                        if (!optItem.optKind.equalsIgnoreCase(tempKind)) {
                            tempKind = optItem.optKind;
                        }
                        try {
                            posPtr.printNormal(ESC + "|lA" + " -" + optItem.optName + LF);
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }

                }

                int countlen = 0;
                String testStr = spaceStr.substring(0, max_char - countlen);

                try {
                    posPtr.printNormal(ESC + "|lA" + testStr + items.orderCount + LF);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
            try {
                posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + " " + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

        }

        {
            int pricelen = 0;
            try {
                pricelen = formattedStringtotalPrice.getBytes("euc-kr").length;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            //123456789012345678901234567890123456789012345678901234567890
            String priceStr = " 총액:";
            String printStr = null;
            int spacelen = 0;
            try {
                spacelen = max_char - pricelen - priceStr.getBytes("euc-kr").length - 2;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if(spacelen <0)
                spacelen = 0;
//            printStr = spaceStr.substring(0, max_char - pricelen - priceStr.getBytes("euc-kr").length - 2);
            printStr = spaceStr.substring(0, spacelen);

            try {
                posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + "------------------------------------------" + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            try {
                posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + priceStr + printStr + formattedStringtotalPrice + "원" + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            try {
                posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + "------------------------------------------" + LF );
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        //<- 주문 정보
        try {
            posPtr.printNormal(ESC + "|lA" + "매장 요청 메시지" + LF+LF);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            if((order.storeMsg!=null)&&(!order.storeMsg.isEmpty()))
                posPtr.printNormal(ESC + "|1F" + ESC + "|lA" + ESC + "|bC" + String.format("%s", order.storeMsg) + LF);
            else
                posPtr.printNormal(ESC + "|1F" + ESC + "|lA" + ESC + "|bC" + LF);

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        try {
            posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + "------------------------------------------" + LF );
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        try {
            posPtr.printNormal(ESC + "|lA" + "이용해 주셔서 감사합니다." + LF);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            posPtr.printNormal(ESC + "|lA" + "기기번호 : " + deviceId + LF);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            posPtr.printNormal(ESC + "|fP");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        posPtr.cutPaper();

        order.printOk = true;
        return STS_NORMAL;

    }

    private int onDeliveryOrderPrintForStore(KioskOrderInfoForPrint order, String deviceId) {
        int max_char = 40;
        String spaceStr = "                                                             ";

        DecimalFormat myFormatter = new DecimalFormat("###,###");
        String formattedStringtotalPrice = myFormatter.format(Float.valueOf(order.goodsAmt));
        SimpleDateFormat sdf = null;
        sdf = new SimpleDateFormat("yyyyMMddHHmmss");

        Date tradingDate = null;
        try {
            tradingDate = sdf.parse(order.orderDate);
        } catch (ParseException ex) {
            Log.v("Exception", ex.getLocalizedMessage());
            Log.e("ESCPSample", "Exception() tradingDate=" + tradingDate);
        }
        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        if (order.cancel.equalsIgnoreCase("Y")) {
            try {
                posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + ESC + "|2C" + "배달 취소주문서[매장용] " + LF + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else {
            try {
                posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + ESC + "|2C" + "배달 주문서 [매장용]" + LF + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        try {
            posPtr.printNormal(ESC + "|lA" + ESC + "|bC" + "------------------------------------------" + LF);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            posPtr.printNormal(ESC + "|lA" + ESC + "|lA" + "배달주소:" + LF);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            posPtr.printAndroidFont(String.format("%s%s", order.roadAddr, order.addrDetail), nLineWidth, 40, ESCPOSConst.LK_ALIGNMENT_LEFT);
            //posPtr.printNormal(ESC + "|1F" + ESC + "|lA" + ESC + "|4C" +String.format("%s%s", order.roadAddr, order.addrDetail)+ LF+LF);
            //posPtr.printNormal(ESC + "|lA" + ESC + "|bC" + String.format("%s%s", order.roadAddr, order.addrDetail) + LF+LF);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            posPtr.printNormal(ESC + "|lA" + ESC + "|lA" + LF+"연락처:" + LF);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            posPtr.printAndroidFont(String.format("%s", order.tel), nLineWidth, 40, ESCPOSConst.LK_ALIGNMENT_LEFT);
            //posPtr.printNormal(ESC + "|1F" + ESC + "|lA" + ESC + "|4C" + String.format("%s", order.tel) + LF);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            posPtr.printNormal(ESC + "|lA" + ESC + "|bC" + "------------------------------------------" + LF);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
/*
        if (sellerInfo != null && sellerInfo.size() > 0) {
            String s = (String) sellerInfo.get(0);
            try {
                posPtr.printNormal(ESC + "|lA" + s + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else {
            if (LOG)
                Log.e("ESCPSample", "Error sellerInfo.size()=" + sellerInfo.size());
        }
*/
        if (tradingDate != null) {
            try {
                posPtr.printNormal(ESC + "|lA" + String.format("주문일시: %s", sdf.format(tradingDate)) + LF );
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else {
            try {
                posPtr.printNormal(ESC + "|lA" + String.format("주문일시: %s", "날짜오류") + LF );
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        if ((order.orderNumber != null) && (!order.orderNumber.isEmpty())) {
            try {
                posPtr.printNormal(ESC + "|1F" + String.format("주문번호: %s", order.orderNumber) + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else {
            try {
                posPtr.printNormal(ESC + "|1F" + String.format("주문번호: %s", "주문번호 오류") + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        ArrayList<KioskOrderInfoForPrint.OrderMenuItem> menuItems = order.menuItems;

        try {
            posPtr.printNormal(ESC + "|lA" + ESC + "|bC" + "------------------------------------------" + LF);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            posPtr.printNormal(ESC + "|lA" + ESC + "|bC" + "  품목                 수량         금액  " + LF);
//            posPtr.printNormal(ESC + "|lA" + ESC + "|bC" + "  품목                              수량  " + LF);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            posPtr.printNormal(ESC + "|lA" + ESC + "|bC" + "------------------------------------------" + LF);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }


        for (int j = 0; j < menuItems.size(); j++) {
            KioskOrderInfoForPrint.OrderMenuItem items = (KioskOrderInfoForPrint.OrderMenuItem) menuItems.get(j);
            if ((items.productName != null) && (!items.productName.isEmpty())) {
                String formattedStringPrice = myFormatter.format(Float.valueOf(items.orderPrice)*Float.valueOf(items.orderCount));
                String printPtr ="";
                if(items.productName.length()>max_char)
                    printPtr = String.format("%s",  items.productName.substring(0, max_char));
                else
                    printPtr = String.format("%s",  items.productName);
                try {
                    posPtr.printNormal(ESC + "|lA" +String.format("%s",  printPtr)+LF);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                if ((items.menuOptItems != null) && (items.menuOptItems.size() > 0)) {
                    String tempKind = "";
                    for (int optCount = 0; optCount < items.menuOptItems.size(); optCount++) {
                        KioskOrderInfoForPrint.OrderMenuItem.MenuOptionItem optItem = items.menuOptItems.get(optCount);
                        if (tempKind.isEmpty())
                            tempKind = optItem.optKind;
                        if (!optItem.optKind.equalsIgnoreCase(tempKind)) {
                            tempKind = optItem.optKind;
                        }
                        try {
                            posPtr.printNormal(ESC + "|lA" + " -" + optItem.optName + LF);
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }

                }
                int countlen = 0;
                int spacelen = (max_char/2)-countlen+4;
                if(spacelen <0 )
                    spacelen = 0;
                String countprintStr = spaceStr.substring(0, spacelen);
                printPtr = String.format("%s%s",  countprintStr, items.orderCount);
                try {
                    countlen = printPtr.getBytes("euc-kr").length;
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                int pricelen = 0;
                try {
                    pricelen = formattedStringPrice.getBytes("euc-kr").length;
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                //123456789012345678901234567890123456789012345678901234567890
                spacelen = max_char - pricelen - countlen;
                if(spacelen<0)
                    spacelen = 0;
                String printStr = spaceStr.substring(0, spacelen);
                //String printStr = spaceStr.substring(0, max_char - pricelen - countlen);
                printPtr += String.format("%s%s",  printStr, formattedStringPrice);

                try {
                    posPtr.printNormal(ESC + "|lA" +String.format("%s",  printPtr)+LF);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

            }
            try {
                posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + " " + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

        }
//            posPtr.printNormal(ESC + "|cA"+" " + LF);

        {
            int pricelen = 0;
            try {
                pricelen = formattedStringtotalPrice.getBytes("euc-kr").length;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            //123456789012345678901234567890123456789012345678901234567890
            String priceStr = " 합계:";
            String printStr = null;
            int spacelen = 0;
            try {
                spacelen = max_char - pricelen - priceStr.getBytes("euc-kr").length - 2;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if(spacelen <0)
                spacelen = 0;
                printStr = spaceStr.substring(0, spacelen);
//                printStr = spaceStr.substring(0, max_char - pricelen - priceStr.getBytes("euc-kr").length - 2);

            try {
                posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + "------------------------------------------" + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            try {
                posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + priceStr + printStr + formattedStringtotalPrice + "원" + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            try {
                posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + "------------------------------------------" + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        //<- 주문 정보
        if(order.payment.equalsIgnoreCase("AD")) {
            try {
                posPtr.printAndroidFont("결제:선불결제 완료", nLineWidth, 40, ESCPOSConst.LK_ALIGNMENT_LEFT);
//                posPtr.printNormal(ESC + "|1F" + ESC + "|lA" + ESC + "|4C"  + "결제:선불결제 완료" + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if(order.payment.equalsIgnoreCase("ED")) {
            try {
                posPtr.printAndroidFont("결제:후불결제", nLineWidth, 40, ESCPOSConst.LK_ALIGNMENT_LEFT);
                //posPtr.printNormal(ESC + "|1F" + ESC + "|lA" + ESC + "|4C"  + "결제:후불결제" + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + "------------------------------------------" + LF);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            posPtr.printNormal(ESC + "|lA" + "매장 요청 메시지" + LF+LF);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            posPtr.printNormal(ESC + "|1F" + ESC + "|lA" + ESC + "|bC" + String.format("%s", order.storeMsg) + LF);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        try {
            posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + "------------------------------------------" + LF );
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        //        posPtr.lineFeed(3);
        // POSPrinter Only.
        try {
            posPtr.printNormal(ESC + "|fP");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        posPtr.cutPaper();

        order.printOk = true;
        return STS_NORMAL;
    }
    private int onDeliveryOrderPrintForCustom(KioskOrderInfoForPrint order, ArrayList<String> sellerInfo, String deviceId)
    {
        int max_char = 39;
        String spaceStr = "                                                            ";

        DecimalFormat myFormatter = new DecimalFormat("###,###");
        String formattedStringtotalPrice = myFormatter.format(Float.valueOf(order.goodsAmt));
        SimpleDateFormat sdf = null;
        sdf = new SimpleDateFormat("yyyyMMddHHmmss");

        Date tradingDate = null;
        try {
            tradingDate = sdf.parse(order.orderDate);
        } catch (ParseException ex) {
            Log.v("Exception", ex.getLocalizedMessage());
            Log.e("ESCPSample", "Exception() tradingDate=" + tradingDate);
        }
        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        if (order.cancel.equalsIgnoreCase("Y")) {
            try {
                posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + ESC + "|2C" + "배달 취소주문서[고객용]" + LF + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        else {
            try {
                posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + ESC + "|2C" + "배달 주문서[고객용]" + LF );
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        try {
            posPtr.printNormal(ESC + "|lA" + ESC + "|bC" + "------------------------------------------" + LF);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            posPtr.printNormal(ESC + "|lA" + ESC + "|lA" + "배달주소:" + LF);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        try {
            posPtr.printAndroidFont(String.format("%s%s", order.roadAddr, order.addrDetail), nLineWidth, 40, ESCPOSConst.LK_ALIGNMENT_LEFT);
            //posPtr.printNormal(ESC + "|1F" + ESC + "|lA" + ESC + "|4C" +String.format("%s%s", order.roadAddr, order.addrDetail)+ LF+LF);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            posPtr.printNormal(ESC + "|lA" + ESC + "|lA" + LF+"연락처:" + LF);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            posPtr.printAndroidFont(String.format(" %s", order.tel), nLineWidth, 40, ESCPOSConst.LK_ALIGNMENT_LEFT);
            //posPtr.printNormal(ESC + "|1F" + ESC + "|lA" + ESC + "|4C" + String.format(" %s", order.tel) + LF);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            posPtr.printNormal(ESC + "|lA" + ESC + "|bC" + "------------------------------------------" + LF);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (tradingDate != null) {
            try {
                posPtr.printNormal(ESC + "|lA" + String.format("주문일시: %s", sdf.format(tradingDate)) + LF );
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        else {
            try {
                posPtr.printNormal(ESC + "|lA" + String.format("주문일시: %s", "날짜오류") + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        if ((order.orderNumber != null) && (!order.orderNumber.isEmpty())) {
            try {
                posPtr.printNormal(ESC + "|1F" + String.format("주문번호: %s",order.orderNumber) + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        else {
            try {
                posPtr.printNormal(ESC + "|1F" + String.format("주문번호: %s", "주문번호 오류") + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        ArrayList<KioskOrderInfoForPrint.OrderMenuItem> menuItems = order.menuItems;
        try {
            posPtr.printNormal(ESC + "|lA" + ESC + "|bC" + "------------------------------------------" + LF);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            posPtr.printNormal(ESC + "|lA" + ESC + "|bC" + "  품목                 수량         금액  " + LF);
//            posPtr.printNormal(ESC + "|lA" + ESC + "|bC" + "  품목                              수량  " + LF);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            posPtr.printNormal(ESC + "|lA" + ESC + "|bC" + "------------------------------------------" + LF);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

/*
        for (int j = 0; j < menuItems.size(); j++) {
            KioskOrderInfoForPrint.OrderMenuItem items = (KioskOrderInfoForPrint.OrderMenuItem) menuItems.get(j);
            if ((items.productName != null) && (!items.productName.isEmpty())) {
                int menulen = 0;
                try {
                    menulen = items.productName.getBytes("euc-kr").length;
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                int countlen = 0;
                try {
                    countlen = items.orderCount.getBytes("euc-kr").length;
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                String testStr = String.format("%.8s  %2s %4s  ", items.productName, items.orderCount, items.orderPrice  );
//                String testStr = items.productName + spaceStr.substring(0, max_char - menulen - countlen);

                try {
                    posPtr.printNormal(ESC + "|cA" + testStr + LF);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
            if ((items.menuOptItems != null) && (items.menuOptItems.size() > 0)) {
                String tempKind = "";
                for (int optCount = 0; optCount < items.menuOptItems.size(); optCount++) {
                    KioskOrderInfoForPrint.OrderMenuItem.MenuOptionItem optItem = items.menuOptItems.get(optCount);
                    if (tempKind.isEmpty())
                        tempKind = optItem.optKind;
                    if (!optItem.optKind.equalsIgnoreCase(tempKind)) {
//                            posPtr.printNormal(ESC + "|cA" +ESC + "|bC" +" " + LF );
                        tempKind = optItem.optKind;
                    }
                    try {
                        posPtr.printNormal(ESC + "|lA" + " -" + optItem.optName + LF);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }

            }
            try {
                posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + " " + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

        }
*/
        for (int j = 0; j < menuItems.size(); j++) {
            KioskOrderInfoForPrint.OrderMenuItem items = (KioskOrderInfoForPrint.OrderMenuItem) menuItems.get(j);
            if ((items.productName != null) && (!items.productName.isEmpty())) {
                String formattedStringPrice = myFormatter.format(Float.valueOf(items.orderPrice)*(Float.valueOf(items.orderCount)));
                String printPtr ="";
                if(items.productName.length()>max_char)
                    printPtr = String.format("%s",  items.productName.substring(0, max_char));
                else
                    printPtr = String.format("%s",  items.productName);
                try {
                    posPtr.printNormal(ESC + "|lA" + printPtr + LF);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                if ((items.menuOptItems != null) && (items.menuOptItems.size() > 0)) {
                    String tempKind = "";
                    for (int optCount = 0; optCount < items.menuOptItems.size(); optCount++) {
                        KioskOrderInfoForPrint.OrderMenuItem.MenuOptionItem optItem = items.menuOptItems.get(optCount);
                        if (tempKind.isEmpty())
                            tempKind = optItem.optKind;
                        if (!optItem.optKind.equalsIgnoreCase(tempKind)) {
                            tempKind = optItem.optKind;
                        }
                        try {
                            if(optItem.optName.length()> max_char-2)
                                posPtr.printNormal(ESC + "|lA" + " -" + optItem.optName.substring(0, max_char-2) + LF);
                            else
                                posPtr.printNormal(ESC + "|lA" + " -" + optItem.optName + LF);
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }

                }

                int countlen = 0;
                int spacelen = (max_char/2)-countlen+4;
                if(spacelen <0 )
                    spacelen = 0;
                String countprintStr = spaceStr.substring(0, spacelen);

//                String countprintStr = spaceStr.substring(0, (max_char/2)-countlen+4);
                printPtr = String.format("%s%s",  countprintStr, items.orderCount);
                try {
                    countlen = printPtr.getBytes("euc-kr").length;
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                int pricelen = 0;
                try {
                    pricelen = formattedStringPrice.getBytes("euc-kr").length;
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                //123456789012345678901234567890123456789012345678901234567890
                spacelen = max_char - pricelen - countlen;
                if(spacelen<0)
                    spacelen = 0;
                String printStr = spaceStr.substring(0, spacelen);
//                String printStr = spaceStr.substring(0, max_char - pricelen - countlen);
                printPtr += String.format("%s%s",  printStr, formattedStringPrice);

                try {
                    posPtr.printNormal(ESC + "|lA" +String.format("%s",  printPtr)+LF);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

            }
            try {
                posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + " " + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

        }

//            posPtr.printNormal(ESC + "|cA"+" " + LF);


        {
            int pricelen = 0;
            try {
                pricelen = formattedStringtotalPrice.getBytes("euc-kr").length;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            //123456789012345678901234567890123456789012345678901234567890
            String priceStr = " 합계:";
            String printStr = null;
            int spacelen = 0;
            try {
                spacelen = max_char - pricelen - priceStr.getBytes("euc-kr").length - 2;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if(spacelen <0)
                spacelen = 0;
            printStr = spaceStr.substring(0, spacelen);
//            printStr = spaceStr.substring(0, max_char - pricelen - priceStr.getBytes("euc-kr").length - 2);

            try {
                posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + "------------------------------------------" + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            try {
                posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + priceStr + printStr + formattedStringtotalPrice + "원" + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            try {
                posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + "------------------------------------------" + LF );
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        //<- 주문 정보

        if(order.payment.equalsIgnoreCase("AD")) {
            try {
                posPtr.printAndroidFont("결제:선불결제 완료", nLineWidth, 40, ESCPOSConst.LK_ALIGNMENT_LEFT);
//                posPtr.printNormal(ESC + "|1F" + ESC + "|lA" + ESC + "|4C" + "결제:선불결제 완료" + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if(order.payment.equalsIgnoreCase("ED")) {
            try {
                posPtr.printAndroidFont("결제:후불결제", nLineWidth, 40, ESCPOSConst.LK_ALIGNMENT_LEFT);
                //posPtr.printNormal(ESC + "|1F" + ESC + "|lA" + ESC + "|4C" + "결제:후불결제" + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + "------------------------------------------" + LF);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        if (sellerInfo != null && sellerInfo.size() > 0) {
            for(int i = 0; i<sellerInfo.size(); i++)
            {
                String s = sellerInfo.get(i);
                try {
                    posPtr.printNormal(ESC + "|lA" + s + LF);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        } else {
            if (LOG)
                Log.e("ESCPSample", "Error sellerInfo.size()=" + sellerInfo.size());
        }
        try {
            posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + "------------------------------------------" + LF );
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        try {
            posPtr.printNormal(ESC + "|lA" + "배달 요청 메시지" + LF+LF);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            posPtr.printNormal(ESC + "|1F" + ESC + "|lA" + ESC + "|bC" + String.format("%s", order.deliMsg) + LF);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        try {
            posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + "------------------------------------------" + LF );
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            posPtr.printNormal(ESC + "|lA" + "불편사항이나 문의는 매장으로 연락주세요." + LF);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            posPtr.printNormal(ESC + "|lA" + "이용해주셔서 감사합니다." + LF);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            posPtr.printNormal(ESC + "|fP");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        posPtr.cutPaper();

        order.printOk = true;
        return STS_NORMAL;
    }
    private int onPackingOrderPrintForCustom(KioskOrderInfoForPrint order, ArrayList<String> sellerInfo, String deviceId)
    {
        int max_char = 39;
        String spaceStr = "                                                            ";

        DecimalFormat myFormatter = new DecimalFormat("###,###");
        String formattedStringtotalPrice = myFormatter.format(Float.valueOf(order.goodsAmt));
        SimpleDateFormat sdf = null;
        sdf = new SimpleDateFormat("yyyyMMddHHmmss");

        Date tradingDate = null;
        try {
            tradingDate = sdf.parse(order.orderDate);
        } catch (ParseException ex) {
            Log.v("Exception", ex.getLocalizedMessage());
            Log.e("ESCPSample", "Exception() tradingDate=" + tradingDate);
        }
        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        if (order.cancel.equalsIgnoreCase("Y")) {
            try {
                posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + ESC + "|2C" + "취소주문서[고객용]" + LF + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        else {
            try {
                posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + ESC + "|2C" + "주문서[고객용]" + LF );
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        if (sellerInfo != null && sellerInfo.size() > 0) {
            String s = (String) sellerInfo.get(0);
            try {
                posPtr.printNormal(ESC + "|lA" + s + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else {
            if (LOG)
                Log.e("ESCPSample", "Error sellerInfo.size()=" + sellerInfo.size());
        }

        if (tradingDate != null) {
            try {
                posPtr.printNormal(ESC + "|lA" + String.format("주문일시: %s", sdf.format(tradingDate)) + LF );
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        else {
            try {
                posPtr.printNormal(ESC + "|lA" + String.format("주문일시: %s", "날짜오류") + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        if ((order.orderNumber != null) && (!order.orderNumber.isEmpty())) {
            try {
                posPtr.printNormal(ESC + "|1F" + "주문번호           " + ESC + "|cA" + ESC + "|bC" + ESC + "|rA" + ESC + "|4C" + order.orderNumber + LF);
                //posPtr.printNormal(ESC + "|1F" + String.format("주문번호: %s",order.orderNumber) + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        else {
            try {
                posPtr.printNormal(ESC + "|1F" + "주문번호           " + ESC + "|cA" + ESC + "|bC" + ESC + "|rA" + ESC + "|4C" + "주문번호 오류" + LF);
                //posPtr.printNormal(ESC + "|1F" + String.format("주문번호: %s", "주문번호 오류") + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        if((order.reservTime!=null)&&(!order.reservTime.isEmpty()))
        {
            try {
                posPtr.printNormal(ESC + "|lA" +  LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            try {
                posPtr.printNormal(ESC + "|1F" + "예약시간         " + ESC + "|cA" + ESC + "|bC" + ESC + "|rA" + ESC + "|4C" + order.reservTime + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        ArrayList<KioskOrderInfoForPrint.OrderMenuItem> menuItems = order.menuItems;

        if (menuItems.size() > 0) {
            KioskOrderInfoForPrint.OrderMenuItem items = (KioskOrderInfoForPrint.OrderMenuItem) menuItems.get(0);
            if (items.orderPackage) {
                try {
                    posPtr.printNormal(ESC + "|lA" +  LF);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                try {
                    posPtr.printNormal(ESC + "|1F" + "주문유형           " + ESC + "|cA" + ESC + "|bC" + ESC + "|rA" + ESC + "|4C" + "포장" + LF);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }



        try {
            posPtr.printNormal(ESC + "|lA" + ESC + "|bC" + "------------------------------------------" + LF);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            posPtr.printNormal(ESC + "|lA" + ESC + "|bC" + "  품목                수량         금액  " + LF);
//            posPtr.printNormal(ESC + "|lA" + ESC + "|bC" + "  품목                              수량  " + LF);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            posPtr.printNormal(ESC + "|lA" + ESC + "|bC" + "------------------------------------------" + LF);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        for (int j = 0; j < menuItems.size(); j++) {
            KioskOrderInfoForPrint.OrderMenuItem items = (KioskOrderInfoForPrint.OrderMenuItem) menuItems.get(j);
            if ((items.productName != null) && (!items.productName.isEmpty())) {
                String formattedStringPrice = myFormatter.format(Float.valueOf(items.orderPrice)*(Float.valueOf(items.orderCount)));
                String printPtr ="";
                if(items.productName.length()>max_char)
                    printPtr = String.format("%s",  items.productName.substring(0, max_char));
                else
                    printPtr = String.format("%s",  items.productName);
                try {
                    posPtr.printNormal(ESC + "|lA" + printPtr + LF);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                if ((items.menuOptItems != null) && (items.menuOptItems.size() > 0)) {
                    String tempKind = "";
                    for (int optCount = 0; optCount < items.menuOptItems.size(); optCount++) {
                        KioskOrderInfoForPrint.OrderMenuItem.MenuOptionItem optItem = items.menuOptItems.get(optCount);
                        if (tempKind.isEmpty())
                            tempKind = optItem.optKind;
                        if (!optItem.optKind.equalsIgnoreCase(tempKind)) {
//                            posPtr.printNormal(ESC + "|cA" +ESC + "|bC" +" " + LF );
                            tempKind = optItem.optKind;
                        }
                        try {
                            if(optItem.optName.length()>max_char-2)
                                posPtr.printNormal(ESC + "|lA" + " -" + optItem.optName.substring(0, max_char-2) + LF);
                            else
                                posPtr.printNormal(ESC + "|lA" + " -" + optItem.optName + LF);
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }

                }
                int countlen = 0;
                int spacelen = (max_char/2)+4;
                if(spacelen <0 )
                    spacelen = 0;
                String countprintStr = spaceStr.substring(0, spacelen);
                printPtr = String.format("%s%s",  countprintStr, items.orderCount);
                try {
                    countlen = printPtr.getBytes("euc-kr").length;
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                printPtr = String.format("%s%s",  countprintStr, items.orderCount);
                try {
                    countlen = printPtr.getBytes("euc-kr").length;
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                int pricelen = 0;
                try {
                    pricelen = formattedStringPrice.getBytes("euc-kr").length;
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                //123456789012345678901234567890123456789012345678901234567890
                spacelen = max_char - pricelen - countlen;
                if(spacelen<0)
                    spacelen = 0;
                String printStr = spaceStr.substring(0, spacelen);
//                String printStr = spaceStr.substring(0, max_char - pricelen - countlen);
                printPtr += String.format("%s%s",  printStr, formattedStringPrice);

                try {
                    posPtr.printNormal(ESC + "|lA" +String.format("%s",  printPtr)+LF);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

            }

            try {
                posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + " " + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

        }

//            posPtr.printNormal(ESC + "|cA"+" " + LF);


        {
            int pricelen = 0;
            try {
                pricelen = formattedStringtotalPrice.getBytes("euc-kr").length;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            //123456789012345678901234567890123456789012345678901234567890
            String priceStr = " 합계:";
            String printStr = null;
            int spacelen = 0;
            try {
                spacelen = max_char - pricelen - priceStr.getBytes("euc-kr").length - 2;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if(spacelen<0)
                spacelen =0;
            printStr = spaceStr.substring(0, spacelen);
//            printStr = spaceStr.substring(0, max_char - pricelen - priceStr.getBytes("euc-kr").length - 2);

            try {
                posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + "------------------------------------------" + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            try {
                posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + priceStr + printStr + formattedStringtotalPrice + "원" + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            try {
                posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + "------------------------------------------" + LF );
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        //<- 주문 정보

        if(order.payment.equalsIgnoreCase("AD")) {
            try {
                posPtr.printAndroidFont("결제:선불결제 완료", nLineWidth, 40, ESCPOSConst.LK_ALIGNMENT_LEFT);
//                posPtr.printNormal(ESC + "|1F" + ESC + "|lA" + ESC + "|4C" + "결제:선불결제 완료" + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if(order.payment.equalsIgnoreCase("ED")) {
            try {
                posPtr.printAndroidFont("결제:후불결제", nLineWidth, 40, ESCPOSConst.LK_ALIGNMENT_LEFT);
                //posPtr.printNormal(ESC + "|1F" + ESC + "|lA" + ESC + "|4C" + "결제:후불결제" + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + "------------------------------------------" + LF);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        if (sellerInfo != null && sellerInfo.size() > 0) {
            for(int i = 0; i<sellerInfo.size(); i++)
            {
                String s = sellerInfo.get(i);
                try {
                    posPtr.printNormal(ESC + "|lA" + s + LF);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        } else {
            if (LOG)
                Log.e("ESCPSample", "Error sellerInfo.size()=" + sellerInfo.size());
        }
        try {
            posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + "------------------------------------------" + LF );
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
/*
        try {
            posPtr.printNormal(ESC + "|lA" + "배달 요청 메시지" + LF+LF);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            posPtr.printNormal(ESC + "|1F" + ESC + "|lA" + ESC + "|bC" + String.format("%s", order.deliMsg) + LF);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        try {
            posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + "------------------------------------------" + LF );
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
*/
        try {
            posPtr.printNormal(ESC + "|lA" + "불편사항이나 문의는 매장으로 연락주세요." + LF);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            posPtr.printNormal(ESC + "|lA" + "이용해주셔서 감사합니다." + LF);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            posPtr.printNormal(ESC + "|fP");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        posPtr.cutPaper();

        order.printOk = true;
        return STS_NORMAL;
    }
    private int onPackingOrderPrintForStore(KioskOrderInfoForPrint order, String deviceId) {
        int max_char = 40;
        String spaceStr = "                                                             ";

        DecimalFormat myFormatter = new DecimalFormat("###,###");
        String formattedStringtotalPrice = myFormatter.format(Float.valueOf(order.goodsAmt));
        SimpleDateFormat sdf = null;
        sdf = new SimpleDateFormat("yyyyMMddHHmmss");

        Date tradingDate = null;
        try {
            tradingDate = sdf.parse(order.orderDate);
        } catch (ParseException ex) {
            Log.v("Exception", ex.getLocalizedMessage());
            Log.e("ESCPSample", "Exception() tradingDate=" + tradingDate);
        }
        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        if (order.cancel.equalsIgnoreCase("Y")) {
            try {
                posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + ESC + "|2C" + "취소주문서[매장용] " + LF + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else {
            try {
                posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + ESC + "|2C" + "주문서 [매장용]" + LF + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        if (tradingDate != null) {
            try {
                posPtr.printNormal(ESC + "|lA" + String.format("주문일시: %s", sdf.format(tradingDate)) + LF );
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else {
            try {
                posPtr.printNormal(ESC + "|lA" + String.format("주문일시: %s", "날짜오류") + LF );
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        if ((order.orderNumber != null) && (!order.orderNumber.isEmpty())) {
            try {
                posPtr.printNormal(ESC + "|1F" + "주문번호           " + ESC + "|cA" + ESC + "|bC" + ESC + "|rA" + ESC + "|4C" + order.orderNumber + LF);
                //posPtr.printNormal(ESC + "|1F" + String.format("주문번호: %s", order.orderNumber) + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else {
            try {
                posPtr.printNormal(ESC + "|1F" + "주문번호           " + ESC + "|cA" + ESC + "|bC" + ESC + "|rA" + ESC + "|4C" + "주문번호 오류" + LF);
                //posPtr.printNormal(ESC + "|1F" + String.format("주문번호: %s", "주문번호 오류") + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        if((order.reservTime!=null)&&(!order.reservTime.isEmpty()))
        {
            try {
                posPtr.printNormal(ESC + "|lA" +  LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            try {
                posPtr.printNormal(ESC + "|1F" + "예약시간         " + ESC + "|cA" + ESC + "|bC" + ESC + "|rA" + ESC + "|4C" + order.reservTime + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        ArrayList<KioskOrderInfoForPrint.OrderMenuItem> menuItems = order.menuItems;
        if (menuItems.size() > 0) {
            KioskOrderInfoForPrint.OrderMenuItem items = (KioskOrderInfoForPrint.OrderMenuItem) menuItems.get(0);
            if (items.orderPackage) {
                try {
                    posPtr.printNormal(ESC + "|lA" +  LF);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                try {
                    posPtr.printNormal(ESC + "|1F" + "주문유형           " + ESC + "|cA" + ESC + "|bC" + ESC + "|rA" + ESC + "|4C" + "포장" + LF);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            posPtr.printNormal(ESC + "|lA" + ESC + "|bC" + "------------------------------------------" + LF);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            posPtr.printNormal(ESC + "|lA" + ESC + "|bC" + "  품목                수량         금액  " + LF);
//            posPtr.printNormal(ESC + "|lA" + ESC + "|bC" + "  품목                              수량  " + LF);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            posPtr.printNormal(ESC + "|lA" + ESC + "|bC" + "------------------------------------------" + LF);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }


        for (int j = 0; j < menuItems.size(); j++) {
            KioskOrderInfoForPrint.OrderMenuItem items = (KioskOrderInfoForPrint.OrderMenuItem) menuItems.get(j);
            if ((items.productName != null) && (!items.productName.isEmpty())) {
                String formattedStringPrice = myFormatter.format(Float.valueOf(items.orderPrice)*Float.valueOf(items.orderCount));
                String printPtr ="";
                if(items.productName.length()>max_char)
                    printPtr = String.format("%s",  items.productName.substring(0, max_char));
                else
                    printPtr = String.format("%s",  items.productName);
                try {
                    posPtr.printNormal(ESC + "|lA" +String.format("%s",  printPtr)+LF);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                if ((items.menuOptItems != null) && (items.menuOptItems.size() > 0)) {
                    String tempKind = "";
                    for (int optCount = 0; optCount < items.menuOptItems.size(); optCount++) {
                        KioskOrderInfoForPrint.OrderMenuItem.MenuOptionItem optItem = items.menuOptItems.get(optCount);
                        if (tempKind.isEmpty())
                            tempKind = optItem.optKind;
                        if (!optItem.optKind.equalsIgnoreCase(tempKind)) {
//                            posPtr.printNormal(ESC + "|cA" +ESC + "|bC" +" " + LF );
                            tempKind = optItem.optKind;
                        }
                        try {
                            posPtr.printNormal(ESC + "|lA" + " -" + optItem.optName + LF);
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }

                }
                int countlen = 0;
                int spacelen = (max_char/2)+4;
                if(spacelen <0 )
                    spacelen = 0;
                String countprintStr = spaceStr.substring(0, spacelen);
                printPtr = String.format("%s%s",  countprintStr, items.orderCount);
                try {
                    countlen = printPtr.getBytes("euc-kr").length;
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                int pricelen = 0;
                try {
                    pricelen = formattedStringPrice.getBytes("euc-kr").length;
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                //123456789012345678901234567890123456789012345678901234567890
                spacelen = max_char - pricelen - countlen;
                if(spacelen<0)
                    spacelen = 0;
                String printStr = spaceStr.substring(0, spacelen);
                printPtr += String.format("%s%s",  printStr, formattedStringPrice);

                try {
                    posPtr.printNormal(ESC + "|lA" +String.format("%s",  printPtr)+LF);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

            }

            try {
                posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + " " + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

        }
//            posPtr.printNormal(ESC + "|cA"+" " + LF);

        {
            int pricelen = 0;
            try {
                pricelen = formattedStringtotalPrice.getBytes("euc-kr").length;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            //123456789012345678901234567890123456789012345678901234567890
            String priceStr = " 합계:";
            String printStr = null;
            int spacelen = 0;
            try {
                spacelen = max_char - pricelen - priceStr.getBytes("euc-kr").length - 2;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if(spacelen < 0)
                spacelen = 0;
            printStr = spaceStr.substring(0, spacelen);
//            printStr = spaceStr.substring(0, max_char - pricelen - priceStr.getBytes("euc-kr").length - 2);

            try {
                posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + "------------------------------------------" + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            try {
                posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + priceStr + printStr + formattedStringtotalPrice + "원" + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            try {
                posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + "------------------------------------------" + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        //<- 주문 정보
        if(order.payment.equalsIgnoreCase("AD")) {
            try {
                posPtr.printAndroidFont("결제:선불결제 완료", nLineWidth, 40, ESCPOSConst.LK_ALIGNMENT_LEFT);
//                posPtr.printNormal(ESC + "|1F" + ESC + "|lA" + ESC + "|4C"  + "결제:선불결제 완료" + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if(order.payment.equalsIgnoreCase("ED")) {
            try {
                posPtr.printAndroidFont("결제:후불결제", nLineWidth, 40, ESCPOSConst.LK_ALIGNMENT_LEFT);
                //posPtr.printNormal(ESC + "|1F" + ESC + "|lA" + ESC + "|4C"  + "결제:후불결제" + LF);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + "------------------------------------------" + LF);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            posPtr.printNormal(ESC + "|lA" + "매장 요청 메시지" + LF+LF);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            posPtr.printNormal(ESC + "|1F" + ESC + "|lA" + ESC + "|bC" + String.format("%s", order.storeMsg) + LF);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        try {
            posPtr.printNormal(ESC + "|cA" + ESC + "|bC" + "------------------------------------------" + LF );
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        //        posPtr.lineFeed(3);
        // POSPrinter Only.
        try {
            posPtr.printNormal(ESC + "|fP");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        posPtr.cutPaper();

        order.printOk = true;
        return STS_NORMAL;
    }
}
