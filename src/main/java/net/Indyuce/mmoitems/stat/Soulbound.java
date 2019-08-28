package net.Indyuce.mmoitems.stat;

import java.util.UUID;
import java.util.regex.Pattern;

import org.bukkit.inventory.ItemStack;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import net.Indyuce.mmoitems.MMOUtils;
import net.Indyuce.mmoitems.api.Message;
import net.Indyuce.mmoitems.api.item.MMOItem;
import net.Indyuce.mmoitems.api.item.NBTItem;
import net.Indyuce.mmoitems.api.item.build.MMOItemBuilder;
import net.Indyuce.mmoitems.stat.data.StatData;
import net.Indyuce.mmoitems.stat.type.InternalStat;
import net.Indyuce.mmoitems.stat.type.ItemStat;
import net.Indyuce.mmoitems.version.VersionMaterial;
import net.Indyuce.mmoitems.version.nms.ItemTag;

public class Soulbound extends InternalStat {
	public Soulbound() {
		super(new ItemStack(VersionMaterial.ENDER_EYE.toMaterial()), "Soulbound", new String[0], "soulbound", new String[] { "all" });
	}

	@Override
	public boolean whenApplied(MMOItemBuilder item, StatData data) {
		item.addItemTag(new ItemTag("MMOITEMS_SOULBOUND", ((SoulboundData) data).toJson().toString()));
		String formattedLoreTag = Message.SOULBOUND_ITEM_LORE.getUpdated().replace("#player#", ((SoulboundData) data).getName()).replace("#level#", MMOUtils.intToRoman(((SoulboundData) data).getLevel()));
		item.getLore().insert("soulbound", formattedLoreTag.split(Pattern.quote("//")));
		return true;
	}

	@Override
	public void whenLoaded(MMOItem mmoitem, NBTItem item) {
		if (item.hasTag("MMOITEMS_SOULBOUND"))
			try {
				mmoitem.setData(ItemStat.SOULBOUND, new SoulboundData(new JsonParser().parse(item.getString("MMOITEMS_SOULBOUND")).getAsJsonObject()));
			} catch (JsonSyntaxException exception) {
				/*
				 * OLD ITEM WHICH MUST BE UPDATED.
				 */
			}
	}

	public SoulboundData newSoulboundData(UUID uuid, String name, int level) {
		return new SoulboundData(uuid, name, level);
	}

	public class SoulboundData extends StatData {
		private UUID uuid;
		private String name;
		private int level;

		public SoulboundData(UUID uuid, String name, int level) {
			this.uuid = uuid;
			this.name = name;
			this.level = level;
		}

		public SoulboundData(JsonObject object) {
			uuid = UUID.fromString(object.get("UUID").getAsString());
			name = object.get("Name").getAsString();
			level = object.get("Level").getAsInt();
		}

		public UUID getUniqueId() {
			return uuid;
		}

		public String getName() {
			return name;
		}

		public int getLevel() {
			return level;
		}

		public JsonObject toJson() {
			JsonObject object = new JsonObject();
			object.addProperty("Level", level);
			object.addProperty("Name", name);
			object.addProperty("UUID", uuid.toString());
			return object;
		}
	}
}