package org.alie.playchess;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

public class LogoActivity extends BaseActivity implements View.OnClickListener {

    private ImageView iv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logo);
        initView();
        initListener();
    }

    private void initView() {
        iv = findViewById(R.id.iv);
    }

    private void initListener() {
        iv.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv:
                Toast.makeText(LogoActivity.this, "点击图片", Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
