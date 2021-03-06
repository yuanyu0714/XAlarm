package com.yuanyu.xalarm.test;

import java.util.List;

import com.yuanyu.xalarm.R;
import com.yuanyu.xalarm.AlarmBroadcastReceiver;
import com.yuanyu.xalarm.model.Constants;
import com.yuanyu.xalarm.sensor.MovementTracker;
import com.yuanyu.xalarm.sensor.MovementTracker.Sample;

import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

public class TestActivity extends Activity {
	
	private final static float FILTER_ALPHA = 0.1f;
	
	private TextView mText;
	
	private float lowX,lowY,lowZ;
	
	public static void startGoOffActivity(Context context) {
		Intent i = new Intent(context, AlarmBroadcastReceiver.class);
		i.putExtra(AlarmBroadcastReceiver.EXTRA_ALARM_LABEL, "TESTING ALARM !!!");
		i.putExtra(AlarmBroadcastReceiver.EXTRA_IS_VIBRATE, true);
		Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
		i.putExtra(AlarmBroadcastReceiver.EXTRA_RINGTONE_URI, uri != null ? uri.toString() : "");
		context.sendBroadcast(i);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
		setContentView(R.layout.activity_test);
		mText = (TextView) findViewById(R.id.activity_alarm_go_off_text);
		mText.setMovementMethod(new ScrollingMovementMethod());

		AlarmGoOffDialog dialog = new AlarmGoOffDialog();
		dialog.show(getFragmentManager(), "alarmGoOff");
	}

	public static class AlarmGoOffDialog extends DialogFragment {

		private MovementTracker mTracker;

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setCancelable(false);

			mTracker = new MovementTracker(getActivity());
			mTracker.start(Constants.STOP_WAY_TEST, Constants.LEVEL_HARD);
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			LayoutInflater inflater = LayoutInflater.from(getActivity());
			View view = inflater.inflate(R.layout.dialog_alarm_go_off, null);
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setView(view);

			// For test
			final TestActivity activity = (TestActivity) getActivity();
			builder.setPositiveButton("Stop", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					mTracker.stop();
					activity.showData(mTracker.getData());
				}
			});

			return builder.create();
		}

		@Override
		public void onDestroyView() {
			mTracker.stop();
			super.onDestroyView();
		}

		@Override
		public void onDestroy() {
			mTracker.stop();
			super.onDestroy();
		}
	}

	private void showData(List<Sample> data) {
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		
		StringBuilder builder = new StringBuilder();
		for(Sample s : data) {
			double independent = s.independentValue();
			builder.append(independent + "\n");
			//builder.append(s.sumValue() + "\n");
			
			////////////// FILTERED DATA ///////////////
			/*float x = s.x;
			float y = s.y;
			float z = s.z;
			//Low-Pass Filter
			lowX = x * FILTER_ALPHA + lowX * (1.0f - FILTER_ALPHA);
			lowY = y * FILTER_ALPHA + lowY * (1.0f - FILTER_ALPHA);
			lowZ = z * FILTER_ALPHA + lowZ * (1.0f - FILTER_ALPHA);
			//High-pass filter
			float highX = x - lowX;
			float highY = y - lowY;
			float highZ = z - lowZ;*/
			
			//double independent = Math.sqrt(highX*highX + highY*highY + highZ*highZ);
			/*float maxHigh = Math.max(highX, Math.max(highY, highZ));
			if(maxHigh > max) {
				max = maxHigh;
			}
			if(maxHigh < min) {
				min = maxHigh;
			}
			builder.append(maxHigh + "\n");*/
		}
		builder.append("Max = " + max + "\nMin = " + min + "\nDelta = " + (max - min) + "\n");
		mText.setText(builder.toString());
	}
}
