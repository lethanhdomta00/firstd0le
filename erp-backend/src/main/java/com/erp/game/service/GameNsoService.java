package com.erp.game.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.*;

@Service
@Slf4j
public class GameNsoService {

    @Value("${nso.datasource.url:jdbc:mysql://127.0.0.1:3306/nsoz?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8}")
    private String nsoDbUrl;

    @Value("${nso.datasource.username:root}")
    private String nsoDbUser;

    @Value("${nso.datasource.password:}")
    private String nsoDbPass;

    @Value("${nso.game.host:127.0.0.1}")
    private String gameHost;

    @Value("${nso.game.port:14444}")
    private int gamePort;

    @Value("${nso.game.java-bin:C:\\Program Files\\Java\\jdk-19\\bin\\java.exe}")
    private String javaBinPath;

    @Value("${nso.game.server-dir:NSO_SETUP_SERVER_FREE/nso_server}")
    private String serverDirConfig;

    @Value("${nso.game.client-dir:NSO_SETUP_SERVER_FREE/NSO_SETUP_SERVER_FREE}")
    private String clientDirConfig;

    private volatile HikariDataSource nsoDataSource;
    private volatile JdbcTemplate nsoJdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private JdbcTemplate getJdbc() {
        if (nsoJdbcTemplate == null) {
            synchronized (this) {
                if (nsoJdbcTemplate == null) {
                    HikariConfig config = new HikariConfig();
                    config.setDriverClassName("com.mysql.cj.jdbc.Driver");
                    config.setJdbcUrl(nsoDbUrl);
                    config.setUsername(nsoDbUser);
                    config.setPassword(nsoDbPass);
                    config.setPoolName("NsoGameHikariPool");
                    config.setMaximumPoolSize(5);
                    config.setMinimumIdle(1);
                    config.setConnectionTimeout(5000);
                    config.setIdleTimeout(300000);
                    nsoDataSource = new HikariDataSource(config);
                    nsoJdbcTemplate = new JdbcTemplate(nsoDataSource);
                    log.info("[NSO-GAME] Khởi tạo HikariPool thành công tới {}", nsoDbUrl);
                }
            }
        }
        return nsoJdbcTemplate;
    }

    @PreDestroy
    public void shutdownPool() {
        if (nsoDataSource != null && !nsoDataSource.isClosed()) {
            nsoDataSource.close();
        }
    }

    public boolean isGameServerOnline() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(gameHost, gamePort), 600);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Map<String, Object> getOverviewStatus() {
        Map<String, Object> res = new LinkedHashMap<>();
        boolean online = isGameServerOnline();
        res.put("serverOnline", online);
        res.put("serverHost", gameHost);
        res.put("serverPort", gamePort);

        try {
            JdbcTemplate jdbc = getJdbc();
            Long totalUsers = jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class);
            Long onlineUsers = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM users u WHERE u.online = 1 OR EXISTS (" +
                            "SELECT 1 FROM players p WHERE p.user_id = u.id AND (p.online = 1 OR p.last_login_time > p.last_logout_time))",
                    Long.class);
            Long totalPlayers = jdbc.queryForObject("SELECT COUNT(*) FROM players", Long.class);
            Long totalItems = jdbc.queryForObject("SELECT COUNT(*) FROM item", Long.class);

            res.put("dbConnected", true);
            res.put("totalUsers", totalUsers != null ? totalUsers : 0L);
            res.put("onlineUsers", onlineUsers != null ? onlineUsers : 0L);
            res.put("totalPlayers", totalPlayers != null ? totalPlayers : 0L);
            res.put("totalItems", totalItems != null ? totalItems : 0L);
        } catch (Exception e) {
            log.warn("Không thể truy vấn thống kê DB nsoz: {}", e.getMessage());
            res.put("dbConnected", false);
            res.put("totalUsers", 0L);
            res.put("onlineUsers", 0L);
            res.put("totalPlayers", 0L);
            res.put("totalItems", 0L);
        }
        return res;
    }

    public Map<String, Object> launchClientApp() {
        Path clientDir = resolveConfiguredPath(clientDirConfig);
        File jarFile = clientDir.resolve("AngelChipEmulator.jar").toFile();
        if (!jarFile.exists()) {
            throw new IllegalStateException("Không tìm thấy AngelChipEmulator.jar tại " + clientDir);
        }
        try {
            String javaBin = new File(javaBinPath).exists() ? javaBinPath : "java";
            File localJar = clientDir.resolve("Local.jar").toFile();

            List<String> cmd = new ArrayList<>();
            cmd.add(javaBin);
            cmd.add("--add-opens");
            cmd.add("java.base/java.lang=ALL-UNNAMED");
            cmd.add("--add-opens");
            cmd.add("java.desktop/java.awt=ALL-UNNAMED");
            cmd.add("--add-opens");
            cmd.add("java.desktop/javax.swing=ALL-UNNAMED");
            cmd.add("-jar");
            cmd.add("AngelChipEmulator.jar");
            cmd.add("--resizableDevice");
            cmd.add("540");
            cmd.add("360");
            if (localJar.exists()) {
                cmd.add("Local.jar");
            }

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(clientDir.toFile());
            // Quan trọng: Redirect output ra file log để tránh đầy pipe 64KB trên Windows gây đơ Swing UI
            pb.redirectErrorStream(true);
            File clientLog = clientDir.resolve("client-emulator.log").toFile();
            pb.redirectOutput(clientLog);
            pb.start();

            return Map.of(
                    "launched", true,
                    "message", "Đã mở trình giả lập AngelChipEmulator (tự nạp Local.jar)! Tài khoản mặc định: admin / 112233."
            );
        } catch (Exception e) {
            throw new RuntimeException("Không thể khởi chạy client game: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> readGameLogs(String source, int lines) {
        int safeLines = Math.min(Math.max(lines, 20), 500);
        Path logPath;
        if ("client".equalsIgnoreCase(source)) {
            logPath = resolveConfiguredPath(clientDirConfig).resolve("client-emulator.log");
        } else if ("console".equalsIgnoreCase(source)) {
            logPath = resolveConfiguredPath(serverDirConfig).resolve("logs").resolve("erp-launcher.log");
        } else {
            logPath = resolveConfiguredPath(serverDirConfig).resolve("logs").resolve("nso.log");
        }

        if (!Files.exists(logPath)) {
            return Map.of(
                    "exists", false,
                    "path", logPath.toString(),
                    "lines", List.of("Chưa tìm thấy file log: " + logPath.getFileName())
            );
        }

        try {
            CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPLACE)
                    .onUnmappableCharacter(CodingErrorAction.REPLACE);
            Deque<String> deque = new ArrayDeque<>(safeLines);
            long totalLines = 0;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(logPath.toFile()), decoder))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    totalLines++;
                    if (deque.size() >= safeLines) {
                        deque.pollFirst();
                    }
                    deque.offerLast(line);
                }
            }
            return Map.of(
                    "exists", true,
                    "path", logPath.getFileName().toString(),
                    "totalLines", totalLines,
                    "lines", new ArrayList<>(deque)
            );
        } catch (Exception e) {
            return Map.of(
                    "exists", true,
                    "path", logPath.getFileName().toString(),
                    "lines", List.of("Không thể đọc file log: " + e.getMessage())
            );
        }
    }

    public Map<String, Object> startGameServer() {
        if (isGameServerOnline()) {
            return Map.of("started", true, "alreadyRunning", true, "message", "Game Server NSO đang chạy sẵn trên cổng " + gamePort + "!");
        }
        Path serverDir = resolveConfiguredPath(serverDirConfig);
        File jarFile = serverDir.resolve("target").resolve("Nso-jar-with-dependencies.jar").toFile();
        if (!jarFile.exists()) {
            throw new IllegalStateException("Không tìm thấy file Server JAR tại " + jarFile.getAbsolutePath());
        }
        try {
            String javaBin = new File(javaBinPath).exists() ? javaBinPath : "java";
            ProcessBuilder pb = new ProcessBuilder(
                    javaBin, "-server", "-Dfile.encoding=UTF-8", "-Xms1G", "-Xmx2G",
                    "-jar", "target" + File.separator + "Nso-jar-with-dependencies.jar"
            );
            pb.directory(serverDir.toFile());
            pb.redirectErrorStream(true);
            File logFile = serverDir.resolve("logs").resolve("erp-launcher.log").toFile();
            if (logFile.getParentFile() != null) {
                logFile.getParentFile().mkdirs();
            }
            pb.redirectOutput(logFile);
            pb.start();
            return Map.of("started", true, "alreadyRunning", false, "message", "Đang khởi động Game Server NSO trên cổng " + gamePort + " (mất khoảng 10-15 giây để nạp Map)!");
        } catch (Exception e) {
            throw new RuntimeException("Không thể khởi động Game Server: " + e.getMessage(), e);
        }
    }

    // =========================================================
    // 1. QUẢN LÝ TÀI KHOẢN GAME (nsoz.users)
    // =========================================================

    public Map<String, Object> listUsers(String keyword, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = Math.max(page, 0) * safeSize;
        JdbcTemplate jdbc = getJdbc();

        String kw = (keyword != null && !keyword.isBlank()) ? "%" + keyword.trim() + "%" : null;
        String where = kw != null ? " WHERE username LIKE ? " : "";

        Long total = kw != null
                ? jdbc.queryForObject("SELECT COUNT(*) FROM users" + where, Long.class, kw)
                : jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class);

        String sql = "SELECT u.id, u.username, u.status, u.activated, u.luong, u.coin, u.balance, u.`lock`, u.role, u.created_at, " +
                "CASE WHEN u.online = 1 OR EXISTS (SELECT 1 FROM players p WHERE p.user_id = u.id AND (p.online = 1 OR p.last_login_time > p.last_logout_time)) THEN 1 ELSE 0 END AS online " +
                "FROM users u" + (kw != null ? " WHERE u.username LIKE ? " : "") + " ORDER BY u.id DESC LIMIT ? OFFSET ?";

        List<Map<String, Object>> rows = kw != null
                ? jdbc.queryForList(sql, kw, safeSize, offset)
                : jdbc.queryForList(sql, safeSize, offset);

        return Map.of("content", rows, "totalElements", total != null ? total : 0L);
    }

    public Map<String, Object> createUser(String username, String password, int luong, int coin) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Tên đăng nhập và mật khẩu không được để trống!");
        }
        JdbcTemplate jdbc = getJdbc();
        Long exists = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE username = ?", Long.class, username.trim());
        if (exists != null && exists > 0) {
            throw new IllegalArgumentException("Tài khoản '" + username.trim() + "' đã tồn tại!");
        }

        jdbc.update(
                "INSERT INTO users (username, password, status, activated, kichhoat, kh, luong, coin, balance, online, `lock`, created_at) " +
                        "VALUES (?, ?, 1, 1, 1, 1, ?, ?, 0, 0, 0, NOW())",
                username.trim(), password.trim(), Math.max(luong, 0), Math.max(coin, 0)
        );
        return Map.of("success", true, "message", "Đã tạo tài khoản game '" + username.trim() + "' thành công!");
    }

    public Map<String, Object> topupUser(long userId, int luongDelta, int coinDelta) {
        JdbcTemplate jdbc = getJdbc();
        int updated = jdbc.update(
                "UPDATE users SET luong = GREATEST(0, luong + ?), coin = GREATEST(0, coin + ?) WHERE id = ?",
                luongDelta, coinDelta, userId
        );
        if (updated == 0) {
            throw new IllegalArgumentException("Không tìm thấy tài khoản #" + userId);
        }
        return Map.of("success", true, "message", "Đã cập nhật Lượng/Coin cho tài khoản #" + userId);
    }

    public Map<String, Object> toggleUserLock(long userId) {
        JdbcTemplate jdbc = getJdbc();
        Integer currentLock = jdbc.queryForObject("SELECT `lock` FROM users WHERE id = ?", Integer.class, userId);
        int nextLock = (currentLock != null && currentLock == 1) ? 0 : 1;
        int nextStatus = nextLock == 1 ? 0 : 1;
        jdbc.update("UPDATE users SET `lock` = ?, status = ? WHERE id = ?", nextLock, nextStatus, userId);
        return Map.of("success", true, "locked", nextLock == 1,
                "message", nextLock == 1 ? "Đã khóa tài khoản #" + userId : "Đã mở khóa tài khoản #" + userId);
    }

    // =========================================================
    // 2. QUẢN LÝ NHÂN VẬT & TIỀN TỆ (nsoz.players)
    // =========================================================

    public Map<String, Object> listPlayers(String keyword, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = Math.max(page, 0) * safeSize;
        JdbcTemplate jdbc = getJdbc();

        String kw = (keyword != null && !keyword.isBlank()) ? "%" + keyword.trim() + "%" : null;
        String where = kw != null ? " WHERE p.name LIKE ? " : "";

        Long total = kw != null
                ? jdbc.queryForObject("SELECT COUNT(*) FROM players p" + where, Long.class, kw)
                : jdbc.queryForObject("SELECT COUNT(*) FROM players p", Long.class);

        String sql = "SELECT p.id, p.user_id, u.username, u.luong, p.name, p.class AS charClass, p.gender, " +
                "p.xu, p.xuInBox, p.yen, p.point, p.spoint, p.potential, p.clan, p.numberCellBag, p.bag, " +
                "CASE WHEN p.online = 1 OR p.last_login_time > p.last_logout_time THEN 1 ELSE 0 END AS online " +
                "FROM players p LEFT JOIN users u ON p.user_id = u.id" + where +
                " ORDER BY p.id DESC LIMIT ? OFFSET ?";

        List<Map<String, Object>> rows = kw != null
                ? jdbc.queryForList(sql, kw, safeSize, offset)
                : jdbc.queryForList(sql, safeSize, offset);

        return Map.of("content", rows, "totalElements", total != null ? total : 0L);
    }

    public Map<String, Object> buffPlayerCurrency(long playerId, long xuDelta, long yenDelta) {
        JdbcTemplate jdbc = getJdbc();
        int updated = jdbc.update(
                "UPDATE players SET xu = GREATEST(0, xu + ?), yen = GREATEST(0, yen + ?) WHERE id = ?",
                xuDelta, yenDelta, playerId
        );
        if (updated == 0) {
            throw new IllegalArgumentException("Không tìm thấy nhân vật #" + playerId);
        }
        return Map.of("success", true, "message", "Đã buff Xu/Yên cho nhân vật #" + playerId);
    }

    /**
     * Tiện ích GM: Buff toàn diện chỉ số cho 1 nhân vật cụ thể (Tiềm năng, Kỹ năng, Phái, Uy danh, EXP, Xu/Yên/Lượng)
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> buffPlayerStats(long playerId,
                                               long xuDelta,
                                               long yenDelta,
                                               int luongDelta,
                                               int pointDelta,
                                               int spointDelta,
                                               Integer charClass,
                                               String potentialOverride,
                                               long uydanhDelta,
                                               long expDelta) {
        JdbcTemplate jdbc = getJdbc();
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, user_id, name, class AS charClass, point, spoint, potential, data FROM players WHERE id = ?",
                playerId
        );
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy nhân vật #" + playerId);
        }
        Map<String, Object> player = rows.get(0);
        Long userId = player.get("user_id") != null ? ((Number) player.get("user_id")).longValue() : null;

        String nextDataJson = String.valueOf(player.get("data"));
        try {
            Map<String, Object> dataMap = objectMapper.readValue(nextDataJson, Map.class);
            if (uydanhDelta != 0) {
                long curUyDanh = ((Number) dataMap.getOrDefault("pointUyDanh", 0)).longValue();
                dataMap.put("pointUyDanh", Math.max(0L, curUyDanh + uydanhDelta));
            }
            if (expDelta != 0) {
                long curExp = ((Number) dataMap.getOrDefault("exp", 0)).longValue();
                dataMap.put("exp", Math.max(0L, curExp + expDelta));
            }
            nextDataJson = objectMapper.writeValueAsString(dataMap);
        } catch (Exception e) {
            log.warn("Không thể parse JSON data của nhân vật #{}: {}", playerId, e.getMessage());
        }

        String nextPotential = (potentialOverride != null && !potentialOverride.isBlank())
                ? potentialOverride.trim()
                : String.valueOf(player.get("potential"));
        int nextClass = charClass != null
                ? charClass
                : ((Number) player.getOrDefault("class", 0)).intValue();

        jdbc.update(
                "UPDATE players SET xu = GREATEST(0, xu + ?), yen = GREATEST(0, yen + ?), " +
                        "point = GREATEST(0, point + ?), spoint = GREATEST(0, spoint + ?), " +
                        "class = ?, potential = ?, data = ? WHERE id = ?",
                xuDelta, yenDelta, pointDelta, spointDelta, nextClass, nextPotential, nextDataJson, playerId
        );

        if (luongDelta != 0 && userId != null) {
            jdbc.update("UPDATE users SET luong = GREATEST(0, luong + ?) WHERE id = ?", luongDelta, userId);
        }

        return Map.of(
                "success", true,
                "message", "Đã buff chỉ số thành công cho nhân vật '" + player.get("name") + "'! (Nếu đang online, thoát ra vào lại để nhận hiệu lực)"
        );
    }

    // =========================================================
    // 4. QUẢN LÝ & TẠO BANG HỘI (nsoz.clan & clan_member)
    // =========================================================

    public Map<String, Object> listClans(String keyword, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = Math.max(page, 0) * safeSize;
        JdbcTemplate jdbc = getJdbc();

        String kw = (keyword != null && !keyword.isBlank()) ? "%" + keyword.trim() + "%" : null;
        String where = kw != null ? " WHERE c.name LIKE ? OR c.main_name LIKE ? " : "";

        Long total = kw != null
                ? jdbc.queryForObject("SELECT COUNT(*) FROM clan c" + where, Long.class, kw, kw)
                : jdbc.queryForObject("SELECT COUNT(*) FROM clan c", Long.class);

        String sql = "SELECT c.id, c.name, c.main_name, c.assist_name, c.level, c.exp, c.coin, c.alert, c.reg_date, " +
                "(SELECT COUNT(*) FROM clan_member cm WHERE cm.clan = c.id) AS memberCount " +
                "FROM clan c" + where + " ORDER BY c.id DESC LIMIT ? OFFSET ?";

        List<Map<String, Object>> rows = kw != null
                ? jdbc.queryForList(sql, kw, kw, safeSize, offset)
                : jdbc.queryForList(sql, safeSize, offset);

        return Map.of("content", rows, "totalElements", total != null ? total : 0L);
    }

    public Map<String, Object> createClan(String clanName, String leaderPlayerName, int level, int coin, String alert) {
        if (clanName == null || clanName.isBlank() || leaderPlayerName == null || leaderPlayerName.isBlank()) {
            throw new IllegalArgumentException("Tên bang hội và tên nhân vật Bang chủ không được để trống!");
        }
        String cleanClanName = clanName.trim();
        String cleanLeader = leaderPlayerName.trim();
        JdbcTemplate jdbc = getJdbc();

        Long dupClan = jdbc.queryForObject("SELECT COUNT(*) FROM clan WHERE LOWER(name) = LOWER(?)", Long.class, cleanClanName);
        if (dupClan != null && dupClan > 0) {
            throw new IllegalArgumentException("Bang hội '" + cleanClanName + "' đã tồn tại!");
        }

        List<Map<String, Object>> players = jdbc.queryForList(
                "SELECT id, name, class AS charClass, clan FROM players WHERE name = ?",
                cleanLeader
        );
        if (players.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy nhân vật '" + cleanLeader + "' trong game!");
        }
        Map<String, Object> leader = players.get(0);
        int leaderId = ((Number) leader.get("id")).intValue();
        int leaderClass = ((Number) leader.getOrDefault("charClass", 0)).intValue();
        int currentClan = ((Number) leader.getOrDefault("clan", 0)).intValue();
        if (currentClan > 0) {
            throw new IllegalArgumentException("Nhân vật '" + cleanLeader + "' đang thuộc bang hội #" + currentClan + "!");
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        String finalAlert = alert != null ? alert.trim() : "Chào mừng đến với bang hội " + cleanClanName;
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO clan (server_id, name, main_name, assist_name, alert, coin, level, exp, box, item_level, open_dun, use_card, reg_date, updated_at) " +
                            "VALUES (1, ?, ?, '', ?, ?, ?, 0, '[]', 0, 1, 1, NOW(), CURDATE())",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, cleanClanName);
            ps.setString(2, cleanLeader);
            ps.setString(3, finalAlert);
            ps.setInt(4, Math.max(coin, 0));
            ps.setInt(5, Math.max(level, 1));
            return ps;
        }, keyHolder);

        int newClanId = keyHolder.getKey() != null ? keyHolder.getKey().intValue() : 0;
        if (newClanId > 0) {
            jdbc.update(
                    "INSERT INTO clan_member (name, class_id, level, clan, point_clan, point_clan_week, type, updated_at) " +
                            "VALUES (?, ?, 1, ?, 0, 0, 4, CURDATE())",
                    cleanLeader, leaderClass, newClanId
            );
            jdbc.update("UPDATE players SET clan = ? WHERE id = ?", newClanId, leaderId);
        }

        return Map.of(
                "success", true,
                "clanId", newClanId,
                "message", "Đã thành lập bang hội '" + cleanClanName + "' (Bang chủ: " + cleanLeader + ") thành công!"
        );
    }

    public Map<String, Object> buffClan(int clanId, int coinDelta, int levelDelta, int expDelta, String alert) {
        JdbcTemplate jdbc = getJdbc();
        int updated = jdbc.update(
                "UPDATE clan SET coin = GREATEST(0, coin + ?), level = GREATEST(1, level + ?), " +
                        "exp = GREATEST(0, exp + ?), alert = COALESCE(?, alert) WHERE id = ?",
                coinDelta, levelDelta, expDelta, (alert != null && !alert.isBlank()) ? alert.trim() : null, clanId
        );
        if (updated == 0) {
            throw new IllegalArgumentException("Không tìm thấy bang hội #" + clanId);
        }
        return Map.of("success", true, "message", "Đã cập nhật chỉ số/ngân quỹ cho bang hội #" + clanId);
    }

    // =========================================================
    // 3. QUẢN LÝ DANH MỤC VẬT PHẨM / ITEM (nsoz.item)
    // =========================================================

    public Map<String, Object> listItems(String keyword, Integer type, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = Math.max(page, 0) * safeSize;
        JdbcTemplate jdbc = getJdbc();

        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            String trimmed = keyword.trim();
            where.append(" AND (CAST(id AS CHAR) = ? OR LOWER(name) LIKE LOWER(?)) ");
            params.add(trimmed);
            params.add("%" + trimmed + "%");
        }
        if (type != null) {
            where.append(" AND type = ? ");
            params.add(type);
        }

        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM item" + where, Long.class, params.toArray());

        List<Object> queryParams = new ArrayList<>(params);
        queryParams.add(safeSize);
        queryParams.add(offset);

        String sql = "SELECT id, name, type, gender, level, description, icon, isUpToUp FROM item" +
                where + " ORDER BY id ASC LIMIT ? OFFSET ?";

        List<Map<String, Object>> rows = jdbc.queryForList(sql, queryParams.toArray());
        return Map.of("content", rows, "totalElements", total != null ? total : 0L);
    }

    public Map<String, Object> updateItem(int itemId, String name, String description, int level) {
        JdbcTemplate jdbc = getJdbc();
        int updated = jdbc.update(
                "UPDATE item SET name = ?, description = ?, level = ? WHERE id = ?",
                name, description, level, itemId
        );
        if (updated == 0) {
            throw new IllegalArgumentException("Không tìm thấy vật phẩm #" + itemId);
        }
        return Map.of("success", true, "message", "Đã cập nhật thông tin vật phẩm #" + itemId);
    }

    public byte[] getItemIconBytes(int iconId) {
        Path smallBase = resolveConfiguredPath(serverDirConfig).resolve("Data").resolve("Img").resolve("Small");
        String[] candidates = {
                "2/Small" + iconId + ".png",
                "1/Small" + iconId + ".png",
                "4/Small" + iconId + ".png",
                "2/" + iconId + ".png",
                "1/" + iconId + ".png"
        };
        for (String rel : candidates) {
            Path p = smallBase.resolve(rel);
            if (Files.exists(p) && Files.isRegularFile(p)) {
                try {
                    return Files.readAllBytes(p);
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getPlayerBagDetailed(long playerId) {
        JdbcTemplate jdbc = getJdbc();
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT bag FROM players WHERE id = ?", playerId);
        if (rows.isEmpty() || rows.get(0).get("bag") == null) {
            return List.of();
        }
        String rawBag = String.valueOf(rows.get(0).get("bag"));
        if (rawBag.isBlank() || "null".equalsIgnoreCase(rawBag.trim())) {
            return List.of();
        }

        try {
            List<Map<String, Object>> bagEntries = objectMapper.readValue(rawBag, List.class);
            if (bagEntries.isEmpty()) {
                return List.of();
            }

            Set<Integer> itemIds = new HashSet<>();
            for (Map<String, Object> entry : bagEntries) {
                Object idObj = entry.get("id");
                if (idObj instanceof Number n) {
                    itemIds.add(n.intValue());
                }
            }

            Map<Integer, Map<String, Object>> itemMetaMap = new HashMap<>();
            if (!itemIds.isEmpty()) {
                String placeholders = String.join(",", Collections.nCopies(itemIds.size(), "?"));
                List<Map<String, Object>> metaRows = jdbc.queryForList(
                        "SELECT id, name, type, icon, description, level FROM item WHERE id IN (" + placeholders + ")",
                        itemIds.toArray()
                );
                for (Map<String, Object> m : metaRows) {
                    itemMetaMap.put(((Number) m.get("id")).intValue(), m);
                }
            }

            List<Map<String, Object>> enriched = new ArrayList<>();
            for (Map<String, Object> entry : bagEntries) {
                Map<String, Object> out = new LinkedHashMap<>(entry);
                Object idObj = entry.get("id");
                if (idObj instanceof Number n) {
                    Map<String, Object> meta = itemMetaMap.get(n.intValue());
                    if (meta != null) {
                        out.put("name", meta.get("name"));
                        out.put("icon", meta.get("icon"));
                        out.put("type", meta.get("type"));
                        out.put("description", meta.get("description"));
                        out.put("level", meta.get("level"));
                    }
                }
                enriched.add(out);
            }
            return enriched;
        } catch (Exception e) {
            log.warn("Không thể parse túi đồ của nhân vật #{}: {}", playerId, e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> giveItemToPlayer(long playerId, int itemId, int quantity, boolean isLock) {
        JdbcTemplate jdbc = getJdbc();
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT name, online, last_login_time, last_logout_time, bag FROM players WHERE id = ?", playerId);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy nhân vật #" + playerId);
        }
        List<Map<String, Object>> itemCheck = jdbc.queryForList("SELECT id, name FROM item WHERE id = ?", itemId);
        if (itemCheck.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy vật phẩm #" + itemId);
        }

        int qty = Math.max(quantity, 1);
        String playerName = String.valueOf(rows.get(0).get("name"));
        String itemName = String.valueOf(itemCheck.get(0).get("name"));

        try {
            // Sử dụng cơ chế gift_codes + giftcode_unpaid của Server NSO để phát quà Realtime ngay cả khi đang Online!
            String giftCode = "ERP" + Long.toString(System.currentTimeMillis(), 36).toUpperCase();
            if (giftCode.length() > 25) {
                giftCode = giftCode.substring(0, 25);
            }

            Map<String, Object> giftItem = new LinkedHashMap<>();
            giftItem.put("id", itemId);
            giftItem.put("quantity", qty);
            giftItem.put("isLock", isLock);
            giftItem.put("expire", -1);
            String itemsJson = objectMapper.writeValueAsString(List.of(giftItem));

            jdbc.update(
                    "INSERT INTO gift_codes (server_id, type, code, coin, gold, yen, items, status, expires_at, created_at, updated_at) " +
                            "VALUES (1, 0, ?, 0, 0, 0, ?, 0, DATE_ADD(NOW(), INTERVAL 1 DAY), NOW(), NOW())",
                    giftCode, itemsJson
            );
            jdbc.update("UPDATE players SET giftcode_unpaid = ? WHERE id = ?", giftCode, playerId);

            return Map.of(
                    "success", true,
                    "giftCode", giftCode,
                    "message", "Đã gửi quà " + qty + "x [" + itemName + "] tới '" + playerName + "' (Mã đồng bộ: " + giftCode + ")! Vào game sẽ nhận được ngay."
            );
        } catch (Exception e) {
            throw new RuntimeException("Không thể gửi quà cho nhân vật: " + e.getMessage(), e);
        }
    }

    private Path resolveConfiguredPath(String configuredPath) {
        Path p = Paths.get(configuredPath);
        if (p.isAbsolute()) {
            return p;
        }
        Path cwd = Paths.get("").toAbsolutePath();
        if (cwd.getFileName() != null && "erp-backend".equalsIgnoreCase(cwd.getFileName().toString())) {
            cwd = cwd.getParent();
        }
        return cwd.resolve(p);
    }
}
