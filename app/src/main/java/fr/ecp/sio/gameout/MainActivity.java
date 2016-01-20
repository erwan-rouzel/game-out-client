package fr.ecp.sio.gameout;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.plus.Plus;
import com.google.example.games.basegameutils.BaseGameUtils;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;

import fr.ecp.sio.gameout.salon.GoogleApiClientSingleton;
import fr.ecp.sio.gameout.salon.InstructionsActivity;
import fr.ecp.sio.gameout.salon.OnlineGameActivity;
import fr.ecp.sio.gameout.salon.SettingsActivity;


public class MainActivity extends ActionBarActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    InterstitialAd mInterstitialAd;

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int RC_SIGN_IN = 9001;

     // Client used to interact with Google APIs
    private GoogleApiClient mGoogleApiClient;

    private GoogleApiClientSingleton singleton;

    // Are we currently resolving a connection failure?
    private boolean mResolvingConnectionFailure = false;

    // Has the user clicked ths sign-in button?
    private boolean mSignInClicked = false;

    // Should the sign-in flow be started automatically?
    private boolean mAutoStartSignInFlow = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        mInterstitialAd = new InterstitialAd(getApplicationContext());
        String adUnitId = getResources().getString(R.string.fs_ads_unit_id);
        mInterstitialAd.setAdUnitId(adUnitId);
        requestNewInterstitial();
        setContentView(R.layout.activity_main);
        final AdView adView = (AdView) findViewById(R.id.banner1);
        adView.loadAd(new AdRequest.Builder().build());
        mInterstitialAd.setAdListener(new AdListener() {

            @Override
            public void onAdLoaded() {
                mInterstitialAd.show();
            }
        });

        // Get invitation from Bundle
        if (savedInstanceState == null) {
            // Create the Google API Client with access to Plus and Games
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(Plus.API).addScope(Plus.SCOPE_PLUS_LOGIN)
                    .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                    .build();

            // Store the Google API Client in GoogleApiClientSingleton
            GoogleApiClientSingleton.getInstance()
                    .setApiClient(mGoogleApiClient);
        }
        if (mInterstitialAd != null && mInterstitialAd.isLoaded())
            mInterstitialAd.show();


        mGoogleApiClient = GoogleApiClientSingleton.getInstance()
                .getApiClient();

        findViewById(R.id.sign_in_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSignInClicked = true;
                mGoogleApiClient.connect();
                findViewById(R.id.sign_in_button).setVisibility(View.GONE);
            }
        });

        findViewById(R.id.instructions_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, InstructionsActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.settings_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.multi_player_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, OnlineGameActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.single_player_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, GameActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.exit_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (GoogleApiClientSingleton.getInstance().isSignedIn())
                    GoogleApiClientSingleton.getInstance().signOut();

                finish();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "onConnected: sign-in successful.");
        // Disable login button
        // Enable Online game button
        Intent intent = new Intent();
        setResult(Activity.RESULT_OK,intent);
        findViewById(R.id.sign_in_button).setVisibility(View.GONE);
        findViewById(R.id.multi_player_button).setVisibility(View.VISIBLE);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "onConnectionSuspended: trying to reconnect.");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (mResolvingConnectionFailure) {
            Log.i(TAG, "onConnectionFailed: already resolving.");
            return;
        }

        if (mSignInClicked || mAutoStartSignInFlow) {
            mAutoStartSignInFlow = false;
            mSignInClicked = false;
            mResolvingConnectionFailure = false;

            Log.i(TAG, "onConnectionFailed: resolving connection failure.");
            if (!BaseGameUtils.resolveConnectionFailure(this, mGoogleApiClient, connectionResult,
                    RC_SIGN_IN, getString(R.string.sign_in_failed))) {
                Log.i(TAG, "onConnectionFailed: could not resolve.");
                mResolvingConnectionFailure = false;
            }
        }
        findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);

    }

    @Override
    public void onActivityResult(int requestCode, int responseCode, Intent intent) {
        super.onActivityResult(requestCode, responseCode, intent);
        Log.i(TAG, "onActivityResult: code = " + requestCode + ", response = " + responseCode);

        // Coming back from resolving a sign-in request
        if (requestCode == RC_SIGN_IN) {
            mSignInClicked = false;
            mResolvingConnectionFailure = false;
            if (responseCode == RESULT_OK) {
                mGoogleApiClient.connect();
            } else {
                BaseGameUtils.showActivityResultError(this, requestCode, responseCode,
                        R.string.sign_in_failed);
            }
        }
    }
    private void requestNewInterstitial() {
        Log.d(TAG,"requestNewInterstitial");
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .build();

        mInterstitialAd.loadAd(adRequest);

    }



}
