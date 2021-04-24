package xyz.rc24.vortiix.commands.moderation;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.ModCommand;
import com.jagrosh.vortex.utils.ArgsUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import xyz.rc24.vortiix.modmail.ModMailManager;
import xyz.rc24.vortiix.modmail.ModMailThread;

public class CloseThreadCmd extends ModCommand
{
    private final ModMailManager modMailManager;

    public CloseThreadCmd(Vortex vortex)
    {
        super(vortex, Permission.BAN_MEMBERS);
        this.name = "closethread";
        this.arguments = "<user id>";
        this.help = "closes a user's mod mail thread";
        this.modMailManager = vortex.getVortiix().getModMail().getManager();
    }

    @Override
    protected void execute(CommandEvent event)
    {
        ArgsUtil.ResolvedArgs args = ArgsUtil.resolve(event.getArgs(), event.getGuild());
        if(args.isEmpty() || args.reason.isEmpty())
        {
            event.replyError("Please include one user to reply (@mention or ID) and the message!");
            return;
        }

        if(args.members.isEmpty())
        {
            event.replyError("The user you mentioned is not on this server");
            return;
        }

        if(args.members.size() > 1)
        {
            event.replyError("You can only reply to one user at the time!");
            return;
        }

        for(Member member : args.members)
        {
            if(member.getUser().isBot())
            {
                event.replyError("I cannot DM bots!");
                return;
            }

            ModMailThread thread = modMailManager.getThreads().remove(member.getIdLong());
            thread.close(event, event.getJDA(), member);
        }
    }
}
