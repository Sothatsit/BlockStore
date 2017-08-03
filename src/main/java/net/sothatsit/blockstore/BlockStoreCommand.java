package net.sothatsit.blockstore;

import java.lang.reflect.Array;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.sothatsit.blockstore.chunkstore.BlockMeta;
import net.sothatsit.blockstore.chunkstore.ChunkManager;
import net.sothatsit.blockstore.chunkstore.ChunkStore;
import net.sothatsit.blockstore.chunkstore.NameStore;

public class BlockStoreCommand implements CommandExecutor {
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String labels, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(translate("&4Invalid Arguments > &c/blockstore <check:info>"));
            return true;
        }

        if (args[0].equalsIgnoreCase("check")) {
            if (!sender.isOp() && !sender.hasPermission("blockstore.check")) {
                sender.sendMessage(translate("&4Error > &cYou do not have permission to run this command"));
                return true;
            }

            if (!(sender instanceof Player)) {
                sender.sendMessage(translate("&4Error > &cYou must be a player to run this command"));
                return true;
            }

            Player p = (Player) sender;

            HashSet<Material> transparentBlocks = new HashSet<>();

            transparentBlocks.add(Material.AIR);

            Block target = p.getTargetBlock(transparentBlocks, 80);

            if (target == null) {
                sender.sendMessage(translate("&4Error > &cYou must be looking at a block within 80 blocks of you"));
                return true;
            }

            ChunkManager manager = BlockStore.getChunkManager(target.getLocation());

            NameStore names = manager.getNameStore();
            ChunkStore store = manager.getChunkStore(target.getLocation());

            boolean blockValue = store.getValue(target.getLocation());
            sender.sendMessage(translate("&7Your target block is " + (blockValue ? "&aPlaced" : "&cNatural")));

            BlockMeta meta = store.getMeta(target.getLocation());

            if (meta.getKeys().size() != 0) {
                sender.sendMessage(translate("&6Metadata&8:"));

                for (Entry<Integer, Map<Integer, Object>> pluginMeta : meta.getRaw().entrySet()) {
                    sender.sendMessage(translate("  &6" + names.fromInt(pluginMeta.getKey()) + "&8:"));

                    for (Entry<Integer, Object> entry : pluginMeta.getValue().entrySet()) {
                        sender.sendMessage(translate("    &e" + names.fromInt(entry.getKey()) + "&8: &7" + toString(entry.getValue())));
                    }
                }
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("info")) {
            if (!sender.isOp() && !sender.hasPermission("blockstore.info")) {
                sender.sendMessage(translate("&4Error > &cYou do not have permission to run this command"));
                return true;
            }

            sender.sendMessage(translate("&6BlockStore World Info&8:"));

            for (ChunkManager manager : BlockStore.getInstance().getChunkManagers().values()) {
                String name = manager.getWorld().getName();
                int stores = manager.getChunkStores().size();
                int chunks = manager.getWorld().getLoadedChunks().length;
                sender.sendMessage(translate("   &e" + name + ": &f" + stores + " stores loaded (" + chunks + " chunks)"));
            }

            return true;
        }

        sender.sendMessage(translate("&4Invalid Arguments > &c/blockstore <check:info>"));
        return true;
    }
    
    public static String translate(String str) {
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
