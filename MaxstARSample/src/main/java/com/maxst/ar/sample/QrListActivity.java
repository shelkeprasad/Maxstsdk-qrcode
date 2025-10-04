package com.maxst.ar.sample;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.maxst.ar.sample.qrcodeFusionTracker.ScanQrcodeActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;


public class QrListActivity extends AppCompatActivity {

    private final int[] videoResIds = {
            R.raw.step1,
            R.raw.step2,
            R.raw.step3,
            R.raw.step4,
            R.raw.fettemp4
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_qr_list);

        ListView listView = findViewById(R.id.activity_qrlist_listview);

        ArrayList<String> items = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            items.add("TextView " + i);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.list_item,
                R.id.tvItem,
                items
        );

        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            openQrCodeFusionActivity(position);
        });
    }

    private void openQrCodeFusionActivity(int position) {
        try {
            JSONObject jsonObject = new JSONObject();
            JSONArray textViewsArray = new JSONArray();
            JSONArray videosArray = new JSONArray();

            for (int i = 0; i <= position; i++) {
                textViewsArray.put("TextView " + (i + 1));

                String videoUri = "android.resource://" + getPackageName() + "/" + videoResIds[i];
                videosArray.put(videoUri);
            }
            switch (position) {
                case 0: jsonObject.put("position", "left"); break;
                case 1: jsonObject.put("position", "top"); break;
                case 2: jsonObject.put("position", "right"); break;
                case 3: jsonObject.put("position", "bottom"); break;
                case 4: jsonObject.put("position", "center"); break;
            }

            jsonObject.put("textViews", textViewsArray);
            jsonObject.put("videos", videosArray);
            Intent intent = new Intent(QrListActivity.this, ScanQrcodeActivity.class);
            intent.putExtra("jsonData", jsonObject.toString());
            startActivity(intent);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
