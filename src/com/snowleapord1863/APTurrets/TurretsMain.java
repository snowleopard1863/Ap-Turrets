package com.snowleapord1863.APTurrets;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.yaml.snakeyaml.Yaml;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;

import net.milkbowl.vault.economy.Economy;

public final class TurretsMain extends JavaPlugin implements Listener {
	private PluginDescriptionFile pdfile = getDescription();
	private Logger logger = Logger.getLogger("Minecraft");
	private List<String> onTurrets = new ArrayList<String>();
	private Boolean Debug = false;
	private FileConfiguration config = getConfig();
	private boolean takeFromInventory, takeFromChest, requireAmmo;
	private Material firingTool;
	private static Economy economy;
	private static TurretsMain main;

	@Override
	public void onEnable() {
		logger.info(pdfile.getName() + " v" + pdfile.getVersion() + " has been enbaled.");
		///// default configs
		config.addDefault("Debug", false);
		config.addDefault("Take arrows from inventory", true);
		config.addDefault("Take arrows from chest", true);
		config.addDefault("Firing Tool", "STONE_BUTTON");
		config.options().copyDefaults(true);
		this.saveConfig();
		///// load configs
		firingTool = Material.getMaterial(getConfig().getString("Firing Tool"));
		takeFromChest = getConfig().getBoolean("Take arrows from chest");
		takeFromInventory = getConfig().getBoolean("Take arrows from inventory");
		getServer().getPluginManager().registerEvents(this, this);
		// vault
		if (getServer().getPluginManager().getPlugin("Vault") != null) {
			RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
			if (rsp != null) {
				economy = rsp.getProvider();
				if (Debug == true)
					logger.info("Found a compatible Vault plugin.");
			} else {
				if (Debug == true)
					logger.info("Could not find compatible Vault plugin. Disabling Vault integration.");
				economy = null;
			}
		} else {
			if (Debug == true)
				logger.info("Could not find compatible Vault plugin. Disabling Vault integration.");
			economy = null;
		}
		File turretsFile = new File(TurretsMain.getInstance().getDataFolder().getAbsolutePath() + "/turrets.yml");
		InputStream input = null;
		try {
			input = new FileInputStream(turretsFile);
		} catch (FileNotFoundException e) {
			Settings.TurretName = null;
			input = null;
		}
		Yaml yaml = new Yaml();
		Map data = (Map) yaml.load(input);
		Map<String, Map> turretsMap = (Map<String, Map>) data.get("turrets");
		Settings.TurretName = turretsMap.keySet();

		Settings.SignSecondLine = new HashMap<String, String>();
		Settings.SignCost = new HashMap<String, Double>();
		Settings.SignPermission = new HashMap<String, String>();
		Settings.UsePermission = new HashMap<String, String>();
		Settings.Entity = new HashMap<String, String>();
		Settings.Velocity = new HashMap<String, Integer>();
		Settings.OnFire = new HashMap<String, Boolean>();
		Settings.RequiresAmmo = new HashMap<String, Boolean>();
		for (String turretName : turretsMap.keySet()) {
			Settings.SignSecondLine.put(turretName, (String) turretsMap.get(turretName).get("SignSecondLine"));
			Settings.SignCost.put(turretName, (Double) turretsMap.get(turretName).get("SignCost"));
			Settings.SignPermission.put(turretName, (String) turretsMap.get(turretName).get("SignPermission"));
			Settings.UsePermission.put(turretName, (String) turretsMap.get(turretName).get("UsePermission"));
			Settings.Entity.put(turretName, (String) turretsMap.get(turretName).get("Entity"));
			Settings.Velocity.put(turretName, (Integer) turretsMap.get(turretName).get("Velocity"));
			Settings.OnFire.put(turretName, (Boolean) turretsMap.get(turretName).get("OnFire"));
			Settings.RequiresAmmo.put(turretName, (Boolean) turretsMap.get(turretName).get("RequiresAmmo"));
		}
		if(input.equals(null) || data.equals(null)) {
			getInstance().logger.severe("NO TURRETS LOADED!!! SHUTTING DOWN");
			getInstance().getPluginLoader().disablePlugin(this);
		}
	}

	@Override
	public void onLoad() {
		super.onLoad();
		logger = getLogger();
		main = this;
	}

	@Override
	public void onDisable() {
		for (int i = 0; i <= onTurrets.size(); i++) {
			String playerName = onTurrets.get(i);
			Player player = Bukkit.getServer().getPlayer(playerName);
			setOffTurret(player, player.getLocation());
			onTurrets.remove(player);
		}
		logger.info(pdfile.getName() + " v" + pdfile.getVersion() + " has been disabled.");
	}

	@EventHandler
	public void onClick(PlayerInteractEvent event) {
		if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			if (Debug == true) {
				logger.info(event.getPlayer() + " has right clicked");
			}
			Player player = event.getPlayer();
			if (onTurrets.contains(player.getName()) && player.hasPermission("ap-turrets.use")) {
				if (Debug == true) {
					logger.info(event.getPlayer() + " is on a turret");
				}
				if (event.getPlayer().getItemInHand().getType() == Material.MILK_BUCKET) {
					if (Debug == true) {
						logger.info(event.getPlayer() + " has right clicked a milk bucket!");
					}
					event.setCancelled(true);
				}
				if (player.getItemInHand().getType() == firingTool && player.getInventory().contains(Material.ARROW)
						&& requireAmmo) {
					fireTurret(player);
					event.setCancelled(true);
					if (Debug == true) {
						logger.info(event.getPlayer() + " has shot");
					}
				} else if (player.getItemInHand().getType() == Material.STONE_BUTTON
						&& player.getInventory().contains(Material.ARROW) != true) {
					World world = player.getWorld();
					world.playSound(player.getLocation(), Sound.CLICK, 1, 2);
					event.setCancelled(true);
					if (Debug == true) {
						logger.info(event.getPlayer() + " is out of ammo");
					}
				}
			}
		}
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			if (Debug == true) {
				logger.info("A block has been right clicked");
			}
			if (event.getClickedBlock().getType() == Material.SIGN_POST
					|| event.getClickedBlock().getType() == Material.WALL_SIGN
					|| event.getClickedBlock().getType() == Material.SIGN) {
				if (Debug == true) {
					logger.info("A sign was clicked");
				}
				Sign sign = (Sign) event.getClickedBlock().getState();
				if (getTurretFromString(org.bukkit.ChatColor.stripColor(sign.getLine(0))) != null
						&& sign.getLine(1) == Settings.SignSecondLine.get(sign.getLine(0))) {
					// Valid sign prompt for ship command.
					if (event.getPlayer()
							.hasPermission("apturrets.use." + Settings.UsePermission.get(sign.getLine(0)))) {
						Location signPos = event.getClickedBlock().getLocation();
						signPos.setPitch(event.getPlayer().getLocation().getPitch());
						signPos.setDirection(event.getPlayer().getVelocity());
						setOnTurret(event.getPlayer(), sign.getLine(0), signPos);
					}
				}
			}
		}
	}

	@EventHandler
	public void eventSignChanged(SignChangeEvent event) {
		// get player who placed the sign
		Player player = event.getPlayer();
		// check if the sign matches the cases for a turret
		if (getTurretFromString(org.bukkit.ChatColor.stripColor(event.getLine(0))) != null
				&& event.getLine(1) == Settings.SignSecondLine.get(event.getLine(0))) {
			// check if player has permission to place a turret, than check if
			// they have enough money to place the sign
			if (player.hasPermission("ap-turrets.place." + Settings.SignPermission.get(event.getLine(0)))) {
				if (economy.has(player, Settings.SignCost.get(event.getLine(0)))) {
					// if true charge player a configurable amount and send a
					// message
					economy.withdrawPlayer(player, Settings.SignCost.get(event.getLine(0)));
					player.sendMessage("Turret created");
					if (Debug == true) {
						logger.info("A Mounted Gun sign has been place");
					}
				} else {
					if (Debug == true) {
						logger.info("A Mounted Gun sign failed to place");
					}
					// if false, clear the sign and return a permision error
					for (int i = 0; i < 4; i++)
						event.setLine(i, "");
					((Sign) event).update();
					player.sendMessage("Sorry, you don't have enough money to place a turret");
				}
			} else {
				if (Debug == true) {
					logger.info("A Mounted Gun sign failed to place");
				}
				// if false, clear the sign and return a permision error
				for (int i = 0; i < 4; i++)
					event.setLine(i, "");
				((Sign) event).update();
				player.sendMessage("Sorry, you don't have that permission");
			}
		}
	}

	public void fireTurret(Player player) {
		if (player.getLocation().getBlock().getType() == Material.SIGN_POST
				|| player.getLocation().getBlock().getType() == Material.WALL_SIGN
				|| player.getLocation().getBlock().getType() == Material.SIGN) {
			Sign sign = (Sign) player.getLocation().getBlock().getState();
			String type = sign.getLine(0);
			Class entity = null;
			try {
				entity = Class.forName(type);
				Entity projectile = player.getWorld().spawn(player.getLocation(), entity);
				if (entity == TNTPrimed.class) {
					((TNTPrimed) projectile).setFuseTicks(100);
					player.getInventory().removeItem(new ItemStack(Material.TNT, 0));
					for (int i = 0; i < player.getInventory().getSize(); i++) {
						ItemStack itm = player.getInventory().getItem(i);
						if (itm != null && itm.getType().equals(Material.TNT)) {
							int amt = itm.getAmount() - 1;
							itm.setAmount(amt);
							player.getInventory().setItem(i, amt > 0 ? itm : null);
							player.updateInventory();
							break;
						}
					}
				}
				projectile.setVelocity(player.getLocation().getDirection().multiply(Settings.Velocity.get(type)));
				if (Settings.OnFire.get(type) == true) {
					projectile.setFireTicks(500);

				}
				if (entity == Arrow.class) {
					((Arrow) projectile).setBounce(false);
					((Arrow) projectile).setCritical(true);
					((Arrow) projectile).setKnockbackStrength(2);
					player.getInventory().removeItem(new ItemStack(Material.ARROW, 0));
					for (int i = 0; i < player.getInventory().getSize(); i++) {
						ItemStack itm = player.getInventory().getItem(i);
						if (itm != null && itm.getType().equals(Material.ARROW)) {
							int amt = itm.getAmount() - 1;
							itm.setAmount(amt);
							player.getInventory().setItem(i, amt > 0 ? itm : null);
							player.updateInventory();
							break;
						}
					}
				}
				projectile.setCustomName("Bullet");
				projectile.setCustomNameVisible(false);
				World world = player.getWorld();
				world.playSound(player.getLocation(), Sound.GHAST_FIREBALL, 1, 2);
				// world.playEffect(player.getLocation(),
				// Effect.EXPLOSION_LARGE, 0);;
				// Location loc = new Location(player.getWorld(),
				// player.getLocation().getX(), player.getLocation().getY(),
				// player.getLocation().getZ());
				// loc.setPitch((float) (player.getLocation().getPitch() -
				// 0.5));
				// player.teleport(loc);
			} catch (ClassNotFoundException e) {
				getInstance().logger.info("Error, no entity with the name of " + Settings.Entity.get(type) + " exists");
			}
		}
		if (Debug == true) {
			logger.info("BAM");
		}
	}

	@EventHandler
	public void onPlayerToggleSneakEvent(PlayerToggleSneakEvent event) {
		Player player = event.getPlayer();
		if (Debug == true) {
			logger.info(player + " sneaked");
		}
		if (player.isSneaking() && onTurrets.contains(player.getName())) {
			setOffTurret(player, player.getLocation());
			if (Debug == true) {
				logger.info(player + " got out of their turret");
			}
		}
	}

	@EventHandler
	public void onDamage(EntityDamageByEntityEvent e) {
		if (e.getDamager() instanceof Arrow) {
			Arrow a = (Arrow) e.getDamager();
			if (a.getCustomName() == "Bullet") {
				Player shooter = (Player) a.getShooter();
				((LivingEntity) e.getEntity()).addPotionEffect(new PotionEffect(PotionEffectType.HARM, 1, 1));
				if (Debug == true) {
					logger.info(e.getEntity() + " was shot by " + shooter.getName());
				}
			}
		}
	}

	@EventHandler
	public void onEntityDamageEvent(EntityDamageEvent event) {
		if (Debug == true) {
			logger.info("An entity was damaged");
		}
		if (event.getEntity() instanceof Player) {
			if (Debug == true) {
				logger.info("It was a player");
			}
			Player player = (Player) event.getEntity();
			if (onTurrets.contains(player.getName())) {
				if (Debug == true) {
					logger.info("on a turret");
				}
				setOffTurret(player, player.getLocation());
			}
		}
	}

	public void setOnTurret(Player player, String turretType, Location signPos) {
		if (signPos.getBlock().getType() == Material.SIGN || signPos.getBlock().getType() == Material.SIGN_POST
				|| signPos.getBlock().getType() == Material.WALL_SIGN) {
			if (Debug == true) {
				logger.info("Sign detected");
			}
			Sign sign = (Sign) signPos.getBlock().getState();
			if (player.getName() != sign.getLine(2)) {
				player.sendMessage("Sorry, that turret is being used");
				if (Debug == true) {
					logger.info("1 player per turret");
				}
			} else {
				if (Debug == true) {
					logger.info(player.getName() + " is now on a turret");
				}
				sign.setLine(2, player.getName());
				sign.update();
				onTurrets.add(player.getName());
				signPos.add(0.5, 0, 0.5);
				player.teleport(signPos);
				player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 1000000, 6));
				player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 1000000, 200));
			}
		} else {
			logger.warning("Sign not found!");
		}
	}

	public void setOffTurret(Player player, Location signPos) {
		if (Debug == true) {
			logger.info(player.getName() + " is being taken off a turret");
		}
		onTurrets.remove(player.getName());
		if (signPos.getBlock().getType() == Material.SIGN || signPos.getBlock().getType() == Material.SIGN_POST
				|| signPos.getBlock().getType() == Material.WALL_SIGN) {
			if (Debug == true) {
				logger.info("sign found and updated");
			}
			Sign sign = (Sign) signPos.getBlock().getState();
			sign.setLine(2, "");
			sign.update();
		} else {
			logger.warning("Sign not found!");
		}
		signPos.subtract(-0.5, 0, -0.5);
		player.removePotionEffect(PotionEffectType.SLOW);
		player.removePotionEffect(PotionEffectType.JUMP);
	}

	public static TurretsMain getInstance() {
		return main;
	}

	private String getTurretFromString(String s) {

		for (String t : Settings.TurretName) {
			if (s.equalsIgnoreCase(t)) {
				return t;
			}
		}

		return null;
	}
}