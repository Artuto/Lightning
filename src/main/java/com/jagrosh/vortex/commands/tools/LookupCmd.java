/*
 * Copyright 2018 John Grosh (john.a.grosh@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://wwwidget.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jagrosh.vortex.commands.tools;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.vortex.Constants;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.database.managers.PremiumManager;
import com.jagrosh.vortex.utils.FormatUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Invite;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.RateLimitedException;
import net.dv8tion.jda.api.utils.TimeUtil;
import net.dv8tion.jda.api.utils.WidgetUtil;
import net.dv8tion.jda.api.utils.WidgetUtil.Widget;

import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class LookupCmd extends Command
{
    private final static String BOT_EMOJI = "<:botTag:230105988211015680>";
    private final static String USER_EMOJI = "\uD83D\uDC64"; // 👤
    private final static String GUILD_EMOJI = "\uD83D\uDDA5"; // 🖥
    private final static String LINESTART = "\u25AB"; // ▫
    
    private final Vortex vortex;
    
    public LookupCmd(Vortex vortex)
    {
        this.vortex = vortex;
        this.name = "lookup";
        this.arguments = "<ID | invite>";
        this.help = "finds information about a user or server";
        this.cooldown = 5;
        this.category = new Category("Tools");
    }

    @Override
    protected void execute(CommandEvent event)
    {
        if(event.isFromType(ChannelType.TEXT) && event.getMember().getRoles().isEmpty())
        {
            event.reactError();
            return;
        }
        if(event.getArgs().isEmpty())
        {
            event.replyError("Please provide a User ID, Server ID, or Invite Code\n"
                    + "This command provides information about a user or server. "
                    + "All of the information provided is information that Discord makes publically-available.");
            return;
        }
        event.getChannel().sendTyping().queue();
        event.async(() -> 
        {
            // determine if this is a valid ID
            long id = -1;
            try
            {
                id = Long.parseLong(event.getArgs());
            }
            catch(NumberFormatException ignore) {}
            
            // if it's valid and we find a user, we're done
            if(id > 0 && lookupUser(id, event))
                return;
            
            // require Vortex Plus for looking up guilds
            if(!vortex.getDatabase().premium.getPremiumInfo(event.getGuild()).level.isAtLeast(PremiumManager.Level.PLUS))
            {
                event.reply(PremiumManager.Level.PLUS.getRequirementMessage());
                return;
            }
            
            // if valid id, use widget, otherwise try invite code
            if(id > 0)
                lookupGuild(id, event);
            else
                lookupGuild(extractCode(event.getArgs()), event);
        });
    }
    
    private String extractCode(String args)
    {
        return args.substring(args.indexOf("/")+1);
    }
    
    private boolean lookupUser(long userId, CommandEvent event)
    {
        User u = vortex.getJDA().getUserById(userId);
        if(u==null) try
        {
            u = vortex.getJDA().retrieveUserById(userId).complete(false);
        }
        catch(RateLimitedException ratelimited)
        {
            event.reactWarning();
            return true;
        }
        catch(Exception ignore) {}
        if(u == null)
            return false;
        String text = (u.isBot() ? BOT_EMOJI : USER_EMOJI) + " Information about **" + u.getName() + "**#" + u.getDiscriminator() + ":";
        EmbedBuilder eb = new EmbedBuilder();
        eb.setThumbnail(u.getEffectiveAvatarUrl());
        String str = LINESTART + "Discord ID: **" + u.getId() + "**";
        if(u.getAvatarId() != null && u.getAvatarId().startsWith("a_"))
            str+= " <:nitro:314068430611415041>";
        str += "\n" + LINESTART + "Account Creation: **" + TimeUtil.getDateTimeString(u.getTimeCreated()) + "**";
        eb.setDescription(str);
        event.reply(new MessageBuilder().append(FormatUtil.filterEveryone(text)).setEmbeds(eb.build()).build());
        return true;
    }
    
    private void lookupGuild(long guildId, CommandEvent event)
    {
        Invite invite = null;
        Widget widget = null;
        try
        {
            widget = WidgetUtil.getWidget(guildId);
        }
        catch(RateLimitedException ratelimited)
        {
            event.reactWarning();
            return;
        }
        catch(Exception ignore) {}
        if(widget != null && widget.isAvailable() && widget.getInviteCode() != null)
        {
            try
            {
                invite = Invite.resolve(event.getJDA(), widget.getInviteCode(), true).complete(false);
            }
            catch(Exception ignore) {}
        }
        event.reply(constructMessage(invite, widget));
    }
    
    private void lookupGuild(String inviteCode, CommandEvent event)
    {
        Invite invite = null;
        Widget widget = null;
        try
        {
            invite = Invite.resolve(event.getJDA(), inviteCode, true).complete(false);
        }
        catch(RateLimitedException ratelimited)
        {
            event.reactWarning();
            return;
        }
        catch(Exception ignore) {}
        if(invite != null && invite.getType() == Invite.InviteType.GUILD)
        {
            try
            {
                //noinspection ConstantConditions
                widget = WidgetUtil.getWidget(invite.getGuild().getIdLong());
            }
            catch(Exception ignore) {}
        }
        event.reply(constructMessage(invite, widget));
    }
    
    private Message constructMessage(Invite invite, Widget widget)
    {
        String gname;
        long gid;
        int users;
        if(invite == null)
        {
            if(widget == null)
                return new MessageBuilder(Constants.ERROR + " No users, guilds, or invites found.").build();
            else if (!widget.isAvailable())
                return new MessageBuilder(Constants.SUCCESS + " Guild with ID `").append(widget.getId()).append("` found; no further information found.").build();
            gid = widget.getIdLong();
            gname = widget.getName();
            users = widget.getMembers().size();
        }
        else
        {
            gid = Objects.requireNonNull(invite.getGuild()).getIdLong();
            gname = invite.getGuild().getName();
            users = invite.getGuild().getOnlineCount();
        }
        
        String text = GUILD_EMOJI + " Information about **" + gname + "**:";
        EmbedBuilder eb = new EmbedBuilder();
        eb.appendDescription(LINESTART + "ID: **" + gid + "**"
                + "\n" + LINESTART + "Creation: **" + TimeUtil.getTimeCreated(gid).format(DateTimeFormatter.RFC_1123_DATE_TIME) + "**"
                + "\n" + LINESTART + "Users: " + (users == -1 ? "N/A" : "**" + users + "** online")
                + (widget != null && widget.isAvailable() ? "\n" + LINESTART + "Channels: **" + widget.getVoiceChannels().size() + "** voice" : ""));
        if(invite != null && invite.getType() == Invite.InviteType.GUILD)
        {
            eb.setThumbnail(invite.getGuild().getIconUrl());
            eb.setImage(invite.getGuild().getSplashId() == null ? null : invite.getGuild().getSplashUrl() + "?size=1024");
            eb.addField("Invite Info", LINESTART + "Invite: **" + invite.getCode() + "**"
                    + "\n" + LINESTART + "Channel: **" + (Objects.requireNonNull(invite.getChannel()).getType() == ChannelType.TEXT ? "#" : "") + invite.getChannel().getName() + "** (ID:" +invite.getChannel().getId() + ")"
                    + "\n" + LINESTART + "Inviter: " + (invite.getInviter() == null ? "N/A" : FormatUtil.formatFullUser(invite.getInviter()))
                    + (invite.getGuild().getSplashId() == null ? "" : "\n" + LINESTART + "Splash: "), false);
        }
        return new MessageBuilder().append(FormatUtil.filterEveryone(text)).setEmbeds(eb.build()).build();
    }
}