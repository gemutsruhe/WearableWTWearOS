package com.example.wearablewtwearos;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.os.HandlerCompat;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class Bluetooth {
    public static final int INTENT_REQUEST_BLUETOOTH_ENABLE = 0x0701;

    private final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
    BluetoothGatt gatt = null;
    private final HashMap<String, BluetoothDevice> hashDeviceMap = new HashMap<>();
    private final Handler mainThreadHandler = HandlerCompat.createAsync(Looper.getMainLooper());

    private boolean scanning = false;
    Context context;
    String selectedDeviceAddress;

    Boolean connected = false;

    BluetoothLeScanner scanner;

    ArrayList<String> trainingRecordList;
    ArrayList<String> trainingIdList;

    Bluetooth(Context context, String selectedDeviceAddress, ArrayList<String> trainingRecordList) {
        this.context = context;
        this.selectedDeviceAddress = selectedDeviceAddress;
        trainingIdList = new ArrayList<>();
    }

    public boolean onActivityResult(int requestCode, int resultCode) {
        return requestCode == Bluetooth.INTENT_REQUEST_BLUETOOTH_ENABLE
                && Activity.RESULT_OK == resultCode;
    }

    private final ScanCallback callback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (connected == false && result.getDevice().getAddress().compareTo(selectedDeviceAddress) == 0) {
                connected = true;
                connGATT(context, result.getDevice());

            } else return;

        }
    };

    public BluetoothGatt getGatt() {
        return gatt;
    }

    public void scanDevices() {
        scanning = true;
        if (!adapter.isEnabled()) return;
        if (!scanning) return;
        scanner = adapter.getBluetoothLeScanner();
        mainThreadHandler.postDelayed(() -> {
            scanning = false;
            scanner.stopScan(callback);
        }, 2 * 60 * 1000);

        scanning = true;
        scanner.startScan(callback);
    }
    public void connGATT(Context context, BluetoothDevice device) {
        gatt = device.connectGatt(context, false, gattCallback);
    }

    public void disconnectGATT() {
        gatt.disconnect();
        gatt.close();
    }


    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (status == BluetoothGatt.GATT_FAILURE) {
                gatt.disconnect();
                gatt.close();
                hashDeviceMap.remove(gatt.getDevice().getAddress());
                return;
            }
            if (status == 133) // Unknown Error
            {
                gatt.disconnect();
                gatt.close();
                hashDeviceMap.remove(gatt.getDevice().getAddress());
                return;
            }
            if (newState == BluetoothGatt.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                gatt.discoverServices();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {

                List<BluetoothGattService> services = gatt.getServices();
                for (BluetoothGattService service : services) {

                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {

                        if (hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_READ)) {
                            gatt.readCharacteristic(characteristic);
                        }

                        if( hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_NOTIFY))
                        {
                            gatt.setCharacteristicNotification(characteristic, true);
                        }

                        if(hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_WRITE)) {
                            characteristic.setValue("data".getBytes());
                            gatt.writeCharacteristic(characteristic);
                            if(trainingRecordList != null) {
                                for(int i = 0; i < trainingRecordList.size(); i++) {
                                    characteristic.setValue(trainingRecordList.get(i).getBytes());
                                    gatt.writeCharacteristic(characteristic);
                                }
                            }
                            return ;
                        }
                    }
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if( status == BluetoothGatt.GATT_SUCCESS) {
                String trainingId = characteristic.getValue().toString();

                if(!trainingIdList.contains(trainingId)) {
                    trainingIdList.add(trainingId);
                    Toast.makeText(context, trainingId, Toast.LENGTH_SHORT);
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

            super.onCharacteristicWrite(gatt, characteristic, status);
            if(status == BluetoothGatt.GATT_SUCCESS) {
                Log.e("TEST", "Data Transfer Success");
            } else {
                Log.e("TEST", "Failed");
            }
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
            if(status == BluetoothGatt.GATT_SUCCESS) {
                Log.e("TEST", "Data Transfer Success");
            } else {
                Log.e("TEST", "Failed");
            }
        }
    };

    public ArrayList<String> getTrainingIdList() {
        return trainingIdList;
    }
    public boolean hasProperty(BluetoothGattCharacteristic characteristic, int property)
    {
        int prop = characteristic.getProperties() & property;
        return prop == property;
    }

    private byte[] stringToBytes(String data) {
        return data.getBytes(StandardCharsets.UTF_8);
    }
}
