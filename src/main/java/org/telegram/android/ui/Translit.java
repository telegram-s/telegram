package org.telegram.android.ui;

import java.util.HashMap;

/**
 * Author: Korshakov Stepan
 * Created: 11.07.13 13:29
 */
public class Translit {

    private static HashMap<Character, String> replaceMap = new HashMap<Character, String>();

    static {
        replaceMap.put('a', "а");
        replaceMap.put('b', "б");
        replaceMap.put('c', "ц");
        replaceMap.put('d', "д");
        replaceMap.put('e', "е");
        replaceMap.put('f', "ф");
        replaceMap.put('g', "г");
        replaceMap.put('h', "х");
        replaceMap.put('i', "и");
        replaceMap.put('j', "й");
        replaceMap.put('k', "к");
        replaceMap.put('l', "л");
        replaceMap.put('m', "м");
        replaceMap.put('n', "н");
        replaceMap.put('o', "о");
        replaceMap.put('p', "п");
        replaceMap.put('q', "к");
        replaceMap.put('r', "р");
        replaceMap.put('s', "с");
        replaceMap.put('t', "т");
        replaceMap.put('u', "ю");
        replaceMap.put('v', "в");
        replaceMap.put('w', "в");
        replaceMap.put('x', "кс");
        replaceMap.put('y', "у");
        replaceMap.put('z', "з");

        replaceMap.put('а', "a");
        replaceMap.put('б', "b");
        replaceMap.put('в', "v");
        replaceMap.put('г', "g");
        replaceMap.put('д', "d");
        replaceMap.put('е', "e");
        replaceMap.put('ё', "e");
        replaceMap.put('ж', "zh");
        replaceMap.put('з', "z");
        replaceMap.put('и', "i");
        replaceMap.put('й', "j");
        replaceMap.put('к', "k");
        replaceMap.put('л', "l");
        replaceMap.put('м', "m");
        replaceMap.put('н', "n");
        replaceMap.put('о', "o");
        replaceMap.put('п', "p");
        replaceMap.put('р', "r");
        replaceMap.put('с', "s");
        replaceMap.put('т', "t");
        replaceMap.put('у', "u");
        replaceMap.put('ф', "f");
        replaceMap.put('х', "h");
        replaceMap.put('ц', "c");
        replaceMap.put('ч', "ch");
        replaceMap.put('ш', "sh");
        replaceMap.put('щ', "shh");
        replaceMap.put('ъ', "");
        replaceMap.put('ы', "u");
        replaceMap.put('ь', "");
        replaceMap.put('э', "je");
        replaceMap.put('ю', "ju");
        replaceMap.put('я', "ja");
    }

    public static String translitText(String text) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char src = text.charAt(i);
            if (replaceMap.containsKey(src)) {
                builder.append(replaceMap.get(src));
            } else {
                builder.append(src);
            }
        }
        return builder.toString();
    }
}
