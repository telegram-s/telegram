package org.telegram.android.ui.plurals;
/**
 * Plural rules for Maltese language:
 * 
 * Locales: mt
 *
 * Languages:
 * - Maltese (mt)
 *
 * Rules:
 * 	one → n is 1;
 * 	few → n is 0 or n mod 100 in 2..10;
 * 	many → n mod 100 in 11..19;
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
public class PluralRules_Maltese extends PluralRules
{
	public int quantityForNumber(int count)
	{
        int rem100 = count % 100;
        
		if (count == 1)
		{
			return QUANTITY_ONE;
		}
		else if (count == 0 || (rem100 >= 2 && rem100 <= 10))
		{
			return QUANTITY_FEW;
		}
		else if (rem100 >= 11 && rem100 <= 19)
		{
			return QUANTITY_MANY;
		}
		else
		{
			return QUANTITY_OTHER;
		}
	}
}