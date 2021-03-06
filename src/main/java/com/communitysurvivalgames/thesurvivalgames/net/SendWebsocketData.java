package com.communitysurvivalgames.thesurvivalgames.net;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.communitysurvivalgames.thesurvivalgames.exception.ArenaNotFoundException;
import com.communitysurvivalgames.thesurvivalgames.managers.SGApi;
import com.communitysurvivalgames.thesurvivalgames.objects.PlayerData;
import com.communitysurvivalgames.thesurvivalgames.objects.SGArena;

public class SendWebsocketData {
	public static Map<String, String> music = new HashMap<String, String>();
	static Random rnd = new Random();

	public static void playToPlayer(final Player p, final String data) {
		Bukkit.getScheduler().scheduleSyncDelayedTask(SGApi.getPlugin(), new Runnable() {

			@Override
			public void run() {
				if (WebsocketSessionManager.getSessionManager().getSessionByName(p.getName()) != null) {
					WebsocketServer.s.sendData(WebsocketSessionManager.getSessionManager().getSessionByName(p.getName()), "sound:" + data);
				}
			}
		});

	}

	public static void playToArena(final SGArena arena, final String data) {
		Bukkit.getScheduler().scheduleSyncDelayedTask(SGApi.getPlugin(), new Runnable() {

			@Override
			public void run() {
				for (String s : arena.getPlayers()) {
					Player p = Bukkit.getPlayer(s);
					if (WebsocketSessionManager.getSessionManager().getSessionByName(p.getName()) != null) {
						WebsocketServer.s.sendData(WebsocketSessionManager.getSessionManager().getSessionByName(p.getName()), "sound:" + data);
					}
				}
			}
		});

	}

	public static void playToAll(final String data) {
		Bukkit.getScheduler().scheduleSyncDelayedTask(SGApi.getPlugin(), new Runnable() {

			@Override
			public void run() {
				for (Player p : Bukkit.getOnlinePlayers()) {
					if (WebsocketSessionManager.getSessionManager().getSessionByName(p.getName()) != null) {
						WebsocketServer.s.sendData(WebsocketSessionManager.getSessionManager().getSessionByName(p.getName()), "sound:" + data);
					}
				}
			}
		});

	}

	public static void updateArenaStatusForPlayer(final Player p) {
		Bukkit.getScheduler().scheduleSyncDelayedTask(SGApi.getPlugin(), new Runnable() {

			@Override
			public void run() {
				PlayerData data = SGApi.getPlugin().getPlayerData(p);
				WebsocketServer.s.sendData(WebsocketSessionManager.getSessionManager().getSessionByName(p.getName()), "points:" + data.getPoints());
				WebsocketServer.s.sendData(WebsocketSessionManager.getSessionManager().getSessionByName(p.getName()), "kills:" + data.getKills());
				WebsocketServer.s.sendData(WebsocketSessionManager.getSessionManager().getSessionByName(p.getName()), "wins:" + data.getWins());
				WebsocketServer.s.sendData(WebsocketSessionManager.getSessionManager().getSessionByName(p.getName()), "rank:" + data.getRank());
				SGArena a = null;
				try {
					a = SGApi.getArenaManager().getArena(p);
				} catch (ArenaNotFoundException e) {
					return;
				}
				WebsocketServer.s.sendData(WebsocketSessionManager.getSessionManager().getSessionByName(p.getName()), "arena:" + a.getId());
				WebsocketServer.s.sendData(WebsocketSessionManager.getSessionManager().getSessionByName(p.getName()), "state:" + a.getState());
				WebsocketServer.s.sendData(WebsocketSessionManager.getSessionManager().getSessionByName(p.getName()), "max:" + a.getMaxPlayers());
				WebsocketServer.s.sendData(WebsocketSessionManager.getSessionManager().getSessionByName(p.getName()), "min:" + a.getMinPlayers());
				WebsocketServer.s.sendData(WebsocketSessionManager.getSessionManager().getSessionByName(p.getName()), "players:" + a.getPlayers().size());
				WebsocketServer.s.sendData(WebsocketSessionManager.getSessionManager().getSessionByName(p.getName()), "alive:" + join(a.getPlayers(), "<br>"));
				WebsocketServer.s.sendData(WebsocketSessionManager.getSessionManager().getSessionByName(p.getName()), "dead:" + join(a.getSpectators(), "<br>"));
				WebsocketServer.s.sendData(WebsocketSessionManager.getSessionManager().getSessionByName(p.getName()), "specs:" + join(a.getSpectators(), "<br>"));
			}
		});

	}

	public static void playMusicToPlayer(Player p, String data) {
		music.remove(p.getName());
		WebsocketServer.s.sendData(WebsocketSessionManager.getSessionManager().getSessionByName(p.getName()), "music:" + data);
	}

	public static void stopMusic(Player p) {
		WebsocketServer.s.sendData(WebsocketSessionManager.getSessionManager().getSessionByName(p.getName()), "stop");
	}

	public static String join(List<String> list, String delim) {

		StringBuilder sb = new StringBuilder();

		String loopDelim = "";

		for (String s : list) {

			sb.append(loopDelim);
			sb.append(s);

			loopDelim = delim;
		}

		return sb.toString();
	}

	public static String getRandomMusic(String key) {
		File soundcolud = new File(SGApi.getPlugin().getDataFolder(), "soundcloud.yml");
		if (!soundcolud.exists()) {
			SGApi.getPlugin().saveResource("soundcloud.yml", false);
		}
		FileConfiguration cfg = YamlConfiguration.loadConfiguration(soundcolud);
		List<Integer> list = cfg.getIntegerList(key);
		return String.valueOf(list.get(rnd.nextInt(list.size())));
	}
}
