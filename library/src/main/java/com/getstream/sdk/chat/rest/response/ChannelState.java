package com.getstream.sdk.chat.rest.response;

import android.text.TextUtils;
import android.util.Log;

import com.getstream.sdk.chat.model.Channel;
import com.getstream.sdk.chat.model.Member;
import com.getstream.sdk.chat.model.ModelType;
import com.getstream.sdk.chat.model.Watcher;
import com.getstream.sdk.chat.rest.Message;
import com.getstream.sdk.chat.rest.User;
import com.getstream.sdk.chat.rest.core.Client;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class ChannelState {

    private static final String TAG = ChannelState.class.getSimpleName();

    @SerializedName("channel")
    private Channel channel;

    @SerializedName("messages")
    private List<Message> messages;

    @SerializedName("read")
    private List<ChannelUserRead> reads;

    @SerializedName("members")
    private List<Member> members;

    public List<Watcher> getWatchers() {
        if (watchers == null) {
            return new ArrayList<>();
        }
        return watchers;
    }

    @SerializedName("watchers")
    private List<Watcher> watchers;

    public int getWatcherCount() {
        return watcherCount;
    }

    @SerializedName("watcher_count")
    private int watcherCount;

    public ChannelState(Channel channel) {
        this.channel = channel;
        messages = new ArrayList<>();
        reads = new ArrayList<>();
        members = new ArrayList<>();
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public Date getLastKnownActiveWatcher() {
        if (lastKnownActiveWatcher == null) {
            lastKnownActiveWatcher = new Date(0);
        }
        return lastKnownActiveWatcher;
    }

    private Date lastKnownActiveWatcher;

    // endregion
    public static void sortUserReads(List<ChannelUserRead> reads) {
        Collections.sort(reads, (ChannelUserRead o1, ChannelUserRead o2) -> o1.getLastRead().compareTo(o2.getLastRead()));
    }

    public synchronized void addWatcher(Watcher watcher){
        if (watchers == null) {
            watchers = new ArrayList<>();
        }
        watchers.remove(watcher);
        watchers.add(watcher);
    }

    public void removeWatcher(Watcher watcher){
        if (watchers == null) {
            watchers = new ArrayList<>();
        }
        if (watcher.getUser().getLastActive().after(getLastKnownActiveWatcher())) {
            lastKnownActiveWatcher = watcher.getUser().getLastActive();
        }
        watchers.remove(watcher);
    }

    public List<User> getOtherUsers() {
        List<User> users = new ArrayList<>();

        if (members != null) {
            for (Member m : members) {
                if (!channel.getClient().fromCurrentUser(m)) {
                    users.add(channel.getClient().getTrackedUser(m.getUser()));
                }
            }
        }

        if (watchers != null) {
            for (Watcher w : watchers) {
                if (!channel.getClient().fromCurrentUser(w)) {
                    users.add(channel.getClient().getTrackedUser(w.getUser()));
                }
            }
        }

        return users;
    }

    public String getOldestMessageId() {
        Message message = getOldestMessage();
        if (message == null) {
            return null;
        }
        return message.getId();
    }

    // last time the channel had a message from another user or (when more recent) the time a watcher was last active
    public Date getLastActive() {
        Date lastActive = channel.getCreatedAt();
        if (lastActive == null) lastActive = new Date();
        if (getLastKnownActiveWatcher().after(lastActive)) {
            lastActive = getLastKnownActiveWatcher();
        }
        Message message = getLastMessageFromOtherUser();
        if (message != null) {
            if (message.getCreatedAt().after(lastActive)) {
                lastActive = message.getCreatedAt();
            }
        }
        for (Watcher watcher: getWatchers()) {
            if (lastActive.before(watcher.getUser().getLastActive())) {
                if (channel.getClient().fromCurrentUser(watcher)) continue;
                lastActive = watcher.getUser().getLastActive();
            }
        }
        return lastActive;
    }

    public Boolean anyOtherUsersOnline() {
        Boolean online = false;
        List<User> users = this.getOtherUsers();
        for (User u: users) {
            if (u.getOnline()) {
                online = true;
                break;
            }
        }
        return online;
    }

    public String getChannelNameOrMembers() {
        String channelName;

        Log.i(TAG, "Channel name is" + channel.getName() + channel.getCid());
        if (!TextUtils.isEmpty(channel.getName())) {
            channelName = channel.getName();
        } else {
            List<User> users = this.getOtherUsers();
            List<User> top3 = users.subList(0, Math.min(3, users.size()));
            List<String> usernames = new ArrayList<>();
            for (User u : top3) {
                usernames.add(u.getName());
            }

            channelName = TextUtils.join(", ", usernames);
            if (users.size() > 3) {
                channelName += "...";
            }
        }
        return channelName;
    }

    public Channel getChannel() {
        return channel;
    }

    public Message getOldestMessage() {
        if (messages == null || messages.size() == 0) {
            return null;
        }
        return messages.get(0);
    }

    public List<Message> getMessages() {
        if (messages == null) {
            return new ArrayList<>();
        }
        return messages;
    }

    public List<ChannelUserRead> getReads() {
        return reads;
    }

    public List<ChannelUserRead> getLastMessageReads() {
        Message lastMessage = this.getLastMessage();
        List<ChannelUserRead> readLastMessage = new ArrayList<>();
        if (reads == null || lastMessage == null) return readLastMessage;
        for (ChannelUserRead r : reads) {
            if (r.getLastRead().compareTo(lastMessage.getCreatedAt()) > -1) {
                readLastMessage.add(r);
            }
        }

        // sort the reads
        Collections.sort(readLastMessage, (ChannelUserRead o1, ChannelUserRead o2) -> o1.getLastRead().compareTo(o2.getLastRead()));
        return readLastMessage;
    }

    public List<Member> getMembers() {
        if (members == null) {
            return new ArrayList<>();
        }
        return members;
    }

    public Message getLastMessage() {
        Message lastMessage = null;
        List<Message> messages = getMessages();
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message message = messages.get(i);
            if (message.getDeletedAt() == null && message.getType().equals(ModelType.message_regular)) {
                lastMessage = message;
                break;
            }
        }
        Message.setStartDay(Arrays.asList(lastMessage), null);

        return lastMessage;
    }

    public Message getLastMessageFromOtherUser() {
        Message lastMessage = null;
        try {
            List<Message> messages = getMessages();
            for (int i = messages.size() - 1; i >= 0; i--) {
                Message message = messages.get(i);
                if (message.getDeletedAt() == null && !channel.getClient().fromCurrentUser(message)) {
                    lastMessage = message;
                    break;
                }
            }
            Message.setStartDay(Arrays.asList(lastMessage), null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lastMessage;
    }

    public User getLastReader() {
        return null;
//        if (this.reads == null || this.reads.isEmpty()) return null;
//        User lastReadUser = null;
//        try {
//            if (!isSorted && this.reads != null) {
//                Global.sortUserReads(this.reads);
//                isSorted = true;
//            }
//            for (int i = reads.size() - 1; i >= 0; i--) {
//                ChannelUserRead channelUserRead = reads.get(i);
//                if (!channelUserRead.getUser().getId().equals(Global.client.user.getId())) {
//                    lastReadUser = channelUserRead.getUser();
//                    break;
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return lastReadUser;
    }

    private void addOrUpdateMessage(Message newMessage) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i).getId().equals(newMessage.getId())) {
                messages.set(i, newMessage);
                return;
            }
            if (messages.get(i).getCreatedAt().before(newMessage.getCreatedAt())) {
                messages.add(newMessage);
                return;
            }
        }
    }

    public void addMessageSorted(Message message){
        List<Message> diff = new ArrayList<>();
        diff.add(message);
        addMessagesSorted(diff);
    }

    private void addMessagesSorted(List<Message> messages){
        int initialSize = messages.size();
        Log.w(TAG, "initial size" + initialSize);
        Log.w(TAG, "incoming size" + messages.size());

        for (Message m : messages) {
            if(m.getParentId() == null) {
                addOrUpdateMessage(m);
            }
        }
    }

    public void setWatcherCount(int watcherCount) {
        this.watcherCount = watcherCount;
    }

    public void init(ChannelState incoming) {
        reads = incoming.reads;
        members = incoming.members;
        watcherCount = incoming.watcherCount;

        if (watcherCount > 1) {
            lastKnownActiveWatcher = new Date();
        }

        if (incoming.messages != null) {
            addMessagesSorted(incoming.messages);
        }

        if (incoming.watchers != null) {
            for (Watcher watcher: incoming.watchers) {
                addWatcher(watcher);
            }
        }
        // TODO: merge with incoming.reads
        // TODO: merge with incoming.members
    }

    public int getCurrentUserUnreadMessageCount() {
        Client client = this.getChannel().getClient();
        String userID = client.getUserId();
        return this.getUnreadMessageCount(userID);
    }

    public int getUnreadMessageCount(String userId) {
        int unreadMessageCount = 0;
        if (this.reads == null || this.reads.isEmpty()) return unreadMessageCount;

        Date lastReadDate = getReadDateOfChannelLastMessage(userId);
        if (lastReadDate == null) return unreadMessageCount;
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message message = messages.get(i);
            if (!message.getUser().getId().equals(userId)) continue;
            if (message.getDeletedAt() != null) continue;
            if (message.getCreatedAt().getTime() > lastReadDate.getTime())
                unreadMessageCount++;
        }
        return unreadMessageCount;
    }

    public Date getReadDateOfChannelLastMessage(String userId) {
        if (this.reads == null || this.reads.isEmpty()) return null;
        Date lastReadDate = null;
        sortUserReads(this.reads);

        try {
            for (int i = reads.size() - 1; i >= 0; i--) {
                ChannelUserRead channelUserRead = reads.get(i);
                if (channelUserRead.getUser().getId().equals(userId)) {
                    lastReadDate = channelUserRead.getLastRead();
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return lastReadDate;
    }

    public void setReadDateOfChannelLastMessage(User user, Date readDate) {
        if (this.reads == null || this.reads.isEmpty()) return;
        boolean isNotSet = true;
        for (ChannelUserRead userLastRead : this.reads) {
            try {
                User user_ = userLastRead.getUser();
                if (user_.getId().equals(user.getId())) {
                    userLastRead.setLastRead(readDate);
                    // Change Order
                    this.reads.remove(userLastRead);
                    this.reads.add(userLastRead);
                    isNotSet = false;
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (isNotSet) {
            ChannelUserRead channelUserRead = new ChannelUserRead();
            channelUserRead.setUser(user);
            channelUserRead.setLastRead(readDate);
            this.reads.add(channelUserRead);
        }
    }
}

