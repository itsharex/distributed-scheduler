/*
 * Copyright 2022-2024 Ponfee (http://www.ponfee.cn/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.ponfee.disjob.test.db;

import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfiguration;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.common.util.Files;
import cn.ponfee.disjob.common.util.MavenProjects;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

import static cn.ponfee.disjob.test.db.DBUtils.*;

/**
 * <pre>
 * MariaDB Server
 * SELECT VERSION()  ->  10.2.11-MariaDB
 * MacOS如果启动失败报"未找到openssl"错误，参考安装步骤：“/disjob-test/src/main/DB/MariaDB/MariaDB.md”
 *
 * username: root
 * password: 无需密码
 *
 *
 * mysql:
 *  Maven GAV    -> com.mysql:mysql-connector-j:8.0.33
 *  jdbc-url     -> jdbc:mysql://localhost:3306/disjob
 *  driver-class -> com.mysql.cj.jdbc.Driver
 *
 * MariaDB:
 *  Maven GAV    -> org.mariadb.jdbc:mariadb-java-client:3.1.4
 *  jdbc-url     -> jdbc:mariadb://localhost:3306/disjob
 *  driver-class -> org.mariadb.jdbc.Driver
 * </pre>
 *
 * @author Ponfee
 */
public class EmbeddedMysqlServerMariaDB {

    public static void main(String[] args) throws Exception {
        DB db = start(3306);
        Runtime.getRuntime().addShutdownHook(new Thread(ThrowingRunnable.toCaught(db::stop)));
    }

    public static DB start(int port) throws Exception {
        DBConfiguration configuration = DBConfigurationBuilder.newBuilder()
            .setPort(port) // OR, default: setPort(0); => autom. detect free port
            .setBaseDir(createDirectory("base"))
            .setDataDir(createDirectory("data"))
            .addArg("--user=root")
            .addArg("--character-set-server=utf8mb4")
            .addArg("--collation-server=utf8mb4_bin")
            //.addArg("--default-character-set=utf8mb4")
            //.addArg("--skip-grant-tables") // 默认就是skip-grant-tables
            .build();

        System.out.println("Embedded maria db starting...");

        DB db = DB.newEmbeddedDB(configuration);
        db.start();

        db.source(IOUtils.toInputStream(loadScript(DISJOB_ADMIN_SCRIPT_CLASSPATH), StandardCharsets.UTF_8));
        db.source(IOUtils.toInputStream(loadScript(DISJOB_SCRIPT_CLASSPATH), StandardCharsets.UTF_8));

        String jdbcUrl = "jdbc:mysql://localhost:" + port + "/" + DB_NAME;
        JdbcTemplate jdbcTemplate = DBUtils.createJdbcTemplate(jdbcUrl, USERNAME, PASSWORD);

        System.out.println("\n--------------------------------------------------------testDatabase");
        DBUtils.testNativeConnection("com.mysql.cj.jdbc.Driver", jdbcUrl, USERNAME, PASSWORD);

        System.out.println("\n--------------------------------------------------------testMysql");
        DBUtils.testMysqlVersion(jdbcTemplate);

        System.out.println("\n--------------------------------------------------------testJdbcTemplate");
        DBUtils.testJdbcTemplate(jdbcTemplate);

        System.out.println("\n--------------------------------------------------------testQuerySql");
        DBUtils.testQuerySchedJob(jdbcTemplate);

        System.out.println("Embedded maria db started!");
        return db;
    }

    private static String loadScript(String scriptPath) throws Exception {
        return Arrays.stream(DBUtils.loadScript(scriptPath).split("\n"))
            // fix error: The MariaDB server is running with the --skip-grant-tables option so it cannot execute this statement
            .filter(s -> !StringUtils.startsWithAny(s, "DROP USER ", "CREATE USER ", "GRANT ALL PRIVILEGES ON ", "FLUSH PRIVILEGES;"))
            .collect(Collectors.joining("\n"));
    }

    private static String createDirectory(String name) throws IOException {
        String dataDir = MavenProjects.getProjectBaseDir() + "/target/mariadb/" + name;
        Files.cleanOrMakeDir(dataDir);
        return dataDir;
    }

}
