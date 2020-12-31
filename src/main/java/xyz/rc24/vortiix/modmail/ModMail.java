package xyz.rc24.vortiix.modmail;

import com.jagrosh.vortex.Vortex;
import com.typesafe.config.Config;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.RawGatewayEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;

import static net.dv8tion.jda.api.requests.GatewayIntent.DIRECT_MESSAGES;
import static net.dv8tion.jda.api.requests.GatewayIntent.DIRECT_MESSAGE_TYPING;

public class ModMail extends ListenerAdapter
{
    private final Logger logger;
    private final Vortex vortex;

    private ModMailManager manager;

    public ModMail(Vortex vortex, Config config)
    {
        this.logger = LoggerFactory.getLogger(ModMail.class);
        this.vortex = vortex;

        String token = config.getString("modmail.token");
        if(token == null || token.isEmpty())
            return;

        try
        {
            JDABuilder.createLight(token, DIRECT_MESSAGES, DIRECT_MESSAGE_TYPING)
                    .setActivity(Activity.playing("DM me to send a message to the mods"))
                    .addEventListeners(this)
                    .setRawEventsEnabled(true)
                    .build();
        }
        catch(LoginException e)
        {
            logger.error("Failed to start ModMail:", e);
            return;
        }

        logger.info("Started ModMail Bot");
        this.manager = new ModMailManager(vortex);
    }

    @Override
    public void onPrivateMessageReceived(PrivateMessageReceivedEvent event)
    {
        manager.onMessage(event);
    }

    @Override
    public void onRawGateway(RawGatewayEvent event)
    {
        if(!(event.getType().equals("TYPING_START")))
            return;

        DataObject payload = event.getPayload();
        if(payload.hasKey("guild_id"))
            return;

        String user = payload.getString("user_id");
        String key = manager.genKey(user, "modmailtyping");

        if(vortex.getClient().getRemainingCooldown(key) > 0)
            return;

        event.getJDA().retrieveUserById(user)
                .flatMap(User::openPrivateChannel)
                .flatMap(pc -> pc.sendMessage("__**READ BEFORE SENDING:**__\n\n" +
                        "Please do **not** use Mod Mail for modding or RiiConnect24 support. " +
                        "If you need help go to our support channel (<#288361557044494337>) and ask there.\n\n" +
                        "Misuse of the Mod Mail will be punished at the moderation's discretion."))
                .queue(s -> vortex.getClient().applyCooldown(key, 259200), e -> {});
    }

    public ModMailManager getManager()
    {
        return manager;
    }
}
