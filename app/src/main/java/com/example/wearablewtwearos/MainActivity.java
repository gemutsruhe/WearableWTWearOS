package com.example.wearablewtwearos;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSION_BLUETOOTH = 2;
    final static int BT_REQUEST_ENABLE = 1;
    final static int BT_MESSAGE_READ = 2;
    final static int BT_CONNECTING_STATUS = 3;
    final static UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    final static String NAME = "Bluetooth";
    final static String TAG = "ERROR";
    ArrayList<String> trainingIdList = new ArrayList<>();
    int nextTrainingIndex;
    String selectedDeviceAddress;

    double weight;
    int repeat;
    double unitWeight;
    int unitRepeat;

    ArrayList<ArrayList<String>> trainingRecord;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findView();
        initValue();

        trainingIdList.add("ChinUp");
        trainingIdList.add("MachineFly");
        trainingIdList.add("PullUp");
        trainingIdList.add("LatPullDown");
        trainingIdList.add("startTraining");

        nextTraining();
        Toast.makeText(getApplicationContext(), "TEST", Toast.LENGTH_SHORT).show();
        permission();
        selectBluetoothDevice();

        Bluetooth bluetooth = new Bluetooth(getApplicationContext(), selectedDeviceAddress, null);
        bluetooth.scanDevices();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    trainingIdList = bluetooth.getTrainingIdList();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        //throw new RuntimeException(e);
                    }
                    if(trainingIdList.size() > 0 && trainingIdList.get(trainingIdList.size() - 1).compareTo("startTraining") == 0) {
                        nextTraining();
                        break;
                    }
                }
            }
        });

        nextTraining.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(nextTrainingIndex < trainingIdList.size() - 1) {
                    nextTraining();
                } else {
                    finishTraining();
                }
                if(nextTrainingIndex == trainingIdList.size() - 2) {
                    nextTraining.setText("운동 종료");
                }
            }
        });

        nextSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int repeat = Integer.parseInt(repeatTextView.getText().toString());
                if(repeat != 0) {
                    nextSet();
                }
            }
        });

        plusWeightTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double weightNum = Double.parseDouble(weightTextView.getText().toString());
                weightNum += unitWeight;
                weightTextView.setText(String.valueOf(weightNum));
            }
        });

        minusWeightTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double weightNum = Double.parseDouble(weightTextView.getText().toString());
                weightNum -= unitWeight;
                weightTextView.setText(String.valueOf(weightNum > 0 ? weightNum : 0));
            }
        });

        weightUnitTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(weightUnitTextView.getText().toString().compareTo("kg") == 0) {
                    weightUnitTextView.setText("lb");
                } else {
                    weightUnitTextView.setText("kg");
                }
            }
        });

        plusRepeatTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int repeatNum = Integer.parseInt(repeatTextView.getText().toString());
                repeatTextView.setText(String.valueOf(repeatNum + 1));
            }
        });

        minusRepeatTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int repeatNum = Integer.parseInt(repeatTextView.getText().toString());
                if(repeatNum > 0)  repeatTextView.setText(String.valueOf(repeatNum - 1));
            }
        });
    }


    private void findView() {
        trainingImageView = findViewById(R.id.trainingImageView);

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
        nextTrainingIndex = 0;
        weight = 0;
        weightTextView.setText(String.valueOf(weight));
        unitWeight = 5;
        plusWeightTextView.setText("+5");
        minusWeightTextView.setText("-5");
        repeat = 0;
        repeatTextView.setText(String.valueOf(repeat));
        unitRepeat = 1;
        plusRepeatTextView.setText("+1");
        minusRepeatTextView.setText("-1");

    }

    private void nextSet() {
        String string = weightTextView.getText().toString() + "," + weightUnitTextView.getText().toString() + "," + repeatTextView.getText().toString();

        repeat = 0;
        repeatTextView.setText(String.valueOf(repeat));
    }

    private void nextTraining(){
        String trainingId = trainingIdList.get(nextTrainingIndex);

        Bitmap resized = DataProcessing.getTrainingImage(this, trainingId);
        trainingImageView.setImageBitmap(resized);

        //trainingNameTextView.setText(trainingId);
        weight = 0;
        weightTextView.setText(String.valueOf(weight));
        repeat = 0;
        repeatTextView.setText(String.valueOf(repeat));
        nextTrainingIndex++;
    }

    private void finishTraining() {
        ArrayList<String> trainingRecordList = new ArrayList<>();
        for(int i = 0; i < trainingIdList.size(); i++) {
            for(int j = 0; j < trainingRecord.get(i).size(); j++) {
                String data = trainingIdList.get(i) + "," + i + "," + j + "," + trainingRecord.get(j);
                trainingRecordList.add(data);
            }
        }
        Bluetooth bluetooth = new Bluetooth(this, selectedDeviceAddress, trainingRecordList);
    }

    public void selectBluetoothDevice() {

        SharedPreferences connectedWearable = getSharedPreferences("connectedWearable", Activity.MODE_PRIVATE);
        String address = connectedWearable.getString("address", null);

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        devices = bluetoothAdapter.getBondedDevices();

        if (address != null && selectDevice(address)) {

        } else {

            int pairedDeviceCount = devices.size();

            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
            View dialogView = inflater.inflate(R.layout.dummy, findViewById(R.id.boxLayout), false);

                builder.setTitle("디바이스 목록");

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
            //}
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

    private void checkBluetoothPermission() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {

        } else if (!bluetoothAdapter.isEnabled()) {

        } else {

        }
    }


    private void permission() {

        int permissionCheck = ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            // 권한이 없는 경우
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.BLUETOOTH)) {
                // 사용자가 권한을 거부한 경우에 설명을 보여줄 수 있습니다.
                Toast.makeText(this, "Bluetooth 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.BLUETOOTH}, REQUEST_PERMISSION_BLUETOOTH);
        } else {
            // 권한이 이미 있는 경우
            Toast.makeText(this, "Bluetooth 권한 있음.", Toast.LENGTH_SHORT).show();
        }
/*
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
        requestPermissions(
                new String[]{
                        android.Manifest.permission.BLUETOOTH,
                        Manifest.permission.BLUETOOTH_ADMIN
                },
                    1);*/
    }


    /*void connectSelectedDevice() {
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
    }*/

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
