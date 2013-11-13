package org.telegram.android.ui.plurals;
/**
 * Plural rules for the following locales and languages
 * 
 * Locales: hr ru sr uk be bs sh
 *
 * Languages:
 * - Belarusian (br)
 * - Bosnian (bs)
 * - Croatian (hr)
 * - Russian (ru)
 * - Serbo-Croatian (sh)
 * - Serbian (sr)
 * - Ukrainian (uk)
 *
 * Rules:
 * 	one → n mod 10 is 1 and n mod 100 is not 11;
 * 	few → n mod 10 in 2..4 and n mod 100 not in 12..14;
 * 	many → n mod 10 is 0 or n mod 10 in 5..9 or n mod 100 in 11..14;
 * 	other → everything else (fractions)
 *
 * Reference CLDR Version 1.9 beta (2010-11-16 21:48:45 GMT)
 * @see http://unicode.org/repos/cldr-tmp/trunk/diff/supplemental/language_plural_rules.html
 * @see http://unicode.org/repos/cldr/trunk/common/supplemental/plurals.xml
 * @see plurals.xml (local copy)
 *
 * @package    I18n_Plural
 * @category   Plural Rules
 * @author     Korney Czukowski
 * @copyright  (c) 2011 Korney Czukowski
 * @license    MIT License
 */

/**
 * Converted to Java by Sam Marshak, 2012 
 */
public class PluralRules_Balkan extends PluralRules
{
	public int quantityForNumber(int count)
	{
        int rem100 = count % 100;
        int rem10 = count % 10;
	    
		if (rem10 == 1 && rem100 != 11)
		{
			return QUANTITY_ONE;
		}
		else if (rem10 >= 2 && rem10 <= 4 && !(rem100 >= 12 && rem100 <= 14)) 
		{
			return QUANTITY_FEW;
		}
		else if ((rem10 == 0 || (rem10 >= 5 && rem10 <= 9) || (rem100 >= 11 && rem100 <= 14)))
		{
			return QUANTITY_MANY;
		}
		else
		{
			return QUANTITY_OTHER;
		}
	}
}