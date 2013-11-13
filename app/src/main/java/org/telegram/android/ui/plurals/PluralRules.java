package org.telegram.android.ui.plurals;

/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/*
 * Yuck-o.  This is not the right way to implement this.  When the ICU PluralRules
 * object has been integrated to android, we should switch to that.  For now, yuck-o.
 */

@SuppressWarnings("nls")
abstract public class PluralRules 
{
    static final int QUANTITY_OTHER = 0x0000;
    static final int QUANTITY_ZERO  = 0x0001;
    static final int QUANTITY_ONE   = 0x0002;
    static final int QUANTITY_TWO   = 0x0004;
    static final int QUANTITY_FEW   = 0x0008;
    static final int QUANTITY_MANY  = 0x0010;

    static final int ID_OTHER = 0x01000004;
    static final int ID_ZERO  = 0x01000005;

    abstract int quantityForNumber(int n);

    final int attrForNumber(int n) {
        return PluralRules.attrForQuantity(quantityForNumber(n));
    }

    static final int attrForQuantity(int quantity) {
        // see include/utils/ResourceTypes.h
        switch (quantity) {
            case QUANTITY_ZERO: return ID_ZERO;
            case QUANTITY_ONE:  return 0x01000006;
            case QUANTITY_TWO:  return 0x01000007;
            case QUANTITY_FEW:  return 0x01000008;
            case QUANTITY_MANY: return 0x01000009;
            default:            return ID_OTHER;
        }
    }

    static final String stringForQuantity(int quantity) {
        switch (quantity) {
            case QUANTITY_ZERO:
                return "zero";
            case QUANTITY_ONE:
                return "one";
            case QUANTITY_TWO:
                return "two";
            case QUANTITY_FEW:
                return "few";
            case QUANTITY_MANY:
                return "many";
            default:
                return "other";
        }
    }
    

    private static Map<String, PluralRules> allRules = new HashMap<String, PluralRules>(); 
    
    static
    {
        addRules( new String [] {"bem", "brx", "da", "de", "el", "en", "eo", "es", "et", "fi", "fo", "gl", "he", "iw", "it", "nb",
                                 "nl", "nn", "no", "sv", "af", "bg", "bn", "ca", "eu", "fur", "fy", "gu", "ha", "is", "ku",
                                 "lb", "ml", "mr", "nah", "ne", "om", "or", "pa", "pap", "ps", "so", "sq", "sw", "ta", "te",
                                 "tk", "ur", "zu", "mn", "gsw", "chr", "rm", "pt"}, new PluralRules_One());        
    	addRules( new String [] {"cs", "sk"}, new PluralRules_Czech());
    	addRules( new String [] {"ff", "fr", "kab"}, new PluralRules_French());
        addRules( new String [] {"hr", "ru", "sr", "uk", "be", "bs", "sh"}, new PluralRules_Balkan());
        addRules( new String [] {"lv"}, new PluralRules_Latvian());
        addRules( new String [] {"lt"}, new PluralRules_Lithuanian());
        addRules( new String [] {"pl"}, new PluralRules_Polish());
        addRules( new String [] {"ro", "mo"}, new PluralRules_Romanian());
        addRules( new String [] {"sl"}, new PluralRules_Slovenian());
        addRules( new String [] {"ar"}, new PluralRules_Arabic());
        addRules( new String [] {"mk"}, new PluralRules_Macedonian());
        addRules( new String [] {"cy"}, new PluralRules_Welsh());
        addRules( new String [] {"br"}, new PluralRules_Breton());
        addRules( new String [] {"lag"}, new PluralRules_Langi());
        addRules( new String [] {"shi"}, new PluralRules_Tachelhit());
        addRules( new String [] {"mt"}, new PluralRules_Maltese());
        addRules( new String [] {"ga", "se", "sma", "smi", "smj", "smn", "sms"}, new PluralRules_Two());
        addRules( new String [] {"ak", "am", "bh", "fil", "tl", "guw", "hi", "ln", "mg", "nso", "ti", "wa"}, new PluralRules_Zero());
        addRules( new String [] {"az", "bm", "fa", "ig", "hu", "ja", "kde", "kea", "ko", "my", "ses", "sg", "to",
                                 "tr", "vi", "wo", "yo", "zh", "bo", "dz", "id", "jv", "ka", "km", "kn", "ms", "th"}, new PluralRules_None());
    }
    
    public static void addRules( String [] languages, PluralRules rules ) {
    	for ( String language : languages )
    		allRules.put(language, rules);
    }
    
    public static void addRules( String language, PluralRules rules ) {
    	allRules.put(language, rules);
    }
    
    static final PluralRules ruleForLocale(Locale locale) {
        
    	return allRules.get(locale.getLanguage());
    }
}
