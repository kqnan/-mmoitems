package net.Indyuce.mmoitems.api.player;

import io.lumine.mythic.lib.MythicLib;
import io.lumine.mythic.lib.api.DamageType;
import io.lumine.mythic.lib.api.item.NBTItem;
import io.lumine.mythic.lib.api.player.MMOPlayerData;
import net.Indyuce.mmoitems.ItemStats;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.MMOUtils;
import net.Indyuce.mmoitems.api.ConfigFile;
import net.Indyuce.mmoitems.api.ItemAttackResult;
import net.Indyuce.mmoitems.api.ItemSet;
import net.Indyuce.mmoitems.api.ItemSet.SetBonuses;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.Type.EquipmentSlot;
import net.Indyuce.mmoitems.api.ability.Ability;
import net.Indyuce.mmoitems.api.ability.Ability.CastingMode;
import net.Indyuce.mmoitems.api.ability.AbilityResult;
import net.Indyuce.mmoitems.api.crafting.CraftingStatus;
import net.Indyuce.mmoitems.api.event.AbilityUseEvent;
import net.Indyuce.mmoitems.api.item.mmoitem.VolatileMMOItem;
import net.Indyuce.mmoitems.api.player.PlayerStats.CachedStats;
import net.Indyuce.mmoitems.api.player.inventory.EquippedItem;
import net.Indyuce.mmoitems.api.player.inventory.EquippedPlayerItem;
import net.Indyuce.mmoitems.api.player.inventory.InventoryUpdateHandler;
import net.Indyuce.mmoitems.comp.flags.FlagPlugin.CustomFlag;
import net.Indyuce.mmoitems.particle.api.ParticleRunnable;
import net.Indyuce.mmoitems.stat.data.*;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class PlayerData {
	private static final Map<UUID, PlayerData> data = new HashMap<>();
	private final MMOPlayerData mmoData;

	/*
	 * reloaded everytime the player reconnects in case of major change.
	 */
	private RPGPlayer rpgPlayer;

	private final InventoryUpdateHandler inventory = new InventoryUpdateHandler(this);
	private final CraftingStatus craftingStatus = new CraftingStatus();
	private final PlayerAbilityData playerAbilityData = new PlayerAbilityData();
	private final Map<String, CooldownInformation> abilityCooldowns = new HashMap<>();
	private final Map<String, Long> itemCooldowns = new HashMap<>();
	private final Map<CooldownType, Long> extraCooldowns = new HashMap<>();

	/*
	 * specific stat calculation TODO compress it in Map<ItemStat,
	 * DynamicStatData>
	 */
	private final Map<PotionEffectType, PotionEffect> permanentEffects = new HashMap<>();
	private final Set<ParticleRunnable> itemParticles = new HashSet<>();
	private ParticleRunnable overridingItemParticles = null;
	private final Set<AbilityData> itemAbilities = new HashSet<>();
	private boolean fullHands = false;
	private SetBonuses setBonuses = null;
	private final PlayerStats stats;

	// Cached so they can be properly removed again
	private final Set<String> permissions = new HashSet<>();

	private PlayerData(MMOPlayerData mmoData) {
		this.mmoData = mmoData;
		this.rpgPlayer = MMOItems.plugin.getRPG().getInfo(this);
		this.stats = new PlayerStats(this);

		load(new ConfigFile("/userdata", getUniqueId().toString()).getConfig());
		updateInventory();
	}

	private void load(FileConfiguration config) {
		if (config.contains("crafting-queue"))
			craftingStatus.load(this, config.getConfigurationSection("crafting-queue"));

		if (MMOItems.plugin.hasPermissions() && config.contains("permissions-from-items")) {
			Permission perms = MMOItems.plugin.getVault().getPermissions();
			config.getStringList("permissions-from-items").forEach(perm -> {
				if (perms.has(getPlayer(), perm))
					perms.playerRemove(getPlayer(), perm);
			});
		}
	}

	public void save() {
		cancelRunnables();

		ConfigFile config = new ConfigFile("/userdata", getUniqueId().toString());
		config.getConfig().createSection("crafting-queue");
		config.getConfig().set("permissions-from-items", new ArrayList<>(permissions));
		craftingStatus.save(config.getConfig().getConfigurationSection("crafting-queue"));
		config.save();
	}

	public MMOPlayerData getMMOPlayerData() {
		return mmoData;
	}

	public UUID getUniqueId() {
		return mmoData.getUniqueId();
	}

	public boolean isOnline() {
		return mmoData.isOnline();
	}

	public Player getPlayer() {
		return mmoData.getPlayer();
	}

	public RPGPlayer getRPG() {
		return rpgPlayer;
	}

	public void cancelRunnables() {
		itemParticles.forEach(BukkitRunnable::cancel);
		if (overridingItemParticles != null)
			overridingItemParticles.cancel();
	}

	/*
	 * returns true if the player hands are full, i.e if the player is holding
	 * one two handed item and one other item at the same time. this will
	 */
	public boolean areHandsFull() {
		if (!mmoData.isOnline())
			return false;

		// Get the mainhand and offhand items.
		NBTItem main = MythicLib.plugin.getVersion().getWrapper().getNBTItem(getPlayer().getInventory().getItemInMainHand());
		NBTItem off = MythicLib.plugin.getVersion().getWrapper().getNBTItem(getPlayer().getInventory().getItemInOffHand());

		// Is either hand two-handed?
		boolean mainhand_twohanded = main.getBoolean(ItemStats.TWO_HANDED.getNBTPath());
		boolean offhand_twohanded = off.getBoolean(ItemStats.TWO_HANDED.getNBTPath());

		// Is either hand encumbering: Not NULL, not AIR, and not Handworn
		boolean mainhand_encumbering = (main.getItem() != null && main.getItem().getType() != Material.AIR && !main.getBoolean(ItemStats.HANDWORN.getNBTPath()));
		boolean offhand_encumbering = (off.getItem() != null && off.getItem().getType() != Material.AIR && !off.getBoolean(ItemStats.HANDWORN.getNBTPath()));

		// Will it encumber?
		return (mainhand_twohanded && offhand_encumbering) || (mainhand_encumbering && offhand_twohanded);
	}

	@SuppressWarnings("deprecation")
	public void updateInventory() {
		if (!mmoData.isOnline())
			return;

		/*
		 * very important, clear particle data AFTER canceling the runnable
		 * otherwise it cannot cancel and the runnable keeps going (severe)
		 */
		inventory.getEquipped().clear();
		permanentEffects.clear();
		itemAbilities.clear();
		cancelRunnables();
		itemParticles.clear();
		overridingItemParticles = null;
		if (MMOItems.plugin.hasPermissions()) {
			Permission perms = MMOItems.plugin.getVault().getPermissions();
			permissions.forEach(perm -> {
				if (perms.has(getPlayer(), perm))
					perms.playerRemove(getPlayer(), perm);
			});
		}
		permissions.clear();

		/*
		 * updates the full-hands boolean, this way it can be cached and used in
		 * the updateEffects() method
		 */
		fullHands = areHandsFull();
		//region EquipmentSlot mainheld_type = [equipment Type of whatever the player is holding]
		EquipmentSlot mainheld_type = null;
		ItemStack mainheld = getPlayer().getInventory().getItemInMainHand();
		if (mainheld.getType().isItem()) {
			NBTItem mainnbt = MythicLib.plugin.getVersion().getWrapper().getNBTItem(mainheld);

			if (mainnbt != null) {
				Type maintype = Type.get(mainnbt.getType());

				if (maintype != null) {

					mainheld_type = maintype.getEquipmentType();
				}
			}
		}
		//endregion

		/*
		 * Find all the items the player can actually use
		 */
		for (EquippedItem item : MMOItems.plugin.getInventory().getInventory(getPlayer())) {
			NBTItem nbtItem = item.getItem();
			Type type = Type.get(nbtItem.getType());

			/*
			 * If the item is a custom item, apply slot and item use
			 * restrictions (items which only work in a specific equipment slot)
			 */
			if (type != null && (!item.matches(type, mainheld_type) || !getRPG().canUse(nbtItem, false, false)))
				continue;

			inventory.getEquipped().add(new EquippedPlayerItem(item));
		}

		for (EquippedPlayerItem equipped : inventory.getEquipped()) {
			VolatileMMOItem item = equipped.getItem();

			/*
			 * Apply permanent potion effects
			 */
			if (item.hasData(ItemStats.PERM_EFFECTS))
				((PotionEffectListData) item.getData(ItemStats.PERM_EFFECTS)).getEffects().forEach(effect -> {
					if (getPermanentPotionEffectAmplifier(effect.getType()) < effect.getLevel() - 1)
						permanentEffects.put(effect.getType(), effect.toEffect());
				});

			/*
			 * Apply item particles
			 */
			if (item.hasData(ItemStats.ITEM_PARTICLES)) {
				ParticleData particleData = (ParticleData) item.getData(ItemStats.ITEM_PARTICLES);

				if (particleData.getType().hasPriority()) {
					if (overridingItemParticles == null)
						overridingItemParticles = particleData.start(this);
				} else
					itemParticles.add(particleData.start(this));
			}

			/*
			 * Apply abilities
			 */
			if (item.hasData(ItemStats.ABILITIES))
				if (equipped.getSlot() != EquipmentSlot.OFF_HAND || !MMOItems.plugin.getConfig().getBoolean("disable-abilities-in-offhand"))
					itemAbilities.addAll(((AbilityListData) item.getData(ItemStats.ABILITIES)).getAbilities());

			/*
			 * Apply permissions if vault exists
			 */
			if (MMOItems.plugin.hasPermissions() && item.hasData(ItemStats.GRANTED_PERMISSIONS)) {
				permissions.addAll(((StringListData) item.getData(ItemStats.GRANTED_PERMISSIONS)).getList());
				Permission perms = MMOItems.plugin.getVault().getPermissions();
				permissions.forEach(perm -> {
					if (!perms.has(getPlayer(), perm))
						perms.playerAdd(getPlayer(), perm);
				});
			}
		}

		/*
		 * calculate the player's item set and add the bonus permanent effects /
		 * bonus abilities to the playerdata maps
		 */
		int max = 0;
		ItemSet set = null;
		Map<ItemSet, Integer> sets = new HashMap<>();
		for (EquippedPlayerItem equipped : inventory.getEquipped()) {
			VolatileMMOItem item = equipped.getItem();
			String tag = item.getNBT().getString("MMOITEMS_ITEM_SET");
			ItemSet itemSet = MMOItems.plugin.getSets().get(tag);
			if (itemSet == null)
				continue;

			int nextInt = (sets.getOrDefault(itemSet, 0)) + 1;
			sets.put(itemSet, nextInt);
			if (nextInt >= max) {
				max = nextInt;
				set = itemSet;
			}
		}
		setBonuses = set == null ? null : set.getBonuses(max);

		if (hasSetBonuses()) {
			itemAbilities.addAll(setBonuses.getAbilities());
			for (ParticleData particle : setBonuses.getParticles())
				itemParticles.add(particle.start(this));
			for (PotionEffect effect : setBonuses.getPotionEffects())
				if (getPermanentPotionEffectAmplifier(effect.getType()) < effect.getAmplifier())
					permanentEffects.put(effect.getType(), effect);
		}

		/*
		 * calculate all stats.
		 */
		stats.updateStats();

		/*
		 * update stuff from the external MMOCore plugins. the 'max mana' stat
		 * currently only supports Heroes since other APIs do not allow other
		 * plugins to easily increase this type of stat.
		 */
		MMOItems.plugin.getRPG().refreshStats(this);

		/*
		 * actually update the player inventory so the task doesn't infinitely
		 * loop on updating
		 */
		inventory.helmet = getPlayer().getInventory().getHelmet();
		inventory.chestplate = getPlayer().getInventory().getChestplate();
		inventory.leggings = getPlayer().getInventory().getLeggings();
		inventory.boots = getPlayer().getInventory().getBoots();
		inventory.hand = getPlayer().getInventory().getItemInMainHand();
		inventory.offhand = getPlayer().getInventory().getItemInOffHand();
	}

	public void updateStats() {
		if (!mmoData.isOnline())
			return;

		// perm effects
		permanentEffects.keySet().forEach(effect -> getPlayer().addPotionEffect(permanentEffects.get(effect)));

		// two handed
		if (fullHands)
			getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 1, true, false));
	}

	public InventoryUpdateHandler getInventory() {
		return inventory;
	}

	public SetBonuses getSetBonuses() {
		return setBonuses;
	}

	public boolean hasSetBonuses() {
		return setBonuses != null;
	}

	public CraftingStatus getCrafting() {
		return craftingStatus;
	}

	public PlayerAbilityData getAbilityData() {
		return playerAbilityData;
	}

	public int getPermanentPotionEffectAmplifier(PotionEffectType type) {
		return permanentEffects.containsKey(type) ? permanentEffects.get(type).getAmplifier() : -1;
	}

	public Collection<PotionEffect> getPermanentPotionEffects() {
		return permanentEffects.values();
	}

	public PlayerStats getStats() {
		return stats;
	}

	public Set<AbilityData> getItemAbilities() {
		return itemAbilities;
	}

	private boolean hasAbility(CastingMode castMode) {
		for (AbilityData ability : itemAbilities)
			if (ability.getCastingMode() == castMode)
				return true;
		return false;
	}

	// While we may never use the return value, external plugins may need to.
	@SuppressWarnings("UnusedReturnValue")
	public ItemAttackResult castAbilities(LivingEntity target, ItemAttackResult result, CastingMode castMode) {
		/*
		 * performance improvement, do not cache the player stats into a
		 * CachedStats if the player has no ability on that cast mode
		 */
		if (!hasAbility(castMode))
			return result;

		return castAbilities(getStats().newTemporary(), target, result, castMode);
	}

	public ItemAttackResult castAbilities(CachedStats stats, LivingEntity target, ItemAttackResult result, CastingMode castMode) {
		if (!mmoData.isOnline())
			return result;

		/*
		 * if ability has target, check for ability flag at location of target
		 * and make sure player can attack target. if ability has no target,
		 * check for WG flag at the caster location
		 */
		if (target == null ? !MMOItems.plugin.getFlags().isFlagAllowed(getPlayer(), CustomFlag.MI_ABILITIES)
				: !MMOItems.plugin.getFlags().isFlagAllowed(target.getLocation(), CustomFlag.MI_ABILITIES)
						|| !MMOUtils.canDamage(getPlayer(), target))
			return result.setSuccessful(false);

		for (AbilityData ability : itemAbilities)
			if (ability.getCastingMode() == castMode)
				cast(stats, target, result, ability);

		return result;
	}

	/*
	 * shall only be used with right click abilites since the on-hit abilities
	 * also requires the initial damage value and a target to be successfully
	 * cast
	 */
	@Deprecated
	public void cast(Ability ability) {
		cast(getStats().newTemporary(), null, new ItemAttackResult(true, DamageType.SKILL), new AbilityData(ability, CastingMode.RIGHT_CLICK));
	}

	public void cast(AbilityData data) {
		cast(getStats().newTemporary(), null, new ItemAttackResult(true, DamageType.SKILL), data);
	}

	public void cast(CachedStats stats, LivingEntity target, ItemAttackResult attack, AbilityData ability) {

		/*
		 * Apply simple conditions including mana and stamina cost, permission
		 * and cooldown checks
		 */
		if (!rpgPlayer.canCast(ability))
			return;

		/*
		 * Apply extra conditions which depend on the ability the player is
		 * casting
		 */
		AbilityResult abilityResult = ability.getAbility().whenRan(stats, target, ability, attack);
		if (!abilityResult.isSuccessful())
			return;

		AbilityUseEvent event = new AbilityUseEvent(this, ability, target);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled())
			return;

		/*
		 * The player can cast the ability, and it was successfully cast on its
		 * target, removes resources needed from the player
		 */
		if (ability.hasModifier("mana"))
			rpgPlayer.giveMana(-abilityResult.getModifier("mana"));
		if (ability.hasModifier("stamina"))
			rpgPlayer.giveStamina(-abilityResult.getModifier("stamina"));

		double cooldown = abilityResult.getModifier("cooldown") * (1 - Math.min(.8, stats.getStat(ItemStats.COOLDOWN_REDUCTION) / 100));
		if (cooldown > 0)
			applyAbilityCooldown(ability.getAbility(), cooldown);

		/*
		 * Finally cast the ability; BUG FIX: cooldown MUST be applied BEFORE
		 * the ability is cast otherwise instantaneously damaging abilities like
		 * Sparkle can trigger deadly crash loops
		 */
		ability.getAbility().whenCast(stats, abilityResult, attack);
	}

	public boolean isOnCooldown(CooldownType type) {
		return extraCooldowns.containsKey(type) && extraCooldowns.get(type) > System.currentTimeMillis();
	}

	/*
	 * 'value' is either the cooldown value if the cooldown type is a regular
	 * attack cooldown, or the cooldown reduction stat value, if the cooldown
	 * type is mitigation
	 */
	public void applyCooldown(CooldownType type, double value) {
		extraCooldowns.put(type, (long) (System.currentTimeMillis() + 1000 * value));
	}

	public boolean canUseItem(String id) {
		return (itemCooldowns.containsKey(id) ? itemCooldowns.get(id) : 0) < System.currentTimeMillis();
	}

	public void applyItemCooldown(String id, double value) {
		itemCooldowns.put(id, (long) (System.currentTimeMillis() + value * 1000));
	}

	public void applyAbilityCooldown(Ability ability, double value) {
		applyAbilityCooldown(ability.getID(), value);
	}

	public void applyAbilityCooldown(String id, double value) {
		abilityCooldowns.put(id, new CooldownInformation(value));
	}

	public boolean hasCooldownInfo(Ability ability) {
		return hasCooldownInfo(ability.getID());
	}

	public boolean hasCooldownInfo(String id) {
		return abilityCooldowns.containsKey(id);
	}

	public CooldownInformation getCooldownInfo(Ability ability) {
		return getCooldownInfo(ability.getID());
	}

	public CooldownInformation getCooldownInfo(String id) {
		return abilityCooldowns.get(id);
	}

	public double getItemCooldown(String id) {
		return Math.max(0, (double) (itemCooldowns.get(id) - System.currentTimeMillis()) / 1000);
	}

	public static PlayerData get(OfflinePlayer player) {
		return get(player.getUniqueId());
	}

	public static PlayerData get(UUID uuid) {
		return data.get(uuid);
	}

	/**
	 * Called when the corresponding MMOPlayerData has already been initialized
	 */
	public static void load(Player player) {

		/*
		 * Double check they are online, for some reason even if this is fired
		 * from the join event the player can be offline if they left in the
		 * same tick or something.
		 */
		if (!data.containsKey(player.getUniqueId())) {
			data.put(player.getUniqueId(), new PlayerData(MMOPlayerData.get(player)));
			return;
		}

		/*
		 * Update the cached RPGPlayer in case of any major change in the player
		 * data of other rpg plugins
		 */
		PlayerData playerData = PlayerData.get(player.getUniqueId());
		playerData.rpgPlayer = MMOItems.plugin.getRPG().getInfo(playerData);
	}

	public static Collection<PlayerData> getLoaded() {
		return data.values();
	}

	public enum CooldownType {

		/**
		 * Basic attack cooldown like staffs and lutes
		 */
		ATTACK,

		/**
		 * Elemental attacks cooldown
		 */
		ELEMENTAL_ATTACK,

		/**
		 * Special attacks like staffs or gauntlets right clicks
		 */
		SPECIAL_ATTACK,

		/**
		 * Special item set attack effects including slashing, piercing and
		 * blunt attack effects
		 */
		SET_TYPE_ATTACK
	}
}
