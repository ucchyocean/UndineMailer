/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine.tellraw;

/**
 * クリックイベント
 * @author ucchy
 */
public enum ClickEventType {

    /** コマンドの実行 */
    RUN_COMMAND("run_command"),

    /** コマンドの提示 */
    SUGGEST_COMMAND("suggest_command"),

    /** リンク先へ飛ぶ */
    OPEN_URL("open_url");

    private String text;

    /**
     * コンストラクタ
     * @param text
     */
    private ClickEventType(String text) {
        this.text = text;
    }

    /**
     * 文字列表現を返す
     * @see java.lang.Enum#toString()
     */
    public String toString() {
        return text;
    }
}
