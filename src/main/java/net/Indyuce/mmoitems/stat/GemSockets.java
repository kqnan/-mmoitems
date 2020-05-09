package net.Indyuce.mmoitems.stat;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.ConfigFile;
import net.Indyuce.mmoitems.api.edition.StatEdition;
import net.Indyuce.mmoitems.api.item.MMOItem;
import net.Indyuce.mmoitems.api.item.ReadMMOItem;
import net.Indyuce.mmoitems.api.item.build.MMOItemBuilder;
import net.Indyuce.mmoitems.api.itemgen.RandomStatData;
import net.Indyuce.mmoitems.gui.edition.EditionInventory;
import net.Indyuce.mmoitems.stat.data.GemSocketsData;
import net.Indyuce.mmoitems.stat.data.GemstoneData;
import net.Indyuce.mmoitems.stat.data.type.StatData;
import net.Indyuce.mmoitems.stat.type.ItemStat;
import net.mmogroup.mmolib.api.item.ItemTag;
import net.mmogroup.mmolib.api.util.AltChar;

public class GemSockets extends ItemStat {
	public GemSockets() {
		super("GEM_SOCKETS", new ItemStack(Material.EMERALD), "Gem Sockets", new String[] { "The amount of gem", "sockets your weapon has." },
				new String[] { "piercing", "slashing", "blunt", "offhand", "range", "tool", "armor", "accessory" });
	}

	@Override
	@SuppressWarnings("unchecked")
	public GemSocketsData whenInitialized(Object object) {
		Validate.isTrue(object instanceof List<?>, "Must specify a string list");
		return new GemSocketsData((List<String>) object);
	}

	@Override
	public RandomStatData whenInitializedGeneration(Object object) {
		return whenInitialized(object);
	}

	@Override
	public void whenApplied(MMOItemBuilder item, StatData data) {
		GemSocketsData sockets = (GemSocketsData) data;
		item.addItemTag(new ItemTag("MMOITEMS_GEM_STONES", sockets.toJson().toString()));

		String empty = ItemStat.translate("empty-gem-socket"), filled = ItemStat.translate("filled-gem-socket");
		List<String> lore = new ArrayList<>();
		sockets.getGemstones().forEach(gem -> lore.add(filled.replace("#", gem.getName())));
		sockets.getEmptySlots().forEach(slot -> lore.add(empty.replace("#", slot)));
		item.getLore().insert("gem-stones", lore);
	}

	@Override
	public void whenLoaded(ReadMMOItem mmoitem) {
		if (mmoitem.getNBT().hasTag("MMOITEMS_GEM_STONES"))
			try {
				JsonObject object = new JsonParser().parse(mmoitem.getNBT().getString("MMOITEMS_GEM_STONES")).getAsJsonObject();
				GemSocketsData sockets = new GemSocketsData(toList(object.getAsJsonArray("EmptySlots")));

				JsonArray array = object.getAsJsonArray("Gemstones");
				array.forEach(element -> sockets.add(new GemstoneData(element.getAsJsonObject())));

				mmoitem.setData(this, sockets);
			} catch (JsonSyntaxException exception) {
				/*
				 * OLD ITEM WHICH MUST BE UPDATED.
				 */
			}
	}

	private List<String> toList(JsonArray array) {
		List<String> list = new ArrayList<>();
		array.forEach(str -> list.add(str.getAsString()));
		return list;
	}

	@Override
	public boolean whenClicked(EditionInventory inv, InventoryClickEvent event) {
		ConfigFile config = inv.getEdited().getType().getConfigFile();
		if (event.getAction() == InventoryAction.PICKUP_ALL)
			new StatEdition(inv, ItemStat.GEM_SOCKETS).enable("Write in the chat the COLOR of the gem socket you want to add.");

		if (event.getAction() == InventoryAction.PICKUP_HALF) {
			if (config.getConfig().getConfigurationSection(inv.getEdited().getId()).contains(getPath())) {
				List<String> lore = config.getConfig().getStringList(inv.getEdited().getId() + "." + getPath());
				if (lore.size() < 1)
					return true;

				String last = lore.get(lore.size() - 1);
				lore.remove(last);
				config.getConfig().set(inv.getEdited().getId() + "." + getPath(), lore);
				inv.registerItemEdition(config);
				inv.open();
				inv.getPlayer().sendMessage(MMOItems.plugin.getPrefix() + "Successfully removed '" + ChatColor.translateAlternateColorCodes('&', last)
						+ ChatColor.GRAY + "'.");
			}
		}
		return true;
	}

	@Override
	public boolean whenInput(EditionInventory inv, ConfigFile config, String message, Object... info) {
		List<String> lore = config.getConfig().getConfigurationSection(inv.getEdited().getId()).contains(getPath())
				? config.getConfig().getStringList(inv.getEdited().getId() + "." + getPath())
				: new ArrayList<>();
		lore.add(message);
		config.getConfig().set(inv.getEdited().getId() + "." + getPath(), lore);
		inv.registerItemEdition(config);
		inv.open();
		inv.getPlayer().sendMessage(MMOItems.plugin.getPrefix() + message + " successfully added.");
		return true;
	}

	@Override
	public void whenDisplayed(List<String> lore, MMOItem mmoitem) {

		if (mmoitem.hasData(this)) {
			lore.add(ChatColor.GRAY + "Current Value:");
			GemSocketsData data = (GemSocketsData) mmoitem.getData(this);
			data.getEmptySlots().forEach(socket -> lore.add(ChatColor.GRAY + "* " + ChatColor.GREEN + socket + " Gem Socket"));

		} else
			lore.add(ChatColor.GRAY + "Current Value: " + ChatColor.RED + "No Sockets");

		lore.add("");
		lore.add(ChatColor.YELLOW + AltChar.listDash + " Click to add a gem socket.");
		lore.add(ChatColor.YELLOW + AltChar.listDash + " Right click to remove the socket.");
	}
}
