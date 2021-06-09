package com.jagrosh.vortex.database.managers;

import com.jagrosh.vortex.automod.Filter;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.json.JSONObject;

import java.util.List;

public interface GenericFilterManager
{
    List<Filter> getFilters(Guild guild);

    MessageEmbed.Field getFiltersDisplay(Guild guild);

    JSONObject getFiltersJson(Guild guild);

    void setFiltersJson(Guild guild, JSONObject json);

    boolean addFilter(Guild guild, Filter filter);

    Filter deleteFilter(Guild guild, String name);

    void deleteAllFilters(long guildId);
}
