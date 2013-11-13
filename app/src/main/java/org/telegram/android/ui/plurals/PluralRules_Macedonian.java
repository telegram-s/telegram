package org.telegram.android.ui.plurals;
/**
 * Plural rules for Macedonian language
 * 
 * Locales: mk
 *
 * Languages:
 * - Macedonian (mk)
 *
 * Rules:
 * 	one → n mod 10 is 1 and n is not 11;
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
public class PluralRules_Macedonian extends PluralRules
{
	public int quantityForNumber(int count)
	{
		if (count % 10 == 1 && count != 11)
		{
			return QUANTITY_ONE;
		}
		else
		{
			return QUANTITY_OTHER;
		}
	}
}