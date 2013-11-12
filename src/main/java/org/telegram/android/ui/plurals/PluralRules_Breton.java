package org.telegram.android.ui.plurals;
/**
 * Plural rules for Breton language:
 *
 * Locales: br
 *
 * Languages:
 * - Breton (br)
 *
 * Rules:
 *  zero → n is 0;
 *  one → n is 1;
 *  two → n is 2;
 *  few → n is 3;
 *  many → n is 6;
 *  other → everything else
 *
 * Note: for now, the rules are the same as with Welsh language, but according to the ticket
 * http://unicode.org/cldr/trac/ticket/2886 it's probably going to change
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
public class PluralRules_Breton extends PluralRules
{
	public int quantityForNumber(int count)
	{
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
		else if (count == 3)
		{
			return QUANTITY_FEW;
		}
		else if (count == 6)
		{
			return QUANTITY_MANY;
		}
		else
		{
			return QUANTITY_OTHER;
		}
	}
}