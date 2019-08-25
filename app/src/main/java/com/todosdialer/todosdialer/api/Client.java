package com.todosdialer.todosdialer.api;

import com.todosdialer.todosdialer.api.body.AppCodeBody;
import com.todosdialer.todosdialer.api.body.AppMemberInfoBody;
import com.todosdialer.todosdialer.api.body.CheckIdBody;
import com.todosdialer.todosdialer.api.body.CheckPhoneBody;
import com.todosdialer.todosdialer.api.body.CheckSessionBody;
import com.todosdialer.todosdialer.api.body.CodeGroupBody;
import com.todosdialer.todosdialer.api.body.FindingPwBody;
import com.todosdialer.todosdialer.api.body.LoadOrderBody;
import com.todosdialer.todosdialer.api.body.PayResultBody;
import com.todosdialer.todosdialer.api.body.RegisterOrderBody;
import com.todosdialer.todosdialer.api.body.SendingMessageBody;
import com.todosdialer.todosdialer.api.body.SignInBody;
import com.todosdialer.todosdialer.api.body.SignUpBody;
import com.todosdialer.todosdialer.api.response.AppMemberInfoResponse;
import com.todosdialer.todosdialer.api.response.BaseResponse;
import com.todosdialer.todosdialer.api.response.LoadAppCodeResponse;
import com.todosdialer.todosdialer.api.response.LoadCodeGroupResponse;
import com.todosdialer.todosdialer.api.response.LoadOrderResponse;
import com.todosdialer.todosdialer.api.response.RegOrderResponse;
import com.todosdialer.todosdialer.api.response.SipSessionInfoResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public class Client {
    public static final String MESSAGE_URL = "";
    public static final String MESSAGE_CLIENT_ID = "";
    public static final String MESSAGE_TOPOIC = "";

    public interface MessageApi {
        @FormUrlEncoded
        @PUT("api/todosdialer/sms/{from}")
        Call<BaseResponse> sendMessage(@Path("from") String phoneNumber,
                                       @Header("password") String password,
                                       @FieldMap SendingMessageBody body);
    }

    public interface Api {
        @POST("app/AppCheckId.asp")
        Call<BaseResponse> checkEmailAsId(@Body CheckIdBody body);

        @POST("app/AppCheckPhone.asp")
        Call<BaseResponse> checkPhoneNumber(@Body CheckPhoneBody body);

        @POST("app/AppMemberReg.asp")
        Call<BaseResponse> signUp(@Body SignUpBody body);

        @POST("app/AppMemberLogin.asp")
        Call<BaseResponse> signIn(@Body SignInBody body);

        @POST("app/AppFindPw.asp")
        Call<BaseResponse> findPassword(@Body FindingPwBody body);

        @POST("app/AppSessionInfo.asp")
        Call<SipSessionInfoResponse> checkSessionInfo(@Body CheckSessionBody body);

        @POST("app/AppOrderList.asp")
        Call<LoadOrderResponse> loadOrder(@Body LoadOrderBody body);

        @POST("app/AppMemberInfo.asp")
        Call<AppMemberInfoResponse> loadAppMemberInfo(@Body AppMemberInfoBody body);


        @POST("app/AppOrderReg.asp")
        Call<RegOrderResponse> registerOrder(@Body RegisterOrderBody body);

        @POST("app/AppCodeGroup.asp")
        Call<LoadCodeGroupResponse> loadAppCodeGroup(@Body CodeGroupBody body);

        @POST("app/AppCode.asp")
        Call<LoadAppCodeResponse> loadAppCode(@Body AppCodeBody body);

        @POST("app/AppPayResult.asp")
        Call<BaseResponse> sendPaymentResult(@Body PayResultBody body);
    }
}