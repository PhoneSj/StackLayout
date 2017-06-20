package com.phone.custom;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.phone.widget.StackLayout;
import com.phone.widget.StackLayout.OnItemClickListener;
import com.phone.widget.StackLayout.OnItemLongClickListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

	private List<String> names;
	private MyAdapter adapter;
	private int imgs[] = { R.mipmap.item_00, R.mipmap.item_01, R.mipmap.item_02, R.mipmap.item_03, R.mipmap.item_03,
			R.mipmap.item_04, R.mipmap.item_05, R.mipmap.item_06, R.mipmap.item_07, R.mipmap.item_08, R.mipmap.item_09,
			R.mipmap.item_10, R.mipmap.item_11, R.mipmap.item_00, R.mipmap.item_01, R.mipmap.item_02, R.mipmap.item_03,
			R.mipmap.item_03, R.mipmap.item_04, R.mipmap.item_05, R.mipmap.item_06, R.mipmap.item_07, R.mipmap.item_08,
			R.mipmap.item_09, R.mipmap.item_10, R.mipmap.item_11 };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		StackLayout stackLayout = (StackLayout) findViewById(R.id.phoneLayout);
		adapter = new MyAdapter();
		stackLayout.setAdapter(adapter);
		stackLayout.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemclick(int position) {
				Toast.makeText(MainActivity.this, "onItemClick:" + position, Toast.LENGTH_SHORT).show();
			}
		});
		stackLayout.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public void onItemLongClick(int position) {
				Toast.makeText(MainActivity.this, "onItemLongClick:" + position, Toast.LENGTH_SHORT).show();
			}
		});
	}

	class MyAdapter extends BaseAdapter {

		public MyAdapter() {
			names = generateNames(imgs.length);
		}

		@Override
		public int getCount() {
			return names.size();
		}

		@Override
		public Object getItem(int i) {
			return names.get(i);
		}

		@Override
		public long getItemId(int i) {
			return i;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup viewGroup) {
			if (convertView == null) {
				convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.item, null);
			}
			TextView textView = (TextView) convertView.findViewById(R.id.textView);
			textView.setText(names.get(position));
			ImageView imageView = (ImageView) convertView.findViewById(R.id.imageView);
			imageView.setImageResource(imgs[position]);
			return convertView;
		}
	}

	private List<String> generateNames(int count) {
		List<String> datas = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			datas.add("应用" + i);
		}
		return datas;
	}
}
