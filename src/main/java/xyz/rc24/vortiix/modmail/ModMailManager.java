package xyz.rc24.vortiix.modmail;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.react.PrivateMessageReactionAddEvent;
import net.dv8tion.jda.api.requests.RestAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.jagrosh.vortex.Constants.ERROR;
import static com.jagrosh.vortex.Constants.SUCCESS;
import static com.jagrosh.vortex.Constants.SUCCESS_REACTION;

public class ModMailManager
{
    private final Logger logger;
    private final Map<Long, ModMailThread> threads;
    private final ModMail modMail;

    public ModMailManager(ModMail modMail)
    {
        this.logger = LoggerFactory.getLogger(ModMail.class);
        this.threads = new HashMap<>();
        this.modMail = modMail;
    }

    public void onMessage(PrivateMessageReceivedEvent event)
    {
        Message message = event.getMessage();
        User author = event.getAuthor();

        if(author.isBot())
            return;

        ModMailThread thread = getThreads().get(author.getIdLong());
        if(thread == null)
        {
            event.getChannel().sendMessage(ModMail.WARNING).queue(warningMessage ->
            {
                warningMessage.addReaction(SUCCESS_REACTION).queueAfter(3, TimeUnit.SECONDS);
                modMail.getWaiter().waitForEvent(PrivateMessageReactionAddEvent.class,
                        this::checkReaction, this::openThread);

                ModMailThread createdThread = new ModMailThread(author, message, warningMessage);
                getThreads().put(author.getIdLong(), createdThread);
            });

            return;
        }
        else if(!(thread.isActive()))
        {
            String contentDisplay = message.getContentDisplay();
            if(!(contentDisplay.isEmpty() || contentDisplay.length() <= 2))
                thread.getQueuedMessages().incrementAndGet();
            return;
        }

        send(event, thread.embed(event.getMessage()));
    }

    void send(PrivateMessageReceivedEvent event, MessageEmbed embed)
    {
        send(false, event.getChannel(), embed);
    }

    void send(boolean initial, PrivateChannel pc, MessageEmbed embed)
    {
        TextChannel channel = pc.getJDA().getTextChannelById(CHANNEL);

        if(channel == null || !(channel.canTalk()))
        {
            pc.sendMessage(ERROR + " I was not able to send the message to the mods." +
                    " Contact an Admin directly and mention this error.").queue(null, e -> {});
            logger.error("I cannot find the ModMail channel or I don't have permission to talk on it");
            return;
        }

        RestAction<Message> action = channel.sendMessage(embed);
        if(initial)
            action = action.flatMap(m -> pc.sendMessage(SUCCESS + " Your message has been sent to the mods"));

        action.queue(null, e ->
        {
            pc.sendMessage(ERROR + " An error occurred when sending the message." +
                    " Contact an Admin directly and mention this error.").queue(null, e2 -> {});
            logger.error("Failed to send ModMail message:", e);
        });
    }

    private void openThread(PrivateMessageReactionAddEvent event)
    {
        PrivateChannel channel = event.getChannel();
        ModMailThread thread = getThreads().get(event.getUserIdLong());
        thread.setActive(true);

        thread.sendHistory(this, channel);
    }

    private boolean checkReaction(PrivateMessageReactionAddEvent event)
    {
        User user = event.getChannel().getUser();
        ModMailThread thread = getThreads().get(user.getIdLong());

        if(!(event.getUserIdLong() == user.getIdLong()))
            return false;

        if(!(event.getMessageIdLong() == thread.getWarningMessage()))
            return false;

        MessageReaction.ReactionEmote emote = event.getReactionEmote();
        return emote.getAsReactionCode().equals(SUCCESS_REACTION);
    }

    public synchronized Map<Long, ModMailThread> getThreads()
    {
        return threads;
    }

    private static final long CHANNEL;

    static
    {
        CHANNEL = Long.getLong("vortiix.modmail.channel", 217711478831185920L);
    }
}
