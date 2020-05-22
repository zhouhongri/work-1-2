package com.lagou.edu.factory;

import com.lagou.edu.annotation.Autowire;
import com.lagou.edu.annotation.Service;
import com.lagou.edu.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class AnnotationFactory {

    private static Map<String, Object> map = new LinkedHashMap<>();

    private static AnnotationFactory annotationFactory = new AnnotationFactory();

    private static Set<String> proxySet = new HashSet<>();

    /**
     * 扫描包路径下所有的class文件
     *
     * @param pkg
     * @return
     */
    private static void getClzFromPkg(String pkg) throws Exception {
        String pkgDirName = pkg.replace('.', '/');
        try {
            Enumeration<URL> urls = Thread.currentThread().getContextClassLoader().getResources(pkgDirName);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                String protocol = url.getProtocol();
                if ("file".equals(protocol)) {// 如果是以文件的形式保存在服务器上
                    String filePath = URLDecoder.decode(url.getFile(), "UTF-8");// 获取包的物理路径
                    findClassesByFile(pkg, filePath);
                } else if ("jar".equals(protocol)) {// 如果是jar包文件
                    JarFile jar = ((JarURLConnection) url.openConnection()).getJarFile();
                    findClassesByJar(pkg, jar);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 扫描包路径下的所有class文件
     *
     * @param pkgName 包名
     * @param pkgPath 包对应的绝对地址
     */
    private static void findClassesByFile(String pkgName, String pkgPath) throws Exception {
        File dir = new File(pkgPath);
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }


        // 过滤获取目录，or class文件
        File[] dirfiles = dir.listFiles(pathname -> pathname.isDirectory() || pathname.getName().endsWith("class"));


        if (dirfiles == null || dirfiles.length == 0) {
            return;
        }


        String className;
        Class clz;
        for (File f : dirfiles) {
            if (f.isDirectory()) {
                findClassesByFile(pkgName + "." + f.getName(),
                        pkgPath + "/" + f.getName());
                continue;
            }


            // 获取类名，干掉 ".class" 后缀
            className = f.getName();
            className = className.substring(0, className.length() - 6);

            // 加载类
            clz = loadClass(pkgName + "." + className);
            putMap(clz, className);
        }
    }

    /**
     * 扫描包路径下的所有class文件
     *
     * @param pkgName 包名
     * @param jar     jar文件
     */
    private static void findClassesByJar(String pkgName, JarFile jar) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        String pkgDir = pkgName.replace(".", "/");


        Enumeration<JarEntry> entry = jar.entries();

        JarEntry jarEntry;
        String name, className;
        Class<?> claze;
        while (entry.hasMoreElements()) {
            jarEntry = entry.nextElement();

            name = jarEntry.getName();
            if (name.charAt(0) == '/') {
                name = name.substring(1);
            }


            if (jarEntry.isDirectory() || !name.startsWith(pkgDir) || !name.endsWith(".class")) {
                // 非指定包路径， 非class文件
                continue;
            }


            // 去掉后面的".class", 将路径转为package格式
            className = name.substring(0, name.length() - 6);
            claze = loadClass(className.replace("/", "."));
            putMap(claze, className);
        }
    }

    /**
     * 将service注解的类对象放到map中
     *
     * @param claze
     * @param className
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    private static void putMap(Class<?> claze, String className) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        if (claze != null) {
            if (claze.isAnnotationPresent(Service.class)) {

                String beanId = className.substring(0, 1).toLowerCase() + className.substring(1);
                Set<String> set = new HashSet<>();
                Class[] interfaces = claze.getInterfaces();
                if (interfaces != null && interfaces.length > 0) {
                    for (Class anInterface : interfaces) {
                        String interfaceName = anInterface.getName().replace(anInterface.getPackageName(), "");
                        className = interfaceName.substring(1);
                        set.add(className);
                    }
                }
                Service service = claze.getAnnotation(Service.class);
                String tempId = service.value();
                if (tempId != null && tempId.length() > 0) {
                    beanId = tempId;
                    putFinalMap(beanId, claze);
                } else {
                    if (set != null && !set.isEmpty()) {
                        for (String s : set) {
                            beanId = s.substring(0, 1).toLowerCase() + s.substring(1);
                            putFinalMap(beanId, claze);
                        }
                    } else {
                        putFinalMap(beanId, claze);
                    }
                }

            }
        }
    }

    /**
     * 判断类方法是否有Transactional注解，如果存在此注解将其对象更新为代理对象
     *
     * @param beanId
     * @param claze
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    private static void putFinalMap(String beanId, Class<?> claze) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        map.put(beanId, Class.forName(claze.getName()).newInstance());
        Method[] methods = claze.getMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(Transactional.class)) {
                proxySet.add(beanId);
            }
        }
    }


    /**
     * 类加载器
     *
     * @param fullClzName
     * @return
     */
    private static Class<?> loadClass(String fullClzName) {
        try {
            return Thread.currentThread().getContextClassLoader().loadClass(fullClzName);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 注入初始化
     *
     * @throws IllegalAccessException
     */
    private static void initEntryField() throws IllegalAccessException {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            attrAssign(entry.getValue());
        }
    }

    /**
     * 对存在Autowire注解的属性进行赋值
     *
     * @param object
     * @throws IllegalAccessException
     */
    private static void attrAssign(Object object) throws IllegalAccessException {
        Field[] declaredFields = object.getClass().getDeclaredFields();
        for (Field declaredField : declaredFields) {
            if (declaredField.isAnnotationPresent(Autowire.class)) {
                String beanId = declaredField.getName();
                Object bean = map.get(beanId);
                if (bean != null) {
                    declaredField.setAccessible(true);
                    declaredField.set(object, bean);
                }
            }
        }
    }

    /**
     * 调用代理工厂
     *
     * @param set
     */
    private static void setProxyObject(Set<String> set) {
        ProxyFactory proxyFactory = (ProxyFactory) map.get("proxyFactory");
        for (String beanId : set) {
            Object object = proxyFactory.getJdkProxy(map.get(beanId));
            map.put(beanId, object);
        }
    }

    /**
     * 单例工厂
     */
    private AnnotationFactory() {

    }

    /**
     * 获取工厂对象
     *
     * @return
     */
    public static AnnotationFactory getInstance() {
        return annotationFactory;
    }

    /**
     * 初始方法
     *
     * @param pkgs
     */
    public void annotationFactoryContext(String[] pkgs) {
        try {
            for (String pkg : pkgs) {
                getClzFromPkg(pkg);
            }
            initEntryField();
            setProxyObject(proxySet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据beanId获取map中存在的对象
     *
     * @param beanId
     * @return
     */
    public Object getBean(String beanId) {
        return map.get(beanId);
    }

    /**
     * 静态块程序入口
     */
    static {
        String[] pkgs = {"com.lagou.edu.service.impl", "com.lagou.edu.dao.impl", "com.lagou.edu.utils", "com.lagou.edu.factory"};
        AnnotationFactory.getInstance().annotationFactoryContext(pkgs);
    }
}
