/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2020
 */
package org.bitbucket.ucchy.undine.bridge;

import java.util.Date;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import at.pcgamingfreaks.UUIDConverter;

/**
 * PCGFPluginLibへアクセスするためのブリッジクラス<br/>
 * NOTE: このブリッジクラスのメソッドはstaticアクセスできるため、load不要。
 * @author ucchy
 */
public class PCGFPluginLibBridge {

    public static String getUUIDFromName(@NotNull String name, boolean onlineMode, boolean withSeparators, boolean offlineUUIDonFail, @Nullable Date lastKnownDate) {
        return UUIDConverter.getUUIDFromName(name, onlineMode, withSeparators, offlineUUIDonFail, lastKnownDate);
    }

    public static String getUUIDFromName(@NotNull String name, boolean onlineMode, @Nullable Date lastKnownDate) {
        return UUIDConverter.getUUIDFromName(name, onlineMode, lastKnownDate);
    }
}
