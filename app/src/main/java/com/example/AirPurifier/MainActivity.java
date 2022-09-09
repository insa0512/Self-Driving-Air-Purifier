package com.example.AirPurifier;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    ImageView imgSet, humiImage, thImage;
    LinearLayout layout;
    TextView conditionTxt, statusTxt, explainTxt, temperaturTxt, humidityTxt;

    BluetoothAdapter mBluetoothAdapter;
    Set<BluetoothDevice> mPairedDevices;
    List<String> mListPairedDevices;

    Handler mBluetoothHandler;
    ConnectedBluetoothThread mThreadConnectedBluetooth;
    BluetoothDevice mBluetoothDevice;
    BluetoothSocket mBluetoothSocket;
    String str = "";
    Toolbar toolbar;

    final static int BT_MESSAGE_READ = 2;
    final static int BT_CONNECTING_STATUS = 3;
    final static UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // UUID: 아두이노 연결에 사용하는 전용 UUID코드임

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        layout = findViewById(R.id.layout);

        imgSet = findViewById(R.id.imageSet);
        conditionTxt = findViewById(R.id.contidionText);
        statusTxt = findViewById(R.id.stausText);
        explainTxt = findViewById(R.id.explainText);
        temperaturTxt = findViewById(R.id.temperaturText);
        humidityTxt = findViewById(R.id.humidityText);
        toolbar = findViewById(R.id.toolbar);
        humiImage = findViewById(R.id.humiImage);
        thImage = findViewById(R.id.thImage);
        setSupportActionBar(toolbar);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // 버전 30이상 부터 권한 추가 부여 요청함
            requestPermissions(
                    new String[]{
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_ADVERTISE,
                            Manifest.permission.BLUETOOTH_CONNECT
                    },
                    1);
        } else { // 버전 23 부터 불루투스 권한 부여 요청함
            requestPermissions(
                    new String[]{
                            Manifest.permission.BLUETOOTH
                    }, 1);
        }
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        mBluetoothHandler = new Handler(Looper.getMainLooper()) {
            public void handleMessage(android.os.Message msg) {
            }
        };
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) { // 종료 버튼시 팝업화면 띄우기
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            String alertTitle = "green home";
            String buttonMessage = "어플을 종료하시겠습니까?";
            String buttonYes = "Yes";
            String buttonNo = "No";

            new AlertDialog.Builder(this)
                    .setTitle(alertTitle)
                    .setMessage(buttonMessage)
                    .setPositiveButton(buttonYes, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // TODO Auto-generated method stub
                            moveTaskToBack(true);
                            finish();
                        }
                    })
                    .setNegativeButton(buttonNo, null)
                    .show();
        }
        return true;
    }


    public boolean onCreateOptionsMenu(Menu menu) { //메뉴바(ToolBar) 만들기
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_option, menu);
        return true;
    }

    // 추가된 소스, ToolBar에 추가된 항목의 select 이벤트를 처리하는 함수
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        super.onOptionsItemSelected(item);
        if (item.getItemId() == R.id.action_bluetooth) { //블루투스 아이콘 누를시
            // User chose the "Settings" item, show the app settings UI...
            Toast.makeText(getApplicationContext(), "블루투스", Toast.LENGTH_LONG).show();
            bluetoothOn(); //블루투스 활성화
            listPairedDevices(); //블루투스 기기 목록
            return true;
        }
        // If we got here, the user's action was not recognized.
        // Invoke the superclass to handle it.
        return true;
    }


    void bluetoothOn() { // 블루투스 활성화시
        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "블루투스를 지원하지 않는 기기입니다.", Toast.LENGTH_LONG).show();
        } else {
            if (mBluetoothAdapter.isEnabled()) { // 블루투스가 이미 켜져있을때
            } else { // 불루투스가 켜져있지 않을 때
                Toast.makeText(getApplicationContext(), "블루투스가 활성화 되어 있지 않습니다.", Toast.LENGTH_LONG).show();
                Intent intentBluetoothEnable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                mStartForResult.launch(intentBluetoothEnable); // startActivityForResult는 옛날 펑션
            }
        }
    }

    ActivityResultLauncher<Intent> mStartForResult = registerForActivityResult( //블루투스 비활성화에서 활성화시 결과 출력
            new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == RESULT_OK) { // 블루투스 활성화를 확인을 클릭하였다면
                        Toast.makeText(getApplicationContext(), "블루투스 활성화", Toast.LENGTH_LONG).show();
                    } else if (result.getResultCode() == RESULT_CANCELED) { // 블루투스 활성화를 취소를 클릭하였다면
                    }
                }
            });

    @SuppressLint("MissingPermission")
    void listPairedDevices() { // 블루투스 기기 목록
        if (mBluetoothAdapter.isEnabled()) { // 블루투스가 이미 켜져 있을 때
            mPairedDevices = mBluetoothAdapter.getBondedDevices(); //블루투스 목록 가져옴

            if (mPairedDevices.size() > 0) { //페어링할 기기의 목록이 1개 이상일 때 장치 목록을 보여줌
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("장치 선택");

                mListPairedDevices = new ArrayList<>();
                for (BluetoothDevice device : mPairedDevices) {
                    mListPairedDevices.add(device.getName());
                }
                final CharSequence[] items = mListPairedDevices.toArray(new CharSequence[mListPairedDevices.size()]);
                mListPairedDevices.toArray(new CharSequence[mListPairedDevices.size()]);

                builder.setItems(items, (dialog, item) -> connectSelectedDevice(items[item].toString()));
                AlertDialog alert = builder.create();
                alert.show();
            } else {
                Toast.makeText(getApplicationContext(), "페어링된 장치가 없습니다.", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(getApplicationContext(), "블루투스가 비활성화 되어 있습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("MissingPermission")
    void connectSelectedDevice(String selectedDeviceName) { // 선택된 디바이스 연결
        for (BluetoothDevice tempDevice : mPairedDevices) { // 블루투스에서 누른 디바이스 이름 탐색 해서 정보 가져옴
            if (selectedDeviceName.equals(tempDevice.getName())) {
                mBluetoothDevice = tempDevice;
                break;
            }
        }
        try {
            mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(BT_UUID); //블루투스 소켓에 아두이노 UUID를 집어 넣음
            mBluetoothSocket.connect(); //블루투스 소켓 연결
            mThreadConnectedBluetooth = new ConnectedBluetoothThread(mBluetoothSocket); //블루투스 스레드로 연결 (정보를 계속 가져옴)
            mThreadConnectedBluetooth.start(); //블루투스 통신 시작
            mBluetoothHandler.obtainMessage(BT_CONNECTING_STATUS, 1, -1).sendToTarget(); //블루투스에서 송신하는 메시지 수신
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "블루투스 연결 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
        }
    }

    private class ConnectedBluetoothThread extends Thread { //블루투스 지속적인 통신을 위한 스레드
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private byte[] mmBuffer;

        public ConnectedBluetoothThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;

            try {
                tmpIn = socket.getInputStream(); //스트림으로 Char형 메시지를 버퍼로 읽어오는 역할
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "소켓 연결 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
            }

            mmInStream = tmpIn;
        }

        public void run() {
            mmBuffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = mmInStream.read(mmBuffer); // Char형 메시지를 버퍼로 읽어 들여서 데이터 처리속도 향상
                    String strBuf = new String(mmBuffer, 0, bytes); // 읽어 들인 메시지를 String으로 처리해 객체 처리함
                    if (bytes != 0) {
                        mBluetoothHandler.obtainMessage(BT_MESSAGE_READ, bytes, 0, mmBuffer).sendToTarget(); // 블루투스에서 송신하는 메시지 수신
                        Log.d("Byte", String.valueOf(bytes));
                    }
                    for (int i = 0; i < bytes; i++) { // 읽어 들인 bytes크기 만큼 StrBuf 반복 수신
                        if (strBuf.charAt(i) == '#') { // 먼지센서값을 #전까지 끊어서 읽음
                            str = str.replace("#", ""); // #를 ""로 바꿔서 화면에 띄움, Concat 토큰을 제거
                            showMessage(str, "Dust");//미세먼지 출력
                            Log.d("Dust2", "str:" + str);
                            str = ""; // str을 비워서 다음 값을 수신함
                            break;
                        } else if (strBuf.charAt(i) == '!') { // 온도센서값을 !전까지 끊어서 읽음
                            str = str.replace("!", ""); // !를 ""로  바꿔서 화면에 띄움, Concat 토큰을 제거
                            showMessage(str, "Temperature"); //온도 출력
                            Log.d("Temp2", "str:" + str);
                            str = ""; // str을 비워서 다음 값을 수신함
                            break;
                        }

                        if (strBuf.charAt(i) == '%') { // 습도센서값을 %전까지 끊어서 읽음
                            str = str.replace("%", ""); // %를 ""로  바꿔서 화면에 띄움, Concat 토큰을 제거
                            showMessage(str, "Humidity"); //습도 출력
                            Log.d("humi2", "str:" + str);
                            str = ""; //
                            break;
                        } else {
                            str += strBuf.charAt(i); // str를 char로 계속 읽어 들이게 함
                            Log.d("test", "str:" + str);
                        }
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }

        public void showMessage(String strMsg, String tmp) {
            // 메시지 텍스트를 핸들러에 전달
            Message msg = Message.obtain(mHandler, 0, strMsg);
            Log.d("DUST", "showmessage:" + strMsg);

            switch (tmp) {
                case "Dust":  //임시로 받아들인 메시지가 미세먼지에 관한 정보면
                    mHandler.sendMessage(msg);
                    break;
                case "Temperature":  //임시로 받아들인 메시지가 온도에 관한 정보면
                    mHandler2.sendMessage(msg);
                    break;
                case "Humidity":  //임시로 받아들인 메시지가 습도에 관한 정보면
                    mHandler3.sendMessage(msg);
                    break;
            }
            Log.d("tag1", strMsg);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "소켓 해제 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
            }
        }
    }

    Handler mHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            String strMsg = (String) msg.obj; //미세먼지 msg를 처리하기 위해 읽어들임
            Log.d("test12", strMsg);
            int dust = Integer.parseInt(strMsg); //미세먼지 msg를 int값으로 변환
            if (dust <= 30) {
                imgSet.setImageResource(R.drawable.stat_good);
                conditionTxt.setText(dust);
                statusTxt.setText("좋음");
                explainTxt.setText("공기가 맑습니다.");
            } else if (dust <= 80) {
                imgSet.setImageResource(R.drawable.stat_nomal);
                conditionTxt.setText(dust);
                statusTxt.setText("보통");
                explainTxt.setText("보통입니다.");
            } else if (dust <= 150) {
                imgSet.setImageResource(R.drawable.stat_bad);
                conditionTxt.setText(dust);
                statusTxt.setText("나쁨");
                explainTxt.setText("환기좀 시켜주세요~~");
            } else {
                imgSet.setImageResource(R.drawable.stat_warning);
                conditionTxt.setText(dust);
                statusTxt.setText("매우 나쁨");
                explainTxt.setText("환기가 시급합니다!!");
            }
        }
    };

    Handler mHandler2 = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            String strMsg = (String) msg.obj; //온도 msg를 처리하기 위해 읽어들임
            Log.d("test13", strMsg);
            int tem = Integer.parseInt(strMsg); //온도 msg를 int값으로 변환
            temperaturTxt.setText(tem);
            if (tem < 22) {
                thImage.setImageResource(R.drawable.th_low);
            } else if (tem <= 26) {
                thImage.setImageResource(R.drawable.th_nomal);
            } else {
                thImage.setImageResource(R.drawable.th_high);
            }
        }
    };

    Handler mHandler3 = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            String strMsg = (String) msg.obj; //습도 msg를 처리하기 위해 읽어들임
            Log.d("test14", strMsg);
            int hum = Integer.parseInt(strMsg); //습도 msg를 int값으로 변환
            humidityTxt.setText(hum);
            if (hum < 40) {
                humiImage.setImageResource(R.drawable.hu_low);
            } else if (hum <= 60) {
                humiImage.setImageResource(R.drawable.hu_nomal);
            } else {
                humiImage.setImageResource(R.drawable.hu_high);
            }
        }
    };
}
