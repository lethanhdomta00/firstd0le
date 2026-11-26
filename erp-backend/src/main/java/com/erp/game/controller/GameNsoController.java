package com.erp.game.controller;

import com.erp.game.service.GameNsoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/game-nso")
@RequiredArgsConstructor
public class GameNsoController {

    private final GameNsoService gameNsoService;

    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(gameNsoService.getOverviewStatus());
    }

    @PostMapping("/launch-client")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> launchClient() {
        return ResponseEntity.ok(gameNsoService.launchClientApp());
    }

    @PostMapping("/start-server")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> startServer() {
        return ResponseEntity.ok(gameNsoService.startGameServer());
    }

    @GetMapping("/logs")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> getGameLogs(
            @RequestParam(defaultValue = "server") String source,
            @RequestParam(defaultValue = "120") int lines) {
        return ResponseEntity.ok(gameNsoService.readGameLogs(source, lines));
    }

    // --- TÀI KHOẢN GAME ---

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        return ResponseEntity.ok(gameNsoService.listUsers(keyword, page, size));
    }

    @PostMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody Map<String, Object> body) {
        String username = String.valueOf(body.getOrDefault("username", ""));
        String password = String.valueOf(body.getOrDefault("password", ""));
        int luong = ((Number) body.getOrDefault("luong", 0)).intValue();
        int coin = ((Number) body.getOrDefault("coin", 0)).intValue();
        return ResponseEntity.ok(gameNsoService.createUser(username, password, luong, coin));
    }

    @PostMapping("/users/{id}/topup")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> topupUser(
            @PathVariable long id,
            @RequestBody Map<String, Object> body) {
        int luongDelta = ((Number) body.getOrDefault("luongDelta", 0)).intValue();
        int coinDelta = ((Number) body.getOrDefault("coinDelta", 0)).intValue();
        return ResponseEntity.ok(gameNsoService.topupUser(id, luongDelta, coinDelta));
    }

    @PatchMapping("/users/{id}/toggle-lock")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> toggleLock(@PathVariable long id) {
        return ResponseEntity.ok(gameNsoService.toggleUserLock(id));
    }

    // --- NHÂN VẬT & TIỀN TỆ ---

    @GetMapping("/players")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> listPlayers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        return ResponseEntity.ok(gameNsoService.listPlayers(keyword, page, size));
    }

    @PostMapping("/players/{id}/buff")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> buffPlayer(
            @PathVariable long id,
            @RequestBody Map<String, Object> body) {
        long xuDelta = ((Number) body.getOrDefault("xuDelta", 0)).longValue();
        long yenDelta = ((Number) body.getOrDefault("yenDelta", 0)).longValue();
        return ResponseEntity.ok(gameNsoService.buffPlayerCurrency(id, xuDelta, yenDelta));
    }

    @GetMapping("/icons/{iconId}")
    public ResponseEntity<byte[]> getItemIcon(@PathVariable int iconId) {
        byte[] bytes = gameNsoService.getItemIconBytes(iconId);
        if (bytes == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS))
                .contentType(MediaType.IMAGE_PNG)
                .body(bytes);
    }

    @GetMapping("/players/{id}/bag")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<Map<String, Object>>> getPlayerBag(@PathVariable long id) {
        return ResponseEntity.ok(gameNsoService.getPlayerBagDetailed(id));
    }

    @PostMapping("/players/{id}/give-item")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> giveItemToPlayer(
            @PathVariable long id,
            @RequestBody Map<String, Object> body) {
        int itemId = ((Number) body.getOrDefault("itemId", 0)).intValue();
        int quantity = ((Number) body.getOrDefault("quantity", 1)).intValue();
        boolean isLock = Boolean.TRUE.equals(body.get("isLock"));
        return ResponseEntity.ok(gameNsoService.giveItemToPlayer(id, itemId, quantity, isLock));
    }

    @PostMapping("/players/{id}/buff-stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> buffPlayerStats(
            @PathVariable long id,
            @RequestBody Map<String, Object> body) {
        long xuDelta = ((Number) body.getOrDefault("xuDelta", 0)).longValue();
        long yenDelta = ((Number) body.getOrDefault("yenDelta", 0)).longValue();
        int luongDelta = ((Number) body.getOrDefault("luongDelta", 0)).intValue();
        int pointDelta = ((Number) body.getOrDefault("pointDelta", 0)).intValue();
        int spointDelta = ((Number) body.getOrDefault("spointDelta", 0)).intValue();
        Integer charClass = body.get("charClass") != null ? ((Number) body.get("charClass")).intValue() : null;
        String potentialOverride = body.get("potentialOverride") != null ? String.valueOf(body.get("potentialOverride")) : null;
        long uydanhDelta = ((Number) body.getOrDefault("uydanhDelta", 0)).longValue();
        long expDelta = ((Number) body.getOrDefault("expDelta", 0)).longValue();
        return ResponseEntity.ok(gameNsoService.buffPlayerStats(
                id, xuDelta, yenDelta, luongDelta, pointDelta, spointDelta, charClass, potentialOverride, uydanhDelta, expDelta));
    }

    // --- BANG HỘI (CLAN) ---

    @GetMapping("/clans")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> listClans(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        return ResponseEntity.ok(gameNsoService.listClans(keyword, page, size));
    }

    @PostMapping("/clans")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> createClan(@RequestBody Map<String, Object> body) {
        String name = String.valueOf(body.getOrDefault("name", ""));
        String leaderName = String.valueOf(body.getOrDefault("leaderName", ""));
        int level = ((Number) body.getOrDefault("level", 1)).intValue();
        int coin = ((Number) body.getOrDefault("coin", 1000000)).intValue();
        String alert = body.get("alert") != null ? String.valueOf(body.get("alert")) : "";
        return ResponseEntity.ok(gameNsoService.createClan(name, leaderName, level, coin, alert));
    }

    @PostMapping("/clans/{id}/buff")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> buffClan(
            @PathVariable int id,
            @RequestBody Map<String, Object> body) {
        int coinDelta = ((Number) body.getOrDefault("coinDelta", 0)).intValue();
        int levelDelta = ((Number) body.getOrDefault("levelDelta", 0)).intValue();
        int expDelta = ((Number) body.getOrDefault("expDelta", 0)).intValue();
        String alert = body.get("alert") != null ? String.valueOf(body.get("alert")) : null;
        return ResponseEntity.ok(gameNsoService.buffClan(id, coinDelta, levelDelta, expDelta, alert));
    }

    // --- VẬT PHẨM / ITEM ---

    @GetMapping("/items")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> listItems(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(gameNsoService.listItems(keyword, type, page, size));
    }

    @PutMapping("/items/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> updateItem(
            @PathVariable int id,
            @RequestBody Map<String, Object> body) {
        String name = String.valueOf(body.getOrDefault("name", ""));
        String description = String.valueOf(body.getOrDefault("description", ""));
        int level = ((Number) body.getOrDefault("level", 1)).intValue();
        return ResponseEntity.ok(gameNsoService.updateItem(id, name, description, level));
    }
}
