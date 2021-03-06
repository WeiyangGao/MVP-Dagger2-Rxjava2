package com.zlb.dagger.module;

import android.app.Application;
import android.content.Context;

import com.kingja.loadsir.core.LoadSir;
//import com.squareup.leakcanary.LeakCanary;
//import com.squareup.leakcanary.RefWatcher;
import com.zlb.Sp.SPDao;
import com.zlb.Sp.SPKey;
import com.zlb.commontips.EmptyCallback;
import com.zlb.commontips.ErrorCallback;
import com.zlb.commontips.TimeoutCallback;
import com.zlb.http.ApiService;
import com.zlb.http.HttpRetrofit;
import com.zlb.persistence.dbmaster.DaoMaster;
import com.zlb.persistence.dbmaster.DaoSession;
import com.zlb.persistence.dbupdate.MySQLiteOpenHelper;

import org.greenrobot.greendao.database.Database;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * 在这里提供全局的并且是唯一的东西，SharedPreferences,DB,HTTP,etc
 * <p>
 * Created by anylife.zlb@gmail.com on 2018/1/11.
 */
@Module
public class BaseGlobalModule {

    public static final boolean ENCRYPTED = false;
    public Application mContext;

    /**
     * Module  带有构造方法并且参数被使用的情况下所产生的Component 是没有Create方法的
     *
     * @param mContext
     */
    public BaseGlobalModule(Application mContext) {
        this.mContext = mContext;
    }


    /***
     * @return
     */
    @Provides
    @Singleton
    public Context provideContext() {
        return mContext;
    }

    /**
     * SharedPreferences 保存KEY VALUE 配置信息
     *
     * @return
     */
    @Provides
    @Singleton
    //在这加了Singleton 的注解就是单例的了，打出内存地址查看一下
    public SPDao provideSPDao() {
        return new SPDao(mContext);
    }


//    /***
//     * @return
//     */
//    @Provides
//    @Singleton
//    public RefWatcher provideRefWatcher(Application mContext) {
//        return LeakCanary.install(mContext);  //只管主进程的,其他的进程自保吧
//    }


    /**
     * 网络访问，拆分到每个模块吧
     *
     * @return
     */
    @Provides
    @Singleton
    public ApiService provideApiService(SPDao spDao, Context mContext) {
        //Retrofit 的create 真是精华所在啊！
        return HttpRetrofit.getRetrofit(spDao, mContext).create(ApiService.class);
    }

//    /**
//     * 网络访问，拆分到每个模块吧
//     *
//     * @return
//     */
//    @Provides
//    @Singleton
//    public ApiService provideApiService2(SPDao spDao, Context mContext) {
//        //Retrofit 的create 真是精华所在啊！
//        return HttpRetrofit.getRetrofit(spDao, mContext).create(ApiService.class);
//    }


    /**
     * 增加Error，empty,Loading,timeout,等通用的场景处理，一处Root注入，处处可用
     *
     * @return
     */
    @Provides
    @Singleton
    public LoadSir provideCommonStatusService() {
        return new LoadSir.Builder()
//                .addCallback(new LoadingCallback())
                .addCallback(new EmptyCallback())
                .addCallback(new ErrorCallback())
                .addCallback(new TimeoutCallback())
                .build();
    }

    /**
     * 数据库访问的DaoSession,因为是分账号分库，那么切换账号后怎么更换DaoSession链接的数据库DB呢？
     */
    @Provides
//    @Singleton //这个怎么能够动态的替换呢DB 链接的a
    public DaoSession provideDaoSession(SPDao spDao) {
        String account = spDao.getData(SPKey.KEY_LAST_ACCOUNT, "default_error_db", String.class);
        String DBName = ENCRYPTED ? account + "encrypted" : account;
        MySQLiteOpenHelper helper = new MySQLiteOpenHelper(mContext, DBName, null);
        Database db = ENCRYPTED ? helper.getEncryptedWritableDb("super-secret") : helper.getWritableDb();
        return new DaoMaster(db).newSession();
    }

}