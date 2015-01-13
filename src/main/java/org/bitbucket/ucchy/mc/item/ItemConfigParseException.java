/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.mc.item;

/**
 * ItemConfigParserでのパース処理の例外クラス
 * @author ucchy
 */
public class ItemConfigParseException extends Exception {

    public ItemConfigParseException(String message) {
        super(message);
    }

    public ItemConfigParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
