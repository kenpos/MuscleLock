package com.example.wekatest;

/**
 * 
 * @author kenpos このクラスは，カウントダウンタイマーを利用して時間計測を行います． 使用するときは，インスタンス作ってうまくやってください
 *         とりあえず今回はDeveiceの学習データ作るときに時間毎に云々かんぬんしております．
 */
public class CountDownTimer extends android.os.CountDownTimer {
	public CountDownTimer(long millisInFuture, long countDownInterval) {
		super(millisInFuture, countDownInterval);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onTick(long millisUntilFinished) {
		// インターバル(countDownInterval)毎に呼ばれる
		// TODO Auto-generated method stub
	}

	@Override
	public void onFinish() {
		// カウントダウン終了時に呼び出される
		// TODO Auto-generated method stub
	}

}
