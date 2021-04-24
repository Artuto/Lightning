package xyz.rc24.vortiix;

import com.typesafe.config.Config;
import xyz.rc24.vortiix.modmail.ModMail;

public class Vortiix
{
    private final ModMail modMail;

    public Vortiix(Config config)
    {
        this.modMail = new ModMail(config);
    }

    public ModMail getModMail()
    {
        return modMail;
    }
}
