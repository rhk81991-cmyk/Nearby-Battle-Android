package com.nox.nearbybattle;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
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
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class BluetoothBattle {

    private final Activity activity;
    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private BluetoothAdapter bluetoothAdapter;
    private volatile BluetoothSocket bluetoothSocket;
    private BluetoothServerSocket serverSocket;

    private Thread workerThread;

    private volatile boolean running = false;
    private volatile boolean roomVerified = false;
    private volatile String roomPassword = "";

    private volatile BufferedReader socketReader;
    private volatile BufferedWriter socketWriter;

    private final Object writeLock = new Object();

    private LinearLayout root;
    private LinearLayout deviceList;
    private TextView statusText;
    private TextView messagesText;

    private Button hostButton;
    private Button scanButton;
    private Button sendButton;
    private Button startBattleButton;

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
                            Manifest.permission.BLUETOOTH_SCAN
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
                new LinearLayout.LayoutParams(-1, 0, 1)
        );

        activity.setContentView(root);

        hostButton.setOnClickListener(v -> createRoom());

        scanButton.setOnClickListener(v -> discoverDevices());

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

    private void setSendEnabled(boolean enabled) {
        handler.post(() -> {
            if (sendButton != null) {
                sendButton.setEnabled(enabled);
            }
        });
    }

    // CREATE PROTECTED ROOM

    private void createRoom() {
        EditText passwordInput = new EditText(activity);

        passwordInput.setHint("Set room password");
        passwordInput.setSingleLine(true);
        passwordInput.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        new AlertDialog.Builder(activity)
                .setTitle("🔒 Create Room")
                .setMessage("Set a password for your room.")
                .setView(passwordInput)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Create", (dialog, which) -> {
                    String password =
                            passwordInput.getText().toString();

                    if (password.trim().isEmpty()) {
                        Toast.makeText(
                                activity,
                                "Password cannot be empty.",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    startRoomServer(password);
                })
                .show();
    }

    private void startRoomServer(String password) {
        if (!hasBluetoothPermissions()) {
            start();
            return;
        }

        stopWorkerOnly();

        roomPassword = password;
        roomVerified = false;

        updateStatus("Creating protected room...");
        setSendEnabled(false);

        handler.post(() -> {
            hostButton.setEnabled(false);
            scanButton.setEnabled(false);
        });

        workerThread = new Thread(() -> {
            try {
                serverSocket =
                        bluetoothAdapter
                                .listenUsingRfcommWithServiceRecord(
                                        SERVICE_NAME,
                                        APP_UUID
                                );

                updateStatus(
                        "Room ready. Waiting for a player..."
                );

                BluetoothSocket socket =
                        serverSocket.accept();

                if (serverSocket != null) {
                    serverSocket.close();
                    serverSocket = null;
                }

                connected(socket, true);

            } catch (SecurityException e) {
                updateStatus("Bluetooth permission missing.");
                resetConnectionButtons();

            } catch (IOException e) {
                if (running) {
                    updateStatus(
                            "Room closed or connection failed."
                    );
                } else {
                    updateStatus("Room closed.");
                }

                resetConnectionButtons();
            }
        });

        workerThread.start();
    }

    // DISCOVER DEVICES

    private void discoverDevices() {
        if (!hasBluetoothPermissions()) {
            start();
            return;
        }

        stopDiscovery();

        devices.clear();
        deviceList.removeAllViews();

        updateStatus("Searching for devices...");

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

            IntentFilter filter = new IntentFilter();

            filter.addAction(BluetoothDevice.ACTION_FOUND);
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

            boolean started = bluetoothAdapter.startDiscovery();

            if (!started) {
                updateStatus(
                        "Could not start search. Check Bluetooth."
                );
            }

        } catch (SecurityException e) {
            updateStatus("Bluetooth permission denied.");
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

    // CONNECT TO ROOM HOST

    private void connectToDevice(BluetoothDevice device) {
        if (!hasBluetoothPermissions()) {
            start();
            return;
        }

        stopDiscovery();
        stopWorkerOnly();

        roomVerified = false;
        setSendEnabled(false);

        updateStatus("Connecting to room...");

        handler.post(() -> {
            scanButton.setEnabled(false);
            hostButton.setEnabled(false);
        });

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

                connected(socket, false);

            } catch (SecurityException e) {
                closeSocket(socket);

                updateStatus("Bluetooth permission missing.");
                resetConnectionButtons();

            } catch (IOException e) {
                closeSocket(socket);

                updateStatus(
                        "Connection failed. Pair devices and try again."
                );

                resetConnectionButtons();
            }
        });

        workerThread.start();
    }

    // CONNECTION AND PASSWORD VERIFICATION

    private void connected(
            BluetoothSocket socket,
            boolean isHost
    ) {
        bluetoothSocket = socket;
        roomVerified = false;
        running = true;

        setSendEnabled(false);

        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            socket.getInputStream(),
                            StandardCharsets.UTF_8
                    )
            );

            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(
                            socket.getOutputStream(),
                            StandardCharsets.UTF_8
                    )
            );

            socketReader = reader;
            socketWriter = writer;

            if (isHost) {
                updateStatus(
                        "Player connected. Verifying password..."
                );

                addMessage("Player connected.");

                writeLine("PASSWORD_REQUIRED");

                String request = reader.readLine();

                if (request == null
                        || !request.startsWith("AUTH:")) {
                    writeLine("AUTH_FAIL");
                    throw new IOException(
                            "Invalid authentication request."
                    );
                }

                String suppliedPassword;

                try {
                    byte[] decoded =
                            android.util.Base64.decode(
                                    request.substring(5),
                                    android.util.Base64.NO_WRAP
                            );

                    suppliedPassword =
                            new String(
                                    decoded,
                                    StandardCharsets.UTF_8
                            );

                } catch (IllegalArgumentException e) {
                    writeLine("AUTH_FAIL");
                    throw new IOException(
                            "Invalid password format."
                    );
                }

                boolean correct = MessageDigest.isEqual(
                        roomPassword.getBytes(StandardCharsets.UTF_8),
                        suppliedPassword.getBytes(StandardCharsets.UTF_8)
                );

                if (!correct) {
                    writeLine("AUTH_FAIL");

                    updateStatus(
                            "❌ Wrong password. Connection rejected."
                    );

                    addMessage("Authentication failed.");
                    return;
                }

                writeLine("AUTH_OK");

            } else {
                updateStatus(
                        "Connected. Enter the room password..."
                );

                String serverMessage = reader.readLine();

                if (!"PASSWORD_REQUIRED".equals(serverMessage)) {
                    throw new IOException(
                            "Host did not request authentication."
                    );
                }

                String password = requestJoinPassword();

                if (password == null) {
                    writeLine("AUTH_CANCEL");

                    updateStatus("Room join cancelled.");
                    return;
                }

                writeLine(
                        "AUTH:"
                                + android.util.Base64.encodeToString(
                                password.getBytes(StandardCharsets.UTF_8),
                                android.util.Base64.NO_WRAP
                        )
                );

                String result = reader.readLine();

                if (!"AUTH_OK".equals(result)) {
                    updateStatus(
                            "❌ Wrong password or room access denied."
                    );

                    addMessage("Room access denied.");
                    return;
                              }
                    }
                  // Only successful authentication reaches this point.

            roomVerified = true;

            updateStatus("✅ Room verified! Bluetooth connected.");
            addMessage("✅ Password verified. Room joined.");

            handler.post(() -> {
                if (hostButton != null) {
                    hostButton.setEnabled(false);
                }

                if (scanButton != null) {
                    scanButton.setEnabled(false);
                }

                if (sendButton != null) {
                    sendButton.setEnabled(true);
                }
            });

            String line;

            while (running
                    && roomVerified
                    && (line = reader.readLine()) != null) {

                if (line.startsWith("MSG:")) {
                    try {
                        byte[] decoded =
                                android.util.Base64.decode(
                                        line.substring(4),
                                        android.util.Base64.NO_WRAP
                                );

                        String message =
                                new String(
                                        decoded,
                                        StandardCharsets.UTF_8
                                );

                        addMessage("Opponent: " + message);

                    } catch (IllegalArgumentException e) {
                        addMessage("Received an invalid message.");
                    }
                }
            }

        } catch (SecurityException e) {
            updateStatus("Bluetooth permission missing.");

        } catch (IOException e) {
            if (running) {
                updateStatus(
                        "Connection ended or authentication failed."
                );
            }

        } finally {
            roomVerified = false;
            running = false;

            if (bluetoothSocket == socket) {
                bluetoothSocket = null;
                socketReader = null;
                socketWriter = null;
            }

            closeSocket(socket);

            setSendEnabled(false);
            resetConnectionButtons();
        }
    }

    // JOINER PASSWORD DIALOG
    // Called from the connection worker thread.

    private String requestJoinPassword() {
        CountDownLatch latch = new CountDownLatch(1);

        AtomicReference<String> result =
                new AtomicReference<>(null);

        handler.post(() -> {
            if (activity.isFinishing()) {
                latch.countDown();
                return;
            }

            EditText input = new EditText(activity);

            input.setHint("Enter room password");
            input.setSingleLine(true);

            input.setInputType(
                    InputType.TYPE_CLASS_TEXT
                            | InputType.TYPE_TEXT_VARIATION_PASSWORD
            );

            AlertDialog dialog =
                    new AlertDialog.Builder(activity)
                            .setTitle("🔒 Join Protected Room")
                            .setMessage(
                                    "Enter the password set by the host."
                            )
                            .setView(input)
                            .setPositiveButton(
                                    "Join",
                                    null
                            )
                            .setNegativeButton(
                                    "Cancel",
                                    (d, which) -> {
                                        result.set(null);
                                        latch.countDown();
                                    }
                            )
                            .create();

            dialog.setOnCancelListener(d -> {
                result.set(null);
                latch.countDown();
            });

            dialog.setOnShowListener(d -> {
                Button joinButton =
                        dialog.getButton(
                                AlertDialog.BUTTON_POSITIVE
                        );

                joinButton.setOnClickListener(v -> {
                    String password =
                            input.getText().toString();

                    if (password.trim().isEmpty()) {
                        input.setError(
                                "Password cannot be empty."
                        );
                        return;
                    }

                    result.set(password);
                    latch.countDown();
                    dialog.dismiss();
                });
            });

            dialog.show();

            // Apply the button listener after showing the dialog.
            dialog.getButton(
                    AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener(v -> {
                String password =
                        input.getText().toString();

                if (password.trim().isEmpty()) {
                    input.setError(
                            "Password cannot be empty."
                    );
                    return;
                }

                result.set(password);
                latch.countDown();
                dialog.dismiss();
            });
        });

        try {
            if (!latch.await(2, TimeUnit.MINUTES)) {
                return null;
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }

        return result.get();
    }

    // SEND MESSAGE: ONLY AFTER AUTHENTICATION

    private void sendMessage(String message) {
        if (!roomVerified) {
            updateStatus(
                    "🔒 Verify the room password first."
            );
            return;
        }

        BluetoothSocket socket = bluetoothSocket;
        BufferedWriter writer = socketWriter;

        if (socket == null
                || writer == null
                || !socket.isConnected()) {
            updateStatus("No connected player.");
            return;
        }

        new Thread(() -> {
            try {
                String encoded =
                        android.util.Base64.encodeToString(
                                message.getBytes(StandardCharsets.UTF_8),
                                android.util.Base64.NO_WRAP
                        );

                synchronized (writeLock) {
                    if (!roomVerified || socketWriter != writer) {
                        return;
                    }

                    writer.write("MSG:" + encoded);
                    writer.newLine();
                    writer.flush();
                }

                addMessage("You: " + message);

            } catch (IOException e) {
                updateStatus("Could not send message.");
            }
        }).start();
    }

    private void writeLine(String line) throws IOException {
        BufferedWriter writer = socketWriter;

        if (writer == null) {
            throw new IOException("Connection output is unavailable.");
        }

        synchronized (writeLock) {
            writer.write(line);
            writer.newLine();
            writer.flush();
        }
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

    private void resetConnectionButtons() {
        handler.post(() -> {
            if (hostButton != null) {
                hostButton.setEnabled(true);
            }

            if (scanButton != null) {
                scanButton.setEnabled(true);
            }

            if (sendButton != null) {
                sendButton.setEnabled(false);
            }
        });
    }

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
                activity.unregisterReceiver(discoveryReceiver);
            } catch (IllegalArgumentException ignored) {
            }

            discoveryReceiver = null;
        }
    }

    private void stopWorkerOnly() {
        running = false;
        roomVerified = false;

        stopDiscovery();

        try {
            if (serverSocket != null) {
                serverSocket.close();
                serverSocket = null;
            }
        } catch (IOException ignored) {
        }

        BluetoothSocket oldSocket = bluetoothSocket;

        bluetoothSocket = null;
        socketReader = null;
        socketWriter = null;

        closeSocket(oldSocket);
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

        roomPassword = "";

        if (workerThread != null) {
            workerThread.interrupt();
            workerThread = null;
        }
    }
}
