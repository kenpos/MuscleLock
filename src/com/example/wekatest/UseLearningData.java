package com.example.wekatest;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Random;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

public class UseLearningData extends Activity {

	private Instance inst_co;
	private TextView viewstate;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.uselearningdata);

		// 初期化
		viewstate = (TextView) findViewById(R.id.getState);

		classify(180,204,336,162,99,-269,-21);

	}

	/**
	 * 
	 * @param s0
	 * @param s1
	 * @param s2
	 * @param s3
	 * @param s4
	 * @param s5
	 * @param s6
	 * @return 
	 *
	 *作成したモデルから分類予測を返します．
	 *とりあえずパラメータは直打ち．
	 *
	 */
	public double classify(int s1, int s2, int s3, int s4, int s5, int s6, int s7) {

		// Create attributes to be used with classifiers
		// Test the model
		double result = -1;
		try {

			ArrayList<Attribute> attributeList = new ArrayList<Attribute>(2);

			// 事前に作成した分類データの項目設定
			Attribute S1 = new Attribute("S1");
			Attribute S2 = new Attribute("S2");
			Attribute S3 = new Attribute("S3");
			Attribute S4 = new Attribute("S4");
			Attribute S5 = new Attribute("S5");
			Attribute S6 = new Attribute("S6");
			Attribute S7 = new Attribute("S7");

			// 分類予測したい値．A,B,Cっていう項目のうちどれかであればこんな感じに書くと良い
			ArrayList<String> classVal = new ArrayList<String>();
			classVal.add("A");
			classVal.add("B");
			classVal.add("C");

			attributeList.add(S1);
			attributeList.add(S2);
			attributeList.add(S3);
			attributeList.add(S4);
			attributeList.add(S5);
			attributeList.add(S6);
			attributeList.add(S7);
			attributeList.add(new Attribute("@@select@@", classVal));

			Instances data = new Instances("TestInstances", attributeList, 0);
			data.setClassIndex(data.numAttributes() - 1); 

			// Create instances for each pollutant with attribute values
			// latitude,
			// longitude and pollutant itself
			inst_co = new DenseInstance(data.numAttributes());
			inst_co.setDataset(data); 

//			data.add(inst_co);
			
			
			// Set instance's values for the attributes "latitude", "longitude",
			// and
			// "pollutant concentration"
			// 確認したいデータの作成
			inst_co.setValue(S1, s1);
			inst_co.setValue(S2, s2);
			inst_co.setValue(S3, s3);
			inst_co.setValue(S4, s4);
			inst_co.setValue(S5, s5);
			inst_co.setValue(S6, s6);
			inst_co.setValue(S7, s7);

			//To assosiate your instance with Instance object in this case dataRaw
			
			//inst_co.setMissing(cluster);

			// load classifier from file
			// モデル読み込み
			FileInputStream file = openFileInput("randomforest.model");
			Classifier cls = (Classifier) weka.core.SerializationHelper.read(file);

			result = cls.classifyInstance(inst_co);
			String finalresult = new Double(result).toString();

			Log.v("wekaresult",finalresult);
			viewstate.setText(finalresult);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
}
