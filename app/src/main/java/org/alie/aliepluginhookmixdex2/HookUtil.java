package org.alie.aliepluginhookmixdex2;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.util.Log;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

/**
 * Created by Alie on 2019/10/6.
 * 类描述
 * 版本
 */
public class HookUtil {

    private static final String TAG = "HookUtil";
    private Context context;

    public void hookActivtyMh() {
        try {
            Class<?> classActivityThread = Class.forName("android.app.ActivityThread");
            Field fieldsCurrentActivityThread = classActivityThread.getDeclaredField("sCurrentActivityThread");
            fieldsCurrentActivityThread.setAccessible(true);
            Object objsCurrentActivityThread = fieldsCurrentActivityThread.get(null);

            Field fieldmH = classActivityThread.getDeclaredField("mH");
            fieldmH.setAccessible(true);
            Handler objmH = (Handler) fieldmH.get(objsCurrentActivityThread);
            /**
             * handler 源码中有
             *     public void dispatchMessage(Message msg) {
             *         if (msg.callback != null) {
             *             handleCallback(msg);
             *         } else {
             *             if (mCallback != null) {
             *                 if (mCallback.handleMessage(msg)) {
             *                     return;
             *                 }
             *             }
             *             handleMessage(msg);
             *         }
             *     }
             *  dispatchMessage 方法是handle最先调用的分发方法，里面有个mCallback，这个mCallback是个接口
             *  从代码中看，如果 if (mCallback != null) 成立，则执行接口方法，并且不会执行 handleMessage(msg)
             *  handleMessage(msg) 方法就是mH中重写后处理 msg消息的方法，所以，需要配合使用
             */


            Field fieldmCallback = Handler.class.getDeclaredField("mCallback");
            fieldmCallback.setAccessible(true);
            fieldmCallback.set(objmH, new ActivityJumpCallbcak(objmH));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void hookStartActivity(Context context) {
        this.context = context;
        try {
            Class<?> classActivityManagerNative = Class.forName("android.app.ActivityManagerNative");
            Field fieldgDefault = classActivityManagerNative.getDeclaredField("gDefault");
            fieldgDefault.setAccessible(true);
            Object objgDefault = fieldgDefault.get(null);

            Class<?> classSingleton = Class.forName("android.util.Singleton");
            Field fieldmInstance = classSingleton.getDeclaredField("mInstance");
            fieldmInstance.setAccessible(true);
            Object objmInstance = fieldmInstance.get(objgDefault);

            Class<?> classIActivityManager = Class.forName("android.app.IActivityManager");

            /**
             * 开始动态代理啦：
             * ClassLoader loader,
             * Class<?>[] interfaces,代表要实现的hook对象的特征接口,这个传入目标类之后，
             * 在newProxyInstance返回的代理对象中就自动实现了 目标类中的接口了
             * InvocationHandler invocationHandler：分发方法来被调用，这个分发是什么意思？所有在 代理对象中实现的方法
             * 都会调用invocationHandler 中的involk方法，
             * 比如我调用startActivity方法，那么当我们设置完后代理对象后，starActivity方法就会走我们的InvocationHandler中的
             * invoke方法 来 并传入相应的 参数
             * 返回的objProxy就已经实现了classIActivityManager中的方法
             */
            Object objProxy = Proxy.newProxyInstance(
                    context.getClassLoader(),
                    new Class[]{classIActivityManager},
                    new StartActivityInvokeHandler(objmInstance));

            /**
             * 这一步操作，是反射中的替换，目的是将 gDefault(Singleton对象)中的mIntsance替换成
             * 我们的objProxy，怎么做呢？
             * fieldmInstance ：mInstance的原属性类
             * objgDefault： mInstance所属的那个类的对象
             * objProxy ：动态代理构造出来的类
             */
            fieldmInstance.set(objgDefault, objProxy);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private class ActivityJumpCallbcak implements Handler.Callback {

        private Handler objmH;

        public ActivityJumpCallbcak(Handler objmH) {
            this.objmH = objmH;
        }

        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == 100) {


                Object obj = msg.obj;
                // 查看ActivityThread LAUNCH_ACTIVITY 后得知，
                // msg中的obj对象ActivityClientRecord就含有intent
                // 所以这里我们需要反射intent
                try {
                    // 开始获取最早的intent来进行替换
                    Field fieldintent = obj.getClass().getDeclaredField("intent");
                    fieldintent.setAccessible(true);
                    Intent newIntent = (Intent) fieldintent.get(obj);
                    Intent rawIntent = newIntent.getParcelableExtra("rawIntent");
                    newIntent.setComponent(rawIntent.getComponent());

                    // handleMessage的时候 ActivityClientRecord是宿主的对象，所以这里要将包名进行判断修改
                    Field activityInfoField = obj.getClass().getDeclaredField("activityInfo");
                    activityInfoField.setAccessible(true);
                    ActivityInfo activityInfo = (ActivityInfo) activityInfoField.get(obj);
                    activityInfo.applicationInfo.packageName = rawIntent.getPackage() == null ? rawIntent.getComponent().getPackageName()
                            : rawIntent.getPackage();
                    hookPackgeManager();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
            objmH.handleMessage(msg);
            return false;
        }
    }

    private void hookPackgeManager() {
//          hook   方法  IPackageManager.getPackgeInfo

        // 这一步是因为 initializeJavaContextClassLoader 这个方法内部无意中检查了这个包是否在系统安装
        // 如果没有安装, 直接抛出异常, 这里需要临时Hook掉 PMS, 绕过这个检查.

        Class<?> activityThreadClass = null;
        try {
            activityThreadClass = Class.forName("android.app.ActivityThread");
            Method currentActivityThreadMethod = activityThreadClass.getDeclaredMethod("currentActivityThread");
            currentActivityThreadMethod.setAccessible(true);
            Object currentActivityThread = currentActivityThreadMethod.invoke(null);

            // 获取ActivityThread里面原始的 sPackageManager
            Field sPackageManagerField = activityThreadClass.getDeclaredField("sPackageManager");
            sPackageManagerField.setAccessible(true);
            Object sPackageManager = sPackageManagerField.get(currentActivityThread);

            // 准备好代理对象, 用来替换原始的对象
            Class<?> iPackageManagerInterface = Class.forName("android.content.pm.IPackageManager");
            Object proxy = Proxy.newProxyInstance(iPackageManagerInterface.getClassLoader()
                    , new Class[]{iPackageManagerInterface}, new IPackageManagerHandler(sPackageManager));
            sPackageManagerField.set(currentActivityThread, proxy);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private class StartActivityInvokeHandler implements InvocationHandler {

        private Object objmInstance;

        public StartActivityInvokeHandler(Object objmInstance) {
            this.objmInstance = objmInstance;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            Log.i(TAG, "====" + method.getName());

            Intent rawIntent = null;
            int index = 0;
            if ("startActivity".equals(method.getName())) {
                Log.i(TAG, "========" + method.getName() + "========");
                for (int i = 0; i < args.length; i++) {
                    Object arg = args[i];
                    if (arg instanceof Intent) {
                        rawIntent = (Intent) arg;
                        index = i;
                    }
                }
                Intent packageIntent = new Intent();
                packageIntent.setComponent(new ComponentName(context, ProxyActivity.class));
                packageIntent.putExtra("rawIntent", rawIntent);
                args[index] = packageIntent;
            }
            return method.invoke(objmInstance, args);
        }
    }

    public void putLoadedApk(String path) {
        try {
            // 1.还原activityThread对象
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Method currentActivityThreadMethod = activityThreadClass.getDeclaredMethod("currentActivityThread");
            currentActivityThreadMethod.setAccessible(true);
            Object currentActivityThread = currentActivityThreadMethod.invoke(null);

            // 2.还原mPackages对象
            // 获取到 mPackages 这个静态成员变量, 这里缓存了apk包的信息
            Field mPackagesField = activityThreadClass.getDeclaredField("mPackages");
            mPackagesField.setAccessible(true);
            Map mPackages = (Map) mPackagesField.get(currentActivityThread);

//z找到  getPackageInfoNoCheck   method 方法
            Class<?> compatibilityInfoClass = Class.forName("android.content.res.CompatibilityInfo");
            Method getPackageInfoNoCheckMethod = activityThreadClass.getDeclaredMethod(
                    "getPackageInfoNoCheck", ApplicationInfo.class, compatibilityInfoClass);

//        得到 CompatibilityInfo  里面的  静态成员变量       DEFAULT_COMPATIBILITY_INFO  类型  CompatibilityInfo
            Field defaultCompatibilityInfoField = compatibilityInfoClass.getDeclaredField("DEFAULT_COMPATIBILITY_INFO");
            Object defaultCompatibilityInfo = defaultCompatibilityInfoField.get(null);

            ApplicationInfo applicationInfo = parseApplicationInfo(context, path);

//            一个问题  传参 ApplicationInfo ai 一定是与插件相关    ApplicationInfo----》插件apk文件
//LoadedApk getPackageInfoNoCheck(ApplicationInfo ai, CompatibilityInfo compatInfo)
            Object loadedApk = getPackageInfoNoCheckMethod.invoke(currentActivityThread, applicationInfo, defaultCompatibilityInfo);


            String odexPath = Utils.getPluginOptDexDir(applicationInfo.packageName).getPath();
            String libDir = Utils.getPluginLibDir(applicationInfo.packageName).getPath();

            ClassLoader classLoader = new CustomClassLoader(path, odexPath, libDir, context.getClassLoader());
            Field mClassLoaderField = loadedApk.getClass().getDeclaredField("mClassLoader");
            mClassLoaderField.setAccessible(true);
            mClassLoaderField.set(loadedApk, classLoader);
            WeakReference weakReference = new WeakReference(loadedApk);

//     最终目的  是要替换ClassLoader  不是替换LoaderApk
            mPackages.put(applicationInfo.packageName, weakReference);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private ApplicationInfo parseApplicationInfo(Context context, String path) {
        try {
            Class packageParserClass = Class.forName("android.content.pm.PackageParser");
            Method parsePackageMethod = packageParserClass.getDeclaredMethod("parsePackage", File.class, int.class);
            Object packageParser = packageParserClass.newInstance();
            Object packageObj = parsePackageMethod.invoke(packageParser, new File(path), PackageManager.GET_ACTIVITIES);

            Class<?> packageUserStateClass = Class.forName("android.content.pm.PackageUserState");
                Object defaltUserState = packageUserStateClass.newInstance();
//目的     generateApplicationInfo  方法  生成  ApplicationInfo
            // 需要调用 android.content.pm.PackageParser#generateActivityInfo(android.content.pm.ActivityInfo, int, android.content.pm.PackageUserState, int)
            //      generateApplicationInfo
            Method generateApplicationInfoMethod = packageParserClass.getDeclaredMethod("generateApplicationInfo",
                    packageObj.getClass(),
                    int.class,
                    packageUserStateClass);
            ApplicationInfo applicationInfo = (ApplicationInfo) generateApplicationInfoMethod.invoke(packageParser, packageObj, 0, defaltUserState);

            applicationInfo.sourceDir = path;
            applicationInfo.publicSourceDir = path;
            return applicationInfo;
            //generateActivityInfo
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }
}
