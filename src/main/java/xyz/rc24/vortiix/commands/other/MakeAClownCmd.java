package xyz.rc24.vortiix.commands.other;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.vortex.Vortex;
import net.dv8tion.jda.api.Permission;

import javax.imageio.ImageIO;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

import static java.util.Calendar.DAY_OF_MONTH;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.YEAR;

public class MakeAClownCmd extends Command
{
    public MakeAClownCmd()
    {
        this.name = "makeaclown";
        this.arguments = "<username>";
        this.help = "Generate a clown";
        this.userPermissions = new Permission[]{Permission.BAN_MEMBERS};
        this.category = new Category("RiiSpecial");
        this.guildOnly = true;
        this.cooldown = 3;
        this.cooldownScope = CooldownScope.GUILD;
    }

    @Override
    protected void execute(CommandEvent event)
    {
        if(event.getArgs().isEmpty())
        {
            event.reactError();
            return;
        }

        event.getChannel().sendTyping().queue();

        try(InputStream resource = Vortex.class.getResourceAsStream("/clown.png"))
        {
            BufferedImage image = ImageIO.read(resource);
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            Graphics g = image.getGraphics();
            g.setFont(g.getFont().deriveFont(30f));
            g.drawString(event.getArgs(), 715, 744);
            g.drawString(getDate(), 755, 806);
            g.dispose();

            ImageIO.write(image, "png", out);

            event.getChannel().sendFile(new ByteArrayInputStream(out.toByteArray()), "clown.png").queue();
        }
        catch(IOException e)
        {
            event.replyError("Error!");
            e.printStackTrace();
        }
    }

    private String getDate()
    {
        Calendar calendar = Calendar.getInstance();
        return calendar.get(DAY_OF_MONTH) + "/" + (calendar.get(MONTH) + 1) + "/" + calendar.get(YEAR);
    }
}
