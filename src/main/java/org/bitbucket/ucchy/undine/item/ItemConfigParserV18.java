/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine.item;

import java.util.List;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;

/**
 * アイテム設定のパーサー for Bukkit v1.8
 * @author ucchy
 */
public class ItemConfigParserV18 {

    /**
     * Bannerのメタデータを含める必要がある場合に、メタ情報を復帰して含めておく。
     * @param section
     * @param item
     * @return 変更後のItemStack
     */
    protected static ItemStack addBannerInfoToItem(
            ConfigurationSection section, ItemStack item) {

        if ( item.getType() == Material.BANNER ) {

            BannerMeta banner = (BannerMeta)item.getItemMeta();
            banner.setBaseColor(getDyeColorFromString(section.getString("basecolor")));

            if ( section.contains("patterns") ) {
                ConfigurationSection psec = section.getConfigurationSection("patterns");

                for ( String name : psec.getKeys(false) ) {

                    // 数値にキャストできなさそうなら無視する
                    if ( !name.matches("[0-9]{1,9}") ) {
                        continue;
                    }
                    int index = Integer.parseInt(name);

                    ConfigurationSection sub = psec.getConfigurationSection(name);
                    PatternType type = getPatternTypeFromString(sub.getString("type"));
                    DyeColor color = getDyeColorFromString(sub.getString("color"));
                    banner.setPattern(index, new Pattern(color, type));
                }
            }
        }

        return item;
    }

    /**
     * 指定されたアイテムがバナーだったときに、メタ情報をセクションに保存する。
     * @param section
     * @param item
     */
    protected static ConfigurationSection addBannerInfoToSection(
            ConfigurationSection section, ItemStack item) {

        if ( item.getType() == Material.BANNER ) {

            BannerMeta banner = (BannerMeta)item.getItemMeta();
            section.set("basecolor", banner.getBaseColor().toString());

            List<Pattern> patterns = banner.getPatterns();
            if ( patterns.size() > 0 ) {
                ConfigurationSection psec = section.createSection("patterns");

                for ( int index=0; index<patterns.size(); index++ ) {
                    Pattern pattern = patterns.get(index);
                    ConfigurationSection sub = psec.createSection(index + "");
                    sub.set("type", pattern.getPattern().toString());
                    sub.set("color", pattern.getColor().toString());
                }
            }
        }

        return section;
    }

    private static DyeColor getDyeColorFromString(String code) {

        if ( code == null ) {
            return DyeColor.WHITE;
        }
        for ( DyeColor c : DyeColor.values() ) {
            if ( c.toString().equalsIgnoreCase(code) ) {
                return c;
            }
        }
        return DyeColor.WHITE;
    }

    private static PatternType getPatternTypeFromString(String code) {

        if ( code == null ) {
            return PatternType.BASE;
        }
        for ( PatternType type : PatternType.values() ) {
            if ( type.toString().equalsIgnoreCase(code) ) {
                return type;
            }
        }
        return PatternType.BASE;
    }
}
