/*
 * Copyright 2017 John Grosh (john.a.grosh@gmail.com).
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
package com.jagrosh.vortex.logging;

import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.logging.MessageCache.CachedMessage;
import com.jagrosh.vortex.utils.FormatUtil;
import com.jagrosh.vortex.utils.LogUtil;
import com.jagrosh.vortex.utils.Usage;
import com.typesafe.config.Config;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateAvatarEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateDiscriminatorEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateNameEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

import java.awt.Color;
import java.io.IOException;
import java.net.URL;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class BasicLogger
{
    private final static String EDIT = "\u26A0"; // ⚠
    private final static String DELETE = "\u274C"; // ❌
    private final static String BULK_DELETE = "\uD83D\uDEAE"; // 🚮
    private final static String VIEW = "\uD83D\uDCC4"; // 📄
    private final static String DOWNLOAD = "\uD83D\uDCE9"; // 📩
    private final static String REDIRECT = "\uD83D\uDD00"; // 🔀
    private final static String REDIR_MID = "\uD83D\uDD39"; // 🔹
    private final static String REDIR_END = "\uD83D\uDD37"; // 🔷
    private final static String NAME = "\uD83D\uDCDB"; // 📛
    private final static String JOIN = "\uD83D\uDCE5"; // 📥
    private final static String NEW = "\uD83C\uDD95"; // 🆕
    private final static String LEAVE = "\uD83D\uDCE4"; // 📤
    private final static String AVATAR = "\uD83D\uDDBC"; // 🖼
    private final static String PHISH = "\u2622"; // ☢
    
    private final Vortex vortex;
    private final AvatarSaver avatarSaver;
    private final Usage usage = new Usage();
    
    public BasicLogger(Vortex vortex, Config config)
    {
        this.vortex = vortex;
        this.avatarSaver = new AvatarSaver(config);
    }
    
    public Usage getUsage()
    {
        return usage;
    }

    private void log(OffsetDateTime now, TextChannel tc, String emote, String message, MessageEmbed embed)
    {
        logEmbeds(now, tc, emote, message, embed == null ? Collections.emptySet() : Set.of(embed));
    }
    
    private void logEmbeds(OffsetDateTime now, TextChannel tc, String emote, String message, Set<MessageEmbed> embeds)
    {
        try
        {
            usage.increment(tc.getGuild().getIdLong());
            tc.sendMessage(new MessageBuilder()
                .append(FormatUtil.filterEveryone(LogUtil.basiclogFormat(now, vortex.getDatabase().settings.getSettings(tc.getGuild()).getTimezone(), emote, message)))
                .setEmbeds(embeds)
                .build()).queue();
        }
        catch(PermissionException ignore) {}
    }
    
    private void logFile(OffsetDateTime now, TextChannel tc, String emote, String message, byte[] file, String filename)
    {
        try
        {
            usage.increment(tc.getGuild().getIdLong());
            tc.sendMessage(new MessageBuilder()
                .append(FormatUtil.filterEveryone(LogUtil.basiclogFormat(now, vortex.getDatabase().settings.getSettings(tc.getGuild()).getTimezone(), emote, message)))
                .build()).addFile(file, filename).queue();
        }
        catch(PermissionException ignore) {}
    }

    private MessageEmbed.Field reuploadAttachments(CachedMessage message)
    {
        TextChannel reuploader = vortex.getJDA().getTextChannelById(766330310471319602L);
        if(reuploader == null)
            return null;

        MessageAction action = null;

        for(Message.Attachment attachment : message.getAttachments())
        {
            if(attachment.getSize() > ((SelfUser) reuploader.getGuild().getSelfMember().getUser()).getAllowedFileSize())
                reuploader = vortex.getJDA().getTextChannelById(742484985159745558L);

            try
            {
                if(action == null)
                    action = reuploader.sendFile(new URL(attachment.getProxyUrl()).openStream(), attachment.getFileName());
                else
                    action = action.addFile(new URL(attachment.getProxyUrl()).openStream(), attachment.getFileName());
            }
            catch(IOException ignored) {}
        }

        if(action == null)
            return null;

        StringBuilder stringBuilder = new StringBuilder();

        for(Message.Attachment reuploaded : action.complete().getAttachments())
        {
            stringBuilder.append(":paperclip: [").append(reuploaded.getFileName()).append("](")
                    .append(reuploaded.getUrl()).append(")\n");
        }

        return new MessageEmbed.Field("Reuploaded Attachments:", stringBuilder.toString(), false);
    }
    
    // Message Logs
    
    public void logMessageEdit(Message newMessage, CachedMessage oldMessage)
    {
        if(oldMessage==null)
            return;
        TextChannel mtc = oldMessage.getTextChannel(vortex.getJDA());
        PermissionOverride po = mtc.getPermissionOverride(mtc.getGuild().getSelfMember());
        if(po!=null && po.getDenied().contains(Permission.MESSAGE_HISTORY))
            return;
        TextChannel tc = vortex.getDatabase().settings.getSettings(newMessage.getGuild()).getMessageLogChannel(newMessage.getGuild());
        if(tc==null)
            return;
        if(newMessage.getContentRaw().equals(oldMessage.getContentRaw()))
            return;
        EmbedBuilder edit = new EmbedBuilder()
                .setColor(Color.YELLOW)
                .appendDescription("**From:** ")
                .appendDescription(FormatUtil.formatMessage(oldMessage));
        String newm = FormatUtil.formatMessage(newMessage);
        if(edit.getDescriptionBuilder().length()+9+newm.length()>2048)
            edit.addField("To:", newm.length()>1024 ? newm.substring(0,1016)+" (...)" : newm, false);
        else
            edit.appendDescription("\n**To:** "+newm);
        log(newMessage.getTimeEdited()==null ? newMessage.getTimeCreated() : newMessage.getTimeEdited(), tc, EDIT,
                FormatUtil.formatFullUser(newMessage.getAuthor())+" edited a message in "+newMessage.getTextChannel().getAsMention()+":", edit.build());
    }
    
    public void logMessageDelete(CachedMessage oldMessage)
    {
        if(oldMessage==null)
            return;
        Guild guild = oldMessage.getGuild(vortex.getJDA());
        if(guild==null)
            return;
        TextChannel mtc = oldMessage.getTextChannel(vortex.getJDA());
        PermissionOverride po = mtc.getPermissionOverride(guild.getSelfMember());
        if(po!=null && po.getDenied().contains(Permission.MESSAGE_HISTORY))
            return;
        TextChannel tc = vortex.getDatabase().settings.getSettings(guild).getMessageLogChannel(guild);
        if(tc==null)
            return;
        String formatted = FormatUtil.formatMessage(oldMessage);
        if(formatted.isEmpty())
            return;
        EmbedBuilder delete = new EmbedBuilder()
                .setColor(Color.RED)
                .appendDescription(formatted)
                .addField(reuploadAttachments(oldMessage));
        User author = oldMessage.getAuthor(vortex.getJDA());
        String user = author==null ? FormatUtil.formatCachedMessageFullUser(oldMessage) : FormatUtil.formatFullUser(author);
        log(OffsetDateTime.now(), tc, DELETE, user+"'s message has been deleted from "+mtc.getAsMention()+":", delete.build());
    }

    public void logMessageBulkDelete(List<CachedMessage> messages, int count, TextChannel text)
    {
        if(count==0)
            return;
        TextChannel tc = vortex.getDatabase().settings.getSettings(text.getGuild()).getMessageLogChannel(text.getGuild());
        if(tc==null)
            return;
        if(messages.isEmpty())
        {
            //log(OffsetDateTime.now(), tc, "\uD83D\uDEAE", "**"+count+"** messages were deleted from "+text.getAsMention()+" (**"+messages.size()+"** logged)", null);
            return;
        }
        TextChannel mtc = messages.get(0).getTextChannel(vortex.getJDA());
        PermissionOverride po = mtc.getPermissionOverride(mtc.getGuild().getSelfMember());
        if(po!=null && po.getDenied().contains(Permission.MESSAGE_HISTORY))
            return;
        if(messages.size()==1)
        {
            String formatted = FormatUtil.formatMessage(messages.get(0));
            if(formatted.isEmpty())
                return;
            EmbedBuilder delete = new EmbedBuilder()
                    .setColor(Color.RED)
                    .appendDescription(formatted);
            User author = messages.get(0).getAuthor(vortex.getJDA());
            String user = author==null ? FormatUtil.formatCachedMessageFullUser(messages.get(0)) : FormatUtil.formatFullUser(author);
            log(OffsetDateTime.now(), tc, DELETE, user+"'s message has been deleted from "+mtc.getAsMention()+":", delete.build());
            return;
        }
        vortex.getTextUploader().upload(LogUtil.logCachedMessagesForwards("Deleted Messages", messages, vortex.getJDA()), "DeletedMessages", (view, download) ->
        {
            log(OffsetDateTime.now(), tc, BULK_DELETE, "**"+count+"** messages were deleted from "+text.getAsMention()+" (**"+messages.size()+"** logged):", 
                new EmbedBuilder().setColor(Color.RED.darker().darker())
                .appendDescription("[`"+VIEW+" View`]("+view+")  |  [`"+DOWNLOAD+" Download`]("+download+")").build());
        });
    }
    
    public void logRedirectPath(Message message, String link, List<String> redirects)
    {
        TextChannel tc = vortex.getDatabase().settings.getSettings(message.getGuild()).getMessageLogChannel(message.getGuild());
        if(tc==null)
            return;
        StringBuilder sb = new StringBuilder(REDIR_END+" **"+link+"**");
        for(int i=0; i<redirects.size(); i++)
            sb.append("\n").append(redirects.size()-1==i ? REDIR_END + " **" : REDIR_MID).append(redirects.get(i)).append(redirects.size()-1==i ? "**" : "");
        log(OffsetDateTime.now(), tc, REDIRECT, 
                FormatUtil.formatFullUser(message.getAuthor())+"'s message in "+message.getTextChannel().getAsMention()+" contained redirects:", 
                new EmbedBuilder().setColor(Color.BLUE.brighter().brighter()).appendDescription(sb.toString()).build());
    }

    public void logPhishLink(Message message, String link)
    {
        TextChannel tc = vortex.getDatabase().settings.getSettings(message.getGuild()).getMessageLogChannel(message.getGuild());
        if(tc==null)
            return;
        log(OffsetDateTime.now(), tc, PHISH,
                FormatUtil.formatFullUser(message.getAuthor())+"'s message in "+message.getTextChannel().getAsMention()+" contained a phishing/scam link:",
                new EmbedBuilder().setColor(Color.BLUE.brighter().brighter()).appendDescription("`" + link + "`")
                        .setFooter("Source: phisherman.gg").build());
    }
    
    
    // Server Logs
    
    public void logNameChange(UserUpdateNameEvent event)
    {
        OffsetDateTime now = OffsetDateTime.now();
        event.getUser().getMutualGuilds().stream()
            .map(guild -> vortex.getDatabase().settings.getSettings(guild).getServerLogChannel(guild))
            .filter(tc -> tc!=null)
            .forEachOrdered(tc ->
            {
                log(now, tc, NAME, "**"+event.getOldName()+"**#"+event.getUser().getDiscriminator()+" (ID:"
                        +event.getUser().getId()+") has changed names to "+FormatUtil.formatUser(event.getUser()), null);
            });
    }
    
    public void logDiscrimChange(UserUpdateDiscriminatorEvent event)
    {
        OffsetDateTime now = OffsetDateTime.now();
        event.getUser().getMutualGuilds().stream()
            .map(guild -> vortex.getDatabase().settings.getSettings(guild).getServerLogChannel(guild))
            .filter(tc -> tc!=null)
            .forEachOrdered(tc ->
            {
                log(now, tc, NAME, "**"+event.getUser().getName()+"**#"+event.getOldDiscriminator()+" (ID:"
                        +event.getUser().getId()+") has changed discrims to "+FormatUtil.formatUser(event.getUser()), null);
            });
    }
    
    public void logGuildJoin(GuildMemberJoinEvent event)
    {
        TextChannel tc = vortex.getDatabase().settings.getSettings(event.getGuild()).getServerLogChannel(event.getGuild());
        if(tc==null)
            return;
        OffsetDateTime now = OffsetDateTime.now();
        long seconds = event.getUser().getTimeCreated().until(now, ChronoUnit.SECONDS);
        log(now, tc, JOIN, FormatUtil.formatFullUser(event.getUser())+" joined the server. "
                +(seconds<16*60 ? NEW : "")
                +"\nCreation: "+event.getUser().getTimeCreated().format(DateTimeFormatter.RFC_1123_DATE_TIME)+" ("+FormatUtil.secondsToTimeCompact(seconds)+" ago)", null);
    }
    
    public void logGuildLeave(GuildMemberRemoveEvent event)
    {
        TextChannel tc = vortex.getDatabase().settings.getSettings(event.getGuild()).getServerLogChannel(event.getGuild());
        if(tc==null)
            return;
        OffsetDateTime now = OffsetDateTime.now();
        long seconds = event.getMember().getTimeJoined().until(now, ChronoUnit.SECONDS);
        StringBuilder rlist;
        if(event.getMember().getRoles().isEmpty())
            rlist = new StringBuilder();
        else
        {
            rlist= new StringBuilder("\nRoles: `"+event.getMember().getRoles().get(0).getName());
            for(int i=1; i<event.getMember().getRoles().size(); i++)
                rlist.append("`, `").append(event.getMember().getRoles().get(i).getName());
            rlist.append("`");
        }
        log(now, tc, LEAVE, FormatUtil.formatFullUser(event.getUser())+" left or was kicked from the server. "
                +"\nJoined: "+event.getMember().getTimeJoined().format(DateTimeFormatter.RFC_1123_DATE_TIME)+" ("+FormatUtil.secondsToTimeCompact(seconds)+" ago)"
                +rlist.toString(), null);
    }
    
    
    // Voice Logs
    
    public void logVoiceJoin(GuildVoiceJoinEvent event)
    {
        TextChannel tc = vortex.getDatabase().settings.getSettings(event.getGuild()).getVoiceLogChannel(event.getGuild());
        if(tc==null)
            return;
        log(OffsetDateTime.now(), tc, "<:voicejoin:753743582245945404>", FormatUtil.formatFullUser(event.getMember().getUser())
                +" has joined voice channel _"+event.getChannelJoined().getName()+"_", null);
    }
    
    public void logVoiceMove(GuildVoiceMoveEvent event)
    {
        TextChannel tc = vortex.getDatabase().settings.getSettings(event.getGuild()).getVoiceLogChannel(event.getGuild());
        if(tc==null)
            return;
        log(OffsetDateTime.now(), tc, "<:voicemove:753743618434400280>", FormatUtil.formatFullUser(event.getMember().getUser())
                +" has moved voice channels from _"+event.getChannelLeft().getName()+"_ to _"+event.getChannelJoined().getName()+"_", null);
    }
    
    public void logVoiceLeave(GuildVoiceLeaveEvent event)
    {
        TextChannel tc = vortex.getDatabase().settings.getSettings(event.getGuild()).getVoiceLogChannel(event.getGuild());
        if(tc==null)
            return;
        log(OffsetDateTime.now(), tc, "<:voiceleave:753743548859416667>", FormatUtil.formatFullUser(event.getMember().getUser())
                +" has left voice channel _"+event.getChannelLeft().getName()+"_", null);
    }
    
    
    // Avatar Logs
    
    public void logAvatarChange(UserUpdateAvatarEvent event)
    {
        List<TextChannel> logs = event.getUser().getMutualGuilds().stream()
            .map(guild -> vortex.getDatabase().settings.getSettings(guild).getAvatarLogChannel(guild))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        if(logs.isEmpty())
            return;
        OffsetDateTime now = OffsetDateTime.now();
        vortex.getThreadpool().execute(() -> 
        {
            byte[] im = avatarSaver.makeAvatarImage(event.getUser(), event.getOldAvatarUrl(), event.getOldAvatarId());
            if(im!=null)
                logs.forEach(tc -> logFile(now, tc, AVATAR, FormatUtil.formatFullUser(event.getUser())+" has changed avatars"
                        +(event.getUser().getAvatarId()!=null && event.getUser().getAvatarId().startsWith("a_") ? " <:gif:314068430624129039>" : "")
                        +":", im, "AvatarChange.png"));
        });
    }
}
