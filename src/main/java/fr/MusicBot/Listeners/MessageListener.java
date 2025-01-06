package fr.MusicBot.Listeners;

import fr.MusicBot.LOGs;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class MessageListener extends ListenerAdapter {

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        String message = event.getMessage().getContentRaw();

        if (message.toLowerCase().matches(".*\\bquoi\\s?\\p{Punct}*$")) {
            event.getMessage().reply("feur").queue();
            LOGs.sendLog("feur envoyé"
                    + "\nUser : @" + event.getAuthor().getName()
                    + "\nServeur : " + event.getGuild().getName()
                    + "\nSalon : #" + event.getChannel().getName(),
                    LOGs.LogType.FEUR);
        }
    }
}
