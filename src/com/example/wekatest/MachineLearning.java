package com.example.wekatest;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;
import weka.core.converters.ConverterUtils.DataSource;
import weka.core.converters.Loader;

public class MachineLearning extends Activity {
	static Context context;
	static TextView textView1;
	TextView textView2;
	TextView textView3;
	TextView textView4;
	TextView textView5;
	TextView textView6;
	TextView textView7;
	TextView textView8;
	TextView textView9;
	TextView textView10;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.machinelearning);
		Log.v("Start", "machinelearning started.");

		//初期化
		textView1 = (TextView) findViewById(R.id.textView1);
		textView2 = (TextView) findViewById(R.id.textView2);
		textView3 = (TextView) findViewById(R.id.textView3);
		textView4 = (TextView) findViewById(R.id.textView4);
		textView5 = (TextView) findViewById(R.id.textView5);
		textView6 = (TextView) findViewById(R.id.textView6);
		textView7 = (TextView) findViewById(R.id.textView7);
		textView8 = (TextView) findViewById(R.id.textView8);
		textView9 = (TextView) findViewById(R.id.textView9);
		textView10 = (TextView) findViewById(R.id.textView10);
		context = this;

		try {
			// センサの値は集めてあるので，それを整形します．
			c2c("data.csv", "lavel.csv", "f_data.csv");
			c2a("f_data.csv", "data.arff");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		weka_randomforest("data.arff");

	}

	/**
	 * CSV to ARFF CSVファイルをARFF形式に変換する
	 * まずはARFFヘッダから作る．
	 * 
	 * @throws IOException
	 * 
	 *             フォーマット
	 * @RELATION タイトル
	 * 
	 * @ATTRIBUTE 項目名 string(文字列)
	 * @ATTRIBUTE 項目名 date(日時 フォーマット:フォーマットは省略可能。
	 *            省略した場合は、"yyyy-MM-dd'T'HH:mm:ss"）
	 * @ATTRIBUTE 数量 numeric(数字)
	 * @ATTRIBUTE 商品 {A,B}(カテゴリ型項目)
	 * 
	 * @DATA(実データ) No.1,20081201,1,10,A No.2,20081202,2,20,A
	 *             No.3,20081203,3,30,A No.4,20081201,4,40,B
	 *             No.5,20081203,5,50,B
	 * 
	 */
	void c2a(String inputdata, String outputdata) throws IOException {
		Log.v("Start", "c2a started.");

		Context ctx = this;
		
		// output
		BufferedWriter out = null;
		FileOutputStream fileoutputstream;
		//一度中身を削除するための処理
		fileoutputstream = context.openFileOutput(outputdata, Context.MODE_PRIVATE);
		fileoutputstream = context.openFileOutput(outputdata, Context.MODE_APPEND);
		out = new BufferedWriter(new OutputStreamWriter(fileoutputstream));
		
		String str = null ;
	        out.write("@relation " + outputdata + "\n\n");
	        out.write("@attribute S1 real\n"  //S1~S4が近距離センサ
	        		+ "@attribute S2 real\n"
	        		+ "@attribute S3 real\n"
	        		+ "@attribute S4 real\n"
	        		+ "@attribute S5 real\n" //S5~S7が加速度センサ
	        		+ "@attribute S6 real\n"
	        		+ "@attribute S7 real\n"
	        		+ "@attribute select {A,B,C} \n"
	        		+ "@data\n");

			// data部分を読み込む
			FileInputStream lavelInputStream = ctx.openFileInput(inputdata);
			BufferedReader br = new BufferedReader(new InputStreamReader(lavelInputStream, "UTF-8"));
			StringBuilder sbtemp = new StringBuilder();
			String line;

			while ((line = br.readLine()) != null) {
				sbtemp.append(line + "\n");
			}
			br.close();
	        out.write(sbtemp.toString());
			out.flush();
			
	}

	/**
	 * C2C data.csvを開き，Arduinoから送られてくる改行文字"e"を受け取ったら改行し
	 * 各行でセンサの値が入っていない場合その行を削除する センサの値が入ったCSVファイルの読み込みを実行する
	 * このセンサの値が入ったファイルは機械学習にかけるには欠損データや縦と横の大きさがあっていないため
	 * 読み込んだCSVファイルは機械学習に投げれるように整形します
	 */
	// lavel + センサの数 +1してある
	private static int sensor_size = 8;

	void c2c(String inPath, String lavelPath, String outPath) {
		Log.v("Start", "C2C started.");
		// Read
		{
			try {
				// data部分を読み込む
				Context ctx = this;
				FileInputStream fileInputStream;
				fileInputStream = ctx.openFileInput(inPath);
				byte[] readBytes = new byte[fileInputStream.available()];
				fileInputStream.read(readBytes);
				String readString = new String(readBytes);

				// lavel部分を読み込む
				FileInputStream lavelInputStream = ctx.openFileInput(lavelPath);
				BufferedReader br = new BufferedReader(new InputStreamReader(lavelInputStream, "UTF-8"));
				StringBuilder sbtemp = new StringBuilder();
				String line;

				while ((line = br.readLine()) != null) {
					sbtemp.append(line);
				}
				sbtemp.toString();
				br.close();

				// 改行文字で切る
				// とりあえずで"e"を改行文字に使っている
				String[] datatemp = readString.split(",");
				String[] laveltemp = sbtemp.toString().split(",");

				// output
				BufferedWriter out = null;
				FileOutputStream fileoutputstream;
				//一度中身を削除するための処理
				fileoutputstream = context.openFileOutput(outPath, Context.MODE_PRIVATE);
				fileoutputstream = context.openFileOutput(outPath, Context.MODE_APPEND);
				out = new BufferedWriter(new OutputStreamWriter(fileoutputstream));

				String str = null;
				// lavelとdataを結合するために使用
				StringBuilder sb = new StringBuilder();
				// dataとlavelの数だけ回す
				// もしここでずれてたりデータ抜けがあったら落ちるけどまぁその時は考えましょう．
				for (int i = 0; i < datatemp.length; i++) {
					str = datatemp[i] + laveltemp[i];
					str = str.replace("  ", " "); //"  "のような場所があれば" "に変換する．
					str = str.trim(); //先頭と末尾の空白を削除する
					str = str.replace(" ", ","); //"  "のような場所があれば" "に変換する．
					// スペースで区切ってみる
					// データの数が正しいかどうかを確かめる
					if (str.split(",").length == sensor_size) {
						out.write(str + "\n");
						out.flush();
					}
				}
			} catch (FileNotFoundException e) {
			} catch (IOException e) {
			}
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
			// out.close();
			Log.v("Write", "File write completed.");
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	
	
	/**
	 * wekaを使ってランダムフォレストを行っている ファイルの指定にはセンサの値から作ったarff形式のファイルを指定してあげると良い
	 */
	void weka_randomforest(String datapath) {
		Log.v("Start", "weka_randomforest started.");
		
		BufferedReader br = null;
		int numFolds = 10;
		try {
			final Context context = this;
			FileInputStream dataInputStream = context.openFileInput(datapath);
			br = new BufferedReader(new InputStreamReader(dataInputStream, "UTF-8"));

			Instances trainData = new Instances(br);
	        trainData.setClassIndex(trainData.numAttributes() - 1);
	        
	        
	        br.close();
	        RandomForest rf = new RandomForest();
	        rf.setNumTrees(100);
	         
	        rf.buildClassifier(trainData);
	        Evaluation evaluation = new Evaluation(trainData);
	        evaluation.crossValidateModel(rf, trainData, numFolds, new Random(1));
	          	         
	        textView1.setText(evaluation.toSummaryString("\nResults\n======\n", true));
	        textView2.setText(evaluation.toClassDetailsString());
	        textView3.setText("Results For Class -1- ");
	        textView4.setText("Precision=  " + evaluation.precision(0));
	        textView5.setText("Recall=  " + evaluation.recall(0));
	        textView6.setText("F-measure=  " + evaluation.fMeasure(0));
	        textView7.setText("Results For Class -2- ");
	        textView8.setText("Precision=  " + evaluation.precision(1));
	        textView9.setText("Recall=  " + evaluation.recall(1));
	        textView10.setText("F-measure=  " + evaluation.fMeasure(1));
	        
	        //作成したモデルを書き出して保存．
	        //最終的にはこの作成したモデルを読み込み利用するのがメイン．
	        // serialize model
			
	        FileOutputStream file = openFileOutput("randomforest.model", Context.MODE_PRIVATE);
	        file = openFileOutput("randomforest.model", Context.MODE_APPEND);

	        //ObjectOutputStream oos = new ObjectOutputStream(file);
	        
//	        oos.writeObject(rf);
	        
	        weka.core.SerializationHelper.write(file, rf);
       
//			weka.core.SerializationHelper.write("/some/where/nBayes.model", cModel);

//	        oos.flush();
//	        oos.close();
	 
		} catch (Exception e) {
			// TODO: handle exception
		}

	}

}
