package xyz.artuto.lightning.commands.automod;

import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.automod.FilterCmd;
import com.jagrosh.vortex.database.managers.GenericFilterManager;

public class UsernameFilterCmd extends FilterCmd
{
    public UsernameFilterCmd(Vortex vortex)
    {
        super(vortex, "usernamefilter", "namefilter");
        this.help = "modifies the username filter";
    }

    @Override
    protected GenericFilterManager getFilterManager()
    {
        return vortex.getDatabase().usernameFilters;
    }
}
