package kr.co.bbmc.selforderutil;

import java.util.ArrayList;

public class KioskOrderInfoForPrint {
    public String payment;
    public String orderType;
    public String tel;
    public String orderTable;
    public String deliMsg;
    public String addrDetail;
    public String roadAddr;
    public String storeMsg;

    public String goodsAmt;
    public String orderDate;
    public String orderNumber;
    public String recommandId;
    public String storeName;
    public String cancel;
    public String reservTime="";
    public ArrayList<OrderMenuItem> menuItems = new ArrayList<OrderMenuItem>();

    public static class OrderMenuItem {
        public String orderPrice;
        public String productName;
        public String productId;
        public String orderCount;
        public boolean orderPackage;
        //public String reservTime;
        public ArrayList<MenuOptionItem> menuOptItems = new ArrayList<MenuOptionItem>();
        public static class MenuOptionItem {
            public String optKind="";  //"0" : 필수정보, "1": 추가정보
            public String optName="";
        }

    }
    public boolean printOk = false;
}
