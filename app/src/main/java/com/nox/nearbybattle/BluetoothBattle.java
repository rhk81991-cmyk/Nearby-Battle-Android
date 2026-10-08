package com.nox.nearbybattle;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class BluetoothBattle {

    private final Activity activity;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private BluetoothServerSocket serverSocket;

    private Thread workerThread;
    private volatile boolean running = false;

    private LinearLayout root;
    private LinearLayout deviceList;
    private TextView statusText;
    private TextView messagesText;
    private Button hostButton;
    private Button scanButton;
    private Button sendButton;

    private final ArrayList<BluetoothDevice> devices =
            new ArrayList<>();

    private BroadcastReceiver discoveryReceiver;

    private static final String SERVICE_NAME =
            "NoXNearbyBattle";

    private static final UUID APP_UUID =
            UUID.fromString(
                    "7b2a9c10-6d41-4f83-a852-9e1c0b7d6201"
            );

    public BluetoothBattle(Activity activity) {
        this.activity = activity;
    }

    // OPEN BLUETOOTH SCREEN

    public void start() {
        if (activity.isFinishing()) {
            return;
        }

        if (Build.VERSION.SDK_INT >= 31
                && activity.checkSelfPermission(
                Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {

            activity.requestPermissions(
                    new String[]{
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    7001
            );

            return;
        }

        if (Build.VERSION.SDK_INT >= 23
                && Build.VERSION.SDK_INT <= 30
                && activity.checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            activity.requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    7001
            );

            return;
        }

        bluetoothAdapter =
                BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(
                    activity,
                    "This phone does not support Bluetooth.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent =
                    new Intent(
                            BluetoothAdapter.ACTION_REQUEST_ENABLE
                    );

            activity.startActivityForResult(
                    enableIntent,
                    7002
            );

            return;
        }

        showScreen();
    }

    // UI

    private void showScreen() {
        root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);

        root.setBackgroundColor(
                android.graphics.Color.rgb(18, 18, 28)
        );

        ScrollView scrollView = new ScrollView(activity);

        LinearLayout content =
                new LinearLayout(activity);

        content.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(activity);
        title.setText("⚔️ NoX Bluetooth Battle");
        title.setTextSize(25);
        title.setTextColor(
                android.graphics.Color.WHITE
        );

        content.addView(title);

        statusText = new TextView(activity);
        statusText.setText(
                "Bluetooth ready. Create or join a room."
        );
        statusText.setTextSize(16);
        statusText.setTextColor(
                android.graphics.Color.LTGRAY
        );
        statusText.setPadding(0, 20, 0, 20);

        content.addView(statusText);

        hostButton = makeButton("👑 CREATE ROOM");
        scanButton = makeButton("🔍 JOIN ROOM");
        sendButton = makeButton("💬 SEND TEST MESSAGE");

        content.addView(hostButton);
        content.addView(scanButton);

        TextView devicesTitle = new TextView(activity);
        devicesTitle.setText("Available devices");
        devicesTitle.setTextSize(18);
        devicesTitle.setTextColor(
                android.graphics.Color.WHITE
        );
        devicesTitle.setPadding(0, 18, 0, 8);

        content.addView(devicesTitle);

        deviceList = new LinearLayout(activity);
        deviceList.setOrientation(
                LinearLayout.VERTICAL
        );

        content.addView(deviceList);

        content.addView(sendButton);

        messagesText = new TextView(activity);
        messagesText.setText("Messages:\n");
        messagesText.setTextSize(15);
        messagesText.setTextColor(
                android.graphics.Color.WHITE
        );
        messagesText.setPadding(0, 18, 0, 18);

        content.addView(messagesText);

        Button backButton = makeButton("⬅ BACK TO HOME");

        content.addView(backButton);

        scrollView.addView(content);
        root.addView(
                scrollView,
                new LinearLayout.LayoutParams(
                        -1, 0, 1
                )
        );

        activity.setContentView(root);

        hostButton.setOnClickListener(v ->
                createRoom()
        );

        scanButton.setOnClickListener(v ->
                discoverDevices()
        );

        sendButton.setEnabled(false);

        sendButton.setOnClickListener(v ->
                sendMessage("Hello from NoX!")
        );

        backButton.setOnClickListener(v -> {
            stop();
            if (activity instanceof MainActivity) {
                ((MainActivity) activity).showHomeScreen();
            }
        });
    }

    private Button makeButton(String text) {
        Button button = new Button(activity);
        button.setText(text);
        button.setTextSize(15);
        return button;
    }

    private void updateStatus(String text) {
        handler.post(() -> {
            if (statusText != null) {
                statusText.setText(text);
            }
        });
    }

    private void addMessage(String message) {
        handler.post(() -> {
            if (messagesText != null) {
                messagesText.append(message + "\n");
            }
        });
    }

    // CREATE ROOM

    private void createRoom() {
        if (!hasBluetoothPermissions()) {
            start();
            return;
        }

        stopWorkerOnly();

        updateStatus("Creating room. Waiting for a player...");
        hostButton.setEnabled(false);

        workerThread = new Thread(() -> {
            try {
                serverSocket =
                        bluetoothAdapter.listenUsingRfcommWithServiceRecord(
                                SERVICE_NAME,
                                APP_UUID
                        );

                BluetoothSocket socket =
                        serverSocket.accept();

                serverSocket.close();
                serverSocket = null;

                connected(socket);

            } catch (SecurityException e) {
                updateStatus(
                        "Bluetooth permission missing."
                );
                hostButton.setEnabled(true);

            } catch (IOException e) {
                updateStatus(
                        "Room closed or connection failed."
                );
                hostButton.setEnabled(true);
            }
        });

        workerThread.start();
    }

    // JOIN ROOM: FIND DEVICES

    private void discoverDevices() {
        if (!hasBluetoothPermissions()) {
            start();
            return;
        }

        stopDiscovery();

        devices.clear();
        deviceList.removeAllViews();

        updateStatus(
                "Searching for devices... Pairing may be required."
        );

        try {
            Set<BluetoothDevice> paired =
                    bluetoothAdapter.getBondedDevices();

            for (BluetoothDevice device : paired) {
                addDevice(device);
            }

            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }

            discoveryReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(
                        Context context,
                        Intent intent
                ) {
                    String action = intent.getAction();

                    if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                        BluetoothDevice device =
                                intent.getParcelableExtra(
                                        BluetoothDevice.EXTRA_DEVICE
                                );

                        if (device != null) {
                            addDevice(device);
                        }
                    } else if (
                            BluetoothAdapter.ACTION_DISCOVERY_FINISHED
                                    .equals(action)
                    ) {
                        updateStatus(
                                "Search finished. Select a device."
                        );
                    }
                }
            };

            IntentFilter filter =
                    new IntentFilter();

            filter.addAction(
                    BluetoothDevice.ACTION_FOUND
            );

            filter.addAction(
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED
            );

            if (Build.VERSION.SDK_INT >= 33) {
                activity.registerReceiver(
                        discoveryReceiver,
                        filter,
                        Context.RECEIVER_NOT_EXPORTED
                );
            } else {
                activity.registerReceiver(
                        discoveryReceiver,
                        filter
                );
            }

            boolean started =
                    bluetoothAdapter.startDiscovery();

            if (!started) {
                updateStatus(
                        "Could not start search. Check Bluetooth."
                );
            }

        } catch (SecurityException e) {
            updateStatus(
                    "Bluetooth permission denied."
            );
        }
    }

    private void addDevice(BluetoothDevice device) {
        handler.post(() -> {
            for (BluetoothDevice existing : devices) {
                if (existing.getAddress().equals(
                        device.getAddress())) {
                    return;
                }
            }

            devices.add(device);

            String name;

            try {
                name = device.getName();
            } catch (SecurityException e) {
                name = null;
            }

            if (name == null || name.trim().isEmpty()) {
                name = "Unknown device";
            }

            Button button = makeButton(
                    name + "\nTap to connect"
            );

            deviceList.addView(button);

            button.setOnClickListener(v ->
                    connectToDevice(device)
            );
        });
    }

    // CONNECT TO DEVICE

    private void connectToDevice(
            BluetoothDevice device
    ) {
        if (!hasBluetoothPermissions()) {
            start();
            return;
        }

        stopDiscovery();
        stopWorkerOnly();

        updateStatus("Connecting...");
        scanButton.setEnabled(false);

        workerThread = new Thread(() -> {
            BluetoothSocket socket = null;

            try {
                if (bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.cancelDiscovery();
                }

                socket =
                        device.createRfcommSocketToServiceRecord(
                                APP_UUID
                        );

                socket.connect();

                connected(socket);

            } catch (SecurityException e) {
                closeSocket(socket);
                updateStatus(
                        "Bluetooth permission missing."
                );
                scanButton.setEnabled(true);

            } catch (IOException e) {
                closeSocket(socket);
                updateStatus(
                        "Connection failed. Pair devices and try again."
                );
                scanButton.setEnabled(true);
            }
        });

        workerThread.start();
    }

    // CONNECTED: READ AND WRITE MESSAGES

    private void connected(BluetoothSocket socket) {
        bluetoothSocket = socket;
        running = true;

        updateStatus("✅ Bluetooth connected!");
        addMessage("Connection established.");

        handler.post(() -> {
            sendButton.setEnabled(true);
            hostButton.setEnabled(false);
            scanButton.setEnabled(false);
        });

        try {
            InputStream input =
                    socket.getInputStream();

            OutputStream output =
                    socket.getOutputStream();

            byte[] buffer = new byte[1024];

            while (running) {
                int count = input.read(buffer);

                if (count == -1) {
                    break;
                }

                String message = new String(
                        buffer,
                        0,
                        count,
                        java.nio.charset.StandardCharsets.UTF_8
                );

                addMessage("Opponent: " + message);
            }

        } catch (IOException e) {
            updateStatus("Connection disconnected.");
        } finally {
            closeSocket(socket);
            running = false;

            handler.post(() -> {
                sendButton.setEnabled(false);
                hostButton.setEnabled(true);
                scanButton.setEnabled(true);
            });
        }
    }

    private void sendMessage(String message) {
        BluetoothSocket socket = bluetoothSocket;

        if (socket == null || !socket.isConnected()) {
            updateStatus("No connected player.");
            return;
        }

        new Thread(() -> {
            try {
                OutputStream output =
                        socket.getOutputStream();

                output.write(
                        (message + "\n").getBytes(
                                java.nio.charset.StandardCharsets.UTF_8
                        )
                );

                output.flush();

                addMessage("You: " + message);

            } catch (IOException e) {
                updateStatus("Could not send message.");
            }
        }).start();
    }

    // PERMISSIONS

    private boolean hasBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= 31) {
            return activity.checkSelfPermission(
                    Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
                    && activity.checkSelfPermission(
                    Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED;
        }

        if (Build.VERSION.SDK_INT >= 23) {
            return activity.checkSelfPermission(
                    Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED;
        }

        return true;
    }

    // CLEANUP

    private void stopDiscovery() {
        try {
            if (bluetoothAdapter != null
                    && bluetoothAdapter.isDiscovering()
                    && hasBluetoothPermissions()) {
                bluetoothAdapter.cancelDiscovery();
            }
        } catch (SecurityException ignored) {
        }

        if (discoveryReceiver != null) {
            try {
                activity.unregisterReceiver(
                        discoveryReceiver
                );
            } catch (IllegalArgumentException ignored) {
            }

            discoveryReceiver = null;
        }
    }

    private void stopWorkerOnly() {
        running = false;

        stopDiscovery();

        try {
            if (serverSocket != null) {
                serverSocket.close();
                serverSocket = null;
            }
        } catch (IOException ignored) {
        }

        closeSocket(bluetoothSocket);
        bluetoothSocket = null;
    }

    private void closeSocket(BluetoothSocket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    public void stop() {
        stopWorkerOnly();

        if (workerThread != null) {
            workerThread.interrupt();
            workerThread = null;
        }
    }
}
