package com.mynyaman.mynyamanapp;

import android.os.Bundle;

public class Chatcon extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startActivity(ConsultantHubActivity.createIntent(this, RoleUtils.ROLE_PARENT));
        finish();
    }
}
