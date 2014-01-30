package org.telegram.android.fragments;

import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.telegram.android.R;
import org.telegram.android.StartActivity;
import org.telegram.android.base.TelegramFragment;
import org.telegram.android.ui.FontController;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 31.10.13
 * Time: 18:02
 */
public class WhatsNewFragment extends TelegramFragment {


    public static class Definition implements Serializable {
        private String title;
        private String[] content;
        private String hint;

        public Definition(String title, String[] content, String hint) {
            this.title = title;
            this.content = content;
            this.hint = hint;
        }

        public String getTitle() {
            return title;
        }

        public String[] getContent() {
            return content;
        }

        public String getHint() {
            return hint;
        }
    }

    private Definition[] definitions;

    public WhatsNewFragment(Definition[] defs) {
        definitions = defs;
    }

    public WhatsNewFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            Object[] tdefs = (Object[]) savedInstanceState.getSerializable("defs");
            definitions = new Definition[tdefs.length];
            for (int i = 0; i < definitions.length; i++) {
                definitions[i] = (Definition) tdefs[i];
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        if (savedInstanceState != null) {
            Object[] tdefs = (Object[]) savedInstanceState.getSerializable("defs");
            definitions = new Definition[tdefs.length];
            for (int i = 0; i < definitions.length; i++) {
                definitions[i] = (Definition) tdefs[i];
            }
        }

        View res = inflater.inflate(R.layout.whats_new, container, false);

        if (definitions == null || definitions.length == 0) {
            res.post(new Runnable() {
                @Override
                public void run() {
                    application.getVersionHolder().markFinishedUpgrade();
                    ((StartActivity) getActivity()).doInitApp(false);
                }
            });
        } else {
            LinearLayout linearLayout = (LinearLayout) res.findViewById(R.id.updatesContainer);
            linearLayout.removeAllViews();
            for (Definition s : definitions) {
                {
                    TextView textView = new TextView(getActivity());
                    textView.setTypeface(FontController.loadTypeface(getActivity(), "regular"));
                    textView.setTextColor(getResources().getColor(R.color.st_black_text));
                    textView.setTextSize(16);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    textView.setLayoutParams(params);
                    textView.setText(s.getTitle());
                    linearLayout.addView(textView);
                }

                for (String record : s.content) {
                    TextView textView = new TextView(getActivity());
                    textView.setTypeface(FontController.loadTypeface(getActivity(), "regular"));
                    textView.setTextColor(getResources().getColor(R.color.st_black_text));
                    textView.setTextSize(16);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    textView.setLayoutParams(params);
                    textView.setText(record);

                    ShapeDrawable drawable = new ShapeDrawable(new RectShape());
                    drawable.getPaint().setColor(0xffc1c1c1);
                    drawable.getPaint().setStyle(Paint.Style.FILL);
                    drawable.setBounds(0, 0, getPx(8), getPx(8));
                    if (application.isRTL()) {
                        textView.setCompoundDrawables(null, null, drawable, null);
                    } else {
                        textView.setCompoundDrawables(drawable, null, null, null);
                    }
                    textView.setCompoundDrawablePadding(getPx(8));
                    linearLayout.addView(textView);
                }

                if (s.getHint() != null) {
                    TextView textView = new TextView(getActivity());
                    textView.setTypeface(FontController.loadTypeface(getActivity(), "regular"));
                    textView.setTextColor(getResources().getColor(R.color.st_black_text));
                    textView.setTextSize(16);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    params.bottomMargin = getPx(16);
                    textView.setLayoutParams(params);
                    textView.setText(s.getHint());
                    linearLayout.addView(textView);
                }
            }
        }

        res.findViewById(R.id.continueMessaging).setOnClickListener(secure(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                application.getVersionHolder().markFinishedUpgrade();
                ((StartActivity) getActivity()).doInitApp(false);
            }
        }));

        return res;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("defs", definitions);
    }
}
