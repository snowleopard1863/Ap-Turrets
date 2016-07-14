package com.snowleopard1863.APTurrets;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
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
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;

import net.milkbowl.vault.economy.Economy;
public final class TurretsMain extends JavaPlugin implements Listener {
    private PluginDescriptionFile pdfile = getDescription();
    private Logger logger = Logger.getLogger("Minecraft");
    private List<String> onTurrets = new ArrayList<String>();
    private Boolean Debug = false;
    private FileConfiguration config = getConfig();
    @SuppressWarnings("unused")
	private boolean takeFromInventory,takeFromChest,requireAmmo;
    private double costToPlace;
    private static Economy economy;
    @Override
    public void onEnable() {
        logger.info(pdfile.getName() + " v" + pdfile.getVersion() + " has been enbaled.");
        /////default configs
        config.addDefault("Cost to Place", 15000.00);
        config.addDefault("Take arrows from inventory", true);
        config.addDefault("Take arrows from chest", true);
        config.addDefault("Require Ammo", true);
        config.addDefault("Incindiary chance", 0.10);
        config.options().copyDefaults(true);
        this.saveConfig();
        /////load configs
        takeFromChest = getConfig().getBoolean("Take arrows from chest");
        takeFromInventory = getConfig().getBoolean("Take arrows from inventory");
        costToPlace = getConfig().getDouble("Cost to Place");
        requireAmmo = getConfig().getBoolean("Require Ammo");
        getServer().getPluginManager().registerEvents(this, this);
        //vault
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
    }

    @Override
    public void onLoad() {
        super.onLoad();
        logger = getLogger();
    }

    @Override
    public void onDisable() {
        if (onTurrets.size() > 0){
        	for (int i = 0; i <= onTurrets.size(); i++) {
            	String playerName = onTurrets.get(i);
            	Player player = Bukkit.getServer().getPlayer(playerName);
            	setOffTurret(player, player.getLocation());
            	onTurrets.remove(player);
        	}
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
                if (player.getInventory().getItemInMainHand().getType() == Material.MILK_BUCKET) {
                    if (Debug == true) {
                        logger.info(event.getPlayer() + " has right clicked a milk bucket!");
                    }
                    event.setCancelled(true);
                }
                if (player.getInventory().getItemInMainHand().getType() == Material.STONE_BUTTON
                && player.getInventory().contains(Material.ARROW)
                && requireAmmo){
                    fireTurret(player);
                    event.setCancelled(true);
                    if (Debug == true) {
                        logger.info(event.getPlayer() + " has shot");
                    }
                } else if (player.getInventory().getItemInMainHand().getType() == Material.STONE_BUTTON
                && player.getInventory().contains(Material.ARROW) != true) {
                    World world = player.getWorld();
                    world.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 1, 2);
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
                if ("Mounted".equalsIgnoreCase(sign.getLine(0)) && "Gun".equalsIgnoreCase(sign.getLine(1))) {
                    if (Debug == true) {
                        logger.info("A Mounted Gun sign has been clicked");
                    }
                    Location signPos = event.getClickedBlock().getLocation();
                    signPos.setPitch(event.getPlayer().getLocation().getPitch());
                    signPos.setDirection(event.getPlayer().getVelocity());
                    setOnTurret(event.getPlayer(), signPos);
                }
            }
        }
    }

    @EventHandler
    public void eventSignChanged(SignChangeEvent event){
        //get player who placed the sign
        Player player = event.getPlayer();
        //check if the sign matches the cases for a turret
        if("Mounted".equalsIgnoreCase(event.getLine(0)) && "Gun".equalsIgnoreCase(event.getLine(1)))
        {
            //check if player has permission to place a turret, than check if they have enough money to place the sign
            if(player.hasPermission("ap-turrets.place"))
            {
                if(economy.has(player,costToPlace))
                {
                    //if true charge player a configurable amount and send a message
                    economy.withdrawPlayer(player,15000);
                    player.sendMessage("Turret created");
                    if (Debug == true) {
                        logger.info("A Mounted Gun sign has been place");
                    }
                }
                else
                {
                    if (Debug == true) {
                        logger.info("A Mounted Gun sign failed to place");
                    }
                    //if false, clear the sign and return a permision error
                    event.setCancelled(true);
                    player.sendMessage("Sorry, you don't have enough money to place a turret");             
                }
            }
            else
            {
                if (Debug == true) {
                    logger.info("A Mounted Gun sign failed to place");
                }
                //if false, clear the sign and return a permision error
                event.setCancelled(true);
                player.sendMessage("Sorry, you don't have that permission");
            }
        }
    }

    public void fireTurret(Player player) 
    {
        Arrow arrow = player.launchProjectile(Arrow.class);
        arrow.setShooter(player);
        arrow.setVelocity(player.getLocation().getDirection().multiply(4));
        arrow.setBounce(false);
        //arrow.setFireTicks(500);
        arrow.setCritical(true);
        arrow.setCustomName("Bullet");
        arrow.setCustomNameVisible(false);
        arrow.setKnockbackStrength(2);
        World world = player.getWorld();
        world.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_BLAST, 1, 2);
        world.playEffect(player.getLocation(), Effect.EXPLOSION_LARGE, 0);
        player.getInventory().removeItem(new ItemStack(Material.ARROW, 0));
        // Location loc = new Location(player.getWorld(),
        // player.getLocation().getX(), player.getLocation().getY(),
        // player.getLocation().getZ());
        // loc.setPitch((float) (player.getLocation().getPitch() - 0.5));
        // player.teleport(loc);
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

    @SuppressWarnings("deprecation")
	@EventHandler
    public void onHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Arrow) {
            Arrow arrow = (Arrow) event.getEntity();
            if (arrow.getCustomName() == "Bullet") {
                if (Debug == true) {
                    logger.info("A bullet has landed");
                }
                Location arrowLoc = arrow.getLocation();
                World world = event.getEntity().getWorld();
                Location l = arrowLoc.getBlock().getLocation();
                arrow.getWorld().playEffect(l, Effect.STEP_SOUND, world.getBlockTypeIdAt(l));
                arrow.getWorld().playSound(l, Sound.ENTITY_ITEM_BREAK, 1, 2);
                //Block block = l.getBlock();
                //double rand = Math.random();
                //double incindiaryPercent = 0.20;
                //if (rand <= incindiaryPercent){
                	//if (Debug = true){
                		//logger.info("Block was set on fire");
                	//}
                	//block.setType(Material.FIRE);	
                }
                // Vector vec = arrow.getVelocity().clone().normalize();
                // vec.setX(vec.getX() * 0.1);
                // vec.setY(vec.getY() * 0.1);
                // vec.setZ(vec.getZ() * 0.1);
                // while (world.getBlockAt(l).getType() == Material.AIR) {
                // l.add(vec);
                // }
                // int glassPane = 102;
                // if (world.getBlockAt(l).getType() == Material.GLASS
                // || world.getBlockAt(l).getType() == Material.STAINED_GLASS
                // || world.getBlockAt(l).getType() ==
                // Material.STAINED_GLASS_PANE
                // || world.getBlockTypeIdAt(l) == glassPane) {
                // arrow.getWorld().playEffect(world.getBlockAt(l).getLocation(),
                // Effect.STEP_SOUND,
                // world.getBlockTypeIdAt(l));
                // world.getBlockAt(l).setType(Material.AIR);
                // }
            }
        }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Arrow) {
            Arrow a = (Arrow) e.getDamager();
            if (a.getCustomName() == "Bullet") {
                Player shooter = (Player) a.getShooter();
                if (e.getEntity().getType() == EntityType.ZOMBIE || e.getEntity().getType() == EntityType.PIG_ZOMBIE || e.getEntity().getType() == EntityType.SKELETON){
                	
                }
                else {
                	((LivingEntity) e.getEntity()).addPotionEffect(new PotionEffect(PotionEffectType.HARM, 1, 1));
                }
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

    public void setOnTurret(Player player, Location signPos) {
        if (signPos.getBlock().getType() == Material.SIGN || signPos.getBlock().getType() == Material.SIGN_POST
        || signPos.getBlock().getType() == Material.WALL_SIGN) {
            if (Debug == true) {
                logger.info("Sign detected");
            }
            Sign sign = (Sign) signPos.getBlock().getState();
            if (onTurrets.contains(player.getName())) {
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

}