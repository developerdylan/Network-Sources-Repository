/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.ddylan.library.menu.Button
 *  com.ddylan.library.util.TimeUtils
 *  net.minecraft.util.org.apache.commons.lang3.StringUtils
 *  org.bukkit.ChatColor
 *  org.bukkit.Material
 *  org.bukkit.entity.Player
 *  org.bukkit.event.inventory.ClickType
 */
package com.ddylan.hydrogen.commands.grant.menu;

import com.ddylan.hydrogen.connection.RequestHandler;
import com.ddylan.hydrogen.connection.RequestResponse;
import com.ddylan.hydrogen.rank.Rank;
import com.ddylan.hydrogen.server.ServerGroup;
import com.ddylan.library.menu.Button;
import com.ddylan.library.util.ItemBuilder;
import com.ddylan.library.util.TimeUtil;
import com.google.common.collect.Lists;
import net.minecraft.util.org.apache.commons.lang3.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class GrantButton extends Button {

    private final Rank rank;
    private final String targetName;
    private final UUID targetUUID;
    private final String reason;
    private final ScopesMenu parent;
    private final List<ServerGroup> scopes;
    private final int duration;

    public String getName(Player player) {
        return ChatColor.GREEN + "Confirm and Grant";
    }

    public List<String> getDescription(Player player) {
        ArrayList description = Lists.newArrayList();
        description.add(ChatColor.GRAY.toString() + ChatColor.STRIKETHROUGH + StringUtils.repeat('-', 30));
        description.add(ChatColor.BLUE + "Click to add the " + ChatColor.WHITE + this.rank.getFormattedName() + ChatColor.BLUE + " to " + ChatColor.WHITE + this.targetName + ChatColor.BLUE + ".");
        if (this.parent.isGlobal()) {
            description.add(ChatColor.BLUE + "This grant will be " + ChatColor.WHITE + "Global" + ChatColor.BLUE + ".");
        } else {
            List scopes = this.scopes.stream().map(ServerGroup::getId).collect(Collectors.toList());
            description.add(ChatColor.BLUE + "This grant will apply on: " + ChatColor.WHITE + scopes.toString());
        }
        description.add(ChatColor.BLUE + "Reasoning: " + ChatColor.WHITE + this.reason);
        description.add(ChatColor.BLUE + "Duration: " + ChatColor.WHITE + (this.duration > 0 ? TimeUtil.formatIntoDetailedString((int)this.duration) : "Permanent"));
        description.add(ChatColor.GRAY.toString() + ChatColor.STRIKETHROUGH + StringUtils.repeat('-', 30));
        return description;
    }

    public Material getMaterial(Player player) {
        return Material.DIAMOND_SWORD;
    }

    public byte getDamageValue(Player player) {
        return 0;
    }

    public void clicked(Player player, int i, ClickType clickType) {
        this.grant(this.targetUUID, this.targetName, this.reason, this.scopes, this.rank, this.duration, player);
        player.closeInventory();
    }

    private void grant(UUID user, String targetName, String reason, List<ServerGroup> scopes, Rank rank, int expiresIn, Player sender) {
        ArrayList finalScopes = Lists.newArrayList();
        finalScopes.addAll(scopes.stream().map(ServerGroup::getId).collect(Collectors.toList()));
        HashMap<String, Object> body = new HashMap<String, Object>();
        body.put("user", user);
        body.put("reason", reason);
        body.put("scopes", finalScopes.toArray(new String[finalScopes.size()]));
        body.put("rank", rank.getId());
        if (expiresIn > 0) {
            body.put("expiresIn", expiresIn);
        }
        body.put("addedBy", sender.getUniqueId().toString());
        body.put("addedByIp", sender.getAddress().getAddress().getHostAddress());
        RequestResponse response = RequestHandler.post("/grants", body);
        if (response.wasSuccessful()) {
            sender.sendMessage(ChatColor.GREEN + "Successfully granted " + ChatColor.WHITE + targetName + ChatColor.GREEN + " the " + ChatColor.WHITE + rank.getFormattedName() + ChatColor.GREEN + " rank.");
            this.parent.setComplete(true);
        } else {
            sender.sendMessage(ChatColor.RED + response.getErrorMessage());
        }
    }

    public GrantButton(Rank rank, String targetName, UUID targetUUID, String reason, ScopesMenu parent, List<ServerGroup> scopes, int duration) {
        this.rank = rank;
        this.targetName = targetName;
        this.targetUUID = targetUUID;
        this.reason = reason;
        this.parent = parent;
        this.scopes = scopes;
        this.duration = duration;
    }

    @Override
    public ItemStack getButtonItem(Player player) {
        return new ItemBuilder(getMaterial(player)).lore(getDescription(player)).name(getName(player)).build();
    }

}
