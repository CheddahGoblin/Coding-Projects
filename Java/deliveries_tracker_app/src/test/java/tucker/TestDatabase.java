package tucker;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Opens throwaway databases for the tests. H2 (a small in-memory database) runs in PostgreSQL mode,
 * so the real PostgresDeliveryRepository code is tested without touching the live Supabase database.
 */
final class TestDatabase {

    private TestDatabase() {}

    /** A brand-new, empty in-memory database (gone when the repository is closed). */
    static PostgresDeliveryRepository inMemory() {
        return open("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL");
    }

    /** Opens (or creates) the database at the given JDBC URL. */
    static PostgresDeliveryRepository open(String jdbcUrl) {
        try {
            return new PostgresDeliveryRepository(DriverManager.getConnection(jdbcUrl), false);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not open the test database", e);
        }
    }
}
