package org.telegram.android.ui.plurals;
/**
 * Plural rules for the following locales and languages:
 * 
 * Locales: ak am bh fil tl guw hi ln mg nso ti wa
 * 
 * Languages:
 *  Akan (ak)
 *  Amharic (am)
 *  Bihari (bh)
 *  Filipino (fil)
 *  Gun (guw)
 *  Hindi (hi)
 *  Lingala (ln)
 *  Malagasy (mg)
 *  Northern Sotho (nso)
 *  Tigrinya (ti)
 *  Tagalog (tl)
 *  Walloon (wa)
 *
 * Rules:
 *  one → n in 0..1;
 *  other → everything else
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
public class PluralRules_Zero extends PluralRules
{
	public int quantityForNumber(int count)
	{
        if (count == 0 || count == 1)
		{
			return QUANTITY_ONE;
        }
		else
		{
			return QUANTITY_OTHER;
        }
	}
}