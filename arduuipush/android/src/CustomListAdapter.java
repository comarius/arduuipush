package com.ermote.ArduUiPush;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.ermote.ArduUiPush.*;

import java.util.ArrayList;

/**
 * Created by marius on 16/01/16.
 */


public class CustomListAdapter extends BaseAdapter {
    private ArrayList<BtDevItem> listData;
    private LayoutInflater layoutInflater;

    public CustomListAdapter(Context aContext, ArrayList<BtDevItem> listData) {
        this.listData = listData;
        layoutInflater = LayoutInflater.from(aContext);
    }

    @Override
    public int getCount() {
        return listData.size();
    }

    @Override
    public Object getItem(int position) {
        return listData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.list_row_layout, null);
            holder = new ViewHolder();
            holder.nameView = (TextView) convertView.findViewById(R.id.nameView);
            holder.macView = (TextView) convertView.findViewById(R.id.macView);
            holder.leView = (TextView) convertView.findViewById(R.id.leView);
            holder.stateView = (TextView) convertView.findViewById(R.id.stateView);
            holder.waiting = (ProgressBar) convertView.findViewById(R.id.waiting);
            // holder.button   = (Button) convertView.findViewById(R.id.delbutt);
            //holder.imageview = (ImageView) convertView.findViewById(R.id.imageView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.nameView.setText(listData.get(position).name);
        holder.macView.setText(listData.get(position).mac);
        holder.leView.setText(listData.get(position).ledev);
        holder.stateView.setText(listData.get(position).state);
        holder.waiting.setVisibility(listData.get(position).waiting==true ? View.VISIBLE : View.INVISIBLE);
        // holder.button.setVisibility(listData.get(position).delbutton==true ? View.VISIBLE : View.INVISIBLE);

        if(listData.get(position).waiting==false) {
//            int istate = listData.get(position).istate;
            //          Bitmap bm = BitmapFactory.decodeResource(null, istate==Konst.STATE_ONLINE ?
            //                                                        R.drawable.connected:
            //                                                      R.drawable.disconnected);
            //holder.imageview.setVisibility( View.VISIBLE);
        }else{
            //holder.imageview.setVisibility( View.INVISIBLE);
        }
        return convertView;
    }

    static class ViewHolder {
        TextView nameView;
        TextView macView;
        TextView leView;
        TextView stateView;
        ProgressBar waiting;
        Button button;
        //ImageView   imageview;
    }
}
