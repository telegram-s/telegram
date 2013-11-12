package org.telegram.android.ui.plurals;
/**
 * Plural rules for the following locales and languages:
 * 
 * Locales: bem brx da de el en eo es et fi fo gl he iw it nb nl nn no pt_PT sv af bg bn ca eu fur fy gu ha is ku lb ml
 *          mr nah ne om or pa pap ps so sq sw ta te tk ur zu mn gsw chr rm pt
 * (in original order)
 * 
 * Languages:
 *  Afrikaans (af)
 *  Bemba (bem)
 *  Bulgarian (bg)
 *  Bodo (brx)
 *  Bengali (bn)
 *  Catalan (ca)
 *  Cherokee (chr)
 *  Danish (da)
 *  German (de)
 *  Greek (el)
 *  English (en)
 *  Esperanto (eo)
 *  Spanish (es)
 *  Estonian (et)
 *  Basque (eu)
 *  Finnish (fi)
 *  Faroese (fo)
 *  Friulian (fur)
 *  Western Frisian (fy)
 *  Galician (gl)
 *  Swiss German (gsw)
 *  Gujarati (gu)
 *  Hausa (ha)
 *  Hebrew (he)
 *  Icelandic (is)
 *  Italian (it)
 *  iw (iw)
 *  Kurdish (ku)
 *  Luxembourgish (lb)
 *  Malayalam (ml)
 *  Mongolian (mn)
 *  Marathi (mr)
 *  Nahuatl (nah)
 *  Norwegian Bokmål (nb)
 *  Nepali (ne)
 *  Dutch (nl)
 *  Norwegian Nynorsk (nn)
 *  Norwegian (no)
 *  Oromo (om)
 *  Oriya (or)
 *  Punjabi (pa)
 *  Papiamento (pap)
 *  Pashto (ps)
 *  Portuguese (pt)
 *  Romansh (rm)
 *  Somali (so)
 *  Albanian (sq)
 *  Swedish (sv)
 *  Swahili (sw)
 *  Tamil (ta)
 *  Telugu (te)
 *  Turkmen (tk)
 *  Urdu (ur)
 *  Zulu (zu)
 *
 * Rules:
 * 	one → n is 1;
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
 *
 */

/**
 * Converted to Java by Sam Marshak, 2012 
 */
public class PluralRules_One extends PluralRules
{
	public int quantityForNumber(int count)
	{
		return count == 1 ? QUANTITY_ONE : QUANTITY_OTHER;
	}
}