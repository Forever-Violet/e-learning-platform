package com.xuecheng.base.exception;

/**
 * @author 12508
 * @description 学成在线项目异常类
 */
public class XueChengPlusException extends RuntimeException{
    private String errMessage;
    private String errCode;

    public XueChengPlusException(){
        super();
    }

    public XueChengPlusException(String errMessage) {
        super(errMessage);
        this.errMessage = errMessage;
    }

    public XueChengPlusException(String errCode, String errMessage) {
        super(errMessage);
        this.errCode = errCode;
        this.errMessage = errMessage;
    }

    public String getErrMessage(){
        return errMessage;
    }

    public String getErrCode() {
        return errCode;
    }

    public static void cast(CommonError commonError) {
        throw new XueChengPlusException(commonError.getErrMessage());
    }

    public static void cast(String errMessage) {
        throw new XueChengPlusException(errMessage);
    }


}
