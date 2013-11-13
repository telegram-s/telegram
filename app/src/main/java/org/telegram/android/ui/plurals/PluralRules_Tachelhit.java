package org.telegram.android.ui.plurals;
/**
 * Plural rules for Tachelhit language:
 *
 * Locales: shi
 *
 * Languages:
 * - Tachelhit (shi)
 *
 * Rules:
 * 	one → n within 0..1;
 * 	few → n in 2..10;
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
public class PluralRules_Tachelhit extends PluralRules
{
	public int quantityForNumber(int count)
	{
		if (count >= 0 && count <= 1)
		{
			return QUANTITY_ONE;
		}
		else if (count >= 2 && count <= 10)
		{
			return QUANTITY_FEW;
        }
		else
		{
			return QUANTITY_OTHER;
        }
	}
}