package com.yuanyu.upwardalarm.model;

import java.io.File;
import java.io.Serializable;
import java.util.Calendar;

import com.yuanyu.upwardalarm.AlarmBroadcastReceiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

public class Alarm implements Serializable {

	private static final String INTENT_DATA_PREFIX = "com.yuanyu.upwardalarm:";
	private static final long serialVersionUID = 1L;
	
	private int mId;

	private String mLabel;
	
	private boolean mEnable;
	
	private int mHour;
	private int mMinute;
	
	private String mRingtone;
	
	private boolean mVibrate;
	
	private boolean mRepeat;
	private boolean[] mWeekRepeat = new boolean[7]; // Begin with Sunday
	
	// TODO maybe define the month repeat ?
	
	private Alarm() {
		mLabel = "";
		mEnable = false;
		mHour = 8;
		mMinute = 0;
		mRingtone = "";
		mVibrate = true;
		mRepeat = false;
		for(int i = 0; i < mWeekRepeat.length; i++) {
			mWeekRepeat[i] = true;
		}
	}
	
	private Alarm(Alarm alarm) {
		mLabel = alarm.mLabel;
		mEnable = alarm.mEnable;
		mHour = alarm.mHour;
		mMinute = alarm.mMinute;
		mRingtone = alarm.mRingtone;
		mVibrate = alarm.mVibrate;
		mRepeat = alarm.mRepeat;
		for(int i = 0; i < mWeekRepeat.length; i++) {
			mWeekRepeat[i] = alarm.mWeekRepeat[i];
		}
	}
	
	public int getId() {
		return mId;
	}
	
	public String getLabel() {
		return mLabel;
	}
	
	public boolean getEnable() {
		return mEnable;
	}
	
	public int getHour() {
		return mHour;
	}
	
	public int getMinute() {
		return mMinute;
	}
	
	/**
	 * @return null if can't get the valid File object
	 */
	public File getRingtone() {
		// TODO take account if selected SILENCE
		if(mRingtone == null || mRingtone.isEmpty()) {
			return null;
		}
		File file = new File(mRingtone);
		if(!file.isFile() || !file.exists()) {
			return null;
		}
		return file;
	}
	
	public boolean getVibrateEnable() {
		return mVibrate;
	}
	
	public boolean isSundayRepeat() {
		return mWeekRepeat[0];
	}
	
	public boolean isMondayRepeat() {
		return mWeekRepeat[1];
	}
	
	public boolean isTuesdayRepeat() {
		return mWeekRepeat[2];
	}
	
	public boolean isWednesdayRepeat() {
		return mWeekRepeat[3];
	}
	
	public boolean isThursdayRepeat() {
		return mWeekRepeat[4];
	}
	
	public boolean isFridatRepeat() {
		return mWeekRepeat[5];
	}
	
	public boolean isSaturdayRepeat() {
		return mWeekRepeat[6];
	}
	
	public boolean isRepeat() {
		return mRepeat;
	}
	
	/**
	 * @return false if isRepeat() returns false,
	 * then it determines if repeat everyday in a whole week.
	 */
	public boolean isRepeatWholeWeek() {
		if(!mRepeat) return false;
		for(int i = 0; i < mWeekRepeat.length; i++) {
			if(!mWeekRepeat[i]) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Get the next time in millisecond with the hour and minute of the alarm
	 * @return
	 */
	private long getNextTimeMillis() {
		return Utils.getNextTimeMillis(mHour, mMinute);
	}
	
	/**
	 * Write the alarm object to file system
	 */
	public void saveToFile(Context context) {
		Manager.INSTANCE.saveAlarm(context, this);
	}
	
	/**
	 * Register the alarm to android system with the given time
	 */
	private void register(Context context, long timeInMillis) {
		Intent intent = new Intent(context, AlarmBroadcastReceiver.class);
		intent.setData(Uri.parse(INTENT_DATA_PREFIX + mId));
		intent.putExtra(AlarmBroadcastReceiver.EXTRA_ALARM_ID, mId);
		intent.putExtra(AlarmBroadcastReceiver.EXTRA_IS_VIBRATE, mVibrate);
		intent.putExtra(AlarmBroadcastReceiver.EXTRA_RINGTONE, getRingtone().getAbsolutePath());
		PendingIntent alarmPending = PendingIntent.getBroadcast(context, mId, intent, 0);
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Service.ALARM_SERVICE);
		alarmManager.set(AlarmManager.RTC_WAKEUP, timeInMillis, alarmPending);
		// TODO take account if API >= 19
	}
	
	/**
	 * Register the alarm to android system
	 */
	public void register(Context context) {
		register(context, getNextTimeMillis());
	}
	
	/**
	 * Unregister the alarm to android system
	 */
	public void unregister(Context context) {
		Intent intent = new Intent(context, AlarmBroadcastReceiver.class);
		intent.setData(Uri.parse(INTENT_DATA_PREFIX + mId));
		PendingIntent alarmPending = PendingIntent.getBroadcast(context, mId, intent, 0);
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Service.ALARM_SERVICE);
		alarmManager.cancel(alarmPending);
	}
	
	/**
	 * Register the alarm to android system for the next time.
	 */
	public void resetIfRepeat(Context context) {
		if(isRepeat()) {
			if(isRepeatWholeWeek()) {
				register(context);
			}
			else {
				Calendar calendar = Calendar.getInstance();
				int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK); // Sunday is 1
				int daysAfter = 0;
				boolean isFound = false;
				for(int i = dayOfWeek; i < mWeekRepeat.length; i++) {
					if(!mWeekRepeat[i]) {
						daysAfter++;
					}
					else {
						isFound = true;
						break;
					}
				}
				if(!isFound) {
					for(int i = 0; i < dayOfWeek; i++) {
						if(!mWeekRepeat[i]) {
							daysAfter++;
						}
						else {
							isFound = true;
							break;
						}
					}
				}
				if(isFound) {
					register(context, Utils.getNextTimeMillisDaysAfter(mHour, mMinute, daysAfter));
				}
			}
		}
	}
	
	public static class Builder {
		
		Alarm mAlarm;
		
		public Builder(Context context) {
			mAlarm = new Alarm();
			mAlarm.mId = Manager.INSTANCE.getUniqueId(context);
			Log.d("YY", "id = " + mAlarm.mId);
		}
		
		public Builder setLable(String label) {
			mAlarm.mLabel = label;
			return this;
		}
		
		public Builder setEnable(boolean enable) {
			mAlarm.mEnable = enable;
			return this;
		}
		
		public Builder setHour(int hour) {
			mAlarm.mHour = hour;
			return this;
		}
		
		public Builder setMinute(int minute) {
			mAlarm.mMinute = minute;
			return this;
		}
		
		public Builder setRingtone(String ringtone) {
			mAlarm.mRingtone = ringtone;
			return this;
		}
		
		public Builder setVibrateEnable(boolean enable) {
			mAlarm.mVibrate = enable;
			return this;
		}
		
		public Builder enableRepeat(boolean enable) {
			mAlarm.mRepeat = enable;
			return this;
		}
		
		/**
		 * This only works if enableRepeat(true) set
		 * @param sun
		 * @param mon
		 * @param tue
		 * @param wed
		 * @param thu
		 * @param fri
		 * @param sat
		 * @return
		 */
		public Builder setWeekRepeat(boolean sun, boolean mon, boolean tue, boolean wed, boolean thu, boolean fri, boolean sat) {
			mAlarm.mWeekRepeat[0] = sun;
			mAlarm.mWeekRepeat[1] = mon;
			mAlarm.mWeekRepeat[2] = tue;
			mAlarm.mWeekRepeat[3] = wed;
			mAlarm.mWeekRepeat[4] = thu;
			mAlarm.mWeekRepeat[5] = fri;
			mAlarm.mWeekRepeat[6] = sat;
			return this;
		}
		
		/**
		 * This only works if enableRepeat(true) set
		 * @return
		 */
		public Builder setRepeatEveryday() {
			for(int i = 0; i < mAlarm.mWeekRepeat.length; i++) {
				mAlarm.mWeekRepeat[i] = true;
			}
			return this;
		}
		
		public Alarm build() {
			return new Alarm(mAlarm);
		}
	}
}
