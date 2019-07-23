package com.jmx.config;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
