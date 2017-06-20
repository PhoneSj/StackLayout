package com.phone.custom;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.phone.widget.SpecialLayout;
import com.phone.widget.StackLayout;
import com.phone.widget.StackLayout.OnItemClickListener;
import com.phone.widget.StackLayout.OnItemLongClickListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

	private List<String> names;
	private MyAdapter adapter;
	private int imgs[] = { R.mipmap.item_00, R.mipmap.item_01, R.mipmap.item_02, R.mipmap.item_03, R.mipmap.item_03,
			R.mipmap.item_04, R.mipmap.item_05, R.mipmap.item_06, R.mipmap.item_07, R.mipmap.item_08, R.mipmap.item_09,
			R.mipmap.item_10, R.mipmap.item_11, R.mipmap.item_00, R.mipmap.item_01, R.mipmap.item_02, R.mipmap.item_03,
			R.mipmap.item_03, R.mipmap.item_04, R.mipmap.item_05, R.mipmap.item_06, R.mipmap.item_07, R.mipmap.item_08,
			R.mipmap.item_09, R.mipmap.item_10, R.mipmap.item_11 };

	private SpecialLayout specialLayout;
	private StackLayout stackLayout;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		specialLayout = (SpecialLayout) findViewById(R.id.specialLayout);
		stackLayout = (StackLayout) findViewById(R.id.stackLayout);
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
		specialLayout.setOnOffsetXFractionListener(new SpecialLayout.OnOffsetXFractionListener() {
			@Override
			public void onOffsetFraction(float fraction) {
				Log.w("phoneTest", "fraction");
				int count = stackLayout.getChildCount();
				for (int i = 0; i < count; i++) {
					final ViewGroup itemView = (ViewGroup) stackLayout.getChildAt(i);
					itemView.getChildAt(1).setAlpha(1 - fraction);
				}
			}
		});
		changeColor();
	}

	private void changeColor() {
		Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.bg);
		Palette.generateAsync(bitmap, new Palette.PaletteAsyncListener() {
			@Override
			public void onGenerated(Palette palette) {
				Palette.Swatch vibrant = palette.getMutedSwatch();
				//				stackLayout.setBackgroundColor(vibrant.getRgb());
				stackLayout.setBgColor(vibrant.getRgb());

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
