package wadotu.orthodoxy.orthowear.presentation;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import wadotu.orthodoxy.orthowear.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btn_holybible = (Button) findViewById(R.id.btn_holybible);

        btn_holybible.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, BookListActivity.class);
                startActivity(intent);
            }
        });

        Button btn_orthocal = (Button) findViewById(R.id.btn_orthocal);

        btn_orthocal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, OrthoCalActivity.class);
                startActivity(intent);
            }
        });

        Button btn_kombo = (Button) findViewById(R.id.btn_kombo);

        btn_kombo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, KomboskiniActivity.class);
                startActivity(intent);
            }
        });

        Button btn_prayers = (Button) findViewById(R.id.btn_prayers);

        btn_prayers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Prayers.class);
                startActivity(intent);
            }
        });

        Button btn_icon = (Button) findViewById(R.id.icon);

        btn_icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Icon.class);
                startActivity(intent);
            }
        });

        Button btn_setting = (Button) findViewById(R.id.btn_setting);

        btn_setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AppSetting.class);
                startActivity(intent);
            }
        });

    }

}
