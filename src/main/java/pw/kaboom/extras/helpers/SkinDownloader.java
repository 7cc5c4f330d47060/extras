package pw.kaboom.extras.helpers;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import pw.kaboom.extras.Main;

public class SkinDownloader {
	private HttpsURLConnection skinConnection;
	private InputStreamReader skinStream;

	private String texture;
	private String signature;

	public void applySkin(Player player, String name, boolean shouldChangeName, boolean shouldSendMessage) {
		new BukkitRunnable() {
			@Override
			public void run() {
				Main.skinInProgress.add(player.getUniqueId());

				final PlayerProfile profile = player.getPlayerProfile();

				if (shouldChangeName && shouldSendMessage) {
					profile.setName(name);
					player.sendMessage("Changing your username. Please wait...");
				}

				try {
					fetchSkinData(name);
					profile.setProperty(new ProfileProperty("textures", texture, signature));

					if (!shouldChangeName && shouldSendMessage) {
						player.sendMessage("Successfully set your skin to " + name + "'s");
					}
				} catch (Exception exception) {
					try {
						skinStream.close();
						skinConnection.disconnect();
					} catch (Exception e) {
					}

					if (!shouldChangeName && shouldSendMessage) {
						player.sendMessage("A player with that username doesn't exist");
					}

					Main.skinInProgress.remove(player.getUniqueId());

					if (!shouldChangeName) {
						return;
					}
				}

				new BukkitRunnable() {
					@Override
					public void run() {
						try {
							if (player.isOnline()) {
								player.setPlayerProfile(profile);

								if (shouldChangeName && shouldSendMessage) {
									player.sendMessage("Successfully set your username to \"" + name + "\"");
								}
							}
						} catch (Exception exception) {
							// Do nothing
						}
						Main.skinInProgress.remove(player.getUniqueId());
					}
				}.runTask(JavaPlugin.getPlugin(Main.class));
			}
		}.runTaskAsynchronously(JavaPlugin.getPlugin(Main.class));
	}

	private void fetchSkinData(String playerName) throws IOException {
		final URL skinUrl = new URL("https://api.ashcon.app/mojang/v2/user/" + playerName);
		skinConnection = (HttpsURLConnection) skinUrl.openConnection();
		skinConnection.setConnectTimeout(0);

		skinStream = new InputStreamReader(skinConnection.getInputStream());
		final JsonObject responseJson = new JsonParser().parse(skinStream).getAsJsonObject();
		final JsonObject rawSkin = responseJson.getAsJsonObject("textures").getAsJsonObject("raw");
		texture = rawSkin.get("value").getAsString();
		signature = rawSkin.get("signature").getAsString();

		skinStream.close();
		skinConnection.disconnect();
	}
}