package org.telegram.android.ui.plurals;
/**
 * Plural rules for Arabic language
 *
 * Locales: ar
 *
 * Languages:
 * - Arabic (ar)
 *
 * Rules:
 * 	zero → n is 0;
 * 	one → n is 1;
 * 	two → n is 2;
 * 	few → n mod 100 in 3..10;
 * 	many → n mod 100 in 11..99;
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
public class PluralRules_Arabic extends PluralRules
{
	public int quantityForNumber(int count)
	{
	    int rem100 = count % 100;
	            
		if (count == 0)
		{
			return QUANTITY_ZERO;
		}
		else if (count == 1)
		{
			return QUANTITY_ONE;
		}
		else if (count == 2)
		{
			return QUANTITY_TWO;
		}
		else if (rem100 >= 3 && rem100 <= 10)
		{
			return QUANTITY_FEW;
		}
		else if (rem100 >= 11 && rem100 <= 99)
		{
			return QUANTITY_MANY;
		}
		else
		{
			return QUANTITY_OTHER;
        }
	}
}