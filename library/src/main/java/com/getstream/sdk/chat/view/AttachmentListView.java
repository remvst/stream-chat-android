package com.getstream.sdk.chat.view;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.getstream.sdk.chat.adapter.AttachmentListItemAdapter;
import com.getstream.sdk.chat.adapter.AttachmentViewHolderFactory;
import com.getstream.sdk.chat.adapter.MessageListItem;

public class AttachmentListView extends RecyclerView {
    final String TAG = AttachmentListView.class.getSimpleName();

    private AttachmentViewHolderFactory viewHolderFactory;
    private RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
    private MessageListViewStyle style;
    private Context context;
    private MessageListView.BubbleHelper bubbleHelper;
    private AttachmentListItemAdapter adapter;

    private MessageListView.AttachmentClickListener attachmentClickListener;
    private MessageListView.MessageLongClickListener longClickListener;
    private MessageListView.GiphySendListener giphySendListener;

    public AttachmentListView(Context context) {
        super(context);
        this.context = context;
        setHasFixedSize(true);
    }

    public AttachmentListView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        setHasFixedSize(true);
    }

    public AttachmentListView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.setLayoutManager(mLayoutManager);
        this.context = context;
        setHasFixedSize(true);
    }

    public void setStyle(MessageListViewStyle style) {
        this.style = style;
    }

    public void setViewHolderFactory(AttachmentViewHolderFactory viewHolderFactory) {
        this.viewHolderFactory = viewHolderFactory;
    }

    public void setEntity(MessageListItem.MessageItem messageListItem) {
        this.setLayoutManager(mLayoutManager);
        this.adapter = new AttachmentListItemAdapter(messageListItem, viewHolderFactory);
        this.adapter.setStyle(style);

        if (this.giphySendListener != null)
            this.adapter.setGiphySendListener(giphySendListener);

        if (this.attachmentClickListener != null)
            this.adapter.setAttachmentClickListener(attachmentClickListener);

        if (this.bubbleHelper != null)
            this.adapter.setBubbleHelper(bubbleHelper);

        this.setAdapter(adapter);
    }

    public void setAttachmentClickListener(MessageListView.AttachmentClickListener attachmentClickListener) {
        this.attachmentClickListener = attachmentClickListener;
        if (this.adapter != null) {
            this.adapter.setAttachmentClickListener(attachmentClickListener);
        }
    }

    public void setLongClickListener(MessageListView.MessageLongClickListener longClickListener) {
        this.longClickListener = longClickListener;
        if (adapter != null)
            adapter.setLongClickListener(this.longClickListener);
    }

    public void setGiphySendListener(MessageListView.GiphySendListener giphySendListener) {
        this.giphySendListener = giphySendListener;
    }

    public void setBubbleHelper(MessageListView.BubbleHelper bubbleHelper) {
        this.bubbleHelper = bubbleHelper;
        if (this.adapter != null) {
            this.adapter.setBubbleHelper(bubbleHelper);
        }
    }
}
