package net.Indyuce.mmoitems.comp.rpg;

import com.archyx.aureliumskills.AureliumSkills;
import com.archyx.aureliumskills.skills.Skills;
import com.willfp.ecoskills.EcoSkillsPlugin;
import com.willfp.ecoskills.api.EcoSkillsAPI;
import com.willfp.ecoskills.api.modifier.ModifierOperation;
import com.willfp.ecoskills.api.modifier.PlayerStatModifier;
import com.willfp.ecoskills.stats.CustomStat;
import com.willfp.ecoskills.stats.CustomStats;
import com.willfp.ecoskills.stats.Stat;
import com.willfp.ecoskills.stats.Stats;
import io.lumine.mythic.lib.api.event.skill.PlayerCastSkillEvent;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.player.EmptyRPGPlayer;
import net.Indyuce.mmoitems.api.player.PlayerData;
import net.Indyuce.mmoitems.api.player.RPGPlayer;
import net.Indyuce.mmoitems.stat.Abilities;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import net.Indyuce.mmoitems.stat.type.ItemStat;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import javax.naming.Name;

public class EcoSkillsHook implements RPGHandler , Listener {
    EcoSkillsPlugin eSkills;
    private NamespacedKey manaKey=new NamespacedKey("mmoitems","mana");
    private NamespacedKey magicdamagekey=new NamespacedKey("mmoitems","magic_damage");
    private NamespacedKey staminakey=new NamespacedKey("mmoitems","saturation");
    private static final ItemStat mana = new DoubleStat("ECO_MANA", Material.BOOK,
            "eco法力",
            new String[]{"eco的法力属性，键值:mana"},
            new String[]{"!miscellaneous", "!block", "all"});
    private static final ItemStat magic_damage = new DoubleStat("ECO_MAGIC_DAMAGE", Material.BOOK,
            "eco魔法伤害",
            new String[]{"eco的魔法伤害属性，键值:magic_damage"},
            new String[]{"!miscellaneous", "!block", "all"});
    public EcoSkillsHook(){

        eSkills = (EcoSkillsPlugin) Bukkit.getPluginManager().getPlugin("EcoSkills");

        // Register wisdom for the max mana stat
        MMOItems.plugin.getStats().register(mana);
        MMOItems.plugin.getStats().register(magic_damage);

    }


    @Override
    public RPGPlayer getInfo(PlayerData data) {

        RPGPlayer rpgPlayer= new EcoSkillsPlayer(data);
        return rpgPlayer;
    }

    CustomStat getCustomState(NamespacedKey key){
        for (CustomStat stat: CustomStats.values()){

            if(stat.getKey().equals(key)){
                return stat;
            }
        }
        return null;
    }

    @Override
    public void refreshStats(PlayerData data) {

        CustomStat mana_stat=getCustomState(new NamespacedKey("ecoskills","mana"));
        CustomStat magicedamage_stat=getCustomState(new NamespacedKey("ecoskills","magic_damage"));

        if(mana_stat!=null){

            PlayerStatModifier mana_modifier=new PlayerStatModifier(manaKey,mana_stat,(int)data.getStats().getStat(mana));
            EcoSkillsAPI.getInstance().addStatModifier(data.getPlayer(),mana_modifier);
        }
        if(magicedamage_stat!=null){

            PlayerStatModifier magic_damage_modifier=new PlayerStatModifier(magicdamagekey, magicedamage_stat,(int)data.getStats().getStat(magic_damage));
            EcoSkillsAPI.getInstance().addStatModifier(data.getPlayer(),magic_damage_modifier);
        }

    }
    public class EcoSkillsPlayer extends RPGPlayer {

        Player player;
        public EcoSkillsPlayer(PlayerData playerData) {
            super(playerData);
            player=playerData.getPlayer();
        }

        @Override
        public int getLevel() {
            return 0;
        }

        @Override
        public String getClassName() {
            return "";
        }


        @Override
        public double getMana() {
        }

        @Override
        public double getStamina() {
            CustomStat stat=getCustomState(new NamespacedKey("ecoskills","saturation"));
            if(stat==null)return 0;
            return EcoSkillsAPI.getInstance().getStatLevel(player,stat);
        }

        @Override
        public void setMana(double value) {
            CustomStat stat=getCustomState(new NamespacedKey("ecoskills","mana"));
            if(stat==null)return;
            PlayerStatModifier mana_modifier=new PlayerStatModifier(manaKey,stat,(int)value);
            EcoSkillsAPI.getInstance().addStatModifier(player,mana_modifier);
        }

        @Override
        public void setStamina(double value) {

        }
    }

}
