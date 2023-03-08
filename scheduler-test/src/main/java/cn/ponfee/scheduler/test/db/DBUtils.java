/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.test.db;

import cn.ponfee.scheduler.common.util.Jsons;
import cn.ponfee.scheduler.common.util.Numbers;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 内存DB测试工具类
 *
 * @author Ponfee
 */
public class DBUtils {

    public static final String DB_NAME = "distributed_scheduler";
    public static final String DB_SCRIPT_PATH = "JOB_TABLES_DDL.sql";

    public static JdbcTemplate createJdbcTemplate(String url, String user, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        return new JdbcTemplate(new HikariDataSource(config));
    }

    public static String loadScript() throws Exception {
        return IOUtils.resourceToString(DB_SCRIPT_PATH, StandardCharsets.UTF_8, DBUtils.class.getClassLoader());
    }

    public static void testNativeConnection(String driver, String url, String user, String password) throws Exception {
        // 非必须：DriverManager.getConnection(url, user, password)时会根据url自动识别来加载Driver实现类
        Class.forName(driver);
        Connection conn = DriverManager.getConnection(url, user, password);

        System.out.println("Testing Database, URL=" + url);
        Statement stat = conn.createStatement();
        stat.execute("DROP TABLE IF EXISTS test");
        stat.execute("CREATE TABLE IF NOT EXISTS test(id INT, `name` VARCHAR(30))");

        // insert
        List<String> data = new ArrayList<>();
        PreparedStatement insertSql = conn.prepareStatement("INSERT INTO test VALUES(?, ?)");
        for (int i = 0; i < 100; i++) {
            String str = RandomStringUtils.randomAlphanumeric(4);
            data.add(str);
            insertSql.setInt(1, i);
            insertSql.setString(2, str);
            // autocommit is on by default, so this commits as well
            insertSql.execute();
        }

        // query
        List<String> result = new ArrayList<>();
        ResultSet resultSet = conn.prepareStatement("SELECT `name` FROM test ORDER BY id ASC").executeQuery();
        while (resultSet.next()) {
            result.add(resultSet.getString(1));
        }

        Assert.isTrue(
            CollectionUtils.isEqualCollection(data, result),
            () -> Jsons.toJson(data) + " != " + Jsons.toJson(result)
        );

        stat.execute("DROP TABLE IF EXISTS test");
        conn.close();
    }

    public static void testJdbcTemplate(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("DROP TABLE IF EXISTS test");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS test(id INT, `name` VARCHAR(30))");

        List<String> data = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            String str = RandomStringUtils.randomAlphanumeric(4);
            data.add(str);
            jdbcTemplate.update("INSERT INTO test VALUES(?, ?)", i, str);
        }

        List<String> result = jdbcTemplate.queryForList("SELECT `name` FROM test ORDER BY id ASC", String.class);
        Assert.isTrue(
            CollectionUtils.isEqualCollection(data, result),
            () -> Jsons.toJson(data) + " != " + Jsons.toJson(result)
        );

        jdbcTemplate.execute("DROP TABLE IF EXISTS test");
    }

    public static void testMysqlVersion(JdbcTemplate jdbcTemplate) {
        String version = jdbcTemplate.queryForObject("SELECT VERSION()", String.class);
        System.out.println("Version: " + version);

        int expect = 1;
        Integer actual = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        Assert.isTrue(expect == actual, () -> expect + " != " + actual);
    }

    public static void testQuerySchedJob(JdbcTemplate jdbcTemplate) {
        List<Map<String, Object>> result = jdbcTemplate.queryForList("SELECT * FROM sched_job ORDER BY id ASC");
        long expect = 3988904755200L;
        long actual = Numbers.toLong(result.get(0).get("job_id"));
        Assert.isTrue(expect == actual, () -> expect + " != " + actual);
        System.out.println("Query result: " + Jsons.toJson(result));
    }

}
