package com.app.sander.bluetoothtest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

/**
 * Main activity of this application
 */
public class MainActivity extends AppCompatActivity {

    protected BluetoothAdapter mBluetoothAdapter;
    private ListView itemList;
    private ArrayList<BluetoothDevice> listData;
    private BluetoothListAdapter listAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        itemList = (ListView) findViewById(R.id.listView);
        listData = new ArrayList<>();
        listAdapter = new BluetoothListAdapter(this, 0, listData);
        itemList.setAdapter(listAdapter);
        itemList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View viewClicked, int position, long id) {
                TextView text = (TextView) viewClicked.findViewById(R.id.device_mac);
                connectToDevice(text.getText().toString());
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Show the paired bluetooth devices
     *
     * @param view
     */
    public void showBluetoothDevices(View view) {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        listData.clear();
        listAdapter.notifyDataSetChanged();
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                listData.add(device);
                listAdapter.notifyDataSetChanged();
            }
        }
    }

    /**
     * Connect to the chosen device
     *
     * @param address
     */
    private void connectToDevice(String address) {
        Log.d("Measurement", address);
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        new Thread(new BluetoothConnection(device)).start();
    }

    /**
     * Inner class to handle an outgoing Bluetooth connection
     */
    private class BluetoothConnection implements Runnable {

        private static final String UUID_STRING = "34824060-611f-11e5-a837-0800200c9a66";

        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public BluetoothConnection(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(UUID.fromString(UUID_STRING));
            } catch (IOException e) {
            }
            mmSocket = tmp;
        }

        @Override
        public void run() {
            // Cancel discovery because it will slow down the connection
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
                giveStatusUpdate(getString(R.string.connection_success));
                byte[] array = new byte["Hello from the other device!".getBytes().length];
                mmSocket.getInputStream().read(array);
                giveStatusUpdate(new String(array, "UTF-8"));
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                giveStatusUpdate(getString(R.string.connection_error));
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                }
            }

            // Do work to manage the connection (in a separate thread)
            //manageConnectedSocket(mmSocket);
        }

        /**
         * Will cancel an in-progress connection, and close the socket
         */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }

        /**
         * Give a status update using Toast.
         *
         * @param text The text to display
         */
        private void giveStatusUpdate(final String text) {
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

}
