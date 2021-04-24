package xyz.rc24.vortiix.modmail;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.vortex.utils.FormatUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.User;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class ModMailThread
{
    private final AtomicInteger queuedMessages;
    private final long user;
    private final long initialMessage;
    private final long warningMessage;

    private boolean active;

    public ModMailThread(User user, Message initial, Message warning)
    {
        this(user.getIdLong(), initial.getIdLong(), warning.getIdLong());
    }

    public ModMailThread(long user, long initialMessage, long warningMessage)
    {
        this.queuedMessages = new AtomicInteger(1);
        this.user = user;
        this.initialMessage = initialMessage;
        this.warningMessage = warningMessage;
    }

    public long getUser()
    {
        return user;
    }

    public long getInitialMessage()
    {
        return initialMessage;
    }

    public long getWarningMessage()
    {
        return warningMessage;
    }

    public AtomicInteger getQueuedMessages()
    {
        return queuedMessages;
    }

    public boolean isActive()
    {
        return active;
    }

    public void setActive(boolean active)
    {
        this.active = active;
    }

    public void sendHistory(ModMailManager manager, PrivateChannel channel)
    {
        channel.getHistoryAfter(getInitialMessage(), Math.min(getQueuedMessages().get(), 10))
                .queue(history -> history.retrievePast(1).queue(s ->
                {
                    boolean initial = true;
                    List<Message> messages = filterMessages(history.getRetrievedHistory());

                    for(Message message : messages)
                    {
                        manager.send(initial, channel, embed(message));
                        initial = false;
                    }
                }));
    }

    public MessageEmbed embed(Message message)
    {
        User author = message.getAuthor();

        return new EmbedBuilder()
                .setAuthor(format("%#s (ID: %s)", author, author.getId()), null, author.getEffectiveAvatarUrl())
                .setDescription(message.getContentRaw())
                .setTitle("Mail received:", message.getJumpUrl())
                .setFooter("Submitted")
                .setTimestamp(message.getTimeCreated())
                .addField("Attachments:", FormatUtil.formatAttachments(message), false)
                .build();
    }

    public void reply(CommandEvent event, JDA jda, Member user, String message)
    {
        jda.retrieveUserById(user.getId())
                .flatMap(User::openPrivateChannel)
                .flatMap(pc -> pc.sendMessage(message))
                .queue(s -> event.reactSuccess(), e -> event.replyError("Failed to send the reply to " +
                        user.getUser().getAsTag() + ", they probably have mutual DMs disabled"));
    }

    private List<Message> filterMessages(List<Message> messages)
    {
        messages = new LinkedList<>(messages);
        Collections.reverse(messages);

        return messages.stream()
                .filter(m -> !(m.getAuthor().isBot()))
                .collect(Collectors.toList());
    }
}
