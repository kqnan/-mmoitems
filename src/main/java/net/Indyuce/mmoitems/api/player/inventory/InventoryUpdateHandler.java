package net.Indyuce.mmoitems.api.player.inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type.EquipmentSlot;
import net.Indyuce.mmoitems.api.player.PlayerData;

/**
 * It's one of the most urgent systems to update. Moving everything to a new
 * class to mark everything that needs to be changed
 * 
 * @author cympe
 *
 */
public class InventoryUpdateHandler {
	private final PlayerData player;

	private final List<EquippedPlayerItem> items = new ArrayList<>();

	@Deprecated
	public ItemStack helmet = null, chestplate = null, leggings = null, boots = null, hand = null, offhand = null;

	/**
	 * Used to handle player inventory updates.
	 */
	public InventoryUpdateHandler(PlayerData player) {
		this.player = player;
	}

	/**
	 * @return All equipped MMOItems in the player's inventory. Also includes
	 *         items from custom inventory plugins like MMOInventory
	 */
	public List<EquippedPlayerItem> getEquipped() {
		return items;
	}

	/**
	 * @return If the player has in his active item inventory an item with a
	 *         specific equipment slot. This is ran by stat updates because MI
	 *         needs to check if the player is holding a WEAPON before apply an
	 *         attribute base stat value offset (attack damage/speed).
	 */
	public boolean hasItemWithType(EquipmentSlot checked) {
		for (EquippedPlayerItem item : items)
			if (item.getSlot() == checked)
				return true;
		return false;
	}

	public void updateCheck() {
		if (!player.isOnline())
			return;

		PlayerInventory inv = player.getPlayer().getInventory();
		if (isNotSame(helmet, inv.getHelmet()) || isNotSame(chestplate, inv.getChestplate()) || isNotSame(leggings, inv.getLeggings())
				|| isNotSame(boots, inv.getBoots()) || isNotSame(hand, inv.getItemInMainHand()) || isNotSame(offhand, inv.getItemInOffHand()))
			player.updateInventory();
	}

	/**
	 * Schedules an inventory update in one tick
	 */
	public void scheduleUpdate() {
		Bukkit.getScheduler().scheduleSyncDelayedTask(MMOItems.plugin, player::updateInventory);
	}

	private boolean isNotSame(ItemStack item, ItemStack item1) {
		return !Objects.equals(item, item1);
	}
}