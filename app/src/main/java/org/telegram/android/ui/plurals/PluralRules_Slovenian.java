package org.telegram.android.ui.plurals;
/**
 * Plural rules for Slovenian language:
 *
 * Locales: sl
 *
 * Languages:
 * - Slovenian (sl)
 *
 * Rules:
 * 	one → n mod 100 is 1;
 * 	two → n mod 100 is 2;
 * 	few → n mod 100 in 3..4;
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
public class PluralRules_Slovenian extends PluralRules
{
	public int quantityForNumber(int count)
	{
        int rem100 = count % 100;
        
		if (rem100 == 1)
		{
			return QUANTITY_ONE;
		}
		else if (rem100 == 2)
		{
			return QUANTITY_TWO;
		}
		else if (rem100 >= 3 && rem100 <= 4)
		{
			return QUANTITY_FEW;
		}
		else
		{
			return QUANTITY_OTHER;
		}
	}
}