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
package com.jagrosh.vortex.commands.owner;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.database.Database;
import com.jagrosh.vortex.utils.OtherUtil;
import net.dv8tion.jda.api.entities.Guild;
import org.json.JSONObject;

/**
 *
 * @author Michaili K (mysteriouscursor+git@protonmail.com)
 */
public class ImportCmd extends Command
{
    private final Vortex vortex;

    public ImportCmd(Vortex vortex)
    {
        this.vortex = vortex;
        this.name = "import";
        this.help = "imports settings";
        this.ownerCommand = true;
        this.guildOnly = true;
        this.hidden = true;
    }

    @Override
    protected void execute(CommandEvent event)
    {
        if(event.getMessage().getAttachments().isEmpty())
        {
            event.replyError("Attachment required");
            return;
        }

        Guild g = event.getGuild();
        Database db = vortex.getDatabase();
        OtherUtil.downloadAttachment(event.getMessage().getAttachments().get(0), attachment ->
        {
            try
            {
                JSONObject data = new JSONObject(attachment);

                db.automod.setSettingsJson(g, data.getJSONObject("automod"));
                db.settings.setSettingsJson(g, data.getJSONObject("settings"));
                db.ignores.setIgnoresJson(g, data.getJSONArray("ignores"));
                db.strikes.setAllStrikesJson(g, data.getJSONObject("strikes"));
                db.actions.setAllPunishmentsJson(g, data.getJSONObject("punishments"));
                db.tempmutes.setAllMutesJson(g, data.getJSONObject("tempmutes"));
                db.tempbans.setAllBansJson(g, data.getJSONObject("tempbans"));
                db.inviteWhitelist.setWhiteListJson(g, data.getJSONArray("inviteWhitelist"));
                db.filters.setFiltersJson(g, data.getJSONObject("filters"));
                if(data.has("usernameFilters"))
                    db.usernameFilters.setFiltersJson(g, data.getJSONObject("usernameFilters"));

                event.replySuccess("Import Complete!");
            }
            catch(Exception ex)
            {
                event.replyError("Error: "+ex+"\nat "+ex.getStackTrace()[0]);
            }
        });
    }
}
