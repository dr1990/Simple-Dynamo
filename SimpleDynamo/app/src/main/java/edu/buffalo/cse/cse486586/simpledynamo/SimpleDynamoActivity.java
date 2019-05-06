package edu.buffalo.cse.cse486586.simpledynamo;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;


public class SimpleDynamoActivity extends Activity {

	//static volatile SharedPreferences sharedPref;
	//static public String portNumber;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_simple_dynamo);

		TextView tv = (TextView) findViewById(R.id.textView1);
		tv.setMovementMethod(new ScrollingMovementMethod());

		//sharedPref = getPreferences(Context.MODE_PRIVATE);

		//TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
		//portNumber = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);

		new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "DUMMY");

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.simple_dynamo, menu);
		return true;
	}

	public void onStop() {
		super.onStop();
		Log.v("Test", "onStop()");
	}


	private class ClientTask extends AsyncTask<String, Void, Void> {

		@Override
		protected Void doInBackground(String... msgs) {

			//new SimpleDynamoProvider().recover();

			return null;
		}
	}

}