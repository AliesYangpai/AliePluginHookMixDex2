package org.alie.aliepluginhookmixdex2;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity implements View.OnClickListener {
    private Button btn1, btn2,btn3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initListener();
    }

    private void initView() {
        btn1 = (Button) findViewById(R.id.btn1);
        btn2 = (Button) findViewById(R.id.btn2);
        btn3 = (Button) findViewById(R.id.btn3);
    }

    private void initListener() {
        btn1.setOnClickListener(this);
        btn2.setOnClickListener(this);
        btn3.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn1:
                goToPluginActivity("org.alie.playchess", "org.alie.playchess.LogoActivity");
                break;
            case R.id.btn2:
                goToPluginActivity("org.alie.playchess", "org.alie.playchess.MainActivity");
                break;
            case R.id.btn3:
                goToPluginActivity("org.alie.aliepluginhookmixdex2", "org.alie.aliepluginhookmixdex2.SecondActivity");
                break;
        }
    }


    private void goToPluginActivity(String packageName, String className) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(packageName, className));
        startActivity(intent);
    }
}
