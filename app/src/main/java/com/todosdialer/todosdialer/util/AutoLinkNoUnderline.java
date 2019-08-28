package com.todosdialer.todosdialer.util;

import android.content.Context;
import android.text.Spannable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * AutoLink 기능에 의해 생성되는 링크에 밑줄이 표시되지 않게 하는 TextView
 *
 * @author YMKim
 */

public class AutoLinkNoUnderline extends android.support.v7.widget.AppCompatTextView {

    public AutoLinkNoUnderline(Context context) {
        super(context);
    }

    public AutoLinkNoUnderline(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AutoLinkNoUnderline(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        super.setText(text, type);

        stripUnderlines();
    }

    private void stripUnderlines() {

        CharSequence seq = getText();

        //XML inflate 를 할 경우에는 Spannable이 아니므로 ClassCastException이 발생함
        if (TextUtils.isEmpty(seq) || !(seq instanceof Spannable)) {
            return ;
        }

        Spannable s = (Spannable)seq;

        URLSpan[] spans = s.getSpans(0, s.length(), URLSpan.class);

        for (URLSpan span: spans) {
            int start = s.getSpanStart(span);
            int end = s.getSpanEnd(span);
            s.removeSpan(span);
            span = new URLSpanNoUnderline(span.getURL());
            s.setSpan(span, start, end, 0);
        }
    }



    private class URLSpanNoUnderline extends URLSpan {
        public URLSpanNoUnderline(String url) {
            super(url);
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            super.updateDrawState(ds);
            ds.setUnderlineText(false);
        }
    }
}