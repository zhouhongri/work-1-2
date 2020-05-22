# spring framework学习笔记

1.spring核心思想

ioc：控制反转，解决了对象的耦合问题

aop：面向切面编程，处理的是横切逻辑，从根本上解决了耦合问题 切面=切入点+增强=切人点+横切信息+方位信息（目的就是在那个地方插入什么横切代码）

2.jdk动态代理和cglib动态代理区别

jdk动态代理委托对象实现类需要完成接口、cglib不需要实现接口（cglib需要引入jar依赖包）

3.spring基础

Beans.xml:定义需要实例化的类的权限定类名以及类之间依赖关系的描述

BeanFactory:通过反射技术来实例化对象并维护对象之间的依赖关系（IOC容器）

Spring框架的IOC实现：

1.纯xml方式（bean的信息全部配置到xml中）容器启动方式 JavaSE应用ApplicationContext applicationContext = new ClassPathXmlApplicationContext("beans.xml")   JavaWeb应用ContextLoaderListener(监听器加载xml)

2.xml+注解（部分bean使用xml定义(第三方依赖)，部分bean使用注解定义）；

3.纯注解方式（bean的信息全部使用注解定义）容器启动方式 JavaSE应用ApplicationContext applicationContext = new AnnotationConfiglApplicationContext(springConfig.class)   JavaWeb应用ContextLoaderListener(监听器加载注解配置)

4.Spring ioc实例化Bean的三种方式

方式一：使用无参构造器（推荐）

方式二：静态方法 factory-method

方式三：实例化方法 factory-bean factory-method

5.bean的作用范围

```
singleton：单例，IOC容器中只有一个该类对象，默认为singleton
prototype：原型(多例)，每次使用该类的对象（getBean），都返回给你一个新的对象，Spring只创建对象，不管理对象
```

6.@Autowired 自动装配，按照类型注入，如果按照类型无法锁定唯一对象(一个接口有多个实现类)，可以结合@Qualifier指定具体id

7.lazt-init：配置bean对象的延迟加载，true或者false，默认为false（立即加载）

应用场景：

1）.开启延迟加载可以一定提高容器启动和运转新能

2）.对于不常使用的bean设置延迟加载

8.BeanFactory和FacrotyBean

BeanFactory接口是容器的顶级夫类，定义了容器的一些基础行为，负责生产和管理Bean的一个工厂，具体使用它下面的子接口，比如：ApplicationContext

Spring中的Bean有两种：普通bean、工厂bean（FactoryBean），FactoryBean可以生成某一个类型的Bean实例，类似于自定义bean的创建（复杂bean的创建）

Object company = applicationContext.getBean("companyBean");// 获取复杂对象

Object companyBean = applicationContext.getBean("&companyBean");// 获取Bean工厂对象

9.后置处理器

两种后处理bean的扩展接口，分别是BeanPostProcessor和BeanFactoryPostProcessor

工厂初始化（BeanFactory）-> 实例化bean对象

beanfactory -> BeanFactoryPostProcessor后置处理

bean对象实例化（并不是bean的整个生命周期结束） -> BeanPostProcessor后置处理

springbean一定是对象，但对象（反射技术生成）不一定是springbean（需要走完整个springbean的创建流程）

springbean的生命周期：1.实例化bean->2.设置属性->3.调用beannameAware的setbeanname方法->4.调用beanfactoryAware的setbeanfactory方法->5.调用applicationContextAware的setapplicationContext方法->6.调用BeanPostProcessor的预处理初始化方法->7.调用inititalzingBean的afterpropertiesSet方法->8.调用定制的初始化方法init-method(@postconstruct)->9.调用BeanPostProcessor的后初始化方法->10.判断是否是单例模式（singleton）存储在spring的缓冲池（map）->11.bean的销毁 调用disposableBean的destory方法->12.调用destory-method（@predestory）的属性配置的销毁方法
Beandefinition存储的配置文件的属性信息

Spring

简介：是以ioc和aop为内核的全栈式的轻量级框架

优势：解耦、aop编程支持、声明式事务、对测试的支持、方便集成各种优秀框架、对javaee api的封装、优秀的源码

核心结构：模块化思想的体现

事务基础：

四大特性：原子性、一致性、隔离性、持久性

并发问题（脏读等，隔离级别解决）、隔离级别、传播行为（A和B相互依赖、提交事务使用谁的事物）

