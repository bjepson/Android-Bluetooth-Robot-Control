// tBlue.java - simple wrapper for Android Bluetooth libraries
// (c) Tero Karvinen & Kimmo Karvinen http://sulautetut.fi
   
  package fi.sulautetut.android.football;
   
  import java.io.IOException;
  import java.io.InputStream;
  import java.io.OutputStream;
  import java.lang.reflect.InvocationTargetException;
  import java.lang.reflect.Method;
 
  import android.bluetooth.BluetoothAdapter;
  import android.bluetooth.BluetoothDevice;
  import android.bluetooth.BluetoothSocket;
  import android.util.Log;
   
  public class TBlue { 
    String address=null; 
    String TAG="tBlue";
    BluetoothAdapter localAdapter=null;
    BluetoothDevice remoteDevice=null;
    BluetoothSocket socket=null;
    public OutputStream outStream = null;
    public InputStream inStream=null;
    boolean failed=false;
   
    public TBlue(String address) 
    {
        this.address=address.toUpperCase();
        localAdapter = BluetoothAdapter.getDefaultAdapter();
        if ((localAdapter!=null) && localAdapter.isEnabled()) {
            Log.i(TAG, "Bluetooth adapter found and enabled on phone. ");
        } else {
            Log.e(TAG, "Bluetooth adapter NOT FOUND or NOT ENABLED!");
            return;
        }
        connect();
    } 
   
    public void connect()
    {
        Log.i(TAG, "Bluetooth connecting to "+address+"...");
        try     {
            remoteDevice = localAdapter.getRemoteDevice(address); 
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Failed to get remote device with MAC address."
                    +"Wrong format? MAC address must be upper case. ", 
                    e);
            return;
        }
   
        Log.i(TAG, "Creating RFCOMM socket..."); 
        try {
            Method m = remoteDevice.getClass().getMethod("createRfcommSocket", 
                                               new Class[] { int.class });
            socket = (BluetoothSocket) m.invoke(remoteDevice, 1);
            Log.i(TAG, "RFCOMM socket created.");
        } catch (NoSuchMethodException e) {
            Log.i(TAG, "Could not invoke createRfcommSocket.");
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            Log.i(TAG, "Bad argument with createRfcommSocket.");
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            Log.i(TAG, "Illegal access with createRfcommSocket.");
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            Log.i(TAG, "Invocation target exception with createRfcommSocket.");
            e.printStackTrace();
        }
        Log.i(TAG, "Got socket for device "+socket.getRemoteDevice()); 
        localAdapter.cancelDiscovery();
   
        Log.i(TAG, "Connecting socket...");
        try {
            socket.connect(); 
            Log.i(TAG, "Socket connected.");
        } catch (IOException e) {
            try {
                Log.e(TAG, "Failed to connect socket. ", e);
                socket.close();
                Log.e(TAG, "Socket closed because of an error. ", e);
            } catch (IOException eb) {
                Log.e(TAG, "Also failed to close socket. ", eb);
            }
            return;
        }
   
        try {
            outStream = socket.getOutputStream(); 
            Log.i(TAG, "Output stream open.");
            inStream = socket.getInputStream();
            Log.i(TAG, "Input stream open.");
        } catch (IOException e) {
            Log.e(TAG, "Failed to create output stream.", e);  
        }
        return;
    }
   
    public void write(String s)
    {
        Log.i(TAG, "Sending \""+s+"\"... "); 
        byte[] outBuffer= s.getBytes();
        try {
            outStream.write(outBuffer);
        } catch (IOException e) {
            Log.e(TAG, "Write failed.", e); 
        }
   
    }
   
    public boolean streaming() 
    {
        return ( (inStream!=null) && (outStream!=null) );
    }
   
    public String read()
    {
        if (!streaming()) return "";
        String inStr="";
        try {
            if (0<inStream.available()) {
                byte[] inBuffer = new byte[1024];
                int bytesRead = inStream.read(inBuffer);
                inStr = new String(inBuffer, "ASCII");
                inStr=inStr.substring(0, bytesRead);
                Log.i(TAG, "byteCount: "+bytesRead+ ", inStr: "+inStr);
            }
        } catch (IOException e) {
            Log.e(TAG, "Read failed", e); 
        }
        return inStr;
    }
   
    public void close()
    {
        Log.i(TAG, "Bluetooth closing... ");
        try    {
            socket.close();
            Log.i(TAG, "BT closed");
        } catch (IOException e2) {
            Log.e(TAG, "Failed to close socket. ", e2); 
        }
    }
  }
