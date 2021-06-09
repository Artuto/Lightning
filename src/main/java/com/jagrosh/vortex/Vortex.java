/*
 * Copyright 2018 John Grosh (jagrosh).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jagrosh.vortex;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.examples.command.PingCommand;
import com.jagrosh.vortex.automod.AutoMod;
import com.jagrosh.vortex.automod.StrikeHandler;
import com.jagrosh.vortex.commands.CommandExceptionListener;
import com.jagrosh.vortex.commands.automod.AnticopypastaCmd;
import com.jagrosh.vortex.commands.automod.AntiduplicateCmd;
import com.jagrosh.vortex.commands.automod.AntieveryoneCmd;
import com.jagrosh.vortex.commands.automod.AntiinviteCmd;
import com.jagrosh.vortex.commands.automod.AntirefCmd;
import com.jagrosh.vortex.commands.automod.AutodehoistCmd;
import com.jagrosh.vortex.commands.automod.AutoraidmodeCmd;
import com.jagrosh.vortex.commands.automod.FilterCmd;
import com.jagrosh.vortex.commands.automod.IgnoreCmd;
import com.jagrosh.vortex.commands.automod.MaxlinesCmd;
import com.jagrosh.vortex.commands.automod.MaxmentionsCmd;
import com.jagrosh.vortex.commands.automod.ResolvelinksCmd;
import com.jagrosh.vortex.commands.automod.UnignoreCmd;
import xyz.rc24.vortiix.commands.automod.UsernameFilterCmd;
import com.jagrosh.vortex.commands.general.RoleinfoCmd;
import com.jagrosh.vortex.commands.general.ServerinfoCmd;
import com.jagrosh.vortex.commands.general.UserinfoCmd;
import com.jagrosh.vortex.commands.moderation.BanCmd;
import com.jagrosh.vortex.commands.moderation.CheckCmd;
import com.jagrosh.vortex.commands.moderation.CleanCmd;
import com.jagrosh.vortex.commands.moderation.KickCmd;
import com.jagrosh.vortex.commands.moderation.MuteCmd;
import com.jagrosh.vortex.commands.moderation.PardonCmd;
import com.jagrosh.vortex.commands.moderation.RaidCmd;
import com.jagrosh.vortex.commands.moderation.ReasonCmd;
import com.jagrosh.vortex.commands.moderation.SilentbanCmd;
import com.jagrosh.vortex.commands.moderation.SoftbanCmd;
import com.jagrosh.vortex.commands.moderation.StrikeCmd;
import com.jagrosh.vortex.commands.moderation.UnbanCmd;
import com.jagrosh.vortex.commands.moderation.UnmuteCmd;
import com.jagrosh.vortex.commands.moderation.VoicekickCmd;
import com.jagrosh.vortex.commands.moderation.VoicemoveCmd;
import com.jagrosh.vortex.commands.owner.DebugCmd;
import com.jagrosh.vortex.commands.owner.EvalCmd;
import com.jagrosh.vortex.commands.owner.ImportCmd;
import com.jagrosh.vortex.commands.owner.PremiumCmd;
import com.jagrosh.vortex.commands.owner.ReloadCmd;
import com.jagrosh.vortex.commands.settings.AvatarlogCmd;
import com.jagrosh.vortex.commands.settings.MessagelogCmd;
import com.jagrosh.vortex.commands.settings.ModlogCmd;
import com.jagrosh.vortex.commands.settings.ModroleCmd;
import com.jagrosh.vortex.commands.settings.PrefixCmd;
import com.jagrosh.vortex.commands.settings.PunishmentCmd;
import com.jagrosh.vortex.commands.settings.ServerlogCmd;
import com.jagrosh.vortex.commands.settings.SettingsCmd;
import com.jagrosh.vortex.commands.settings.SetupCmd;
import com.jagrosh.vortex.commands.settings.TimezoneCmd;
import com.jagrosh.vortex.commands.settings.VoicelogCmd;
import com.jagrosh.vortex.commands.tools.AnnounceCmd;
import com.jagrosh.vortex.commands.tools.AuditCmd;
import com.jagrosh.vortex.commands.tools.DehoistCmd;
import com.jagrosh.vortex.commands.tools.ExportCmd;
import com.jagrosh.vortex.commands.tools.InvitepruneCmd;
import com.jagrosh.vortex.commands.tools.LookupCmd;
import com.jagrosh.vortex.database.Database;
import com.jagrosh.vortex.logging.BasicLogger;
import com.jagrosh.vortex.logging.MessageCache;
import com.jagrosh.vortex.logging.ModLogger;
import com.jagrosh.vortex.logging.TextUploader;
import com.jagrosh.vortex.utils.BlockingSessionController;
import com.jagrosh.vortex.utils.FormatUtil;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import xyz.rc24.vortiix.Vortiix;
import xyz.rc24.vortiix.commands.moderation.CloseThreadCmd;
import xyz.rc24.vortiix.commands.moderation.ModReplyCmd;
import xyz.rc24.vortiix.commands.other.MakeAClownCmd;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Vortex
{
    private final EventWaiter waiter;
    private final ScheduledExecutorService threadpool;
    private final Database database;
    private final TextUploader uploader;
    private final JDA jda;
    private final CommandClient client;
    private final ModLogger modlog;
    private final BasicLogger basiclog;
    private final MessageCache messages;
    private final WebhookClient logwebhook;
    private final AutoMod automod;
    private final StrikeHandler strikehandler;
    private final Vortiix vortiix;

    public Vortex() throws Exception
    {
        System.setProperty("config.file", System.getProperty("config.file", "application.conf"));
        Config config = ConfigFactory.load();
        waiter = new EventWaiter(Executors.newSingleThreadScheduledExecutor(), false);
        threadpool = Executors.newScheduledThreadPool(100);
        database = new Database(config.getString("database.host"), 
                                       config.getString("database.username"), 
                                       config.getString("database.password"));
        uploader = new TextUploader(this, config.getLong("uploader.guild"), config.getLong("uploader.category"));
        modlog = new ModLogger(this);
        basiclog = new BasicLogger(this, config);
        messages = new MessageCache();
        logwebhook = new WebhookClientBuilder(config.getString("webhook-url")).build();
        automod = new AutoMod(this, config);
        strikehandler = new StrikeHandler(this);
        this.vortiix = new Vortiix(config);

        this.client = new CommandClientBuilder()
                        .setPrefix(Constants.PREFIX)
                        .setOwnerId(Constants.OWNER_ID)
                        .setEmojis(Constants.SUCCESS, Constants.WARNING, Constants.ERROR)
                        .setLinkedCacheSize(0)
                        .setGuildSettingsManager(database.settings)
                        .setListener(new CommandExceptionListener())
                        .setScheduleExecutor(threadpool)
                        .setShutdownAutomatically(false)
                        .addCommands(
                            // General
                            //new AboutCmd(),
                            //new InviteCmd(),
                            new PingCommand(),
                            new RoleinfoCmd(),
                            new ServerinfoCmd(),
                            new UserinfoCmd(),

                            // Moderation
                            new KickCmd(this),
                            new BanCmd(this),
                            new SoftbanCmd(this),
                            new SilentbanCmd(this),
                            new UnbanCmd(this),
                            new CleanCmd(this),
                            new VoicemoveCmd(this),
                            new VoicekickCmd(this),
                            new MuteCmd(this),
                            new UnmuteCmd(this),
                            new RaidCmd(this),
                            new StrikeCmd(this),
                            new PardonCmd(this),
                            new CheckCmd(this),
                            new ReasonCmd(this),
                            new ModReplyCmd(this),
                            new CloseThreadCmd(this),

                            // Settings
                            new SetupCmd(this),
                            new PunishmentCmd(this),
                            new MessagelogCmd(this),
                            new ModlogCmd(this),
                            new ServerlogCmd(this),
                            new VoicelogCmd(this),
                            new AvatarlogCmd(this),
                            new TimezoneCmd(this),
                            new ModroleCmd(this),
                            new PrefixCmd(this),
                            new SettingsCmd(this),

                            // Automoderation
                            new AntiinviteCmd(this),
                            new AnticopypastaCmd(this),
                            new AntieveryoneCmd(this),
                            new AntirefCmd(this),
                            new MaxlinesCmd(this),
                            new MaxmentionsCmd(this),
                            new AntiduplicateCmd(this),
                            new AutodehoistCmd(this),
                            new FilterCmd(this),
                            new ResolvelinksCmd(this),
                            new AutoraidmodeCmd(this),
                            new IgnoreCmd(this),
                            new UnignoreCmd(this),
                            new UsernameFilterCmd(this),
                            
                            // Tools
                            new AnnounceCmd(),
                            new AuditCmd(),
                            new DehoistCmd(),
                            new ExportCmd(this),
                            new InvitepruneCmd(this),
                            new LookupCmd(this),

                            // RiiSpecial
                            new MakeAClownCmd(),

                            // Owner
                            new EvalCmd(this),
                            new DebugCmd(this),
                            new ImportCmd(this),
                            new PremiumCmd(this),
                            new ReloadCmd(this)
                            //new TransferCmd(this)
                        )
                        .setHelpConsumer(event -> event.replyInDm(FormatUtil.formatHelp(event), m ->
                        {
                            if(event.isFromType(ChannelType.TEXT))
                                try
                                {
                                    event.getMessage().addReaction(Constants.HELP_REACTION).queue(s->{}, f->{});
                                } catch(PermissionException ignore) {}
                        }, t -> event.replyWarning("Help cannot be sent because you are blocking Direct Messages.")))
                        .build();

        jda = JDABuilder.createDefault(config.getString("bot-token"))
                .enableIntents(GatewayIntent.GUILD_MEMBERS)
                .setChunkingFilter(ChunkingFilter.ALL)
                .addEventListeners(new Listener(this), client, waiter)
                .setStatus(OnlineStatus.DO_NOT_DISTURB)
                .setActivity(Activity.playing("loading..."))
                .setBulkDeleteSplittingEnabled(false)
                .setRequestTimeoutRetry(true)
                .setSessionController(new BlockingSessionController())
                .build().awaitReady();
        
        modlog.start();
        threadpool.scheduleWithFixedDelay(System::gc, 12, 6, TimeUnit.HOURS);
    }
    
    // Getters
    public EventWaiter getEventWaiter()
    {
        return waiter;
    }
    
    public Database getDatabase()
    {
        return database;
    }
    
    public ScheduledExecutorService getThreadpool()
    {
        return threadpool;
    }
    
    public TextUploader getTextUploader()
    {
        return uploader;
    }
    
    public JDA getJDA()
    {
        return jda;
    }

    public CommandClient getClient()
    {
        return client;
    }

    public ModLogger getModLogger()
    {
        return modlog;
    }
    
    public BasicLogger getBasicLogger()
    {
        return basiclog;
    }
    
    public MessageCache getMessageCache()
    {
        return messages;
    }
    
    public WebhookClient getLogWebhook()
    {
        return logwebhook;
    }
    
    public AutoMod getAutoMod()
    {
        return automod;
    }
    
    public StrikeHandler getStrikeHandler()
    {
        return strikehandler;
    }

    public Vortiix getVortiix()
    {
        return vortiix;
    }

    /**
     * @param args the command line arguments
     * @throws java.lang.Exception if an error occurred
     */
    public static void main(String[] args) throws Exception
    {
        new Vortex();
    }
}
