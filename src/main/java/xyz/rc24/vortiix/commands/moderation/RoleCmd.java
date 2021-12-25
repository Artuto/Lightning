package xyz.rc24.vortiix.commands.moderation;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.utils.FinderUtil;
import com.jagrosh.vortex.commands.CommandExceptionListener;
import com.jagrosh.vortex.utils.FormatUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.util.EnumSet;
import java.util.List;

public class RoleCmd extends Command
{
    public RoleCmd()
    {
        this.name = "role";
        this.help = "give or take a role from a member";
        this.guildOnly = true;
        this.userPermissions = new Permission[]{Permission.MODERATE_MEMBERS};
        this.botPermissions = new Permission[]{Permission.MANAGE_ROLES};
        this.children = new Command[]{new GiveCmd(), new TakeCmd()};
    }

    @Override
    protected void execute(CommandEvent event)
    {
        event.replyWarning("Valid subcommands: `give`, `take`");
    }

    private static class GiveCmd extends Command
    {
        public GiveCmd()
        {
            this.name = "give";
            this.help = "give a role to a member";
            this.arguments = "<member> <role>";
            this.guildOnly = true;
            this.userPermissions = new Permission[]{Permission.MODERATE_MEMBERS};
            this.botPermissions = new Permission[]{Permission.MANAGE_ROLES};
        }

        @Override
        protected void execute(CommandEvent event)
        {
            String[] parts = event.getArgs().split("to", 2);
            if(parts.length < 2)
            {
                event.reactError();
                return;
            }

            Role role;
            List<Role> found = FinderUtil.findRoles(parts[0].trim(), event.getGuild());
            if(found.isEmpty())
            {
                event.replyError("I couldn't find the role you were looking for!");
                return;
            }
            else if(found.size()>1)
            {
                event.replyWarning(FormatUtil.listOfRoles(found, event.getArgs()));
                return;
            }
            else
                role = found.get(0);

            Member member;
            List<Member> mFound = FinderUtil.findMembers(parts[1].trim(), event.getGuild());
            if(mFound.isEmpty())
            {
                event.replyError("I couldn't find the member you were looking for!");
                return;
            }
            else if(mFound.size()>1)
            {
                event.replyWarning(FormatUtil.listOfMember(mFound, parts[1]));
                return;
            }
            else
                member = mFound.get(0);

            if(!event.getSelfMember().canInteract(role))
                throw new CommandExceptionListener.CommandErrorException("I cannot interact with that role!");

            if(!event.getMember().canInteract(role))
                throw new CommandExceptionListener.CommandErrorException("You cannot interact with that role!");

            if(!event.getSelfMember().canInteract(member))
                throw new CommandExceptionListener.CommandErrorException("I cannot interact with that member!");

            if(!event.getMember().canInteract(member))
                throw new CommandExceptionListener.CommandErrorException("You cannot interact with that member!");

            for(Permission perm : role.getPermissions())
            {
                if(ADMINISTRATIVE_PERMS.contains(perm))
                {
                    event.reactError();
                    return;
                }
            }

            if(member.getRoles().contains(role))
                throw new CommandExceptionListener.CommandWarningException("That member already has the specified role!");

            event.getGuild().addRoleToMember(member, role)
                    .reason(event.getAuthor().getAsTag() + ": Role give")
                    .queue(s -> event.reactSuccess(), e ->
                    {
                        event.replyError("Failed to give role!");
                        e.printStackTrace();
                    });
        }
    }

    private static class TakeCmd extends Command
    {
        public TakeCmd()
        {
            this.name = "take";
            this.help = "take a role from a member";
            this.arguments = "<member> <role>";
            this.guildOnly = true;
            this.userPermissions = new Permission[]{Permission.MODERATE_MEMBERS};
            this.botPermissions = new Permission[]{Permission.MANAGE_ROLES};
        }

        @Override
        protected void execute(CommandEvent event)
        {
            String[] parts = event.getArgs().split("from", 2);
            if(parts.length < 2)
            {
                event.reactError();
                return;
            }

            Role role;
            List<Role> found = FinderUtil.findRoles(parts[0].trim(), event.getGuild());
            if(found.isEmpty())
            {
                event.replyError("I couldn't find the role you were looking for!");
                return;
            }
            else if(found.size()>1)
            {
                event.replyWarning(FormatUtil.listOfRoles(found, event.getArgs()));
                return;
            }
            else
                role = found.get(0);

            Member member;
            List<Member> mFound = FinderUtil.findMembers(parts[1].trim(), event.getGuild());
            if(mFound.isEmpty())
            {
                event.replyError("I couldn't find the member you were looking for!");
                return;
            }
            else if(mFound.size()>1)
            {
                event.replyWarning(FormatUtil.listOfMember(mFound, parts[1]));
                return;
            }
            else
                member = mFound.get(0);

            if(!event.getSelfMember().canInteract(role))
                throw new CommandExceptionListener.CommandErrorException("I cannot interact with that role!");

            if(!event.getMember().canInteract(role))
                throw new CommandExceptionListener.CommandErrorException("You cannot interact with that role!");

            if(!event.getSelfMember().canInteract(member))
                throw new CommandExceptionListener.CommandErrorException("I cannot interact with that member!");

            if(!event.getMember().canInteract(member))
                throw new CommandExceptionListener.CommandErrorException("You cannot interact with that member!");

            if(!member.getRoles().contains(role))
                throw new CommandExceptionListener.CommandWarningException("That member does not have the specified role!");

            event.getGuild().removeRoleFromMember(member, role)
                    .reason(event.getAuthor().getAsTag() + ": Role take")
                    .queue(s -> event.reactSuccess(), e ->
                    {
                        event.replyError("Failed to take role!");
                        e.printStackTrace();
                    });
        }
    }

    private static final EnumSet<Permission> ADMINISTRATIVE_PERMS = EnumSet.of(Permission.KICK_MEMBERS,
            Permission.BAN_MEMBERS, Permission.ADMINISTRATOR, Permission.MANAGE_CHANNEL,
            Permission.MANAGE_SERVER, Permission.VIEW_AUDIT_LOGS, Permission.VIEW_GUILD_INSIGHTS,
            Permission.MESSAGE_MANAGE, Permission.MESSAGE_MENTION_EVERYONE, Permission.MANAGE_THREADS,
            Permission.VOICE_MUTE_OTHERS, Permission.VOICE_DEAF_OTHERS, Permission.VOICE_MOVE_OTHERS,
            Permission.NICKNAME_MANAGE, Permission.MODERATE_MEMBERS, Permission.MANAGE_ROLES,
            Permission.MANAGE_PERMISSIONS, Permission.MANAGE_WEBHOOKS, Permission.MANAGE_EMOTES);
}
