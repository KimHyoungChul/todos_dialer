package com.todosdialer.todosdialer.model;

public class OrderInfo {
    public static final int STATE_READY_FOR_PAY = 100;
    public static final int STATE_COMPLETE = 200;
    public static final int STATE_CANCE = 400;

    public String no;
    public String orderNo;
    public float orderPrice;
    public int orderDayCount;
    public float orderTotalPrice;
    public String basicTelecomCodeName;
    public String outNationCodeName;
    public String outDate;
    public String inDate;
    public String orderStateName;
    public String usimPhone;
    public String usimPhone2;
    public String usimMethodName;
}
