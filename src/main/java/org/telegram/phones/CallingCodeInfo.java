package org.telegram.phones;

import java.util.ArrayList;

public class CallingCodeInfo {
    public ArrayList<String> countries;
    public String callingCode;
    public ArrayList<String> trunkPrefixes;
    public ArrayList<String> intlPrefixes;
    public ArrayList<RuleSet> ruleSets;
    //public ArrayList formatStrings;

    String matchingAccessCode(String str) {
        for (String code : intlPrefixes) {
            if (str.startsWith(code)) {
                return code;
            }
        }
        return null;
    }

    String matchingTrunkCode(String str) {
        for (String code : trunkPrefixes) {
            if (str.startsWith(code)) {
                return code;
            }
        }

        return null;
    }

    String format(String orig) {
        String str = orig;
        String trunkPrefix = null;
        String intlPrefix = null;
        if (str.startsWith(callingCode)) {
            intlPrefix = callingCode;
            str = str.substring(intlPrefix.length());
        } else {
            String trunk = matchingTrunkCode(str);
            if (trunk != null) {
                trunkPrefix = trunk;
                str = str.substring(trunkPrefix.length());
            }
        }

        for (RuleSet set : ruleSets) {
            String phone = set.format(str, intlPrefix, trunkPrefix, true);
            if (phone != null) {
                return phone;
            }
        }

        for (RuleSet set : ruleSets) {
            String phone = set.format(str, intlPrefix, trunkPrefix, false);
            if (phone != null) {
                return phone;
            }
        }

        if (intlPrefix != null && str.length() != 0) {
            return String.format("%s %s", intlPrefix, str);
        }

        return orig;
    }

    boolean isValidPhoneNumber(String orig) {
        String str = orig;
        String trunkPrefix = null;
        String intlPrefix = null;
        if (str.startsWith(callingCode)) {
            intlPrefix = callingCode;
            str = str.substring(intlPrefix.length());
        } else {
            String trunk = matchingTrunkCode(str);
            if (trunk != null) {
                trunkPrefix = trunk;
                str = str.substring(trunkPrefix.length());
            }
        }

        for (RuleSet set : ruleSets) {
            boolean valid = set.isValid(str, intlPrefix, trunkPrefix, true);
            if (valid) {
                return valid;
            }
        }

        for (RuleSet set : ruleSets) {
            boolean valid = set.isValid(str, intlPrefix, trunkPrefix, false);
            if (valid) {
                return valid;
            }
        }

        return false;
    }
}
