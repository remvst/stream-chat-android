package io.getstream.chat.android.ui.messages.adapter.viewholder

import android.view.LayoutInflater
import android.view.ViewGroup
import com.getstream.sdk.chat.adapter.MessageListItem
import io.getstream.chat.android.client.models.Attachment
import io.getstream.chat.android.ui.databinding.StreamUiItemMessageAttachmentsOnlyBinding
import io.getstream.chat.android.ui.messages.adapter.BaseMessageItemViewHolder

public class OnlyMediaAttachmentsViewHolder(
    parent: ViewGroup,
    internal val binding: StreamUiItemMessageAttachmentsOnlyBinding = StreamUiItemMessageAttachmentsOnlyBinding.inflate(
        LayoutInflater.from(
            parent.context
        ),
        parent,
        false
    )
) : BaseMessageItemViewHolder<MessageListItem.MessageItem>(binding.root) {

    override fun bindData(data: MessageListItem.MessageItem) {
        if (data.message.attachments.all { it.type == "image" }) {
            showAttachments(data.message.attachments, data.isMine)
        }
    }

    private fun showAttachments(imageAttachments: Collection<Attachment>, isMine: Boolean) {
        constraintView(isMine, binding.mediaAttachmentsGroupView, binding.root)
        binding.mediaAttachmentsGroupView.showAttachments(isMine, *imageAttachments.toTypedArray())
    }
}
