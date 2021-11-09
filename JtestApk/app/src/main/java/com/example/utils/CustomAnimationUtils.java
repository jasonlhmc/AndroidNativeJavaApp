package com.example.utils;

import android.content.Context;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.example.jtestapk.R;

public class CustomAnimationUtils {

    public Animation fadeInAnimationDefault(Context context) {
        Animation anim = AnimationUtils.loadAnimation(context, R.anim.fragment_fade_enter);
        anim.setStartOffset(75);
        return anim;
    }

    public Animation fadeOutAnimationDefault(Context context) {
        Animation anim = AnimationUtils.loadAnimation(context, R.anim.fragment_fade_exit);
        anim.setStartOffset(100);
        return anim;
    }

}
