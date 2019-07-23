# 手写框架汇总
## 手写MyBatis
### 1. MyBatis框架流程介绍

![image](https://upload-images.jianshu.io/upload_images/15462057-52cc0116f630d5c9?imageMogr2/auto-orient/strip%7CimageView2/2/w/640/format/webp)
MyBatis的源码中使用了大量的设计模式，可参考如下网址

[MyBatis源码解读](http://www.crazyant.net/2022.html)
### 2. MyBatis的核心部件：
1. config.xml，mybatis的全局配置文件，用于配置数据源等，全局只有一个该文件
2. mapper.xml，包含多个statement，对应多个sql语句，mybatis中可以有多个
3. configuration，配置类，解析全局配置文件并生成对应的sqlSessionFactory
4. sqlSessionFactory，会话工厂，创建sqlSession
5. sqlSession，会话，提供给调用者的接口，包含CRUD等功能
6. Executor，执行器，包含基本执行器和缓存执行器，通过该类对数据库进行操作
7. mapperStatement是通过mapper.xml中定义statement生成的对象，自动将结果集映射为java对象
 
### 3. 手写mybatis设计思路：
根据主要部件的分析，设计思路如下：
![image](http://assets.processon.com/chart_image/5d3564b1e4b0b3e4dccddb92.png)
1. 创建configuration，读取xml配置文件，建立连接，可以采用dom4j进行xml解析
2. 创建sqlSession，类似于前端的一个request请求，可以直接执行sql语句。而内部通过executor和configuration进行集中调配，对应每个不同的mapper接口，通过getMapper和动态代理去获取它的对象进行方法调用，进行数据库的操作
3. 创建executor，封装jdbc数据库操作。执行器executor负责sql语句的生成和查询缓存的维护
4. 创建MapperProxy，通过动态代理生成mapper接口的对象。从而使用接口中的方法（sql方法），因为接口无法执行调用方法，故使用动态代理获取其对象。在执行时回到sqlSession去调用该查询，通过jdbc进行数据库的操作。

### 4. 实现自己的mybatis
1. 创建自己的项目，maven中导入dom4j, lombok和mysql连接的依赖

```
<dependencies>
        <!--dom4j处理xml文件-->
        <dependency>
            <groupId>dom4j</groupId>
            <artifactId>dom4j</artifactId>
            <version>1.6.1</version>
        </dependency>
        <!--mysql连接器处理jdbc连接-->
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>5.1.46</version>
        </dependency>
        <!--lombok-->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>1.18.8</version>
        </dependency>
    </dependencies>
```

2. 创建mybatis的全局配置文件config.xml

```
<?xml version="1.0" encoding="utf-8" ?>
<database>
    <property name="driverClassName">com.mysql.jdbc.Driver</property>
    <property name="url">jdbc:mysql://localhost:3306/test?useUnicode=true&amp;characterEncoding=utf8</property>
    <property name="username">root</property>
    <property name="password">123456</property>
</database>
```

3. 创建test库，执行sql语句

```
CREATE TABLE `user` (
 `id` varchar(64) NOT NULL,
 `password` varchar(255) DEFAULT NULL,
 `username` varchar(255) DEFAULT NULL,
 PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;
INSERT INTO `test`.`user` (`id`, `password`, `username`) VALUES ('1', '123456', 'jmx');
```

4. 创建User实体类，和对应的UserMapper及UserMapper.xml文件
```
@Data
public class User {
    private String id;
    private String username;
    private String password;
}
```
```
public interface UserMapper {
    //根据id查找User
    User getUserById(String id);
}
```

```
<?xml version="1.0" encoding="UTF-8"?>
<mapper nameSpace="com.jmx.mapper.UserMapper">
    <select id="getUserById" resultType ="com.jmx.pojo.User">
        select * from user where id = ?
    </select>
</mapper>
```

5. 配置MyConfiguration，解析全局配置和mapper的xml文件，需要定义MapperBean和Function接收Mapper和statement中的方法

```
/**
 * 用于读取mapper，获取mapper对应的接口及statement语句
 */
@Data
public class MapperBean {
    //接口名
    private String interfaceName;
    //接口下所有方法
    private List<Function> list;
}
```

```
/**
 * Function对象包括sql的类型、方法名、sql语句、返回类型和参数类型。
 * 接收xml中的statement
 */
@Data
public class Function {
    private String sqlType;
    private String funcName;
    private String sql;       
    private Object resultType;  
    private String parameterType;
}
```

```
/**
 * 读取与解析xml配置信息，并返回处理后的Connection
 * 1. 解析全局配置xml文件
 * 2. 解析mapper.xml文件
 */
public class MyConfiguration {
    //获取系统类加载器
    public static ClassLoader loader = ClassLoader.getSystemClassLoader();

    /**
     * 传入流，利用dom4j读取xml信息并处理，建立database连接
     */
    public Connection build(String resource){
        try {
            //通过resource建立处理流
            InputStream stream = loader.getResourceAsStream(resource);
            //利用dom4j读取xml配置，传入流
            SAXReader reader = new SAXReader();
            Document document = reader.read(stream);
            //获取根元素,根元素为database
            Element root = document.getRootElement();
            return evalDatasource(root);
        } catch (DocumentException e) {
            throw new RuntimeException("error occured while evaling xml " + resource);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 转化数据源参数
     * @param node  dom4j解析到的xml节点
     * @return Connection
     * @throws ClassNotFoundException
     */
    private Connection evalDatasource(Element node) throws ClassNotFoundException{
        if(!node.getName().equals("database")){
            throw new RuntimeException("root should be <database>");
        }
        String driverClassName = null;
        String url = null;
        String username = null;
        String password = null;
        //获取属性节点
        for (Object item : node.elements("property")) {
            Element i = (Element) item;
            //获取property属性的值
            String value = getValue(i);
            //获取name标签里的名字
            String name = i.attributeValue("name");
            if (name == null || value == null) {
                throw new RuntimeException("[database]: <property> should contain name and value");
            }
            //赋值给定义的变量
            switch (name){
                case "url": url = value;break;
                case "driverClassName": driverClassName = value;break;
                case "username": username = value;break;
                case "password": password = value;break;
            }
        }
        //建立数据库连接
        Class.forName(driverClassName);
        Connection connection = null;
        try{
            //建立数据库链接
            connection = DriverManager.getConnection(url, username, password);
        }catch (SQLException e){
            e.printStackTrace();
        }
        return connection;
    }

    //获取property属性的值,如果有value值,则读取；没有设置value,则读取内容
    private  String getValue(Element node) {
        return node.hasContent() ? node.getText() : node.attributeValue("value");
    }

    /**
     * 读取mapper.xml，映射到对应的mapperBean
     * @param path mapper.xml文件
     * @return MapperBean
     */
    @SuppressWarnings("rawtypes")
    public  MapperBean readMapper(String path){
        MapperBean mapper = new MapperBean();
        try{
            InputStream stream = loader.getResourceAsStream(path);
            SAXReader reader = new SAXReader();
            Document document = reader.read(stream);
            Element root = document.getRootElement();
            //把mapper节点的nameSpace值存为接口名
            mapper.setInterfaceName(root.attributeValue("nameSpace").trim());
            //用来存储方法的List
            List<Function> list = new ArrayList<Function>();
            //遍历根节点下所有子节点
            for(Iterator rootIter = root.elementIterator(); rootIter.hasNext();) {
                //用来存储mapper.xml每一条方法的信息
                Function fun = new Function();
                Element e = (Element) rootIter.next();
                String sqlType = e.getName().trim();
                String funcName = e.attributeValue("id").trim();
                String sql = e.getText().trim();
                String resultType = e.attributeValue("resultType").trim();
                fun.setSqlType(sqlType);
                fun.setFuncName(funcName);
                Object newInstance=null;
                try {
                    newInstance = Class.forName(resultType).newInstance();
                } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e1) {
                    e1.printStackTrace();
                }
                fun.setResultType(newInstance);
                fun.setSql(sql);
                //将方法加入到list中
                list.add(fun);
            }
            mapper.setList(list);
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        return mapper;
    }
}

```

6. 配置MySqlSession

```
/**
 * 实现我的sqlSession，成员变量里有Executor和MyConfiguration
 * 通过sqlSession获取mapper接口的动态代理对象，并调用其方法执行
 */
public class MySqlSession {
    private Executor executor = new MyExecutor();
    private MyConfiguration myConfiguration = new MyConfiguration();

    public <T>T selectOne(String statement, Object parameter){
        return executor.query(statement,parameter);
    }

    /**
     * 根据相应接口声明的信息，通过动态代理生成对应的mapper实例
     * @param clas 代理的接口，为mapper接口
     * @param <T> 返回对应的对象
     * @return 具有proxy代理类的指定调用处理程序的代理实例，该代理实例由指定的类加载器定义并实现指定的接口
     */
    @SuppressWarnings("unchecked")
    public <T> T getMapper(Class<T> clas){
        //动态代理调用,,返回指定接口的代理类的实例，该接口将方法调用分派给指定的调用处理程序。
        return (T) Proxy.newProxyInstance(clas.getClassLoader(),new Class[]{clas},
                new MyMapperProxy(myConfiguration,this));
    }
}
```
7. 配置MyExecutor，封装jdbc相关的操作

```
/**
 * Executor接口，定义CRUD语句，此处只增加查询的功能
 */
public interface Executor {
    <T>T query(String statement, Object parameter);
}
```

```
/**
 * 封装JDBC的操作，进行数据库的CRUD
 * 1.建立连接
 * 2.进行jdbc操作
 */
public class MyExecutor implements Executor {

    private MyConfiguration xmlConfiguration = new MyConfiguration();

    /**
     * 执行查询方法
     * @param statement 查询语句，即sql语句
     * @param parameter 参数，即？代表的参数
     * @param <T> 返回值泛型，传递什么类型，返回对应的类型
     * @return 返回T
     */
    @Override
    public <T>T query(String statement, Object parameter) {
        Connection connection = getConnection();
        ResultSet set = null;
        PreparedStatement pre = null;
        if (connection != null) {
            try {
                pre = connection.prepareStatement(statement);
                //设置参数
                pre.setString(1,parameter.toString());
                //执行查询获取结果
                set = pre.executeQuery();
                //返回User对象
                User user = new User();
                //遍历结果集
                while(set.next()){
                    user.setId(set.getString(1));
                    user.setUsername(set.getString(2));
                    user.setPassword(set.getString(3));
                }
                T t = (T) user;
                return t;
            } catch (SQLException e) {
                e.printStackTrace();
            }finally{
                //关闭相应连接
                try{
                    if(set!=null){
                        set.close();
                    }if(pre!=null){
                        pre.close();
                    }
                    connection.close();
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * 通过解析xml全局配置文件，建立数据库连接
     * @return Connection
     */
    private Connection getConnection() {
        try {
            return xmlConfiguration.build("config.xml");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
```

8. 配置MyMapperProxy，完成xml方法和真实方法对应
```
/**
 * 我的mapper的代理类，实现动态代理
 * 完成xml方法和真实方法对应，执行查询
 * 每个代理实例都有一个关联的调用处理程序。
 * 在代理实例上调用方法时，方法调用将被编码并调度到其调用处理程序的invoke方法。
 */

public class MyMapperProxy implements InvocationHandler {

    private MyConfiguration myConfiguration;
    private MySqlSession mySqlSession;

    public MyMapperProxy(MyConfiguration myConfiguration, MySqlSession mySqlSession) {
        this.myConfiguration = myConfiguration;
        this.mySqlSession = mySqlSession;
    }

    /**
     * 当代理调用方法时，会调用invoke方法
     * @param proxy 代理对象
     * @param method 代理的方法
     * @param args 代理方法的参数
     * @return 返回结果
     * @throws Throwable
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        MapperBean mapperBean = myConfiguration.readMapper("UserMapper.xml");
        //是否是xml文件对应的接口
        if(!method.getDeclaringClass().getName().equals(mapperBean.getInterfaceName())){
            return null;
        }
        List<Function> list = mapperBean.getList();
        if(null != list || 0 != list.size()){
            for (Function function : list) {
                //id是否和接口方法名一样
                if(method.getName().equals(function.getFuncName())){
                    return mySqlSession.selectOne(function.getSql(), String.valueOf(args[0]));
                }
            }
        }
        return null;
    }
}
```

9. 执行测试
```
/**
 * 测试mybatis的查询是否正常
 */
public class TestMybatis {
    public static void main(String[] args) {  
        MySqlSession sqlSession=new MySqlSession();
        //获取mapper
        UserMapper mapper = sqlSession.getMapper(UserMapper.class);
        User user = mapper.getUserById("1");
        User user2 = mapper.getUserById("2");
        System.out.println(user);
        System.out.println(user2);
    } 
}
```
输出结果如下：
```
User(id=1, username=123456, password=jmx)
User(id=null, username=null, password=null)
```
至此手写MyBatis完成。
