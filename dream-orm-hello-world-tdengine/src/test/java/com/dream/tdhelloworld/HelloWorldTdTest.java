package com.dream.tdhelloworld;

import com.dream.system.config.Page;
import com.dream.tdengine.mapper.FlexTdChainMapper;
import com.dream.tdengine.mapper.FlexTdMapper;
import com.dream.tdhelloworld.table.Meters;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.dream.flex.def.FunctionDef.*;
import static com.dream.tdengine.def.TdFunctionDef.first;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TdHelloWorldApplication.class)
public class HelloWorldTdTest {
    @Autowired
    private FlexTdMapper flexTdMapper;

    /**
     * 数据切分查询
     *
     * @throws Exception
     */
    @Test
    public void testPartitionBy() {
        List<Map> list = flexTdMapper.select(max(col("current"))).from("meters").partitionBy("location").interval("10m").limit(1, 2).list(Map.class);
        System.out.println("查询结果：" + list);
    }

    /**
     * 时间窗口查询
     *
     * @throws Exception
     */
    @Test
    public void testSliding() {
        List<Map> list = flexTdMapper.select(max(col("current"))).from("meters").partitionBy("location").interval("10m").sliding("5m").limit(1, 2).list(Map.class);
        System.out.println("查询结果：" + list);
    }

    /**
     * 测试状态窗口
     *
     * @throws Exception
     */
    @Test
    public void testState() {
        List<Map> list = flexTdMapper.select("voltage").from("meters").state_window("voltage").limit(1, 2).list(Map.class);
        System.out.println("查询结果：" + list);
    }

    /**
     * 测试会话窗口
     *
     * @throws Exception
     */
    @Test
    public void testSession() {
        List<Map> list = flexTdMapper.select(col("voltage"), first("ts")).from("meters").session("ts", "10s").limit(1, 2).list(Map.class);
        System.out.println("查询结果：" + list);
    }

    /**
     * 测试插入
     */
    @Test
    public void testInsert() {
        flexTdMapper.insertInto("d1001").values(new Date(), 10.2, 219, 0.32).execute();
    }

    /**
     * 测试插入多条
     */
    @Test
    public void testInsertMany() {
        List<Object[]> list = new ArrayList<>();
        list.add(new Object[]{new Date(), 10.2, 219, 0.32});
        list.add(new Object[]{new Date(), 11.2, 219, 0.32});
        flexTdMapper.insertInto("d1001").valuesList(list, o -> (Object[]) o).execute();
    }

    /**
     * 测试插入并自动建表
     */
    @Test
    public void testInsertAndCreate() {
        flexTdMapper.insertInto("d2001").using("meters").tags(2, "abc").values(new Date(), 10.2, 219, 0.32).execute();
    }

    /**
     * 测试插入来自文件
     */
    @Test
    public void testInsertFile() {
        flexTdMapper.insertInto("d2001").using("meters").tags(2, "abc").file("fff.txt").execute();
    }

    /**
     * 测试分页查询
     */
    @Test
    public void testPageQuery() {
        Page<Map> page = flexTdMapper.select(col("voltage"), count(col("*"))).from("meters").partitionBy("voltage").page(Map.class, new Page(1, 2));
        System.out.println("总数：" + page.getTotal() + "\n查询结果：" + page.getRows());
    }

    /**
     * 测试插入实体
     */
    @Test
    public void insertEntity() {
        Meters meters=new Meters();
        meters.setTs(new Date());
        meters.setCurrent(1.23);
        meters.setPhase(3.45);
        meters.setVoltage(4);
        meters.setGroupid(1);
        meters.setLocation("a");
        flexTdMapper.insert("d1001", meters);
    }

}
