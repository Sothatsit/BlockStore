package net.sothatsit.blockstore;

import java.lang.reflect.Array;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.sothatsit.blockstore.chunkstore.*;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BlockStoreCommand implements CommandExecutor {

    private static final Set<Material> transparentBlocks = new HashSet<Material>() {{
        add(Material.AIR);
    }};

    @Override
    public boolean onCommand(CommandSender sender, Command command, String labels, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(colour("&4Invalid Arguments > &c/blockstore <check:info:reload>"));
            return true;
        }

        if (args[0].equalsIgnoreCase("check")) {
            if (!sender.isOp() && !sender.hasPermission("blockstore.check")) {
                sender.sendMessage(colour("&4Error > &cYou do not have permission to run this command"));
                return true;
            }

            if (!(sender instanceof Player)) {
                sender.sendMessage(colour("&4Error > &cYou must be a player to run this command"));
                return true;
            }

            Player player = (Player) sender;

            Block block;

            if(args.length == 1) {
                block = player.getTargetBlock(transparentBlocks, 80);

                if (block == null || block.getType() == Material.AIR) {
                    sender.sendMessage(colour("&4Error > &cYou must be looking at a block within 80 blocks of you"));
                    return true;
                }
            } else if(args.length > 2) {
                sender.sendMessage(colour("&4Invalid Arguments > &c/blockstore check [feet:head]"));
                return true;
            } else if(args[1].equalsIgnoreCase("feet")) {
                block = player.getLocation().getBlock();

                if (block == null) {
                    sender.sendMessage(colour("&4Error > &cYou must be standing in a block"));
                    return true;
                }
            } else if(args[1].equalsIgnoreCase("head")) {
                block = player.getEyeLocation().getBlock();

                if (block == null) {
                    sender.sendMessage(colour("&4Error > &cYour head must be in a block"));
                    return true;
                }
            } else {
                sender.sendMessage(colour("&4Invalid Arguments > &c/blockstore check [feet:head]"));
                return true;
            }

            BlockStoreApi.retrieveIsPlaced(block, isPlaced -> {
                sender.sendMessage(colour("&7Your target block is " + (isPlaced ? "&aPlaced" : "&cNatural")));

                Map<String, Map<String, Object>> metadata = BlockStoreApi.getAllBlockMeta(block);

                if(metadata.size() == 0)
                    return;

                sender.sendMessage(colour("&6Metadata&8:"));

                for (Entry<String, Map<String, Object>> pluginMeta : metadata.entrySet()) {
                    String plugin = pluginMeta.getKey();

                    sender.sendMessage(colour("  &6") + plugin + colour("&8:"));

                    for (Entry<String, Object> metaValue : pluginMeta.getValue().entrySet()) {
                        String key = metaValue.getKey();
                        String value = toString(metaValue.getValue());

                        sender.sendMessage(colour("    &e") + key + colour("&8: &7") + value);
                    }
                }
            });
            return true;
        }

        if (args[0].equalsIgnoreCase("info")) {
            if (!sender.isOp() && !sender.hasPermission("blockstore.info")) {
                sender.sendMessage(colour("&4Error > &cYou do not have permission to run this command"));
                return true;
            }

            sender.sendMessage(colour("&6BlockStore World Info&8:"));

            for (ChunkManager manager : BlockStore.getInstance().getChunkManagers().values()) {
                String name = manager.getWorld().getName();
                int stores = manager.getChunkStores().size();
                int chunks = manager.getWorld().getLoadedChunks().length;
                sender.sendMessage(colour("   &e" + name + ": &f" + stores + " stores loaded (" + chunks + " chunks loaded)"));
            }

            return true;
        }

        if(args[0].equalsIgnoreCase("reload")) {
            if (!sender.isOp() && !sender.hasPermission("blockstore.reload")) {
                sender.sendMessage(colour("&4Error > &cYou do not have permission to run this command"));
                return true;
            }

            BlockStoreConfig config = BlockStore.getInstance().getBlockStoreConfig();

            config.reload();

            sender.sendMessage(colour("&7Config reloaded."));
            return true;
        }

        // Used to add test metadata on to arbitrary blocks
        /*if(args[0].equalsIgnoreCase("test")) {
            sender.sendMessage(colour("&cThis command should not be available in a release version."));

            Player player = (Player) sender;
            Block block = player.getTargetBlock(transparentBlocks, 80);

            if (block == null || block.getType() == Material.AIR) {
                sender.sendMessage(colour("&4Error > &cYou must be looking at a block within 80 blocks of you"));
                return true;
            }

            BlockStore plugin = BlockStore.getInstance();

            BlockStoreApi.setPlaced(block, true);
            BlockStoreApi.setBlockMeta(block, plugin, "apple", true);
            BlockStoreApi.setBlockMeta(block, plugin, "number", Math.random());
            BlockStoreApi.setBlockMeta(block, plugin, "args", args);

            return true;
        }*/

        sender.sendMessage(colour("&4Invalid Arguments > &c/blockstore <check:info:reload>"));
        return true;
    }
    
    public static String colour(String str) {
        return ChatColor.translateAlternateColorCodes('&', str);
    }
    
    public static String toString(Object obj) {
        if (obj == null) {
            return "null";
        }
        
        if (obj.getClass().isArray()) {
            StringBuilder builder = new StringBuilder();
            
            builder.append('[');
            
            for (int i = 0; i < Array.getLength(obj); i++) {
                if (i != 0) {
                    builder.append(", ");
                }
                
                builder.append(toString(Array.get(obj, i)));
            }
            
            builder.append(']');
            
            return builder.toString();
        } else {
            return obj.toString();
        }
    }
    
}
