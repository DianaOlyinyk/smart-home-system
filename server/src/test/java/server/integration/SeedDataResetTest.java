package server.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import server.support.IntegrationTest;
import server.support.SeedData;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class SeedDataResetTest {

    @Autowired
    private JdbcClient jdbc;

    @Test
    void database_ShouldContainOnlySeedData() {
        assertThat(jdbc.sql("select count(*) from users").query(Integer.class).single()).isEqualTo(3);
        assertThat(jdbc.sql("select count(*) from devices where name = ?")
                .param(SeedData.SEED_DEVICE_NAME).query(Integer.class).single()).isEqualTo(1);
        assertThat(jdbc.sql("select count(*) from command").query(Integer.class).single()).isZero();
    }
}
