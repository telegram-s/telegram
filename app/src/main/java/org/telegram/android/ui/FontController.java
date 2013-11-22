package org.telegram.android.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import org.telegram.android.R;

import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 16.11.12
 * Time: 12:13
 */
public class FontController {

    public static final boolean USE_SUBPIXEL = false;

    private static final HashMap<String, Typeface> typefaces = new HashMap<String, Typeface>();
    public static HashMap<String, String> fontsMap = new HashMap<String, String>();

    static {
        fontsMap.put("0", "fonts/Roboto-Regular.ttf");
        fontsMap.put("1", "fonts/Roboto-Thin.ttf");
        fontsMap.put("2", "fonts/Roboto-ThinItalic.ttf");
        fontsMap.put("3", "fonts/Roboto-Medium.ttf");
        fontsMap.put("4", "fonts/Roboto-MediumItalic.ttf");
        fontsMap.put("5", "fonts/Roboto-Light.ttf");
        fontsMap.put("6", "fonts/Roboto-LightItalic.ttf");
        fontsMap.put("7", "fonts/Roboto-Italic.ttf");
        fontsMap.put("8", "fonts/Roboto-Condensed.ttf");
        fontsMap.put("9", "fonts/Roboto-CondensedItalic.ttf");
        fontsMap.put("10", "fonts/Roboto-Bold.ttf");
        fontsMap.put("11", "fonts/Roboto-BoldCondensed.ttf");
        fontsMap.put("12", "fonts/Roboto-BoldCondensedItalic.ttf");
        fontsMap.put("13", "fonts/Roboto-BoldItalic.ttf");
        fontsMap.put("14", "fonts/Roboto-Black.ttf");
        fontsMap.put("15", "fonts/Roboto-BlackItalic.ttf");

        fontsMap.put("regular", "fonts/Roboto-Regular.ttf");
        fontsMap.put("thin", "fonts/Roboto-Thin.ttf");
        fontsMap.put("thin_italic", "fonts/Roboto-ThinItalic.ttf");
        fontsMap.put("medium", "fonts/Roboto-Medium.ttf");
        fontsMap.put("medium_italic", "fonts/Roboto-MediumItalic.ttf");
        fontsMap.put("light", "fonts/Roboto-Light.ttf");
        fontsMap.put("light_italic", "fonts/Roboto-LightItalic.ttf");
        fontsMap.put("italic", "fonts/Roboto-Italic.ttf");
        fontsMap.put("condensed", "fonts/Roboto-Condensed.ttf");
        fontsMap.put("condensed_italic", "fonts/Roboto-CondensedItalic.ttf");
        fontsMap.put("bold", "fonts/Roboto-Bold.ttf");
        fontsMap.put("bold_condensed", "fonts/Roboto-BoldCondensed.ttf");
        fontsMap.put("bold_condensed_italic", "fonts/Roboto-BoldCondensedItalic.ttf");
        fontsMap.put("bold_italic", "fonts/Roboto-BoldItalic.ttf");
        fontsMap.put("black", "fonts/Roboto-Black.ttf");
        fontsMap.put("black_italic", "fonts/Roboto-BlackItalic.ttf");
    }

    public static Typeface loadTypeface(Context ctx, String key) {
        if (typefaces.containsKey(key)) {
            return typefaces.get(key);
        }

        String asset = key;
        if (fontsMap.containsKey(asset)) {
            asset = fontsMap.get(asset);
        }

        Typeface tf = null;
        try {
            tf = Typeface.createFromAsset(ctx.getAssets(), asset);
        } catch (Exception e) {
            return null;
        }
        typefaces.put(key, tf);
        return tf;
    }

    private static Typeface buildCustomTypeface(Context context, Typeface source) {
        if (source != null && source.isBold()) {
            return loadTypeface(context, "bold");
        } else if (source != null && source.isItalic()) {
            return loadTypeface(context, "italic");
        } else {
            return loadTypeface(context, "regular");
        }
    }

    private static void applyCustomFont(TextView view, Context context, AttributeSet attributeSet) {
        TypedArray a = context.obtainStyledAttributes(attributeSet, R.styleable.CustomFonts);
        String customFont = a.getString(R.styleable.CustomFonts_customFont);
        if (customFont == null) {
            view.setTypeface(buildCustomTypeface(context, view.getTypeface()));
        } else {
            Typeface typeface = loadTypeface(context, customFont);
            if (typeface != null) {
                view.setTypeface(typeface);
            } else {
                view.setTypeface(buildCustomTypeface(context, view.getTypeface()));
            }
        }
        if (USE_SUBPIXEL) {
            view.setPaintFlags(view.getPaintFlags() | Paint.SUBPIXEL_TEXT_FLAG);
        }
    }

    public static View loadTextView(Context context, AttributeSet attributeSet) {
        TextView view = new TextView(context, attributeSet);
        applyCustomFont(view, context, attributeSet);
        return view;
    }

    public static View loadButtonView(Context context, AttributeSet attributeSet) {
        Button view = new Button(context, attributeSet);
        applyCustomFont(view, context, attributeSet);
        return view;
    }

    public static void initInflater(Activity activity) {
        LayoutInflater.from(activity).setFactory(new ViewFactory());
    }

    private static class ViewFactory implements LayoutInflater.Factory {

        @Override
        public View onCreateView(String name, Context context, AttributeSet attributeSet) {
            if (name.equals("TextView")) {
                return FontController.loadTextView(context, attributeSet);
            }
            if (name.equals("Button")) {
                return FontController.loadButtonView(context, attributeSet);
            }
            return null;
        }
    }
}
