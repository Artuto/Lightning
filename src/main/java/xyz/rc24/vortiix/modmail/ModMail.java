package xyz.rc24.vortiix.modmail;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.vortex.Constants;
import com.typesafe.config.Config;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;

import static net.dv8tion.jda.api.requests.GatewayIntent.DIRECT_MESSAGES;
import static net.dv8tion.jda.api.requests.GatewayIntent.DIRECT_MESSAGE_REACTIONS;

public class ModMail extends ListenerAdapter
{
    private final EventWaiter waiter;
    private final Logger logger;

    private JDA jda;
    private ModMailManager manager;

    public ModMail(Config config)
    {
        this.waiter = new EventWaiter();
        this.logger = LoggerFactory.getLogger(ModMail.class);

        String token = config.getString("modmail.token");
        if(token == null || token.isEmpty())
            return;

        try
        {
            this.jda = JDABuilder.createLight(token, DIRECT_MESSAGES, DIRECT_MESSAGE_REACTIONS)
                    .setActivity(Activity.playing("DM me to send a message to the mods"))
                    .addEventListeners(this, waiter)
                    .build().awaitReady();
        }
        catch(LoginException | InterruptedException e)
        {
            logger.error("Failed to start ModMail:", e);
            return;
        }

        logger.info("Started ModMail Bot");
        this.manager = new ModMailManager(this);
    }

    @Override
    public void onPrivateMessageReceived(PrivateMessageReceivedEvent event)
    {
        manager.onMessage(event);
    }

    public EventWaiter getWaiter()
    {
        return waiter;
    }

    public JDA getJDA()
    {
        return jda;
    }

    public ModMailManager getManager()
    {
        return manager;
    }

    static final String WARNING = "__**READ BEFORE SENDING:**__\n\n" +
            "Please do **not** use Mod Mail for modding or RiiConnect24 support. " +
            "If you need help go to our support channel (<#288361557044494337>) and ask there.\n" +
            "Abuse of the Mod Mail will be punished at the moderation's discretion.\n\n" +
            "If you agree with this react with " + Constants.SUCCESS;
}
