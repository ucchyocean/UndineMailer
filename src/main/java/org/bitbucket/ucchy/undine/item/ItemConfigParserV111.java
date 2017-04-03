/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2017
 */
package org.bitbucket.ucchy.undine.item;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

/**
 * アイテム設定のパーサー for Bukkit v1.11
 * @author ucchy
 */
public class ItemConfigParserV111 {

    /**
     * 指定されたアイテムがシャルカーボックスだったときに、メタ情報をセクションに保存する。
     * @param item
     * @param section
     */
    protected static void addShulkerBoxInfoToSection(ItemStack item, ConfigurationSection section) {

        if ( !isShulkerBox(item) ) return;

        if ( !item.hasItemMeta() || !(item.getItemMeta() instanceof BlockStateMeta) ) return;
        BlockStateMeta meta = (BlockStateMeta)item.getItemMeta();

        if ( !meta.hasBlockState() || !(meta.getBlockState() instanceof ShulkerBox) ) return;
        ShulkerBox box = (ShulkerBox)meta.getBlockState();

        // ボックスの中身をひとつひとつ保存する
        ConfigurationSection contentsSection = section.createSection("contents");
        Inventory inv = box.getInventory();
        for ( int index=0; index<inv.getSize(); index++ ) {
            ItemStack content = inv.getItem(index);
            if ( content == null || content.getType() == Material.AIR ) continue;
            ConfigurationSection contentSection = contentsSection.createSection("item" + index);
            ItemConfigParser.setItemToSection(contentSection, content);
        }
    }

    /**
     * シャルカーボックスのメタデータを含める必要がある場合に、メタ情報を復帰して含めておく。
     * @param item
     * @param section
     * @throws ItemConfigParseException
     * @return 変更後のItemStack
     */
    protected static ItemStack addShulkerBoxInfoToItem(ItemStack item, ConfigurationSection section)
            throws ItemConfigParseException {

        if ( !isShulkerBox(item) ) return item;

        if ( !section.contains("contents") ) return item;
        ConfigurationSection contentsSection = section.getConfigurationSection("contents");

        if ( !(item.getItemMeta() instanceof BlockStateMeta) ) return item;
        BlockStateMeta meta = (BlockStateMeta)item.getItemMeta();

        if ( !(meta.getBlockState() instanceof ShulkerBox) ) return item;
        ShulkerBox box = (ShulkerBox)meta.getBlockState();

        Inventory inv = box.getInventory();

        // 一旦内容をクリアする
        inv.clear();

        // ボックスの中身をひとつひとつ復帰する
        for ( String key : contentsSection.getKeys(false) ) {
            if ( !key.matches("item[0-9]+") ) continue;
            int index = Integer.parseInt(key.substring(4));
            ConfigurationSection contentSection = contentsSection.getConfigurationSection(key);
            try {
                ItemStack content = ItemConfigParser.getItemFromSection(contentSection);
                inv.setItem(index, content);
            } catch (ItemConfigParseException e) {
//                throw new ItemConfigParseException(
//                        "Shulker box content '" + key + "' is invalid.", e);
                continue;
            }
        }

        // 変更内容を適用する
        meta.setBlockState(box);
        item.setItemMeta(meta);

        return item;
    }

    /**
     * シャルカーボックスの中に、指定された種類のアイテムが存在するかどうかをチェックする
     * @param materials アイテムの種類リスト
     * @param item シャルカーボックス
     * @return 指定された種類のアイテムが含まれているかどうか
     */
    public static boolean containsMaterialsInShulkerBox(List<Material> materials, ItemStack item) {

        if ( item == null || !isShulkerBox(item) ) return false;

        if ( !item.hasItemMeta() || !(item.getItemMeta() instanceof BlockStateMeta) ) return false;
        BlockStateMeta meta = (BlockStateMeta)item.getItemMeta();

        if ( !meta.hasBlockState() || !(meta.getBlockState() instanceof ShulkerBox) ) return false;
        ShulkerBox box = (ShulkerBox)meta.getBlockState();

        Inventory inv = box.getInventory();
        for ( ItemStack content : inv.getContents() ) {
            if ( content != null ) {
                for ( Material mat : materials ) {
                    if ( content.getType() == mat ) return true;
                }
            }
        }

        return false;
    }

    /**
     * 指定されたアイテムがシャルカーボックスかどうかを判定する
     * @param item アイテム
     * @return シャルカーボックスかどうか
     */
    public static boolean isShulkerBox(ItemStack item) {
        if ( item == null ) return false;
        Material type = item.getType();
        return type == Material.WHITE_SHULKER_BOX
                || type == Material.ORANGE_SHULKER_BOX
                || type == Material.MAGENTA_SHULKER_BOX
                || type == Material.LIGHT_BLUE_SHULKER_BOX
                || type == Material.YELLOW_SHULKER_BOX
                || type == Material.LIME_SHULKER_BOX
                || type == Material.PINK_SHULKER_BOX
                || type == Material.GRAY_SHULKER_BOX
                || type == Material.SILVER_SHULKER_BOX
                || type == Material.CYAN_SHULKER_BOX
                || type == Material.PURPLE_SHULKER_BOX
                || type == Material.BLUE_SHULKER_BOX
                || type == Material.BROWN_SHULKER_BOX
                || type == Material.GREEN_SHULKER_BOX
                || type == Material.RED_SHULKER_BOX
                || type == Material.BLACK_SHULKER_BOX;
    }
}
