package org.alie.aliepluginhookmixdex2;

import dalvik.system.DexClassLoader;

/**
 * Created by Alie on 2019/10/18.
 * 类描述  创建一个classLoader 用于替换
 * 版本
 */
public class CustomClassLoader extends DexClassLoader {
    /**
     *
     * @param dexPath  apk的path路径即可
     * @param optimizedDirectory 之前我们这里传递的是Context.getCodeCacheDir() 这只是Api26之前要求的
     *                           由于真正的业务开发中我们可能是多个插件，那么为了方便管理，我们可以这样自定义路径：
     *                           odex这样存储：data/data/plugin/插件包名/odex
     *                           lib库这样存储：data/data/plugin/插件包名/lib
     * @param libraryPath
     * @param parent
     */
    public CustomClassLoader(String dexPath, String optimizedDirectory, String libraryPath, ClassLoader parent) {
        super(dexPath, optimizedDirectory, libraryPath, parent);
    }
}
