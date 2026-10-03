package edu.hcmute.cnpm.cinema.config;

import edu.hcmute.cnpm.cinema.controller.DemoWalletHealthController;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DemoWalletHealthTest {
    @Test void reportsReadyOnlyAfterValidDatabaseConnection() throws Exception {
        DataSource source = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(source.getConnection()).thenReturn(connection);
        when(connection.isValid(1)).thenReturn(true);
        var response = new DemoWalletHealthController(new DemoWalletSettings(true, "https://g8.test"), source).health();
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("status", "UP").containsEntry("checkoutVersion", "2").hasSize(2);
        verify(connection).close();
    }
    @Test void concealsDatabaseErrorAndReportsUnavailable() throws Exception {
        DataSource source = mock(DataSource.class);
        when(source.getConnection()).thenThrow(new SQLException("password should never leave backend"));
        var response = new DemoWalletHealthController(new DemoWalletSettings(true, "https://g8.test"), source).health();
        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody()).containsEntry("status", "UNAVAILABLE").hasSize(1);
    }
    @Test void disabledWalletNeverChecksDatabase() {
        DataSource source = mock(DataSource.class);
        var response = new DemoWalletHealthController(new DemoWalletSettings(false, "https://g8.test"), source).health();
        assertThat(response.getStatusCode().value()).isEqualTo(503);
        verifyNoInteractions(source);
    }
    @Test void invalidConnectionIsClosedAndNotReportedAsReady() throws Exception {
        DataSource source = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(source.getConnection()).thenReturn(connection);
        when(connection.isValid(1)).thenReturn(false);
        assertThat(new DemoWalletHealthController(new DemoWalletSettings(true, "https://g8.test"), source)
                .health().getStatusCode().value()).isEqualTo(503);
        verify(connection).close();
    }
}
