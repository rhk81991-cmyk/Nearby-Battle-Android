package com.nox.nearbybattle;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button bluetoothButton = findViewById(R.id.btnBluetooth);
        Button offlineButton = findViewById(R.id.btnOffline);
        Button gamesButton = findViewById(R.id.btnGames);

        bluetoothButton.setOnClickListener(v ->
                Toast.makeText(
                        this,
                        "Bluetooth Battle — Setup coming next!",
                        Toast.LENGTH_SHORT
                ).show()
        );

        offlineButton.setOnClickListener(v ->
                Toast.makeText(
                        this,
                        "Offline Local — Setup coming next!",
                        Toast.LENGTH_SHORT
                ).show()
        );

        gamesButton.setOnClickListener(v ->
                Toast.makeText(
                        this,
                        "Games — Game Hub coming next!",
                        Toast.LENGTH_SHORT
                ).show()
        );
    }
}
