package com.bitpay.bitpayforclover;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.bitpay.sdk.android.BitPayAndroid;
import com.bitpay.sdk.android.interfaces.BitpayPromiseCallback;
import com.bitpay.sdk.android.interfaces.PromiseCallback;
import com.bitpay.sdk.controller.BitPay;
import com.bitpay.sdk.controller.BitPayException;
import com.bitpay.sdk.controller.KeyUtils;
import com.google.bitcoin.core.ECKey;


public class ConfigureActivity extends ActionBarActivity implements View.OnClickListener{

    public static final String TAG = "Pairing Status: ";
    private EditText pairingCode;
    private Button submitButton;
    /* package */ BitPayAndroid client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure);

        pairingCode = (EditText) findViewById(R.id.pairingCodeField);
        submitButton = (Button) findViewById(R.id.submit_pair);

        submitButton.setOnClickListener(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_configure, menu);
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

    @Override
    public void onClick(View view) {
        String clientName = "whyaname";
        String code = pairingCode.getText().toString();
        ECKey ecKey = KeyUtils.createEcKey();
        String ecKeyHexa = KeyUtils.exportPrivateKeyToHexa(ecKey);
        BitPayAndroid.getClient(ecKeyHexa, "https://test.bitpay.com").then(new BitpayPromiseCallback() {
            @Override
            public void onSuccess(BitPayAndroid bitPayAndroid) {
                client = bitPayAndroid;
            }

            @Override
            public void onError(BitPayException e) {
                Log.d(TAG, "no client");
            }
            });

        client.authorizeClientAsync(code).then(new PromiseCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d(TAG, "success");
            }

            @Override
            public void onError(BitPayException e) {
                Log.d(TAG, "failure");
            }
        });


    }
}
