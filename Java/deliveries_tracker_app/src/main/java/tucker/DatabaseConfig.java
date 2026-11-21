package tucker;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

/**
 * Connection details for the hosted PostgreSQL (Supabase) database.
 * They come from the DATABASE_URL environment variable, never from the source code,
 * so the password is not stored in the repository.
 */
public record DatabaseConfig(String jdbcUrl, String username, String password) {

    /** Reads DATABASE_URL from the environment; empty if it isn't set. */
    public static Optional<DatabaseConfig> fromEnvironment() {
        String value = System.getenv("DATABASE_URL");
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(fromUri(value));
    }

    /**
     * Converts a URL like {@code postgresql://user:password@host:5432/postgres} into the form the
     * JDBC driver needs. SSL is required unless the URL says otherwise.
     *
     * @throws IllegalArgumentException if the URL is empty or malformed
     */
    public static DatabaseConfig fromUri(String uri) {
        if (uri == null || uri.isBlank()) {
            throw new IllegalArgumentException("DATABASE_URL is empty.");
        }
        URI u;
        try {
            u = new URI(uri.trim());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(
                    "DATABASE_URL is not a valid address. If the password has special characters "
                            + "such as @ / # or ?, percent-encode them.", e);
        }
        if (!"postgresql".equals(u.getScheme()) && !"postgres".equals(u.getScheme())) {
            throw new IllegalArgumentException("DATABASE_URL must start with postgresql://");
        }
        if (u.getHost() == null || u.getHost().isBlank()) {
            throw new IllegalArgumentException("DATABASE_URL has no host name.");
        }
        String info = u.getUserInfo(); // decoded "user:password"
        int colon = info == null ? -1 : info.indexOf(':');
        if (colon <= 0 || colon == info.length() - 1) {
            throw new IllegalArgumentException("DATABASE_URL must include a username and password (user:password@host).");
        }
        String user = info.substring(0, colon);
        String password = info.substring(colon + 1);

        int port = u.getPort() == -1 ? 5432 : u.getPort();
        String database = u.getPath() == null ? "" : u.getPath().replaceFirst("^/", "");
        if (database.isEmpty()) database = "postgres";

        String query = u.getRawQuery();
        if (query == null) query = "sslmode=require";
        else if (!query.contains("sslmode=")) query += "&sslmode=require";

        return new DatabaseConfig("jdbc:postgresql://" + u.getHost() + ":" + port + "/" + database + "?" + query,
                user, password);
    }

    /** Never prints the password. */
    @Override
    public String toString() {
        return "DatabaseConfig[" + jdbcUrl + ", user=" + username + "]";
    }
}
