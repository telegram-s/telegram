package org.telegram.android.ui.plurals;
/**
 * Plural rules for the following locales and languages:
 * 
 * Locales: az bm fa ig hu ja kde kea ko my ses sg to tr vi wo yo zh bo dz id jv ka km kn ms th
 * 
 * Languages:
 *  Azerbaijani (az)
 *  Bambara (bm)
 *  Persian (fa)
 *  Igbo (ig)
 *  Hungarian (hu)
 *  Japanese (ja)
 *  Makonde (kde)
 *  Kabuverdianu (kea)
 *  Korean (ko)
 *  Burmese (my)
 *  Koyraboro Senni (ses)
 *  Sango (sg)
 *  Tonga (to)
 *  Turkish (tr)
 *  Vietnamese (vi)
 *  Wolof (wo)
 *  Yoruba (yo)
 *  Chinese (zh)
 *  Tibetan (bo)
 *  Dzongkha (dz)
 *  Indonesian (id)
 *  Javanese (jv)
 *  Georgian (ka)
 *  Khmer (km)
 *  Kannada (kn)
 *  Malay (ms)
 *  Thai (th)
 *
 * These are known to have no plurals, there are no rules:
 *  other → everything
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
public class PluralRules_None extends PluralRules
{
	public int quantityForNumber(int count)
	{
		return QUANTITY_OTHER;
	}
}