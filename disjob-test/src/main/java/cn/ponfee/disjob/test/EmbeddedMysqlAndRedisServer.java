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

package cn.ponfee.disjob.test;

import ch.vorburger.mariadb4j.DB;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingSupplier;
import cn.ponfee.disjob.test.db.EmbeddedMysqlServerMariaDB;
import cn.ponfee.disjob.test.redis.EmbeddedRedisServerKstyrc;
import org.slf4j.impl.SimpleLogger;
import redis.embedded.RedisServer;

/**
 * Embedded mysql & redis server
 *
 * @author Ponfee
 */
public final class EmbeddedMysqlAndRedisServer {

    static {
        System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "INFO");
        System.setProperty(SimpleLogger.LOG_FILE_KEY, "System.out");
    }

    public static void main(String[] args) {
        EmbeddedMysqlAndRedisServer.starter()
            .mysqlPort(3306)
            .redisMasterPort(6379)
            .redisSlavePort(6380)
            .start();
    }

    private volatile DB mariaDBServer;
    private volatile RedisServer redisServer;

    private EmbeddedMysqlAndRedisServer(int mysqlPort, int redisMasterPort, int redisSlavePort) {
        System.out.println("/*============================================================*\\");
        this.mariaDBServer = ThrowingSupplier.doChecked(() -> EmbeddedMysqlServerMariaDB.start(mysqlPort));
        System.out.println("\\*============================================================*/");

        System.out.println("\n\n\n\n\n\n");

        System.out.println("/*============================================================*\\");
        this.redisServer = EmbeddedRedisServerKstyrc.start(redisMasterPort, redisSlavePort);
        System.out.println("\\*============================================================*/");

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    public synchronized void stop() {
        ThrowingRunnable.doCaught(() -> Thread.sleep(10000));
        if (mariaDBServer != null) {
            ThrowingRunnable.doCaught(mariaDBServer::stop);
            mariaDBServer = null;
        }
        if (redisServer != null) {
            ThrowingRunnable.doCaught(redisServer::stop);
            redisServer = null;
        }
    }

    public static Starter starter() {
        return new Starter();
    }

    public static class Starter {
        private int mysqlPort = 3306;
        private int redisMasterPort = 6379;
        private int redisSlavePort = 6380;

        private Starter() {
        }

        public Starter mysqlPort(int mysqlPort) {
            this.mysqlPort = mysqlPort;
            return this;
        }

        public Starter redisMasterPort(int redisMasterPort) {
            this.redisMasterPort = redisMasterPort;
            return this;
        }

        public Starter redisSlavePort(int redisSlavePort) {
            this.redisSlavePort = redisSlavePort;
            return this;
        }

        public EmbeddedMysqlAndRedisServer start() {
            return new EmbeddedMysqlAndRedisServer(mysqlPort, redisMasterPort, redisSlavePort);
        }
    }

}
