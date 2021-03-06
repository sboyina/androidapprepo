package com.hitsquadtechnologies.sifyconnect.View;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.hitsquadtechnologies.sifyconnect.Adapters.AntennaAdapter;
import com.hitsquadtechnologies.sifyconnect.Model.AntennaSignal;
import com.hitsquadtechnologies.sifyconnect.R;
import com.hitsquadtechnologies.sifyconnect.ServerPrograms.RouterService;
import com.hitsquadtechnologies.sifyconnect.utils.SharedPreference;
import com.hsq.kw.packet.KeywestPacket;
import com.hsq.kw.packet.vo.KWWirelessLinkStats;

import java.util.LinkedList;
import java.util.List;

public class AlignmentActivity extends BaseActivity {

    SharedPreference mSharedPreference;
    RouterService.Subscription mSubscription;
    TextView registerLabel;
    AntennaSignal localA1;
    AntennaSignal localA2;
    AntennaSignal remoteA1;
    AntennaSignal remoteA2;
    String localRadio;
    String remoteRadio;
    ListView antennaList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alignment);
        this.onCreate("Alignment", R.id.toolbar, true);
        registerLabel = findViewById(R.id.register_label);
        antennaList = findViewById(R.id.antenna_list);
        mSharedPreference = new SharedPreference(this);
        if( mSharedPreference.getRadioMode() == 1 ) {
            localRadio = "BSU";
            remoteRadio = "SU";
        } else {
            localRadio = "SU";
            remoteRadio = "BSU";
        }
        AlignInit();
    }


    private void AlignInit() {
        mSharedPreference = new SharedPreference(AlignmentActivity.this);
    }

    /** not required for this release **/
    public void renderSignal(Activity a, LinearLayout v, int strength) {
        int height = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                8,
                getResources().getDisplayMetrics()
        );
        v.removeAllViews();
        for(int i = 0; i < strength; i++) {
            ImageView imageView = new ImageView(a);
            imageView.setImageDrawable(a.getResources().getDrawable(R.drawable.signal_line));
            imageView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
            v.addView(imageView);
        }
    }

    private void requestToServer() {
        KeywestPacket wirelessLinkPacket = new KeywestPacket((byte)1, (byte)1, (byte)2);
        mSubscription = RouterService.INSTANCE.observe(wirelessLinkPacket, new RouterService.Callback<KeywestPacket>() {
            @Override
            public void onSuccess(KeywestPacket packet) {
                updateUI(new KWWirelessLinkStats(packet));
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mSubscription != null) {
            mSubscription.cancel();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        requestToServer();
    }

    @SuppressLint("SetTextI18n")
    private void updateUI(KWWirelessLinkStats wirelessLinkStats) {
        if (wirelessLinkStats.getNoOfLinks() > 0) {
            registerLabel.setText("Registered");
        } else {
            registerLabel.setText("Unregistered");
        }
        if (localA1 == null) {
            localA1 = new AntennaSignal(localRadio, "A1", R.drawable.summary_bg1, R.drawable.summary_bg6);
        }
        if (localA2 == null) {
            localA2 = new AntennaSignal(localRadio, "A2", R.drawable.summary_bg1, R.drawable.summary_bg6);
        }
        if (remoteA1 == null) {
            remoteA1 = new AntennaSignal(remoteRadio, "A1", R.drawable.summary_bg3, R.drawable.summary_bg5);
        }
        if (remoteA2 == null) {
            remoteA2 = new AntennaSignal(remoteRadio, "A2", R.drawable.summary_bg3, R.drawable.summary_bg5);
        }
        localA1.setCurrent(wirelessLinkStats.getLocalSignalA1());
        localA2.setCurrent(wirelessLinkStats.getLocalSignalA2());
        remoteA1.setCurrent(wirelessLinkStats.getRemoteSignalA1());
        remoteA2.setCurrent(wirelessLinkStats.getRemoteSignalA2());
        List<AntennaSignal> list = new LinkedList<>();
        list.add(localA1);
        list.add(localA2);
        list.add(remoteA1);
        list.add(remoteA2);
        AntennaAdapter adapter = new AntennaAdapter(this, list);
        antennaList.setAdapter(adapter);
    }
}