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
import at.pcgamingfreaks.UUIDConverter.NameChange;

/**
 * PCGFPluginLibへアクセスするためのブリッジクラス<br/>
 * NOTE: このブリッジクラスのメソッドはstaticアクセスできるため、load不要。
 * @author ucchy
 */
public class PCGFPluginLibBridge {

    public static String getUUIDFromName(@NotNull String name, boolean onlineMode, boolean withSeparators, @Nullable Date lastKnownDate) {
        return UUIDConverter.getUUIDFromName(name, onlineMode, withSeparators, lastKnownDate);
    }

    public static String getNameFromUUID(@NotNull String uuid) {

        // NOTE: UUIDConverter.getNameFromUUID(uuid) の方は、不正なUUIDを指定するとNullPointerExceptionを起こす問題があるので、
        //       UUIDConverter.getNamesFromUUID(uuid) を呼び出すようにしている。
        NameChange[] names = UUIDConverter.getNamesFromUUID(uuid);
        if ( names == null ) return "-unknown-";
        return names[names.length - 1].name;
    }
}
