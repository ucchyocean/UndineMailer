/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine;

import net.milkbowl.vault.Vault;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Vaultを経由して経済プラグインへアクセスするためのブリッジクラス
 * @author ucchy
 */
public class VaultEcoBridge {

    /** vault-ecoクラス */
    private Economy eco;

    /** コンストラクタは使用不可 */
    private VaultEcoBridge() {
    }

    /**
     * vault-ecoをロードする
     * @param plugin vaultのプラグインインスタンス
     * @param ロードしたbridgeのインスタンス
     */
    public static VaultEcoBridge load(Plugin plugin) {

        if ( plugin instanceof Vault ) {
            RegisteredServiceProvider<Economy> economyProvider =
                    Bukkit.getServicesManager().getRegistration(Economy.class);
            if ( economyProvider != null ) {
                VaultEcoBridge bridge = new VaultEcoBridge();
                bridge.eco = economyProvider.getProvider();
                if ( bridge.eco.isEnabled() ) return bridge;
            }
        }

        return null;
    }

    /**
     * 指定されたプレイヤーの所持金を取得する
     * @param player プレイヤー
     * @return 所持金（端数は切り捨て）
     */
    public int getBalance(OfflinePlayer player) {
        return (int)eco.getBalance(player);
    }

    /**
     * 指定されたプレイヤーが、指定された金額を所持しているかどうかを返す
     * @param player プレイヤー
     * @param amount 確認する金額
     * @return 持っているかどうか
     */
    public boolean has(OfflinePlayer player, int amount) {
        return eco.has(player, amount);
    }

    /**
     * 指定されたプレイヤーの所持金から、指定された額を引き落とす
     * @param player プレイヤー
     * @param amount 差し引く金額
     * @return 正常に完了したかどうか
     */
    public boolean withdrawPlayer(OfflinePlayer player, int amount) {
        EconomyResponse response = eco.withdrawPlayer(player, amount);
        return response.transactionSuccess();
    }

    /**
     * 指定されたプレイヤーに、指定された金額を入金する
     * @param player プレイヤー
     * @param amount 入金する金額
     * @return 正常に完了したかどうか
     */
    public boolean depositPlayer(OfflinePlayer player, int amount) {
        EconomyResponse response = eco.depositPlayer(player, amount);
        return response.transactionSuccess();
    }

    /**
     * 指定された金額を、経済プラグインに設定されている書式で表示する
     * @param amount 金額
     * @return 表示用の文字列
     */
    public String format(int amount) {
        return eco.format(amount);
    }
}
