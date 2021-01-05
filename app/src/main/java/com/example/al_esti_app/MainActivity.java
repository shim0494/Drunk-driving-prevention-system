package com.example.al_esti_app;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity {
    Button startBtn, chauBtn, emerBtn;
    TextView penalView, BAC_View;
    GradientDrawable BAC_Back;
    Socket socket = null;
    View dialogView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("알코올 측정기");

        startBtn = (Button) findViewById(R.id.startBtn);
        chauBtn = (Button) findViewById(R.id.chauBtn);
        emerBtn = (Button) findViewById(R.id.emerBtn);
        BAC_View = (TextView) findViewById(R.id.tempaView);
        BAC_Back = (GradientDrawable) ContextCompat.getDrawable(this, R.drawable.oval);

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BAC_View.setText("측정중...");
                Drawable roundDrawable = getResources().getDrawable(R.drawable.oval);
                roundDrawable.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                    BAC_View.setBackgroundDrawable(roundDrawable);
                } else {
                    BAC_View.setBackground(roundDrawable);
                }
            MyClientTask myClientTask = new MyClientTask();
                myClientTask.execute();
        }
    });

        chauBtn.setOnClickListener(new View.OnClickListener()

    {
        public void onClick (View v){
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://search.naver.com/search.naver?where=nexearch&sm=top_hty&fbm=1&ie=utf8&query=%EB%8C%80%EB%A6%AC%EC%9A%B4%EC%A0%84"));
        startActivity(intent);
    }
    });

        emerBtn.setOnClickListener(new View.OnClickListener()

    {
        public void onClick (View v){
        Uri uri = Uri.parse("tel:119");
        Intent intent = new Intent(Intent.ACTION_DIAL, uri);
        startActivity(intent);
    }
    });

        /* 알코올 일정 수치 이상일 시, 다이얼로그 출력.
        .setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialogView = (View) View.inflate(MainActivity.this, R.layout.dialog1, null);
                AlertDialog.Builder dlg = new AlertDialog.Builder(MainActivity.this);
                dlg.setTitle("경고");
                dlg.setIcon(R.drawable.siren);
                dlg.setView(dialogView);
                dlg.setPositiveButton("확인",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
                                intent.putExtra(SearchManager.QUERY, "대리운전");
                                startActivity(intent);
                            }
                        });
                dlg.setNegativeButton("취소", null);
                dlg.show();
            }
        });*/
}

public class MyClientTask extends AsyncTask<Void, String, Void> {
    String response = "";

    @Override
    protected Void doInBackground(Void... arg0) {
        Socket socket = null;
        try {
            socket = new Socket("192.168.0.111", 8888);

            //송신 (없음)
                /*OutputStream out = socket.getOutputStream();
                out.write(myMessage.getBytes());*/


            //수신
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(1024);
            byte[] buffer = new byte[1024];
            int bytesRead;
            InputStream inputStream = socket.getInputStream();

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
                response += byteArrayOutputStream.toString("UTF-8");
                publishProgress(response);
                Thread.sleep(2000);
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
            response = "UnknownHostException:" + e.toString();
        } catch (IOException e) {
            e.printStackTrace();
            response = "IOException: " + e.toString();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    protected void onProgressUpdate(String... values) {
        BAC_View.setText(response);
        double k = Double.parseDouble(response);

        //BAC(혈중알콜농도)가 0.03을 초과하면 하게 되는 동작.
        if (k >= 0.03) {
            // BAC 표시하는 텍스트 밑에 깔린 원판(TextView - drawable) 색깔 변경.
            Drawable roundDrawable = getResources().getDrawable(R.drawable.oval);
            roundDrawable.setColorFilter(Color.RED, PorterDuff.Mode.SRC_ATOP);
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                BAC_View.setBackgroundDrawable(roundDrawable);
            } else {
                BAC_View.setBackground(roundDrawable);
            }
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(2000);

            dialogView = (View) View.inflate(MainActivity.this, R.layout.dialog1, null);
            AlertDialog.Builder dlg = new AlertDialog.Builder(MainActivity.this);
            dlg.setTitle("경고");
            dlg.setIcon(R.drawable.siren);
            dlg.setView(dialogView);
            dlg.setPositiveButton("확인",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://search.naver.com/search.naver?where=nexearch&sm=top_hty&fbm=1&ie=utf8&query=%EB%8C%80%EB%A6%AC%EC%9A%B4%EC%A0%84"));
                            startActivity(intent);
                        }
                    });
            dlg.setNegativeButton("취소", null);
            dlg.show();
        }
    }


    @Override
    protected void onPostExecute(Void result) {
        BAC_View.setText("음주 측정이 완료되었습니다.");
        super.onPostExecute(result);
    }

}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater mInflater = getMenuInflater();
        mInflater.inflate(R.menu.menu1, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.punishment:
                Intent intent = new Intent(MainActivity.this, SecondActivity.class);
                startActivity(intent);
                return true;
            case R.id.bodychange:
                Intent intent1 = new Intent(MainActivity.this, ThirdActivity.class);
                startActivity(intent1);
                return true;
            case R.id.recordAlcoh:
                Intent intent3 = new Intent(Intent.ACTION_WEB_SEARCH);
                intent3.putExtra(SearchManager.QUERY, "192.168.0.102:9000/sensor/select_0.03");
                startActivity(intent3);
                return true;
            case R.id.recordAll:
                Intent intent2 = new Intent(Intent.ACTION_WEB_SEARCH);
                intent2.putExtra(SearchManager.QUERY, "192.168.0.102:9000/sensor/select");
                startActivity(intent2);
                return true;
        }
        return false;
    }
}

