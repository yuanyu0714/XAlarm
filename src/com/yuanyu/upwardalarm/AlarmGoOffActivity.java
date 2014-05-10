package com.yuanyu.upwardalarm;

import com.yuanyu.upwardalarm.model.Constants;
import com.yuanyu.upwardalarm.model.RealTimeProvider;
import com.yuanyu.upwardalarm.sensor.MovementAnalysor;
import com.yuanyu.upwardalarm.ui.AlarmItemAnimator;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;

public class AlarmGoOffActivity extends Activity implements MovementAnalysor.MovementListener {

	public final static String EXTRA_IS_TEST_SENSOR = "is_test_sensor"; // Boolean extra
	
	private static final String TAG = "AlarmGoOffActivity";

	private String mLabel;
	private boolean mVibrate;
	private String mRingtoneUri;
	
	private int mStopWay;
	private int mStopLevel;
	private int mStopTimes;
	
	private AlarmGoOffDialog mDialog;
	private int mStopTimesCount;
	
	private boolean mIsTestSensor = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
				| WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
				| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
				| WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		setContentView(R.layout.activity_test);

		Intent intent = getIntent();
		mLabel = intent.getStringExtra(AlarmBroadcastReceiver.EXTRA_ALARM_LABEL);
		mVibrate = intent.getBooleanExtra(AlarmBroadcastReceiver.EXTRA_IS_VIBRATE, false);
		mRingtoneUri = intent.getStringExtra(AlarmBroadcastReceiver.EXTRA_RINGTONE_URI);
		
		mStopWay = intent.getIntExtra(AlarmBroadcastReceiver.EXTRA_STOP_WAY, Constants.STOP_WAY_BUTTON);
		mStopLevel = intent.getIntExtra(AlarmBroadcastReceiver.EXTRA_STOP_LEVEL, Constants.LEVEL_EASY);
		mStopTimes = intent.getIntExtra(AlarmBroadcastReceiver.EXTRA_STOP_TIMES, 1);
		
		if(mStopWay != Constants.STOP_WAY_BUTTON) {
			mStopTimesCount = 0;
			MovementAnalysor.INSTANCE.addMovementListener(this);
		}
		
		mIsTestSensor = intent.getBooleanExtra(EXTRA_IS_TEST_SENSOR, false);

		mDialog = new AlarmGoOffDialog();
		Bundle args = new Bundle();
		args.putString(AlarmBroadcastReceiver.EXTRA_ALARM_LABEL, mLabel);
		args.putBoolean(AlarmBroadcastReceiver.EXTRA_IS_VIBRATE, mVibrate);
		args.putString(AlarmBroadcastReceiver.EXTRA_RINGTONE_URI, mRingtoneUri);
		args.putInt(AlarmBroadcastReceiver.EXTRA_STOP_WAY, mStopWay);
		args.putInt(AlarmBroadcastReceiver.EXTRA_STOP_LEVEL, mStopLevel);
		args.putInt(AlarmBroadcastReceiver.EXTRA_STOP_TIMES, mStopTimes);
		args.putBoolean(EXTRA_IS_TEST_SENSOR, mIsTestSensor);
		mDialog.setArguments(args);
		mDialog.show(getFragmentManager(), "alarmGoOff");
	}

	@Override
	public void onMovementDetected() {
		mStopTimesCount++;
		mDialog.updateTimesText(mStopTimesCount);
		if(mStopTimesCount < mStopTimes) {
			return;
		}
		finish();
		Log.d(TAG, "finish()");
	}

	public static class AlarmGoOffDialog extends DialogFragment {

		private RealTimeProvider mTimeProvider;
		private String mLabel;
		private boolean mIsVibrate;
		private String mRingtoneUri;
		
		private int mStopWay;
		private int mStopLevel;
		private int mStopTimes;
		
		private TextView mStopTimesText;

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			Bundle args = getArguments();
			mLabel = args.getString(AlarmBroadcastReceiver.EXTRA_ALARM_LABEL);
			mIsVibrate = args.getBoolean(AlarmBroadcastReceiver.EXTRA_IS_VIBRATE);
			mRingtoneUri = args.getString(AlarmBroadcastReceiver.EXTRA_RINGTONE_URI);
			mStopWay = args.getInt(AlarmBroadcastReceiver.EXTRA_STOP_WAY);
			mStopLevel = args.getInt(AlarmBroadcastReceiver.EXTRA_STOP_LEVEL);
			mStopTimes = args.getInt(AlarmBroadcastReceiver.EXTRA_STOP_TIMES, 1);
			
			if(!args.getBoolean(EXTRA_IS_TEST_SENSOR)) {
				setCancelable(false);
			}
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			LayoutInflater inflater = LayoutInflater.from(getActivity());
			View view = inflater.inflate(R.layout.dialog_alarm_go_off, null);
			
			TextView labelView = (TextView) view.findViewById(R.id.dialog_alarm_go_off_label);
			if(mLabel!= null && !mLabel.isEmpty()) {
				labelView.setText(mLabel);
				labelView.setVisibility(View.VISIBLE);
			}

			TextView timeText = (TextView) view.findViewById(R.id.dialog_alarm_go_off_time);
			mTimeProvider = new RealTimeProvider();
			mTimeProvider.start(timeText);

			ImageView icon = (ImageView) view.findViewById(R.id.dialog_alarm_go_off_icon);
			AlarmItemAnimator.shakeForever(getActivity(), icon);

			TextView stopWayText = (TextView) view.findViewById(R.id.dialog_alarm_go_off_stop_way);
			stopWayText.setText(getResources().getStringArray(R.array.stop_selections)[mStopWay]);
			
			TextView stopLevelText = (TextView) view.findViewById(R.id.dialog_alarm_go_off_stop_level);
			stopLevelText.setText(getResources().getStringArray(R.array.level)[mStopLevel]);
			
			mStopTimesText = (TextView) view.findViewById(R.id.dialog_alarm_go_off_stop_times);
			updateTimesText(0);
			
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setView(view);
			
			// Start the ringtone and vibrate service
			AlarmGoOffService.startService(getActivity(), mIsVibrate, mRingtoneUri, mStopWay, mStopLevel, mStopTimes);
			
			if(mStopWay == Constants.STOP_WAY_BUTTON) {
				View separator = view.findViewById(R.id.dialog_alarm_go_off_separator);
				separator.setVisibility(View.GONE);
				stopWayText.setVisibility(View.GONE);
				stopLevelText.setVisibility(View.GONE);
				mStopTimesText.setVisibility(View.GONE);
				builder.setPositiveButton(R.string.stop_alarm, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						AlarmGoOffService.stopService(getActivity());
						getActivity().finish();
					}
				});
			}
			
			return builder.create();
		}

		public void updateTimesText(int times) {
			String text = times + "/" + mStopTimes;
			mStopTimesText.setText(text);
		}

		public void release() {
			mTimeProvider.stop();
		}

		@Override
		public void onCancel(DialogInterface dialog) {
			AlarmGoOffService.stopService(getActivity());
			getActivity().finish();
		}
	}
}