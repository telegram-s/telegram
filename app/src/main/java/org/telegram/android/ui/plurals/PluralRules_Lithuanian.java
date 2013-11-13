package org.telegram.android.ui.plurals;
/**
 * Plural rules for Lithuanian language
 * 
 * Locales: lt
 *
 * Languages:
 * - Lithuanian (lt)
 *
 * Rules:
 * 	one → n mod 10 is 1 and n mod 100 not in 11..19;
 * 	few → n mod 10 in 2..9 and n mod 100 not in 11..19;
 * 	other → everything else
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
public class PluralRules_Lithuanian extends PluralRules
{
	public int quantityForNumber(int count)
	{
        int rem100 = count % 100;
        int rem10 = count % 10;
	    
		if (rem10 == 1 && !(rem100 >= 11 && rem100 <= 19))
		{
			return QUANTITY_ONE;
		}
		else if (rem10 >= 2 && rem10 <= 9 && !(rem100 >= 11 && rem100 <= 19))
		{
			return QUANTITY_FEW;
		}
		else
		{
			return QUANTITY_OTHER;
		}
	}
}