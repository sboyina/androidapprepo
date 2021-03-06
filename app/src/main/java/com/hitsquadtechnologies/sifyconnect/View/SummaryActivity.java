package com.hitsquadtechnologies.sifyconnect.View;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hitsquadtechnologies.sifyconnect.Adapters.SharedLinkSpeedGraphData;
import com.hitsquadtechnologies.sifyconnect.R;
import com.hitsquadtechnologies.sifyconnect.ServerPrograms.RouterService;
import com.hitsquadtechnologies.sifyconnect.utils.SharedPreference;
import com.hsq.kw.packet.KeywestPacket;
import com.hsq.kw.packet.vo.Configuration;
import com.hsq.kw.packet.vo.KWWirelessLinkStats;
import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.sdsmdg.harjot.crollerTest.Croller;


public class SummaryActivity extends BaseActivity {

    private static final int MAX_DATA_POINTS = 10;

    RouterService.Subscription mSubscription;
    TextView mLocalIPAddress,mRemoteIPaddress;
    TextView mLocalMacAddress,mRemoteMacaddress;
    TextView mLocalGPSAddress,mRemoteGPSaddress;
    TextView mLocalRate,mRemoteRate;
    TextView mLocalRadio,mRemoteRadio;
    SharedPreference mSharedPreference;
    GraphView areaGraph;
    LineGraphSeries<DataPoint> localSeries;
    LineGraphSeries<DataPoint> remoteSeries;
    Croller localRateGraph;
    Croller remoteRateGraph;
    TextView mLocalSNRLabel;
    TextView mRemoteSNRLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);
        this.onCreate("Summary", R.id.toolbar, true);
        initialization();
    }

    private void initialization() {

        mLocalIPAddress     = (TextView)findViewById(R.id.WLink_LocalIpNetwork);
        mRemoteIPaddress    = (TextView)findViewById(R.id.WLink_RemoteIpNetwork);
        mLocalMacAddress    = (TextView)findViewById(R.id.WLess_LocalMacAddress);
        mRemoteMacaddress   = (TextView)findViewById(R.id.WLess_RemoteMacAddress);
        mLocalGPSAddress    = (TextView)findViewById(R.id.WLess_LocalGPS);
        mRemoteGPSaddress   = (TextView)findViewById(R.id.WLess_RemoteGPS);
        mLocalRate          = (TextView)findViewById(R.id.WLess_LocalRate);
        mRemoteRate         = (TextView)findViewById(R.id.WLess_RemoteRate);

        localRateGraph      = findViewById(R.id.localRateGraph);
        remoteRateGraph     = findViewById(R.id.remoteRateGraph);
        mLocalRadio         = (TextView)findViewById(R.id.Local_radio);
        mRemoteRadio        = (TextView)findViewById(R.id.Remote_radio);
        mSharedPreference   = new SharedPreference(SummaryActivity.this);
        areaGraph           = findViewById(R.id.area_graph);
        mLocalSNRLabel      = findViewById(R.id.localSNRLabel);
        mRemoteSNRLabel     = findViewById(R.id.remoteSNRLabel);
        findViewById(R.id.mask).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
        initAreaGraph();
    }

    @Override
    protected void onResume() {
        super.onResume();

        KeywestPacket configRequest = new Configuration().getPacket();
        RouterService.INSTANCE.sendRequest(configRequest, new RouterService.Callback<KeywestPacket>() {
            @Override
            public void onSuccess(final KeywestPacket packet) {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Configuration configuration = new Configuration(packet);
                        mLocalMacAddress.setText(mSharedPreference.getMacAddress());
                        mLocalIPAddress.setText(mSharedPreference.getLocalIPAddress());
                    }
                });
            }
        });
        KeywestPacket assocListPacket = new KeywestPacket((byte)1, (byte)1, (byte)2);
        mSubscription = RouterService.INSTANCE.observe(assocListPacket, new RouterService.Callback<KeywestPacket>() {
            @Override
            public void onSuccess(final KeywestPacket packet) {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        updateUI(packet);
                    }
                });
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (this.mSubscription != null) {
            this.mSubscription.cancel();
        }
    }

    public void renderSNR(Activity a, LinearLayout v, int strength, int max) {
        v.removeAllViews();
        int noOfIndicators = 8;
        double scale = max / noOfIndicators;
        strength = Double.valueOf(Math.ceil(strength / scale)).intValue();
        int w = v.getLayoutParams().width;
        for(int i = 0; i < noOfIndicators; i++) {
            int imageId = i < strength ? R.drawable.signal_bar_active : R.drawable.signal_bar;
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams((int) (0.07 * w), ViewGroup.LayoutParams.MATCH_PARENT);
            ImageView imageView = new ImageView(a);
            imageView.setImageDrawable(a.getResources().getDrawable(imageId));
            layoutParams.rightMargin = (int) (0.03 * w);
            imageView.setLayoutParams(layoutParams);
            v.addView(imageView);
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateUI(KeywestPacket testPacket) {
        KWWirelessLinkStats wirelessLinkStats = new KWWirelessLinkStats(testPacket);

        mRemoteIPaddress.setText(wirelessLinkStats.getRemoteIP());
        mRemoteMacaddress.setText(wirelessLinkStats.getMacAddress());

        SharedLinkSpeedGraphData.INSTANCE.add(wirelessLinkStats.getTxInput(), wirelessLinkStats.getRxInput());
        renderGraph();
        //String strLocalGPS = convertTODegress(Double.parseDouble(wirelessLinkStats.getLocalLat()),Double.parseDouble(wirelessLinkStats.getLocalLong()));
        //String strRemoteGPS = convertTODegress(Double.parseDouble(wirelessLinkStats.getRemoteLat()),Double.parseDouble(wirelessLinkStats.getRemoteLong()));
        mLocalGPSAddress.setText(wirelessLinkStats.getLocalLat() +"\n" + wirelessLinkStats.getLocalLong());
        mRemoteGPSaddress.setText(wirelessLinkStats.getRemoteLat()+"\n" +wirelessLinkStats.getRemoteLong());
        mLocalRate.setText(Integer.toString(wirelessLinkStats.getLocalRate())+ " Mbps");
        mRemoteRate.setText(Integer.toString(wirelessLinkStats.getRemoteRate())+ " Mbps");
        localRateGraph.setProgress(wirelessLinkStats.getLocalRate());
        remoteRateGraph.setProgress(wirelessLinkStats.getRemoteRate());


        renderSNR(this, (LinearLayout) findViewById(R.id.localA1Rating), wirelessLinkStats.getLocalSNRA1(), 100);
        renderSNR(this, (LinearLayout) findViewById(R.id.localA2Rating), wirelessLinkStats.getLocalSNRA2(), 100);
        renderSNR(this, (LinearLayout) findViewById(R.id.remoteA1Rating), wirelessLinkStats.getRemoteSNRA1(), 100);
        renderSNR(this, (LinearLayout) findViewById(R.id.remoteA2Rating), wirelessLinkStats.getRemoteSNRA2(), 100);

        if( mSharedPreference.getRadioMode() == 1 ) {
            mLocalRadio.setText("BSU");
            mRemoteRadio.setText("SU");
            mLocalSNRLabel.setText("BSU");
            mRemoteSNRLabel.setText("SU");
        } else {
            mLocalRadio.setText("SU");
            mRemoteRadio.setText("BSU");
            mLocalSNRLabel.setText("SU");
            mRemoteSNRLabel.setText("BSU");
        }
    }

    /*private String convertTODegress(double latitude, double longitude) {
        StringBuilder builder = new StringBuilder();
        if (latitude < 0) {
            builder.append("S ");
        } else {
            builder.append("N ");
        }
        String latitudeDegrees = Location.convert(Math.abs(latitude), Location.FORMAT_SECONDS);
        String[] latitudeSplit = latitudeDegrees.split(":");
        builder.append(latitudeSplit[0]);
        builder.append("°");
        builder.append(latitudeSplit[1]);
        builder.append("'");
        builder.append(latitudeSplit[2]);
        builder.append("\"");
        builder.append(" ");
        if (longitude < 0) {
            builder.append("W ");
        } else {
            builder.append("E ");
        }
        String longitudeDegrees = Location.convert(Math.abs(longitude), Location.FORMAT_SECONDS);
        String[] longitudeSplit = longitudeDegrees.split(":");
        builder.append(longitudeSplit[0]);
        builder.append("°");
        builder.append(longitudeSplit[1]);
        builder.append("'");
        builder.append(longitudeSplit[2]);
        builder.append("\"");
        return builder.toString();
    }*/

    private void initAreaGraph() {
        localSeries = new LineGraphSeries<>(new DataPoint[] {});
        int localSeriesColor = getResources().getColor(R.color.local_line_graph_color);
        localSeries.setTitle("Local (Mbps)");
        localSeries.setColor(localSeriesColor);
        localSeries.setDrawBackground(true);
        localSeries.setBackgroundColor(Color.argb(64, Color.red(localSeriesColor), Color.green(localSeriesColor), Color.blue(localSeriesColor)));
        localSeries.setDrawDataPoints(false);
        remoteSeries = new LineGraphSeries<>(new DataPoint[] {});
        areaGraph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                return isValueX ? null : Double.valueOf(value).intValue() + "";
            }
        });
        int remoteSeriesColor = getResources().getColor(R.color.remote_line_graph_color);
        remoteSeries.setColor(remoteSeriesColor);
        remoteSeries.setDrawBackground(true);
        remoteSeries.setBackgroundColor(Color.argb(64, Color.red(remoteSeriesColor), Color.green(remoteSeriesColor), Color.blue(remoteSeriesColor)));
        remoteSeries.setTitle("Remote (Mbps)");
        remoteSeries.setDrawDataPoints(false);
        LegendRenderer legendRenderer = areaGraph.getLegendRenderer();
        legendRenderer.setVisible(true);
        legendRenderer.setFixedPosition(0, -20);
        legendRenderer.setTextSize(32);
        legendRenderer.setTextColor(Color.parseColor("#000000"));
        legendRenderer.setBackgroundColor(Color.argb(0, 0, 0, 0));
        areaGraph.getViewport().setMinY(0);
        areaGraph.getViewport().setMaxY(200);
        areaGraph.getViewport().setMinX(0);
        areaGraph.getViewport().setMaxX(MAX_DATA_POINTS + 1);
        areaGraph.getViewport().setYAxisBoundsManual(true);
        areaGraph.addSeries(localSeries);
        areaGraph.addSeries(remoteSeries);
        renderGraph();
    }

    public void renderGraph() {
        int maxValue = Double.valueOf(SharedLinkSpeedGraphData.INSTANCE.max() * 1.25).intValue();
        areaGraph.getViewport().setMaxY(Math.max(maxValue, 10));
        localSeries.resetData(SharedLinkSpeedGraphData.INSTANCE.getLocalData());
        remoteSeries.resetData(SharedLinkSpeedGraphData.INSTANCE.getRemoteData());
    }
}
