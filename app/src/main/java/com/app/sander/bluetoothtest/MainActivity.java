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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

/**
 * Main activity of this application
 */
public class MainActivity extends AppCompatActivity {

    // The bluetoothadapter
    protected BluetoothAdapter mBluetoothAdapter;
    // The bluetooth devices to show in the listview
    private ArrayList<BluetoothDevice> listData;
    // The ListAdapter
    private BluetoothListAdapter listAdapter;
    // The bluetooth connection
    private BluetoothConnection connector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ListView itemList = (ListView) findViewById(R.id.listView);
        listData = new ArrayList<>();
        listAdapter = new BluetoothListAdapter(this, 0, listData);
        itemList.setAdapter(listAdapter);
        itemList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View viewClicked, int position, long id) {
                connectToDevice(((TextView) viewClicked.findViewById(R.id.device_mac)).getText().toString());
            }
        });
        refreshBluetoothDevices(null);
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
    public void refreshBluetoothDevices(View view) {
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
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (connector != null) {
            connector.cancel();
        }
        connector = new BluetoothConnection(device);
        new Thread(connector).start();
    }

    /**
     * Inner class to handle an outgoing Bluetooth connection
     */
    private class BluetoothConnection implements Runnable {

        // Unique identifier necessary for a connection
        private static final String UUID_STRING = "34824060-611f-11e5-a837-0800200c9a66";
        // The bluetoothsocket
        private BluetoothSocket mmSocket;

        /**
         * Create the BluetoothConnection thread to try and connect with the given device
         *
         * @param device
         */
        public BluetoothConnection(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
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
                readFromDevice();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                giveStatusUpdate(getString(R.string.connection_error));
                Log.d("BluetoothTest", connectException.getMessage());
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.d("BluetoothTest", closeException.getMessage());
                }
            }
        }

        /**
         * Will cancel an in-progress connection, and close the socket
         */
        public synchronized void cancel() {
            try {
                mmSocket.close();
            } catch (Exception e) {
                Log.d("BluetoothTest", e.getMessage());
            }
        }

        /**
         * Read incoming data from the device
         */
        private void readFromDevice() throws IOException {
            BufferedReader reader = new BufferedReader(new InputStreamReader(mmSocket.getInputStream()));
            giveStatusUpdate(reader.readLine());
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
