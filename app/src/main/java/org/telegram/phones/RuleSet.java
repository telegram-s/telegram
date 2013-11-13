package org.telegram.phones;

import org.telegram.phones.PhoneRule;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RuleSet {
    public int matchLen;
    public ArrayList<PhoneRule> rules;
    public boolean hasRuleWithIntlPrefix;
    public boolean hasRuleWithTrunkPrefix;
    public static Pattern pattern = Pattern.compile("[0-9]+");

    String format(String str, String intlPrefix, String trunkPrefix, boolean prefixRequired) {
        if (str.length() >= matchLen) {
            String begin = str.substring(0, matchLen);

            int val = 0;
            Matcher matcher = pattern.matcher(begin);
            if (matcher.find()) {
                String num = matcher.group(0);
                val = Integer.parseInt(num);
            }

            for (PhoneRule rule : rules) {
                if (val >= rule.minVal && val <= rule.maxVal && str.length() <= rule.maxLen) {
                    if (prefixRequired) {
                        if (((rule.flag12 & 0x03) == 0 && trunkPrefix == null && intlPrefix == null) || (trunkPrefix != null && (rule.flag12 & 0x01) != 0) || (intlPrefix != null && (rule.flag12 & 0x02) != 0)) {
                            return rule.format(str, intlPrefix, trunkPrefix);
                        }
                    } else {
                        if ((trunkPrefix == null && intlPrefix == null) || (trunkPrefix != null && (rule.flag12 & 0x01) != 0) || (intlPrefix != null && (rule.flag12 & 0x02) != 0)) {
                            return rule.format(str, intlPrefix, trunkPrefix);
                        }
                    }
                }
            }

            if (!prefixRequired) {
                if (intlPrefix != null) {
                    for (PhoneRule rule : rules) {
                        if (val >= rule.minVal && val <= rule.maxVal && str.length() <= rule.maxLen) {
                            if (trunkPrefix == null || (rule.flag12 & 0x01) != 0) {
                                return rule.format(str, intlPrefix, trunkPrefix);
                            }
                        }
                    }
                } else if (trunkPrefix != null) {
                    for (PhoneRule rule : rules) {
                        if (val >= rule.minVal && val <= rule.maxVal && str.length() <= rule.maxLen) {
                            if (intlPrefix == null || (rule.flag12 & 0x02) != 0) {
                                return rule.format(str, intlPrefix, trunkPrefix);
                            }
                        }
                    }
                }
            }

            return null;
        } else {
            return null;
        }
    }

    boolean isValid(String str, String intlPrefix, String trunkPrefix, boolean prefixRequired) {
        if (str.length() >= matchLen) {
            String begin = str.substring(0, matchLen);
            int val = 0;
            Matcher matcher = pattern.matcher(begin);
            if (matcher.find()) {
                String num = matcher.group(0);
                val = Integer.parseInt(num);
            }

            for (PhoneRule rule : rules) {
                if (val >= rule.minVal && val <= rule.maxVal && str.length() == rule.maxLen) {
                    if (prefixRequired) {
                        if (((rule.flag12 & 0x03) == 0 && trunkPrefix == null && intlPrefix == null) || (trunkPrefix != null && (rule.flag12 & 0x01) != 0) || (intlPrefix != null && (rule.flag12 & 0x02) != 0)) {
                            return true;
                        }
                    } else {
                        if ((trunkPrefix == null && intlPrefix == null) || (trunkPrefix != null && (rule.flag12 & 0x01) != 0) || (intlPrefix != null && (rule.flag12 & 0x02) != 0)) {
                            return true;
                        }
                    }
                }
            }

            if (!prefixRequired) {
                if (intlPrefix != null && !hasRuleWithIntlPrefix) {
                    for (PhoneRule rule : rules) {
                        if (val >= rule.minVal && val <= rule.maxVal && str.length() == rule.maxLen) {
                            if (trunkPrefix == null || (rule.flag12 & 0x01) != 0) {
                                return true;
                            }
                        }
                    }
                } else if (trunkPrefix != null && !hasRuleWithTrunkPrefix) {
                    for (PhoneRule rule : rules) {
                        if (val >= rule.minVal && val <= rule.maxVal && str.length() == rule.maxLen) {
                            if (intlPrefix == null || (rule.flag12 & 0x02) != 0) {
                                return true;
                            }
                        }
                    }
                }
            }

            return false;
        } else {
            return false;
        }
    }
}
