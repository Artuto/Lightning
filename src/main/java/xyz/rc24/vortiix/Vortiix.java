package xyz.rc24.vortiix;

import com.jagrosh.vortex.Vortex;
import com.typesafe.config.Config;
import xyz.rc24.vortiix.modmail.ModMail;

public class Vortiix
{
    private final ModMail modMail;

    public Vortiix(Vortex vortex, Config config)
    {
        this.modMail = new ModMail(vortex, config);
    }

    public ModMail getModMail()
    {
        return modMail;
    }
}
