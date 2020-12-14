package xyz.rc24.vortiix;

import com.jagrosh.vortex.Vortex;

public class Vortiix
{
    private final ModMail modMail;

    public Vortiix(Vortex vortex)
    {
        this.modMail = new ModMail(vortex);
    }

    public ModMail getModMail()
    {
        return modMail;
    }
}
