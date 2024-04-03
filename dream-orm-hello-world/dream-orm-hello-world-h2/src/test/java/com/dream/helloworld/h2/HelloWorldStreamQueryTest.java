package com.dream.helloworld.h2;


import com.dream.antlr.sql.ToMySQL;
import com.dream.helloworld.h2.table.Account;
import com.dream.stream.support.Wrappers;
import com.dream.stream.wrapper.QueryWrapper;
import com.dream.struct.factory.CommandDialectFactory;
import com.dream.struct.factory.DefaultCommandDialectFactory;
import com.dream.system.config.MappedStatement;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = HelloWorldApplication.class)
public class HelloWorldStreamQueryTest {
    private CommandDialectFactory dialectFactory = new DefaultCommandDialectFactory(new ToMySQL());

    /**
     * 测试select多个字段
     */
    @Test
    public void testSelectColumn() {
        QueryWrapper wrapper = Wrappers.query(Account.class).select("a", "b", "c");
        MappedStatement mappedStatement = dialectFactory.compile(wrapper, null);
        System.out.println(mappedStatement.getSql());
    }

    @Test
    public void testSelectFunc() {
        QueryWrapper wrapper = Wrappers.query(Account.class).select(i -> i.len("a").ascii("b").length("c"));
        MappedStatement mappedStatement = dialectFactory.compile(wrapper, null);
        System.out.println(mappedStatement.getSql());
    }

    @Test
    public void testWhere() {
        QueryWrapper wrapper = Wrappers.query(Account.class)
                .leq("b", 11).and(a -> a.leq("age", 11).or(b -> b.like("a", "11")));
        MappedStatement mappedStatement = dialectFactory.compile(wrapper, null);
        System.out.println(mappedStatement.getSql());
    }

    @Test
    public void testWhere2() {
        QueryWrapper wrapper = Wrappers.query(Account.class).where(i -> i.leq("b", 11).and(a -> a.leq("age", 11).or(b -> b.like("a", "11"))));
        MappedStatement mappedStatement = dialectFactory.compile(wrapper, null);
        System.out.println(mappedStatement.getSql());
    }

    @Test
    public void testGroup() {
        QueryWrapper wrapper = Wrappers.query(Account.class).groupBy("a");
        MappedStatement mappedStatement = dialectFactory.compile(wrapper, null);
        System.out.println(mappedStatement.getSql());
    }

    @Test
    public void testHaving() {
        QueryWrapper wrapper = Wrappers.query(Account.class).groupBy("a").leq("b", 11).and(a -> a.leq("age", 11).or(b -> b.like("a", "11")));
        MappedStatement mappedStatement = dialectFactory.compile(wrapper, null);
        System.out.println(mappedStatement.getSql());
    }

    @Test
    public void testHaving2() {
        QueryWrapper wrapper = Wrappers.query(Account.class).groupBy("a").having(i -> i.leq("b", 11).and(a -> a.leq("age", 11).or(b -> b.like("a", "11"))));
        MappedStatement mappedStatement = dialectFactory.compile(wrapper, null);
        System.out.println(mappedStatement.getSql());
    }

    @Test
    public void testOrder() {
        QueryWrapper wrapper = Wrappers.query(Account.class).orderBy("a", "b");
        MappedStatement mappedStatement = dialectFactory.compile(wrapper, null);
        System.out.println(mappedStatement.getSql());
    }

    @Test
    public void testOrder2() {
        QueryWrapper wrapper = Wrappers.query(Account.class).orderBy(i -> i.asc("a").desc("b"));
        MappedStatement mappedStatement = dialectFactory.compile(wrapper, null);
        System.out.println(mappedStatement.getSql());
    }

    @Test
    public void testLimit() {
        QueryWrapper wrapper = Wrappers.query(Account.class).limit(5, 10);
        MappedStatement mappedStatement = dialectFactory.compile(wrapper, null);
        System.out.println(mappedStatement.getSql());
    }

    @Test
    public void testOffset() {
        QueryWrapper wrapper = Wrappers.query(Account.class).offset(5, 10);
        MappedStatement mappedStatement = dialectFactory.compile(wrapper, null);
        System.out.println(mappedStatement.getSql());
    }

    @Test
    public void testUnion() {
        QueryWrapper wrapper = Wrappers.query(Account.class).union(Wrappers.query(Account.class));
        MappedStatement mappedStatement = dialectFactory.compile(wrapper, null);
        System.out.println(mappedStatement.getSql());
    }

    @Test
    public void testUnionAll() {
        QueryWrapper wrapper = Wrappers.query(Account.class).unionAll(Wrappers.query(Account.class));
        MappedStatement mappedStatement = dialectFactory.compile(wrapper, null);
        System.out.println(mappedStatement.getSql());
    }

    @Test
    public void testForUpdate() {
        QueryWrapper wrapper = Wrappers.query(Account.class).forUpdate();
        MappedStatement mappedStatement = dialectFactory.compile(wrapper, null);
        System.out.println(mappedStatement.getSql());
    }

    @Test
    public void testForUpdateNoWait() {
        QueryWrapper wrapper = Wrappers.query(Account.class).forUpdateNoWait();
        MappedStatement mappedStatement = dialectFactory.compile(wrapper, null);
        System.out.println(mappedStatement.getSql());
    }

}
