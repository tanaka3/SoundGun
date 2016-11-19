package net.masaya3.gunsound;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import net.masaya3.gunsound.data.BulletInfo;

import java.util.List;

/**
 * Created by masaya3 on 15/08/29.
 */
public class BulletListAdapter extends ArrayAdapter<BulletInfo> {

    /*
    private static class ViewHolder{
        public ImageView gohanImage;
    }
    */

    /** */
    private LayoutInflater mInflater;

    public BulletListAdapter(Context context, List<BulletInfo> objects) {
        super(context, R.layout.adapter_bulletlist, objects);
        mInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        convertView = this.mInflater.inflate(R.layout.adapter_bulletlist, null);

        /*
        ViewHolder holder;
        Resources resources = getContext().getResources();

        if(convertView == null){
            convertView = this.mInflater.inflate(this.mResource, null);
            holder = new ViewHolder();

            holder.gohanImage = (ImageView)convertView.findViewById(R.id.foodImage);
            holder.hostImage = (ImageView)convertView.findViewById(R.id.hostImage);
            holder.hostName = (TextView)convertView.findViewById(R.id.hostText);
            holder.mainText = (TextView) convertView.findViewById(R.id.mainText);

            convertView.setTag(holder);
        }
        else{
            holder = (ViewHolder)convertView.getTag();
        }

        Event data = ((Event)getItem(position));
        //holder.gohanImage.setImageResource(R.mipmap.yo_eat);
        //holder.hostImage.setImageResource(R.mipmap.sample_host);
        holder.hostName.setText(data.getName() + "さん");

        holder.mainText.setVisibility(data.getMainMenu().isEmpty() ? View.INVISIBLE: View.VISIBLE);
        holder.mainText.setText(data.getMainMenu());
        */
        return convertView;
    }

}
