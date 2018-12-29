package de.tu_dresden.et.kva_monitor;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.util.Xml;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.HttpsURLConnection;

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

    static final String NAMESPACE_SOAP          = "http://schemas.xmlsoap.org/soap/envelope/";
    static final String NAMESPACE_XSI           = "http://www.w3.org/2001/XMLSchema-instance";
    static final String NAMESPACE_XSD           = "http://www.w3.org/2001/XMLSchema";

    static final String PATH_WEAR_UI            = "/wear_UI";
    static final String PATH_OPC_REQUEST        = "/OPC_request";
    static final String PATH_LAUNCH_ACTIVITY    = "/launch_activity";

    static final String CHANNEL_ID_ALARM        = "Alarm";
    static final String CHANNEL_ID_CONN_STATUS  = "Connection Status";

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

    private String XML_write;

    private DataClient myDataClient;

    private Map<String, BinaryAlarm> alarmDatabase;

    private NotificationCompat.Builder notificationBuilder;


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

    // any action that is not supposed to be run on the UI thread

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

                    // continue polling
                    message = this.obtainMessage(READ_REQUEST_WHATID);
                    this.sendMessageDelayed(message, POLLING_INTERVAL_MS);
                    break;
                case WRITE_REQUEST_WHATID:
                    sendRequest("Write", XML_write); // write request

                    // follow up immediately with updated data (check response)
                    message = this.obtainMessage(READ_REQUEST_WHATID);
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


        // Client for data exchanges between handheld and wearable
        myDataClient = Wearable.getDataClient(this);

        // Dictionary (hashtable) as database for the AlarmManager
        createNotificationChannel();
        alarmDatabase = new Hashtable<>();
        String [] alarmDataPoints = getResources().getStringArray( R.array.alarm_datapoints_list );
        for (String dataPoint: alarmDataPoints) {
            alarmDatabase.put( dataPoint, new BinaryAlarm(this, dataPoint) );
        }

    }


    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start service from the UI.
        //TODO: Edit notification properties for running service, change icon

        // Run HTTP requests
        Message msg = serviceHandler.obtainMessage(READ_REQUEST_WHATID); //start new loop
        serviceHandler.sendMessage(msg);

        // Refer to the handheld activity that started the OPC communication
        // This activity is started when the notification is tapped (setContentIntent)
        Intent notificationIntent = new Intent(this, ControlActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent,0);

        notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID_CONN_STATUS)
                .setContentTitle(getString(R.string.notification_OPCconnection_failed_title))
                .setContentText(getString(R.string.notification_OPCconnection_failed_text))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true);

        startForeground(ONGOING_NOTIFICATION_ID, notificationBuilder.build());


        // Listen to commands sent from wearable and listen for changes to alarm data pointsandorid java dictionary

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

            notificationBuilder
                    .setContentTitle(getString(R.string.notification_OPCconnection_failed_title))
                    .setContentText(getString(R.string.notification_OPCconnection_failed_text));
            NotificationManagerCompat.from(this)
                    .notify(ONGOING_NOTIFICATION_ID, notificationBuilder.build());
            return;
        } else {
            notificationBuilder
                    .setContentTitle(getString(R.string.notification_OPC_connection_title))
                    .setContentText(getString(R.string.notification_OPC_connection_text));
            NotificationManagerCompat.from(this)
                    .notify(ONGOING_NOTIFICATION_ID, notificationBuilder.build());
        }

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
            BinaryAlarm binaryAlarm;

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

                                // Set values for the (custom) AlarmManager
                                if ( alarmDatabase.containsKey(itemName) ) {
                                    binaryAlarm = alarmDatabase.get(itemName);
                                    binaryAlarm.setPresentValue( Boolean.parseBoolean(value) );
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

                // ReadResponse body read and all information fetched, skip parsing
                else if ( eventType == XmlPullParser.END_TAG &&
                        parser.getName().equals("ReadResponse") ) {
                    break;
                }
            }


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

    // For future TargetAPI, starting from Android 8.0
    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Notification channel for alarming data points
            CharSequence name = getString(R.string.channelName_alarm);
            String description = getString(R.string.channelDescription_alarm);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID_ALARM,
                    name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);


            // Notification channel for this service running
            name = getString(R.string.channelName_OPCconnection);
            description = getString(R.string.channelDescription_OPCconnection);
            importance = NotificationManager.IMPORTANCE_DEFAULT;
            channel = new NotificationChannel(CHANNEL_ID_CONN_STATUS, name, importance);
            channel.setDescription(description);

            notificationManager.createNotificationChannel(channel);
        }
    }


}
