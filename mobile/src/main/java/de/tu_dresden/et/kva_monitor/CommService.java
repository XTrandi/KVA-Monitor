package de.tu_dresden.et.kva_monitor;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.icu.util.Output;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.util.Xml;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import static android.support.v4.app.NotificationCompat.PRIORITY_LOW;

public class CommService extends Service implements DataClient.OnDataChangedListener {

    private Looper looper;
    private ServiceHandler serviceHandler;
    private HandlerThread thread;

    static final int STOP_SERVICE_WHATID        = 0;
    static final int READ_REQUEST_WHATID        = 1;
    static final int WRITE_REQUEST_WHATID       = 2;

    static final int ONGOING_NOTIFICATION_ID    = 1;

    static final int POLLING_INTERVAL_MS        = 1000;

    static final String XML_DA_URL              = "http://141.30.154.211:8087/OPC/DA";

    static final String NAMESPACE_SOAP = "http://schemas.xmlsoap.org/soap/envelope/";
    static final String NAMESPACE_XSI  = "http://www.w3.org/2001/XMLSchema-instance";
    static final String NAMESPACE_XSD  = "http://www.w3.org/2001/XMLSchema";

    static final String PATH_WEAR_UI            = "/wear_UI";
    static final String PATH_OPC_REQUEST        = "/OPC_request";

    // basic frame for XML files to be sent to the OPC DA server
    // TODO: Build OPC UA request dynamically
    static final String XML_READ =
            "<SOAP-ENV:Envelope\n" +
            "   xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "   xmlns:SOAP-ENC=\"http://schemas.xmlsoap.org/soap/encoding/\"\n" +
            "   xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "   xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "   <SOAP-ENV:Body>\n" +
            "     <m:Read xmlns:m=\"http://opcfoundation.org/webservices/XMLDA/1.0/\">\n" +
            "      <m:Options\n" +
            "        ReturnErrorText=\"false\"\n" +
            "        ReturnDiagnosticInfo=\"false\"\n" +
            "        ReturnItemTime=\"false\"\n" +
            "        ReturnItemPath=\"false\"\n" +
            "        ReturnItemName=\"true\"\n" +
            "      />\n" +
            "      <m:ItemList>\n" +
            "       <m:Items ItemName=\"Schneider/Fuellstand1_Ist\"/>\n" +
            "       <m:Items ItemName=\"Schneider/Fuellstand2_Ist\"/>\n" +
            "       <m:Items ItemName=\"Schneider/Fuellstand3_Ist\"/>\n" +
            "       <m:Items ItemName=\"Schneider/LH1\"/>\n" +
            "       <m:Items ItemName=\"Schneider/LH2\"/>\n" +
            "       <m:Items ItemName=\"Schneider/LH3\"/>\n" +
            "       <m:Items ItemName=\"Schneider/LL1\"/>\n" +
            "       <m:Items ItemName=\"Schneider/LL2\"/>\n" +
            "       <m:Items ItemName=\"Schneider/LL3\"/>\n" +
            "       <m:Items ItemName=\"Schneider/P1\"/>\n" +
            "       <m:Items ItemName=\"Schneider/P2\"/>\n" +
            "       <m:Items ItemName=\"Schneider/P3\"/>\n" +
            "       <m:Items ItemName=\"Schneider/M\"/>\n" + // right now not in use
            "       <m:Items ItemName=\"Schneider/V1\"/>\n" +
            "       <m:Items ItemName=\"Schneider/V2\"/>\n" +
            "       <m:Items ItemName=\"Schneider/Y1\"/>\n" +
            "       <m:Items ItemName=\"Schneider/Y2\"/>\n" +
            "       <m:Items ItemName=\"Schneider/Y5\"/>\n" +
            "       <m:Items ItemName=\"Schneider/Y6\"/>\n" +
            "       <m:Items ItemName=\"Schneider/Y7\"/>\n" +
            "      </m:ItemList>\n" +
            "     </m:Read>\n" +
            "  </SOAP-ENV:Body>\n" +
            "</SOAP-ENV:Envelope>";


    static final String XML_WRITE_BEGIN =
            "<SOAP-ENV:Envelope\n" +
            "   xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "   xmlns:SOAP-ENC=\"http://schemas.xmlsoap.org/soap/encoding/\"\n" +
            "   xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "   xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "   <SOAP-ENV:Body>\n" +
            "     <m:Write xmlns:m=\"http://opcfoundation.org/webservices/XMLDA/1.0/\">\n" +
            "       <m:Options\n" +
            "         ReturnErrorText=\"true\"\n" +
            "         ReturnDiagnosticInfo=\"true\"\n" +
            "         ReturnItemTime=\"false\"\n" +
            "         ReturnItemPath=\"true\"\n" +
            "         ReturnItemName=\"true\"\n" +
            "       />\n" +
            "       <m:ItemList>\n";

    static final String XML_WRITE_END =
            "      </m:ItemList>\n" +
            "     </m:Write>\n" +
            "  </SOAP-ENV:Body>\n" +
            "</SOAP-ENV:Envelope>";

    String XML_write;

    DataClient myDataClient;


    @Override
    public void onDataChanged(@NonNull DataEventBuffer dataEventBuffer) {
        for (DataEvent event: dataEventBuffer) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().compareTo(PATH_OPC_REQUEST) == 0) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();

                    String[] itemNames  = dataMap.getStringArray("item_names");
                    String[] itemTypes  = dataMap.getStringArray("item_types");
                    String[] itemValues = dataMap.getStringArray("item_values");

                    String XMLitemList = "";

                    for (int i=0; i<itemNames.length; i++) {
                        XMLitemList = XMLitemList +
                                "         <m:Items ItemName=\"Schneider/"+ itemNames[i] +"\">\n" +
                                "           <m:Value xsi:type=\"xsd:"+ itemTypes[i] +"\">"+
                                                itemValues[i] +"</m:Value>\n" +
                                "         </m:Items>\n";
                    }

                    XML_write = XML_WRITE_BEGIN + XMLitemList + XML_WRITE_END;

                    // queue write request to background service
                    Message message = serviceHandler.obtainMessage(WRITE_REQUEST_WHATID);
                    serviceHandler.sendMessage(message);

                }
            }
        }
    }

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            // Background process to send and receive SOAP-Envelopes and parsing them.

            this.removeMessages(READ_REQUEST_WHATID); // always remove these messages to avoid duplicates

            Message message;

            switch (msg.what) {
                case STOP_SERVICE_WHATID:
                    Log.d("Service", "Disconnected.");
                    break;
                case READ_REQUEST_WHATID:
                    readOPCResponse( sendRequest("Read", XML_READ ) );

                    message = this.obtainMessage(READ_REQUEST_WHATID);
                    this.sendMessageDelayed(message, POLLING_INTERVAL_MS); // continue polling
                    break;
                case WRITE_REQUEST_WHATID:
                    sendRequest("Write", XML_write); // write request

                    message = this.obtainMessage(READ_REQUEST_WHATID);
                    // follow up immediately with updated data (check response)
                    this.sendMessage(message);
                    break;
                default: break;
            }
        }
    }



    @Override
    public void onCreate() {
        // Creates a separate thread independent of the UI-thread for communication purposes.
        thread = new HandlerThread("ServiceStartArguments",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        looper = thread.getLooper();
        serviceHandler = new ServiceHandler(looper);

        myDataClient = Wearable.getDataClient(this);

    }


    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start service from the UI.
        //TODO: Edit notification properties for running service, change icon

        Message msg = serviceHandler.obtainMessage(READ_REQUEST_WHATID); //start new loop
        serviceHandler.sendMessage(msg);

        Intent notificationIntent = new Intent(this, ControlActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification =
                new NotificationCompat.Builder(this, "ChannelID")
                        .setContentTitle("Title")
                        .setContentText("Benachrichtigungstext")
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentIntent(pendingIntent)
                        .setTicker("Tickertext")
                        .build();



        startForeground(ONGOING_NOTIFICATION_ID, notification);

        myDataClient.addListener(this);

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: onBindMethod placeholder for AndroidWear connection (optional)
        return null;
    }

    @Override
    public void onDestroy() {
        // Stop service
        myDataClient.removeListener(this);
        serviceHandler.sendEmptyMessage(STOP_SERVICE_WHATID);
    }

    private InputStream sendRequest (String typeIO, String body) {
        // Method to set standard parameter for connection. typeIO can be either "Read" or "Write".
        try {
            URL url = new URL(XML_DA_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setReadTimeout(3000);
            connection.setConnectTimeout(3000);
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.addRequestProperty("SOAPAction",
                    "\"http://opcfoundation.org/webservices/XMLDA/1.0/" + typeIO + "\""); //header

            connection.setFixedLengthStreamingMode(body.length());
            OutputStream outputStream = new BufferedOutputStream(connection.getOutputStream());
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(outputStream, "UTF-8"));
            writer.write(body);
            writer.flush();
            writer.close();
            outputStream.close();

            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpsURLConnection.HTTP_OK) {
                Log.d("Service", "HTPP error code: " + responseCode);
                throw new IOException("HTTP error code: " + responseCode);
            }

            return connection.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    private void readOPCResponse (InputStream stream) {
        // Parse XML response from OPC server and forward data via data clients.
        // TODO: usage of advanced XML parser (optional, since more memory inefficient)

        // In case connecting to the OPC UA Server is not successful, skip reading the response
        // (there is no InputStream available)
        if (stream == null) {
            Log.d("Service", "No InputStream set, did not receive HTTP response");
            return; }

        // Prepare data map to contain data items to send
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(PATH_WEAR_UI);
        DataMap dataMap = putDataMapRequest.getDataMap();

        // Parse XML data
        XmlPullParser parser = Xml.newPullParser();
        try {
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
            parser.setInput(stream, null);

            parser.nextTag();

            // correct XML structure
            parser.require(XmlPullParser.START_TAG, NAMESPACE_SOAP, "Envelope"); parser.next();
            parser.require(XmlPullParser.START_TAG, NAMESPACE_SOAP, "Body"); parser.next();
            parser.require(XmlPullParser.START_TAG, null, "ReadResponse");


            int eventType;
            String itemName, dataType, value;

            while ( true ) {
                eventType = parser.next();

                // consume item list and its values
                if ( (eventType == XmlPullParser.START_TAG &&
                        parser.getName().equals("RItemList")) ) {

                    // scroll through item list UNTIL RItemList closing tag is found
                    while ( !(eventType == XmlPullParser.END_TAG &&
                            parser.getName().equals("RItemList")) ){

                        // fetch item and its item name
                        if ( parser.getEventType() == XmlPullParser.START_TAG &&
                                parser.getName().equals("Items")) {
                            itemName = parser.getAttributeValue(null, "ItemName")
                                    .replace("Schneider/", "");
                            parser.next();

                            // fetch item's value and its xsi:type
                            if ( parser.getEventType() == XmlPullParser.START_TAG &&
                                    parser.getName().equals("Value")) {
                                dataType = parser.getAttributeValue(NAMESPACE_XSI, "type")
                                        .replace("xsd:", "");
                                value = parser.nextText();

                                // Fill data map from fetched XML document
                                switch (dataType) {
                                    case "boolean":
                                        dataMap.putBoolean(itemName, Boolean.parseBoolean(value));
                                        break;
                                    case "short":
                                        dataMap.putInt(itemName, Integer.parseInt(value));
                                        break;
                                    case "float":
                                        dataMap.putFloat(itemName, Float.parseFloat(value));
                                    default:
                                        break;
                                }
                            }
                            // otherwise, discard item if it has no value
                        }

                        eventType = parser.next();
                    }
                }

                else if ( eventType == XmlPullParser.START_TAG &&
                        parser.getName().equals("ReadResult") ) {
                    // ToDo: display connection status via ReplyTime
                    String sReplyTime = parser.getAttributeValue(null, "ReplyTime");
                }

                // ReadResponse body investigated, skip parsing
                else if ( eventType == XmlPullParser.END_TAG &&
                        parser.getName().equals("ReadResponse") ) {
                    break;
                }
            }
/*
            // navigate to information nodes
            while ( !(eventType == XmlPullParser.START_TAG &&
                    parser.getName().equals("RItemList")) ){
                eventType = parser.next();
            }

*/

            stream.close();

        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Data map is finished, convert to data and send via data request
        PutDataRequest request = putDataMapRequest.asPutDataRequest();
        request.setUrgent();
        Task<DataItem> putDataTask = myDataClient.putDataItem(request);
        // Optionally a Success / Failure listener may be added to the task

    }


    /**
     * Given a URL, sets up a connection and gets the HTTP response body from the server.
     * If the network request is successful, it returns the response body in String form. Otherwise,
     * it will throw an IOException.
     */
    private String downloadUrl(URL url) throws IOException {
        InputStream stream = null;
        HttpURLConnection connection;
        String result = null;
        connection = (HttpURLConnection) url.openConnection();


        String data = "<SOAP-ENV:Envelope\n" +
                "   xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
                "   xmlns:SOAP-ENC=\"http://schemas.xmlsoap.org/soap/encoding/\"\n" +
                "   xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "   xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">\n" +
                "   <SOAP-ENV:Body>\n" +
                "     <m:Read xmlns:m=\"http://opcfoundation.org/webservices/XMLDA/1.0/\">\n" +
                "      <m:Options\n" +
                "        ReturnErrorText=\"false\"\n" +
                "        ReturnDiagnosticInfo=\"false\"\n" +
                "        ReturnItemTime=\"false\"\n" +
                "        ReturnItemPath=\"false\"\n" +
                "        ReturnItemName=\"true\"\n" +
                "      />\n" +
                "      <m:ItemList>\n" +
                "       <m:Items ItemName=\"Schneider/Fuellstand1_Ist\"/>\n" +
                "       <m:Items ItemName=\"Schneider/Fuellstand2_Ist\"/>\n" +
                "       <m:Items ItemName=\"Schneider/Fuellstand3_Ist\"/>\n" +
                "       <m:Items ItemName=\"Schneider/Start_Demo_FL\"/>\n" +
                "       <m:Items ItemName=\"Schneider/Demo_Status\"/>\n" +
                "       <m:Items ItemName=\"Schneider/LH3\"/>\n" +
                "       <m:Items ItemName=\"Schneider/LL1\"/>\n" +
                "       <m:Items ItemName=\"Schneider/LL2\"/>\n" +
                "       <m:Items ItemName=\"Schneider/LL3\"/>\n" +
                "       <m:Items ItemName=\"Schneider/P1\"/>\n" +
                "       <m:Items ItemName=\"Schneider/P2\"/>\n" +
                "       <m:Items ItemName=\"Schneider/P3\"/>\n" +
                "       <m:Items ItemName=\"Schneider/M\"/>\n" +
                "      </m:ItemList>\n" +
                "     </m:Read>\n" +
                "  </SOAP-ENV:Body>\n" +
                "</SOAP-ENV:Envelope>";

        try {

            // Timeout for reading InputStream arbitrarily set to 3000ms.
            connection.setReadTimeout(3000);
            // Timeout for connection.connect() arbitrarily set to 3000ms.
            connection.setConnectTimeout(3000);
            // For this use case, set HTTP method to GET.
            connection.setRequestMethod("POST");
            // Already true by default but setting just in case; needs to be true since this request
            // is carrying an input (response) body.
            connection.setDoInput(true);
            // Open communications link (network traffic occurs here).

            //Header
            connection.addRequestProperty("SOAPAction", "\"http://opcfoundation.org/webservices/XMLDA/1.0/Read\"");
            connection.setFixedLengthStreamingMode( data.length() ); // wuerde ich nochmal berechnen

            OutputStream mOutputStream = new BufferedOutputStream(connection.getOutputStream());
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(mOutputStream, "UTF-8"));
            writer.write(data);
            writer.flush();
            writer.close();
            mOutputStream.close();


            //mOutputStream.write

            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpsURLConnection.HTTP_OK) {
                throw new IOException("HTTP error code: " + responseCode);
            }
            // Retrieve the response body as an InputStream.
            stream = connection.getInputStream();

            if (stream != null) {
                // Pure stream read out

                /*
                // Converts Stream to String with max length of 500.
                int maxReadSize = 5000;
                Reader reader = null;
                reader = new InputStreamReader(stream, "UTF-8");
                char[] rawBuffer = new char[maxReadSize];
                int readSize;
                StringBuffer buffer = new StringBuffer();
                while (((readSize = reader.read(rawBuffer)) != -1) && maxReadSize > 0) {
                    if (readSize > maxReadSize) {
                        readSize = maxReadSize;
                    }
                    buffer.append(rawBuffer, 0, readSize);
                    maxReadSize -= readSize;
                }
                result = buffer.toString();
                Log.d("Service", result);
                */



                // XML parsing

                XmlPullParser parser = Xml.newPullParser();
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
                stream = connection.getInputStream();
                parser.setInput(stream, null);

                parser.nextTag();
                String nsSOAP = parser.getNamespace();
                String nsXSI    = parser.getNamespace();
                String nsXSD    = parser.getNamespace();
                Log.d("XML", nsSOAP);
                Log.d("XML", nsXSI);
                Log.d("XML", nsXSD);

                // stricter investigation of SOAP response
                parser.require(XmlPullParser.START_TAG, nsSOAP, "Envelope"); parser.next();
                parser.require(XmlPullParser.START_TAG, nsSOAP, "Body"); parser.next();
                parser.require(XmlPullParser.START_TAG, null, "ReadResponse");

                int eventType = parser.next();
                String itemName;
                String value;
                // fast forward to that interesting events
                while ( !(eventType == XmlPullParser.START_TAG &&
                        parser.getName().equals("RItemList")) ){
                    eventType = parser.next();
                }

                while ( !(eventType == XmlPullParser.END_TAG &&
                        parser.getName().equals("RItemList")) ){

                    if ( parser.getEventType() == XmlPullParser.START_TAG &&
                            parser.getName().equals("Items")) {
                        itemName = parser.getAttributeValue(null, "ItemName");
                        parser.next();

                        if ( parser.getEventType() == XmlPullParser.START_TAG &&
                                parser.getName().equals("Value")) {
                            value = parser.nextText();

                        }
                    }

                    eventType = parser.next();
                }



            }
        }

        catch (IOException e) {
            Log.d("Service", "Exception: " + e.getMessage() );
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }

        // Close Stream and disconnect HTTPS connection.
        if (stream != null) {
            stream.close();
        }
        if (connection != null) {
            connection.disconnect();
        }

        return result;
    }

}

