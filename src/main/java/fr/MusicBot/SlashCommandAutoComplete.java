package fr.MusicBot;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;

import java.util.List;

public class SlashCommandAutoComplete extends ListenerAdapter {
    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        String focusedOption = event.getFocusedOption().getName();

        if (focusedOption.equals("musique")) {
            List<String> musicFiles = SlashCommandListener.listMusicFiles();

            List<Command.Choice> choices = musicFiles.stream()
                    .map(name -> new Command.Choice(name, name))
                    .limit(25)
                    .toList();

            event.replyChoices(choices).queue();
        }
    }
}
