package com.ll.springFramework;

import com.ll.springFramework.anotation.*;
import com.ll.springFramework.aware.ApplicationContextAware;
import com.ll.springFramework.aware.BeanNameAware;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.beans.Introspector;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyApplicationContext {

    /**
     * 配置类
     */
    private Class configClass;

    /**
     * BeanDefinition缓存
     */
    private Map<String, BeanDefinition> beanDefinitionMap = new HashMap<>();

    /**
     * 非懒加载的单例Bean缓存
     */
    private Map<String, Object> singletonObjects = new HashMap<>();

    public MyApplicationContext(Class configClass) {
        this.configClass = configClass;

        // 扫描配置类指定的包路径，加载@Component注解标注的bean，生成对应的BeanDefinition对象并缓存到map中
        scan(configClass);

        // 扫描后，根据BeanDefinitionMap创建非懒加载的单例Bean实例，并缓存到singletonObjects
        // 不是单例Bean的不需要创建
        for (String beanName : beanDefinitionMap.keySet()) {
            BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
            // 是否为单例Bean
            if ("singleton".equals(beanDefinition.getScope()) && !beanDefinition.isLazy()) {
                Object bean = createBean(beanName, beanDefinition);
                singletonObjects.put(beanName, bean);
            }
        }
    }

    /**
     * 扫描配置类指定的包路径，加载@Component注解标注的bean，生成对应的BeanDefinition对象并缓存到map中
     * @param configClass 配置类
     */
    private void scan(Class configClass) {
        // 1. 判断配置类是否有ComponentScan注解
        if (configClass.isAnnotationPresent(ComponentScan.class)) {
            // 2. 根据配置的扫描路径，通过类加载器加载扫描路径下的.class文件
            ComponentScan componentScanAnnotation = (ComponentScan) configClass.getAnnotation(ComponentScan.class);
            String path = componentScanAnnotation.value();
            // 最终的目标是拿到.class文件，所以需要把.替换为/
            path = path.replace('.', '/');

            // 利用类加载器加载.class文件，这里传入的是相对路径，相对的就是target文件夹
            ClassLoader classLoader = getClass().getClassLoader();
            URL resource = classLoader.getResource(path);
            File file = new File(resource.getFile());
            // System.out.println(file.getAbsolutePath());

            // 扫默路径配置的是具体的包，即一个文件夹。加载文件夹下的.class文件
            List<File> classFiles = new ArrayList<>();
            if (file.isDirectory()) {
                for (File classFile : file.listFiles()) {
                    if (!classFile.isDirectory()) {
                        classFiles.add(classFile);
                    }
                }
            }

            // 3. 遍历扫描的.class文件，生成对应的BeanDefinition对象并缓存到map中
            for (File classFile : classFiles) {
                // 拿到文件的绝对路径，取出类的全限定类名。截取+替换
                String absolutePath = classFile.getAbsolutePath();
                String className = absolutePath.substring(absolutePath.indexOf("com"), absolutePath.indexOf(".class"))
                        .replace("\\", ".");
                // System.out.println(className);

                try {
                    Class<?> clazz = classLoader.loadClass(className);

                    // 判断类是否有@Component
                    if (clazz.isAnnotationPresent(Component.class)) {
                        BeanDefinition beanDefinition = new BeanDefinition();
                        beanDefinition.setType(clazz);
                        beanDefinition.setLazy(clazz.isAnnotationPresent(Lazy.class));
                        // 如果没有配置scope为prototype，默认该bean为单例
                        if (clazz.isAnnotationPresent(Scope.class)) {
                            beanDefinition.setScope(clazz.getAnnotation(Scope.class).value());
                        } else {
                            beanDefinition.setScope("singleton");
                        }

                        // 缓存beanDefinition到map中
                        String beanName = clazz.getAnnotation(Component.class).value();
                        // 如果没有指定beanName，自动生成一个驼峰式的beanName
                        if (beanName.isEmpty()) {
                            beanName = Introspector.decapitalize(clazz.getSimpleName());
                        }
                        beanDefinitionMap.put(beanName, beanDefinition);
                    }

                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * 根据beanName和beanDefinition创建bean实例对象
     * @param beanName
     * @param beanDefinition
     * @return
     */
    private Object createBean(String beanName, BeanDefinition beanDefinition) {

        Class clazz = beanDefinition.getType();

        try {
            Object o = clazz.newInstance();

            // 属性赋值，对依赖注入的处理
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(Autowired.class)) {
                    Object bean = getBean(field.getName());
                    field.setAccessible(true);
                    field.set(o, bean);
                }
            }

            // beanName回调，如果bean实现这个接口，就可以获取到beanName
            if (o instanceof BeanNameAware) {
                ((BeanNameAware)o).setBeanName(beanName);
            }

            // ApplicationContext回调，如果bean实现这个接口，就可以获取到ApplicationContext
            if (o instanceof ApplicationContextAware) {
                ((ApplicationContextAware)o).setApplicationContext(this);
            }

            // AOP，对@Transactional注解的处理，此处利用CGLIB生成代理对象
            if (clazz.isAnnotationPresent(Transactional.class)) {
                Enhancer enhancer = new Enhancer();
                enhancer.setSuperclass(clazz);
                Object target = o;
                enhancer.setCallback(new MethodInterceptor() {
                    @Override
                    public Object intercept(Object proxy, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
                        System.out.println("开启事务");
                        Object result = method.invoke(target, objects);
                        System.out.println("提交事务");
                        return result;
                    }
                });
                o = enhancer.create();
            }

            return o;

        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据beanName获取bean实例对象
     * @param beanName
     * @return
     */
    public Object getBean(String beanName) {
        BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
        if (beanDefinition == null) {
            throw new RuntimeException("未定义bean");
        }

        // 如果是单例bean直接从缓存中取出
        if ("singleton".equals(beanDefinition.getScope())) {
            Object result = singletonObjects.get(beanName);

            // 可能为空，重新生成单例bean放到缓存中
            // A类有属性B类，A生成对象时需要依赖注入B的对象，但是此时B对象还未生成
            if (result == null) {
                result = createBean(beanName, beanDefinition);
                singletonObjects.put(beanName, result);
            }

            return result;
        } else {
            return createBean(beanName, beanDefinition);
        }
    }
}
