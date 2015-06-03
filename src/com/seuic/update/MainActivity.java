package com.seuic.update;


import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity implements OnClickListener {

    
    private Button btm;
    public UpdateManager manager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btm = (Button) findViewById(R.id.button);
        btm.setOnClickListener(this);
        manager = UpdateManager.getInstance(this);
    }
    
    @Override
    public void onClick(View v) {
        manager.showCheckUpdateDailog();
    }

}
