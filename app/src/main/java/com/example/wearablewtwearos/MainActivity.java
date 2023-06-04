package com.example.wearablewtwearos;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.gun0912.tedpermission.PermissionListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    ImageView trainingImageView;
    TextView trainingNameTextView;
    TextView weightTextView;
    TextView weightUnitTextView;
    TextView minusWeightTextView;
    TextView plusWeightTextView;

    TextView repeatTextView;
    TextView minusRepeatTextView;
    TextView plusRepeatTextView;

    TextView nextSet;
    TextView nextTraining;


    private BluetoothAdapter bluetoothAdapter;
    private Set<BluetoothDevice> devices;
    private BluetoothDevice bluetoothDevice;
    Handler bluetoothHandler;
    ConnectedBluetoothThread connectedBluetoothThread;
    BluetoothSocket bluetoothSocket;

    final static int BT_REQUEST_ENABLE = 1;
    final static int BT_MESSAGE_READ = 2;
    final static int BT_CONNECTING_STATUS = 3;
    final static UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    final static String NAME = "Bluetooth";
    final static String TAG = "ERROR";
    ArrayList<String> trainingIdList = new ArrayList<>();
    int nextTrainingIndex;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findView();
        initValue();
        BroadcastReceiver br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if(action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
                    Toast.makeText(MainActivity.this, "블루투스 연결됨", Toast.LENGTH_LONG);
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String deviceAddress = device.getAddress();
                    selectDevice(deviceAddress);
                    connectSelectedDevice();
                    try {
                        connectedBluetoothThread.join();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    nextTrainingIndex = 0;
                }
            }
        };

        nextTraining.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(nextTrainingIndex < trainingIdList.size() - 1) {
                    nextTraining();
                }
            }
        });
    }


    private void findView() {
        weightTextView = findViewById(R.id.weightTextView);
        weightUnitTextView = findViewById(R.id.weightUnitTextView);
        minusWeightTextView = findViewById(R.id.minusWeightTextView);
        plusWeightTextView = findViewById(R.id.plusWeightTextView);

        repeatTextView = findViewById(R.id.repeatTextView);
        minusRepeatTextView = findViewById(R.id.minusRepeatTextView);
        plusRepeatTextView = findViewById(R.id.plusRepeatTextView);

        nextSet = findViewById(R.id.nextSetTextView);
        nextTraining = findViewById(R.id.nextTrainingTextView);
    }

    private void initValue() {
        weightTextView.setText("0");
        plusWeightTextView.setText(" ");
        minusWeightTextView.setText(" ");
        repeatTextView.setText("0");
        plusRepeatTextView.setText(" ");
        minusRepeatTextView.setText(" ");

    }

    private void nextTraining(){
        String trainingId = trainingIdList.get(nextTrainingIndex);
        Bitmap resized = DataProcessing.getTrainingImage(this, trainingId);
        trainingImageView.setImageBitmap(resized);

        trainingNameTextView.setText(trainingId);
    }

    public void selectBluetoothDevice() {

        SharedPreferences connectedWearable = getSharedPreferences("connectedWearable", Activity.MODE_PRIVATE);
        String address = connectedWearable.getString("address", null);

        if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            //ActivityCompat.requestPermissions( TrainingRecordActivity.this, new String[]{android.Manifest.permission.BLUETOOTH_CONNECT},
        }
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        devices = bluetoothAdapter.getBondedDevices();

        if (address != null && selectDevice(address)) {
            Toast.makeText(MainActivity.this, "자동 연결 완료", Toast.LENGTH_SHORT).show();
        } else {
            int pairedDeviceCount = devices.size();
            if (pairedDeviceCount == 0) {
                Toast.makeText(MainActivity.this, "페어링 되어있는 디바이스가 없습니다", Toast.LENGTH_SHORT).show();
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("페어링 블루투스 디바이스 목록");

                List<String> bluetoothDeviceNameList = new ArrayList<>();
                List<String> bluetoothDeviceAddressList = new ArrayList<>();

                for (BluetoothDevice bluetoothDevice : devices) {
                    bluetoothDeviceNameList.add(bluetoothDevice.getName());
                    bluetoothDeviceAddressList.add(bluetoothDevice.getAddress());
                }
                bluetoothDeviceNameList.add("취소");
                // List를 CharSequence 배열로 변경
                final CharSequence[] charSequences = bluetoothDeviceNameList.toArray(new CharSequence[bluetoothDeviceNameList.size()]);
                bluetoothDeviceNameList.toArray(new CharSequence[bluetoothDeviceNameList.size()]);
                // 해당 아이템을 눌렀을 때 호출 되는 이벤트 리스너
                builder.setItems(charSequences, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(MainActivity.this, charSequences[which], Toast.LENGTH_LONG).show();
                        CharSequence cs = bluetoothDeviceAddressList.get(which);
                        selectDevice(cs.toString());
                    }
                });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            }
        }
    }

    private boolean selectDevice(String address) {
        for (BluetoothDevice device : devices) {
            if (address.equals(device.getAddress())) {
                bluetoothDevice = device;
                SharedPreferences connectedWearable = getSharedPreferences("connectedWearable", Activity.MODE_PRIVATE);
                SharedPreferences.Editor editor = connectedWearable.edit();
                editor.putString("address", address);
                editor.apply();
                return true;
            }
        }

        return false;
    }

    private void tedPermission() {

        PermissionListener permissionlistener = new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                Toast.makeText(MainActivity.this, "Permission Granted", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPermissionDenied(List<String> deniedPermissions) {
                Toast.makeText(MainActivity.this, "Permission Denied\n" + deniedPermissions.toString(), Toast.LENGTH_SHORT).show();
            }
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissions(
                    new String[]{
                            android.Manifest.permission.BLUETOOTH,
                            android.Manifest.permission.BLUETOOTH_SCAN,
                            android.Manifest.permission.BLUETOOTH_ADVERTISE,
                            android.Manifest.permission.BLUETOOTH_CONNECT
                    },
                    1);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            requestPermissions(
                    new String[]{
                            android.Manifest.permission.BLUETOOTH
                    },
                    1);
        }

        /*TedPermission.create()
                .setPermissionListener(permissionlistener)
                .setDeniedMessage("If you reject permission,you can not use this service\n\nPlease turn on permissions at [Setting] > [Permission]")
                .setPermissions(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
                .check();*/
    }


    void connectSelectedDevice() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(BT_UUID);
            bluetoothSocket.connect();
            connectedBluetoothThread = new ConnectedBluetoothThread(bluetoothSocket);
            connectedBluetoothThread.start();

            bluetoothHandler.obtainMessage(BT_CONNECTING_STATUS, 1, -1).sendToTarget();
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "블루투스 연결 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
        }
    }

    private class ConnectedBluetoothThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedBluetoothThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "소켓 연결 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = mmInStream.available();
                    if (bytes != 0) {
                        SystemClock.sleep(100);
                        bytes = mmInStream.available();
                        bytes = mmInStream.read(buffer, 0, bytes);
                        bluetoothHandler.obtainMessage(BT_MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                        trainingIdList.add(buffer.toString());
                        if(buffer.toString().compareTo("startTraining") == 0) {
                            return ;
                        } else {

                        }
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }
        public void write(String str) {
            byte[] bytes = str.getBytes();
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "데이터 전송 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
            }
        }
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "소켓 해제 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
