package xyz.rc24.vortiix;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.utils.FormatUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static com.jagrosh.vortex.Constants.ERROR;
import static com.jagrosh.vortex.Constants.RC24_ID;
import static com.jagrosh.vortex.Constants.SUCCESS;
import static java.lang.String.format;

public class ModMail
{
    private final Logger logger;
    private final Map<Long, Integer> uses;
    private final Vortex vortex;

    public ModMail(Vortex vortex)
    {
        this.logger = LoggerFactory.getLogger(ModMail.class);
        this.uses = new HashMap<>();
        this.vortex = vortex;
    }

    public void onMessage(MessageReceivedEvent event)
    {
        Message message = event.getMessage();
        User author = event.getAuthor();

        if(!(isOnGuild(author)))
            return;

        // Cooldown check
        if(COOLDOWN > 0)
        {
            String key = genKey(author.getId());
            int cooldown = COOLDOWN;
            int remaining = vortex.getClient().getRemainingCooldown(key);
            int uses = this.uses.getOrDefault(author.getIdLong(), 0);

            if(remaining > 0)
            {
                if(uses >= 3)
                    vortex.getClient().applyCooldown(key, cooldown * 2);
                return;
            }
            else
            {
                vortex.getClient().applyCooldown(key, cooldown);
                if(uses < 3)
                {
                    uses++;
                    this.uses.put(author.getIdLong(), uses);
                }
                else
                    this.uses.remove(author.getIdLong());
            }
        }

        MessageEmbed embed = new EmbedBuilder()
                .setAuthor(format("%#s (ID: %s)", author, author.getId()), null, author.getEffectiveAvatarUrl())
                .setDescription(message.getContentRaw())
                .setTitle("Mail received: " + message.getId(), message.getJumpUrl())
                .setFooter("Submitted")
                .setTimestamp(message.getTimeCreated())
                .addField("Attachments:", FormatUtil.formatAttachments(message), false)
                .build();

        send(event, embed);
    }

    public void reply(CommandEvent event, Member user, String message)
    {
        user.getUser().openPrivateChannel()
                .flatMap(pc -> pc.sendMessage(message))
                .queue(s -> event.reactSuccess(), e -> event.replyError("Failed to send the reply to " +
                        user.getUser().getAsTag() + ", they probably have mutual DMs disabled"));
    }

    private void send(MessageReceivedEvent event, MessageEmbed embed)
    {
        TextChannel channel = event.getJDA().getTextChannelById(CHANNEL);

        if(channel == null || !(channel.canTalk()))
        {
            event.getPrivateChannel().sendMessage(ERROR + " I was not able to send the message to the mods." +
                    " Contact an Admin directly and mention this error.").queue(null, e -> {});
            logger.error("I cannot find the ModMail channel or I don't have permission to talk on it");
            return;
        }

        channel.sendMessage(embed)
                .flatMap(s -> event.getMessage().addReaction(SUCCESS))
                .queue(null, e ->
                {
                    event.getPrivateChannel().sendMessage(ERROR + " An error occurred when sending the message." +
                            " Contact an Admin directly and mention this error.").queue(null, e2 -> {});
                    logger.error("Failed to send ModMail message:", e);
                });
    }

    private boolean isOnGuild(User author)
    {
        return author.getMutualGuilds().stream().anyMatch(guild -> guild.getId().equals(RC24_ID));
    }

    private String genKey(String id)
    {
        return "modmail|U:" + id;
    }

    private static final int COOLDOWN;
    private static final long CHANNEL;

    static
    {
        COOLDOWN = Integer.getInteger("vortiix.modmail.cooldown", 5);
        CHANNEL = Long.getLong("vortiix.modmail.channel", 217711478831185920L);
    }
}
