package com.todosdialer.todosdialer.manager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.Toast;

import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;
import com.todosdialer.todosdialer.BuildConfig;
import com.todosdialer.todosdialer.R;
import com.todosdialer.todosdialer.util.Utils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class RetrofitManager {
    public static final String BASE_URL = "http://www.todosdialer.com/";//todos_dialer main 페이지
    public static final String PRODUCT_URL = "http://brand.todosdialer.com/?c=83";//todos_dialer 상품안내 페이지
    public static final String INFOMATION = "http://brand.todosdialer.com/?c=106";//todos_dialer 서비스 소개 페이지
    private static final String BASE_SSL_URL = "http://www.todosdialer.com";

    private static Retrofit retrofit = null;
    private static Retrofit sslRetrofit = null;

    private static PersistentCookieJar cookieJar = null;

    public static Retrofit retrofit(Context context) {
        if (retrofit == null) {
            OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
            httpClient.cookieJar(cookieJar(context));
            httpClient.addInterceptor(new NetworkCheckInterceptor(context));

            if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                logging.setLevel(HttpLoggingInterceptor.Level.BODY);
                httpClient.addInterceptor(logging);
            }

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(httpClient.build())
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create(GsonManager.getGson()))
                    .build();
        }
        return retrofit;
    }

    private static PersistentCookieJar cookieJar(Context context) {
        if (cookieJar == null) {
            cookieJar = new PersistentCookieJar(new SetCookieCache(), new SharedPrefsCookiePersistor(context));
        }
        return cookieJar;
    }

    public static void clearCookie() {
        if (cookieJar != null) {
            cookieJar.clearSession();
            cookieJar.clear();
        }
    }

    public static Retrofit messagingRetrofit(Context context, String url) {
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.cookieJar(cookieJar(context));
        httpClient.addInterceptor(new NetworkCheckInterceptor(context));

        if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            httpClient.addInterceptor(logging);
        }

        return new Retrofit.Builder()
                .baseUrl(url)
                .client(httpClient.build())
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(GsonManager.getGson()))
                .build();
    }

    public static Retrofit sslRetrofit(Context context) {
        if (sslRetrofit == null) {
            OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
            httpClient.cookieJar(cookieJar(context));
            httpClient.addInterceptor(new NetworkCheckInterceptor(context));

            if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                logging.setLevel(HttpLoggingInterceptor.Level.BODY);
                httpClient.addInterceptor(logging);
            }

            sslRetrofit = new Retrofit.Builder()
                    .baseUrl(BASE_SSL_URL)
                    .client(addSslSocketFactory(httpClient.build()))
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create(GsonManager.getGson()))
                    .build();
        }
        return sslRetrofit;
    }

    private static OkHttpClient addSslSocketFactory(OkHttpClient httpClient) {
        try {
            SSLContext sc;
            sc = SSLContext.getInstance("SSL");
            sc.init(null, new TrustManager[]{new X509TrustManager() {
                @SuppressLint("TrustAllX509TrustManager")
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                }

                @SuppressLint("TrustAllX509TrustManager")
                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            }}, new SecureRandom());
            HostnameVerifier hv = new HostnameVerifier() {
                @SuppressLint("BadHostnameVerifier")
                @Override
                public boolean verify(String s, SSLSession sslSession) {
                    return true;
                }
            };
            String workerClassName = "okhttp3.OkHttpClient";
            try {
                Class workerClass = Class.forName(workerClassName);
                Field hostnameVerifier = workerClass.getDeclaredField("hostnameVerifier");
                hostnameVerifier.setAccessible(true);
                hostnameVerifier.set(httpClient, hv);
                Field sslSocketFactory = workerClass.getDeclaredField("sslSocketFactory");
                sslSocketFactory.setAccessible(true);
                sslSocketFactory.set(httpClient, sc.getSocketFactory());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return httpClient;
    }

    public static class NetworkCheckInterceptor implements Interceptor {
        private Context context;

        public NetworkCheckInterceptor(Context context) {
            this.context = context;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            if (Utils.isNetworkStateFine(context)) {
                return chain.proceed(chain.request());
            } else {
                throw new NoConnectivityException();
            }
        }
    }

    public static class NoConnectivityException extends IOException {
        @Override
        public String getMessage() {
            return "No network available, please check your WiFi or Data connection";
        }
    }
}