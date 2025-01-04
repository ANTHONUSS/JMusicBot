package fr.MusicBot.Listeners;

import fr.MusicBot.LOGs;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import static fr.MusicBot.Listeners.SlashCommandListener.*;

public class ButtonInteractionListener extends ListenerAdapter {
    public static SlashCommandInteractionEvent currentEvent;

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();
        int currentPage = 1;

        int totalPages = (int) Math.ceil((double) listMusicFiles().size() / 10);

        if (buttonId.startsWith("previous_page_")) {
            currentPage = Integer.parseInt(buttonId.replace("previous_page_", "")) - 1;
        } else if (buttonId.startsWith("next_page_")) {
            currentPage = Integer.parseInt(buttonId.replace("next_page_", "")) + 1;
        }


        String currentList = makeList(currentPage);
        if (currentPage <= 1) {
            currentPage = 1;

            event.editMessage(currentList)
                    .setActionRow(
                            Button.primary("previous_page_" + currentPage, "⬅️ Page précédente").asDisabled(),
                            Button.primary("next_page_" + currentPage, "➡️ Page suivante")
                    )
                    .queue();
        }
        if (currentPage >= totalPages) {
            currentPage = totalPages;

            event.editMessage(currentList)
                    .setActionRow(
                            Button.primary("previous_page_" + currentPage, "⬅️ Page précédente"),
                            Button.primary("next_page_" + currentPage, "➡️ Page suivante").asDisabled()
                    )
                    .queue();
        } else {
            event.editMessage(currentList)
                    .setActionRow(
                            Button.primary("previous_page_" + currentPage, "⬅️ Page précédente"),
                            Button.primary("next_page_" + currentPage, "➡️ Page suivante")
                    )
                    .queue();
        }


    }
}
