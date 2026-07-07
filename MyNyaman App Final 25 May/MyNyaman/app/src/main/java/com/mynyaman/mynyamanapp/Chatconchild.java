package com.mynyaman.mynyamanapp;

import android.os.Bundle;

public class Chatconchild extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startActivity(ConsultantHubActivity.createIntent(this, RoleUtils.ROLE_CHILD));
        finish();
    }
}
