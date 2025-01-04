package fr.MusicBot.Listeners;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;

import java.util.List;

public class SlashCommandAutoCompleteListener extends ListenerAdapter {
    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        String focusedOption = event.getFocusedOption().getName();

        if (focusedOption.equals("musique")) {
            String userInput = event.getFocusedOption().getValue();
            List<String> musicFiles = SlashCommandListener.listMusicFiles();

            List<Command.Choice> choices = musicFiles.stream()
                    .filter(name -> name.toLowerCase().contains(userInput.toLowerCase()))
                    .map(name -> new Command.Choice(name, name))
                    .limit(25)
                    .toList();

            event.replyChoices(choices).queue();
        }
    }
}
