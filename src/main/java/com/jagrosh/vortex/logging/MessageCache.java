/*
 * Copyright 2017 John Grosh (jagrosh).
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

import com.jagrosh.vortex.utils.FixedCache;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class MessageCache
{
    private final static int SIZE = 1000;
    private final HashMap<Long,FixedCache<Long,CachedMessage>> cache = new HashMap<>();
    
    public CachedMessage putMessage(Message m)
    {
        if(!cache.containsKey(m.getGuild().getIdLong()))
            cache.put(m.getGuild().getIdLong(), new FixedCache<>(SIZE));
        return cache.get(m.getGuild().getIdLong()).put(m.getIdLong(), new CachedMessage(m));
    }
    
    public CachedMessage pullMessage(Guild guild, long messageId)
    {
        if(!cache.containsKey(guild.getIdLong()))
            return null;
        return cache.get(guild.getIdLong()).pull(messageId);
    }
    
    public List<CachedMessage> getMessages(Guild guild, Predicate<CachedMessage> predicate)
    {
        if(!cache.containsKey(guild.getIdLong()))
            return Collections.emptyList();
        return cache.get(guild.getIdLong()).getValues().stream().filter(predicate).collect(Collectors.toList());
    }
    
    public static class CachedMessage implements ISnowflake
    {
        private final String content, username, discriminator;
        private final long id, author, channel, guild;
        private final List<Attachment> attachments;
        
        private CachedMessage(Message message)
        {
            content = message.getContentRaw();
            id = message.getIdLong();
            author = message.getAuthor().getIdLong();
            username = message.getAuthor().getName();
            discriminator = message.getAuthor().getDiscriminator();
            channel = message.getChannel().getIdLong();
            guild = message.isFromType(ChannelType.TEXT) ? message.getGuild().getIdLong() : 0L;
            attachments = message.getAttachments();
        }
        
        public String getContentRaw()
        {
            return content;
        }
        
        public List<Attachment> getAttachments()
        {
            return attachments;
        }
        
        public User getAuthor(JDA jda)
        {
            return jda.getUserById(author);
        }
        
        public String getUsername()
        {
            return username;
        }
        
        public String getDiscriminator()
        {
            return discriminator;
        }
        
        public long getAuthorId()
        {
            return author;
        }
        
        public TextChannel getTextChannel(JDA jda)
        {
            if (guild == 0L)
                return null;
            Guild g = jda.getGuildById(guild);
            if (g == null)
                return null;
            return g.getTextChannelById(channel);
        }
        
        public long getTextChannelId()
        {
            return channel;
        }
        
        public Guild getGuild(JDA jda)
        {
            if (guild == 0L)
                return null;
            return jda.getGuildById(guild);
        }

        @Override
        public long getIdLong()
        {
            return id;
        }
    }
}
