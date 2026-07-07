package com.mynyaman.mynyamanapp;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.MotionEvent;
import android.widget.EditText;

import androidx.core.content.ContextCompat;

public class PasswordVisibilityToggle {

    @SuppressLint("ClickableViewAccessibility")
    public static void attach(final EditText editText) {
        // Use a standard Android drawable if a custom one isn't available
        final Drawable eyeIcon = ContextCompat.getDrawable(editText.getContext(), android.R.drawable.ic_menu_view);
        
        // Use relative drawables to preserve drawableStart/drawableEnd
        Drawable[] drawables = editText.getCompoundDrawablesRelative();
        editText.setCompoundDrawablesRelativeWithIntrinsicBounds(
                drawables[0], // Start
                drawables[1], // Top
                eyeIcon,      // End
                drawables[3]  // Bottom
        );

        editText.setOnTouchListener((v, event) -> {
            final int DRAWABLE_END = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Drawable endDrawable = editText.getCompoundDrawablesRelative()[DRAWABLE_END];
                if (endDrawable != null) {
                    // Check if the touch was on the right side (End)
                    int extraPadding = editText.getPaddingEnd();
                    int drawableWidth = endDrawable.getBounds().width();
                    
                    // Simple check for RTL/LTR - for this app we assume standard LTR for now but use relative logic
                    if (event.getX() >= (editText.getWidth() - drawableWidth - extraPadding)) {
                        togglePasswordVisibility(editText);
                        return true;
                    }
                }
            }
            return false;
        });
    }

    private static void togglePasswordVisibility(EditText editText) {
        if (editText.getTransformationMethod() instanceof PasswordTransformationMethod) {
            editText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
        } else {
            editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
        }
        editText.setSelection(editText.getText().length());
    }
}
