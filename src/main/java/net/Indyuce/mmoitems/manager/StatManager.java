package net.Indyuce.mmoitems.manager;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.stat.type.AttributeStat;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import net.Indyuce.mmoitems.stat.type.ItemStat;

public class StatManager {
	private Map<String, ItemStat> stats = new LinkedHashMap<>();
	private Set<DoubleStat> gem = new HashSet<>();
	private Set<AttributeStat> attribute = new HashSet<>();

	/*
	 * load default stats using java reflection, get all public static final
	 * fields in the ItemStat and register them as stat instances
	 */
	public StatManager() {
		for (Field field : ItemStat.class.getFields())
			try {
				if (Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers()) && field.get(null) instanceof ItemStat)
					register(field.getName(), (ItemStat) field.get(null));
			} catch (IllegalArgumentException | IllegalAccessException exception) {
				MMOItems.plugin.getLogger().log(Level.WARNING, "Couldn't register stat called " + field.getName());
			}
	}

	public Collection<ItemStat> getAll() {
		return stats.values();
	}

	/*
	 * cache specific stats for better performance using these extra sets
	 */
	public Set<AttributeStat> getAttributeStats() {
		return attribute;
	}

	public Set<DoubleStat> getDoubleStats() {
		return gem;
	}

	/*
	 * the extra checks in that method to register stats even after the plugin
	 * has successfully enabled otherwise the other sets would not be updated.
	 */
	public void register(String id, ItemStat stat) {
		if (!stat.isEnabled())
			return;

		stat.setId(id);
		stats.put(stat.getId(), stat);

		if (isGemStoneStat(stat))
			gem.add((DoubleStat) stat);

		if (stat instanceof AttributeStat)
			attribute.add((AttributeStat) stat);
	}

	public ItemStat get(String str) {
		return stats.containsKey(str) ? stats.get(str) : null;
	}

	private boolean isGemStoneStat(ItemStat stat) {
		return Type.GEM_STONE.canHaveStat(stat) && stat != ItemStat.REQUIRED_LEVEL && stat != ItemStat.CUSTOM_MODEL_DATA && stat != ItemStat.DURABILITY && stat != ItemStat.MAX_CUSTOM_DURABILITY && stat != ItemStat.SUCCESS_RATE && stat instanceof DoubleStat;
	}
}