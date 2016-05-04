package com.example.wekatest;


/* Copyright 2011-2013 Google Inc.
 * Copyright 2013 mike wakerly <opensource@hoho.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * Project home page: https://github.com/mik3y/usb-serial-for-android
 */

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.util.HexDump;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Monitors a single {@link UsbSerialPort} instance, showing all data received.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class SerialConsoleActivity extends Activity {
	private final String TAG = SerialConsoleActivity.class.getSimpleName();
	private final int SerialRate = 9600;

	/**
	 * Driver instance, passed in statically via
	 * {@link #show(Context, UsbSerialPort)}.
	 *
	 * <p/>
	 * This is a devious hack; it'd be cleaner to re-create the driver using
	 * arguments passed in with the {@link #startActivity(Intent)} intent. We
	 * can get away with it because both activities will run in the same
	 * process, and this is a simple demo.
	 */
	private static UsbSerialPort sPort = null;

	private TextView mTitleTextView;
	private TextView mDumpTextView;
	private ScrollView mScrollView;
	private Button mStart, mlearn, mstop,mlbutton;
	private boolean loop = false;
	private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
	private TextView demotitle;
	
	private SerialInputOutputManager mSerialIoManager;

	private final SerialInputOutputManager.Listener mListener = new SerialInputOutputManager.Listener() {

		@Override
		public void onRunError(Exception e) {
			Log.d(TAG, "Runner stopped.");
		}

		@Override
		public void onNewData(final byte[] data) {
			SerialConsoleActivity.this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					SerialConsoleActivity.this.updateReceivedData(data);
				}
			});
		}
	};
	protected int lavelCount;
	protected int cont;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.serial_console);
		mTitleTextView = (TextView) findViewById(R.id.demoTitle);
		mDumpTextView = (TextView) findViewById(R.id.consoleText);
		mScrollView = (ScrollView) findViewById(R.id.demoScroller);
		mStart = (Button) findViewById(R.id.Button);
		mlearn = (Button) findViewById(R.id.learn);
		mstop = (Button) findViewById(R.id.Stop);
		mlbutton = (Button) findViewById(R.id.mlbutton);
		loop = true;
		demotitle = (TextView)findViewById(R.id.demoTitle);

		
		mlbutton.setOnClickListener(new View.OnClickListener() {
			// クリック時に呼ばれるメソッド
			@Override
			public void onClick(View view) {
				// ボタンが押されたらスタートする
				// ボタンがクリックされた時に呼び出されるコールバックリスナーを登録します
				Intent intent = new Intent();
				intent.setClassName("com.example.wekatest", "com.example.wekatest.MachineLearning");
				// SubActivity の起動
				startActivity(intent);
			}
		});

	}

	@Override
	protected void onPause() {
		super.onPause();
		stopIoManager();
		if (sPort != null) {
			try {
				sPort.close();
			} catch (IOException e) {
				// Ignore.
			}
			sPort = null;
		}
		finish();
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "Resumed, port=" + sPort);
		if (sPort == null) {
			mTitleTextView.setText("No serial device.");
		} else {
			final UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

			UsbDeviceConnection connection = usbManager.openDevice(sPort.getDriver().getDevice());
			if (connection == null) {
				mTitleTextView.setText("Opening device failed");
				return;
			}

			try {
				sPort.open(connection);
				sPort.setParameters(SerialRate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
			} catch (IOException e) {
				Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
				mTitleTextView.setText("Error opening device: " + e.getMessage());
				try {
					sPort.close();
				} catch (IOException e2) {
					// Ignore.
				}
				sPort = null;
				return;
			}
			mTitleTextView.setText("Serial device: " + sPort.getClass().getSimpleName());
		}
		onDeviceStateChange();
	}

	private void stopIoManager() {
		if (mSerialIoManager != null) {
			Log.i(TAG, "Stopping io manager ..");
			mSerialIoManager.stop();
			mSerialIoManager = null;
		}
	}

	private void startIoManager() {
		if (sPort != null) {
			Log.i(TAG, "Starting io manager ..");
			mSerialIoManager = new SerialInputOutputManager(sPort, mListener);
			mExecutor.submit(mSerialIoManager);
		}
	}

	private void onDeviceStateChange() {
		stopIoManager();
		startIoManager();
	}

	//受信データ関連
	private int responseCounter;    //レスポンスの現在位置の読み込み用
	private byte[] response;
	private final int buflen = 100; //とりあえず
	public final static String[] lavel = { "A", "B", "C" };

	// デバイスから値を読み込む度に呼び出される
	private void updateReceivedData(byte[] data) {

		String dstFileName = "data.csv";
		String lavelFileName = "lavel.csv";
		int len = lavel.length;
		
		// 文字もラベルで用意したものを超えてしまっていたらいけないので
		if (lavelCount == len) {
			lavelCount = 0;
		}
		
		mStart.setOnClickListener(new View.OnClickListener() {
			// クリック時に呼ばれるメソッド
			@Override
			public void onClick(View view) {
				// ボタンが押されたらスタートする
				// ボタンがクリックされた時に呼び出されるコールバックリスナーを登録します
				loop = false;
			}
		});

		// 学習する機能を次に進める
		mlearn.setOnClickListener(new View.OnClickListener() {
			// クリック時に呼ばれるメソッド
			@Override
			public void onClick(View view) {
				lavelCount++;
				cont++;
				
			}
		});

		mstop.setOnClickListener(new View.OnClickListener() {
			// クリック時に呼ばれるメソッド
			@Override
			public void onClick(View view) {
				// ボタンが押されたらスタートする
				// ボタンがクリックされた時に呼び出されるコールバックリスナーを登録します
				loop = true;
			}
		});

		// ボタンが押されたら処理されるようになるやつ
		if (loop != true) {
			// それぞれのlavelを何回ずつか繰り返したとき終了する
			// スタートボタンが押された時間を代入
			long startTime = System.currentTimeMillis();

			// メッセージとしてセンサの値を格納．FileOutputで書き出す
			final String message = HexDump.dumpHexString(data);
			//mDumpTextViewに表示
			mDumpTextView.append(message);
			
			mScrollView.smoothScrollTo(0, mDumpTextView.getBottom());

			// 書き込み回数制限
			// この場合だと，項目数×回数という感じ
			if (cont < len * 4) {
				FileOutput(dstFileName, message);
				FileOutput(lavelFileName, lavel[lavelCount]+",");
				demotitle.setText(lavel[lavelCount]);
			}

			// ストップボタンが押された時間を代入
//			long stopTime = System.currentTimeMillis();
//			long time = stopTime - startTime;
//			int second = (int) (time / 1000);
			// int comma = (int) (time % 1000);

			// 4秒たったときにとりあえず呼び出される
//			if (second % 4 == 0) {
//				i++;
//				cont++;
//			}

		}
	}

	// 川嶋「コピーするメソッドね．分かるわ」
	// ファイルname，書き込むデータの並びで
	private void FileOutput(String dstFilePath, String data) {
		BufferedWriter out = null;
		try {
			final Context context = this;
			FileOutputStream file = openFileOutput(dstFilePath, Context.MODE_APPEND);
			out = new BufferedWriter(new OutputStreamWriter(file));
			out.write(data);
			out.flush();
			//out.close();
			Log.v("Write", "File write completed.");
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
	/**
	 * Starts the activity, using the supplied driver instance.
	 *
	 * @param context
	 * @param driver
	 */
	static void show(Context context, UsbSerialPort port) {
		sPort = port;
		final Intent intent = new Intent(context, SerialConsoleActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY);
		context.startActivity(intent);
	}

}
