package com.bangnizhao.www.circleprogressbar;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Random;

public class MainActivity extends Activity implements View.OnClickListener {

    private CircleProgressbar mProgressbar;
    private EditText mEditText;

    /**
     * CircleProgressbar ;
     * 1、对外主要暴漏两个方法
     * 不用管它  直接在xml里边配置  通过findViewById() 拿到对象后
     * mProgressbar.setDrawCircleListener(new DrawCircleListener() {  //如果需要在圆转慢一圈 时 在其他处理  比如更新显示文本  在这里设置回调
     *
     * @Override public void onCircleComplete(float angle) {
     * mProgressbar.setCenterText(String.format("第 %d 次KEY", count++)); //这里是每当圆转满一圈时 所要做的事   千万不要在这里更新其他地方的UI 切记 切记 切记
     * }
     * });
     * 2、
     * mProgressbar.setCurrentValue(current); //应你们要求  current 有效 范围为 0 ~ 3600  小于0时会被置为0   大于3600时 对3600取余  需要改为其他  直接修改 MAX_VALUE
     * 3、内环半径 138
     * 外环   143
     * 默认居中  位置轻微调整 在xml中设置控件太小 以及其相对父布局的位置 即可{
     * layout_width
     * layout_height
     * }
     */


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mEditText = (EditText) findViewById(R.id.edit);
        mProgressbar = (CircleProgressbar) findViewById(R.id.progress);
        findViewById(R.id.btn).setOnClickListener(this);
        mProgressbar.setDrawCircleListener(new DrawCircleListener() {
            @Override
            public void onCircleComplete(float angle) {
                mProgressbar.setCenterText(String.valueOf(new Random().nextInt(10000000) + 1000000));
            }
        });
    }


    @Override
    public void onClick(View v) {
        String str = mEditText.getText().toString().trim();
        if (TextUtils.isEmpty(str)) {
            Toast.makeText(MainActivity.this, "在输入框内输入进度值 0 - 30000", Toast.LENGTH_SHORT).show();
            return;
        }
        mProgressbar.setCurrentValue(Integer.valueOf(str));
    }

}
