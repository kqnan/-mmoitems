package net.Indyuce.mmoitems.comp.rpg;

import com.archyx.aureliumskills.AureliumSkills;
import com.archyx.aureliumskills.skills.Skills;
import com.willfp.eco.core.Eco;
import com.willfp.eco.core.EcoPlugin;
import com.willfp.eco.core.data.PlayerProfile;
import com.willfp.ecoskills.EcoSkillsPlugin;
import com.willfp.ecoskills.api.EcoSkillsAPI;
import com.willfp.ecoskills.api.modifier.ModifierOperation;
import com.willfp.ecoskills.api.modifier.PlayerStatModifier;
import com.willfp.ecoskills.libreforge.PointCostHandler;
import com.willfp.ecoskills.libreforge.PointsUtils;
import com.willfp.ecoskills.libreforge.effects.effects.EffectGivePoints;
import com.willfp.ecoskills.libreforge.events.PointsChangeEvent;
import com.willfp.ecoskills.libreforge.triggers.TriggerData;
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
import net.Indyuce.mmoitems.stat.data.AbilityData;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import net.Indyuce.mmoitems.stat.type.ItemStat;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import javax.naming.Name;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class EcoSkillsHook implements RPGHandler , Listener {


    private HashMap<ItemStat,Stat> map_stats=new HashMap<>();
    EcoSkillsPlugin eSkills;

    public EcoSkillsHook(){

            eSkills = (EcoSkillsPlugin) Bukkit.getPluginManager().getPlugin("EcoSkills");

   

                for (Stat stat:Stats.values()){
                    String str=stat.getKey().toString().replace(":","_").toUpperCase();
                    ItemStat mmstat=new DoubleStat(str, Material.BOOK,
                            "eco数值之"+str,
                            new String[]{"键名:"+str},
                            new String[]{"!miscellaneous", "!block", "all"});
                    map_stats.put(mmstat,stat);
                    MMOItems.plugin.getStats().register(mmstat);
                }
                for (CustomStat stat:CustomStats.values()){
                    String str=stat.getKey().toString().replace(":","_").toUpperCase();
                    ItemStat mmstat=new DoubleStat(str, Material.BOOK,
                            "eco数值之"+str,
                            new String[]{"键名:"+str},
                            new String[]{"!miscellaneous", "!block", "all"});
                    map_stats.put(mmstat,stat);
                    MMOItems.plugin.getStats().register(mmstat);
                }



    }


    @Override
    public RPGPlayer getInfo(PlayerData data) {

        return  new EcoSkillsPlayer(data);
    }
    //修正技能伤害
    @EventHandler
    public void onSkillCast(PlayerCastSkillEvent event){
        if(event.getCast() instanceof AbilityData abilityData){
            if(abilityData.getModifier("damage")!=0){
                double level=EcoSkillsAPI.getInstance().getStatLevel(event.getPlayer(),getCustomState(new NamespacedKey("ecoskills","magic_damage")))
                ;
                abilityData.setModifier("damage",abilityData.getModifier("damage")*(1+(level/200)));
            }
        }
    }
    CustomStat getCustomState(NamespacedKey key){
        for(CustomStat stat:CustomStats.values()){
            if(stat.getKey().equals(key)){
                return stat;
            }
        }
        return null;
    }

    @Override
    public void refreshStats(PlayerData data) {
        for (Map.Entry<ItemStat,Stat> entry:map_stats.entrySet()){
            ItemStat itemStat=entry.getKey();
            Stat stat=entry.getValue();
            PlayerStatModifier modifier=new PlayerStatModifier(new NamespacedKey("mmoitems",itemStat.getId().replace(":","").toLowerCase()),stat,(int)data.getStats().getStat(itemStat));
            EcoSkillsAPI.getInstance().addStatModifier(data.getPlayer(),modifier);
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
            return PointsUtils.getPoints(player,"g_mana");
        }

        @Override
        public double getStamina() {
            return getPlayer().getFoodLevel();
        }

        @Override
        public void setMana(double value) {
            PointsUtils.setPoints(player,"g_mana",value);
        }

        @Override
        public void setStamina(double value) {

        }
    }


}
