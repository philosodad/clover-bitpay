package com.bitpay.bitpayforclover;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.bitpay.sdk.android.BitPayAndroid;
import com.bitpay.sdk.android.interfaces.BitpayPromiseCallback;
import com.bitpay.sdk.android.interfaces.PromiseCallback;
import com.bitpay.sdk.controller.BitPayException;
import com.bitpay.sdk.controller.KeyUtils;
import com.bitpay.sdk.model.Token;
import com.clover.sdk.util.CloverAccount;
import com.clover.sdk.util.CloverAuth;
import com.clover.sdk.v1.ResultStatus;
import com.clover.sdk.v1.tender.Tender;
import com.clover.sdk.v1.tender.TenderConnector;

import com.google.bitcoin.core.ECKey;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class ConfigureActivity extends Activity implements View.OnClickListener{

    public static final String TAG = "Pairing Status: ";
    private static final int REQUEST_ACCOUNT = 1;

    public Map<String, String> tokenMap;
    public List<Token> tokens;
    private EditText pairingCode;
    private Button submitButton;
    private Button tenderButton;
    private TextView clientIdView;
    private TextView tokenView;
    private TextView tenderResult;
    private String code;
    private String KEYFILE;
    private String TOKENFILE;
    private Context cntxt;
    private TenderConnector tenderConnector;
    private Account account;

    private CloverAuth.AuthResult mCloverAuth;



    /* package */ BitPayAndroid client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure);

        pairingCode = (EditText) findViewById(R.id.pairingCodeField);
        submitButton = (Button) findViewById(R.id.submit_pair);
        clientIdView = (TextView) findViewById(R.id.clientID);
        tokenView = (TextView) findViewById(R.id.tokenValue);
        tenderResult = (TextView) findViewById(R.id.tenderResult);
        submitButton.setOnClickListener(this);
        tenderButton = (Button) findViewById(R.id.tenderButton);
        KEYFILE = "ec_file";
        TOKENFILE = "token_file";
        cntxt = this.getApplicationContext();


        account = CloverAccount.getAccount(this);
        ArrayList<String> keyFind = readFromFile(KEYFILE);
        if ( keyFind.get(0).equals("OK") ) {
            String key = keyFind.get(1);
            String value;
            try {
                ECKey k = KeyUtils.loadFromHexaEncodedPrivateKey(key);
                value = KeyUtils.deriveSIN(k);
            }
            catch ( Exception e ){
                value = "INVALID ID";
            }
            clientIdView.setText(value);
        }
        else {
            clientIdView.setText("No Client ID");
        }
        ArrayList<String> tokenFind = readFromFile(TOKENFILE);
        if(tokenFind.get(0).equals("OK") ) {
            tokenView.setText(tokenFind.get(1));
        }
        else {
            tokenView.setText("No Token");
        }
        getCloverAuth();
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
        code = pairingCode.getText().toString();
        ECKey ecKey = KeyUtils.createEcKey();
        final String ecKeyHexa = KeyUtils.exportPrivateKeyToHexa(ecKey);
        BitPayAndroid.getClient(ecKeyHexa, "https://test.bitpay.com").then(new BitpayPromiseCallback() {
            @Override
            public void onSuccess(BitPayAndroid bitPayAndroid) {
                Log.d(TAG, "Client created.");
                client = bitPayAndroid;
                authorizeClient();

                String hexakey = client.getPrivateKey();
                ECKey akey = KeyUtils.loadFromHexaEncodedPrivateKey(hexakey);
                String clientId = KeyUtils.deriveSIN(akey);
                clientIdView.setText(clientId);
                writeToFile(KEYFILE, hexakey);

            }

            @Override
            public void onError(BitPayException e) {
                Log.d(TAG, "no client");
                e.printStackTrace();
            }
        });
        Log.d(TAG, "Left the onClick method");

    }

    private void authorizeClient(){
        client.authorizeClientAsync(code).then(new PromiseCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d(TAG, "success");
                client.getTokensAsync().then(new PromiseCallback<List<Token>>() {
                    @Override
                    public void onSuccess(List<Token> rTokens) {
                        String toke = "";
                        for( Token t : rTokens){
                            toke = t.getValue();
                        }
                        tokenView.setText(toke);
                        writeToFile(TOKENFILE, toke);
                    }

                    @Override
                    public void onError(BitPayException e) {
                        Log.d(TAG, "no tokens returned");
                        File dir = getFilesDir();
                        File file = new File(dir, TOKENFILE);
                        file.delete();
                    }
                });
            }

            @Override
            public void onError(BitPayException e) {
                Log.d(TAG, "failure");
            }
        });
    }

    private ArrayList<String> readFromFile(String filename){
        String status;
        String value;
        try {
            FileInputStream fis = cntxt.openFileInput(filename);
            InputStreamReader isr = new InputStreamReader(fis);
            StringBuilder stbdl = new StringBuilder();
            int data = isr.read();
            while(data != -1){
                char theChar = (char) data;
                stbdl.append(theChar);
                data = isr.read();
            }
            status = "OK";
            value = stbdl.toString();
        }
        catch (IOException e){
            status = "ERROR";
            value = e.getMessage();
        }
        ArrayList<String> statusAndValue = new ArrayList<String>();
        statusAndValue.add(status);
        statusAndValue.add(value);
        return statusAndValue;
    }

    private String writeToFile(String Filename, String value){
        String whatHappened;
        try {
            FileOutputStream fos = cntxt.openFileOutput(Filename, Context.MODE_PRIVATE);
            try {
                fos.write(value.getBytes());
                fos.close();
                whatHappened = "OK";
            }
            catch (IOException e) {
                whatHappened = e.getMessage();
            }
        } catch (FileNotFoundException e) {
            whatHappened = e.getMessage();
        }
        return whatHappened;
    }

    public void createTender(View view){
        final String tenderName = "bitcoin";
        final String packageName = getPackageName();
        tenderConnector = new TenderConnector(this, account, null);


        tenderConnector.checkAndCreateTender(tenderName, packageName, true, false, new TenderConnector.TenderCallback<Tender>() {
            @Override
            public void onServiceSuccess(Tender result, ResultStatus status) {
                super.onServiceSuccess(result, status);
                String text = "Custom Tender:\n";
                text += "  " + result.getId() + " , " + result.getLabel() + " , " + result.getLabelKey() + " , " + result.getEnabled() + " , " + result.getOpensCashDrawer() + "\n";
                tenderResult.setText(text);
            }

            @Override
            public void onServiceFailure(ResultStatus status) {
                super.onServiceFailure(status);
                tenderResult.setText(status.getStatusMessage());
            }

            @Override
            public void onServiceConnectionFailure() {
                super.onServiceConnectionFailure();
                tenderResult.setText("Service Connection Failure");
            }
        });
    }

    private void getCloverAuth() {
        // This needs to be done on a background thread
        new AsyncTask<Void, Void, CloverAuth.AuthResult>() {
            @Override
            protected CloverAuth.AuthResult doInBackground(Void... params) {
                try {
                    return CloverAuth.authenticate(ConfigureActivity.this, account);
                } catch (OperationCanceledException e) {
                    Log.e(TAG, "Authentication cancelled", e);
                } catch (Exception e) {
                    Log.e(TAG, "Error retrieving authentication", e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(CloverAuth.AuthResult result) {
                mCloverAuth = result;

                // To get a valid auth result you need to have installed the app from the App Market. The Clover servers
                // only creates the token once installed the first time.
                if (mCloverAuth != null && mCloverAuth.authToken !=null) {
                    Log.d(TAG, mCloverAuth.authToken);
                } else {
                    Log.d(TAG, "Couldn't get Token");
                }
            }
        }.execute();
    }
}
