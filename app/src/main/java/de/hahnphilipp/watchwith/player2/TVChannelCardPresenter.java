package de.hahnphilipp.watchwith.player2;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.ViewGroup;

import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.Presenter;

import de.hahnphilipp.watchwith.utils.ChannelUtils;

public class TVChannelCardPresenter extends Presenter {

    private Context context;
    private static int CARD_WIDTH = 313;
    private static int CARD_HEIGHT = 176;
    private Drawable defaultCardImage;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        context = parent.getContext();
        //defaultCardImage = context.getResources().getDrawable(R.drawable.movie);
        ImageCardView cardView = new ImageCardView(context) {
            @Override
            public void setSelected(boolean selected) {
                /*int selected_background = context.getResources().getColor(R.color.detail_background);
                int default_background = context.getResources().getColor(R.color.default_background);
                int color = selected ? selected_background : default_background;
                findViewById(R.id.info_field).setBackgroundColor(color);*/
                super.setSelected(selected);
            }
        };
        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);
        return new ViewHolder(cardView);

    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        ChannelUtils.Channel channel = (ChannelUtils.Channel) item;
        ImageCardView cardView = (ImageCardView) viewHolder.view;
        cardView.setTitleText(channel.title);
        cardView.setContentText(channel.number + "");
        cardView.setMainImageDimensions(CARD_WIDTH * 2, CARD_HEIGHT * 2);
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {

    }
}
