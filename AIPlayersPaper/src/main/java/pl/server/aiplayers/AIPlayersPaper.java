package pl.server.aiplayers;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AIPlayersPaper extends JavaPlugin implements Listener, CommandExecutor {
    private final Map<String, NPC> bots = new ConcurrentHashMap<>();
    private final Map<String, List<String>> histories = new ConcurrentHashMap<>();
    private HttpClient http;

    @Override public void onEnable() {
        saveDefaultConfig();
        http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("aiplayers")).setExecutor(this);
        getLogger().info("AIPlayersPaper uruchomiony. Wymaga Citizens i endpointu AI.");
        // Nie odtwarzamy automatycznie NPC, aby uniknąć duplikatów po restarcie.
    }

    @Override public void onDisable() {
        // NPC pozostają zarządzane przez Citizens; plugin nie usuwa ich przy wyłączeniu.
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("aiplayers.admin")) {
            sender.sendMessage(ChatColor.RED + "Nie masz uprawnień.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "/aiplayers create <nick>");
            sender.sendMessage(ChatColor.YELLOW + "/aiplayers remove <nick>");
            sender.sendMessage(ChatColor.YELLOW + "/aiplayers list");
            sender.sendMessage(ChatColor.YELLOW + "/aiplayers follow <nick> <gracz>");
            sender.sendMessage(ChatColor.YELLOW + "/aiplayers stop <nick>");
            sender.sendMessage(ChatColor.YELLOW + "/aiplayers reload");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create" -> {
                if (args.length < 2) { sender.sendMessage("Użycie: /aiplayers create <nick>"); return true; }
                String name = args[1];
                if (bots.containsKey(name.toLowerCase(Locale.ROOT))) { sender.sendMessage("Bot o tej nazwie już jest na liście pluginu."); return true; }
                if (bots.size() >= getConfig().getInt("max-bots", 5)) { sender.sendMessage("Osiągnięto limit botów: " + getConfig().getInt("max-bots", 5)); return true; }
                if (!(sender instanceof Player p)) { sender.sendMessage("Tę komendę wykonaj w grze."); return true; }
                NPC npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, name);
                npc.spawn(p.getLocation());
                bots.put(name.toLowerCase(Locale.ROOT), npc);
                histories.put(name.toLowerCase(Locale.ROOT), new CopyOnWriteArrayList<>());
                sender.sendMessage(ChatColor.GREEN + "Utworzono NPC " + name + ". Napisz: " + name + ", cześć!");
            }
            case "remove" -> {
                if (args.length < 2) return usage(sender);
                NPC npc = bots.remove(args[1].toLowerCase(Locale.ROOT));
                histories.remove(args[1].toLowerCase(Locale.ROOT));
                if (npc == null) sender.sendMessage("Nie znaleziono bota utworzonego w tej sesji pluginu.");
                else { npc.destroy(); sender.sendMessage("Usunięto NPC " + args[1]); }
            }
            case "list" -> sender.sendMessage("Boty: " + (bots.isEmpty() ? "brak" : String.join(", ", bots.keySet())));
            case "follow" -> {
                if (args.length < 3) return usage(sender);
                NPC npc = bots.get(args[1].toLowerCase(Locale.ROOT));
                Player target = Bukkit.getPlayerExact(args[2]);
                if (npc == null || target == null || !npc.isSpawned()) sender.sendMessage("Nie znaleziono bota lub gracza.");
                else { npc.getNavigator().setTarget(target, true); sender.sendMessage("Bot podąża za " + target.getName()); }
            }
            case "stop" -> {
                if (args.length < 2) return usage(sender);
                NPC npc = bots.get(args[1].toLowerCase(Locale.ROOT));
                if (npc == null) sender.sendMessage("Nie znaleziono bota.");
                else { npc.getNavigator().cancelNavigation(); sender.sendMessage("Bot zatrzymany."); }
            }
            case "reload" -> { reloadConfig(); sender.sendMessage("Konfiguracja przeładowana."); }
            default -> usage(sender);
        }
        return true;
    }

    private boolean usage(CommandSender s) {
        s.sendMessage(ChatColor.YELLOW + "Użycie: /aiplayers <create|remove|list|follow|stop|reload>");
        return true;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        String message = event.getMessage();
        for (String key : bots.keySet()) {
            NPC npc = bots.get(key);
            if (npc == null) continue;
            String displayName = npc.getName();
            String prefix = displayName + ",";
            if (getConfig().getBoolean("chat.trigger-by-name", true) &&
                message.regionMatches(true, 0, prefix, 0, prefix.length())) {
                String prompt = message.substring(prefix.length()).trim();
                if (!prompt.isEmpty()) {
                    event.setCancelled(true);
                    askBot(key, displayName, event.getPlayer().getName(), prompt);
                    return;
                }
            }
        }
    }

    private void askBot(String key, String botName, String playerName, String prompt) {
        List<String> history = histories.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>());
        history.add("Gracz " + playerName + ": " + prompt);
        while (history.size() > 12) history.remove(0);

        String system = getConfig().getString("chat.system-prompt", "Rozmawiaj po polsku jak gracz Minecrafta.");
        StringBuilder messages = new StringBuilder("[{\"role\":\"system\",\"content\":\"")
            .append(jsonEscape(system + " Nazywasz się " + botName + "."))
            .append("\"}");
        for (String line : history) {
            messages.append(",{\"role\":\"user\",\"content\":\"").append(jsonEscape(line)).append("\"}");
        }
        messages.append("]");
        String body = "{\"model\":\"" + jsonEscape(getConfig().getString("ai.model", "mistral")) +
            "\",\"temperature\":" + getConfig().getDouble("ai.temperature", 0.8) +
            ",\"max_tokens\":" + getConfig().getInt("ai.max-tokens", 180) +
            ",\"messages\":" + messages + "}";

        CompletableFuture.runAsync(() -> {
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(getConfig().getString("ai.endpoint")))
                    .timeout(Duration.ofSeconds(40))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
                String apiKey = getConfig().getString("ai.api-key", "");
                if (apiKey != null && !apiKey.isBlank()) builder.header("Authorization", "Bearer " + apiKey);
                HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() < 200 || response.statusCode() >= 300)
                    throw new IllegalStateException("HTTP " + response.statusCode() + ": " + response.body());
                String answer = extractContent(response.body());
                history.add("Bot " + botName + ": " + answer);
                Bukkit.getScheduler().runTask(this, () ->
                    Bukkit.broadcastMessage(ChatColor.GREEN + "<" + botName + "> " + ChatColor.WHITE + answer));
            } catch (Exception ex) {
                getLogger().warning("Nie udało się uzyskać odpowiedzi AI: " + ex.getMessage());
                Bukkit.getScheduler().runTask(this, () ->
                    Bukkit.broadcastMessage(ChatColor.GREEN + "<" + botName + "> " + ChatColor.GRAY +
                        "Chwilowo nie mogę odpowiedzieć. Spróbuj za moment."));
            }
        });
    }

    private String extractContent(String json) {
        Matcher m = Pattern.compile("\"content\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").matcher(json);
        String found = null;
        while (m.find()) found = m.group(1);
        if (found == null) throw new IllegalStateException("Brak pola choices[].message.content w odpowiedzi API.");
        return found.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
            .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
