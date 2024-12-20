package fr.MusicBot;

import fr.MusicBot.Listeners.MessageListener;
import fr.MusicBot.Listeners.SlashCommandAutoComplete;
import fr.MusicBot.Listeners.SlashCommandListener;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.exceptions.RateLimitedException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;

import javax.security.auth.login.LoginException;

import static net.dv8tion.jda.api.interactions.commands.OptionType.BOOLEAN;
import static net.dv8tion.jda.api.interactions.commands.OptionType.STRING;

public class JMusicBot extends ListenerAdapter {
    public static void main(String[] args) throws IllegalArgumentException, LoginException, RateLimitedException {
        //Load discord token
        Dotenv dotenv = Dotenv.load();
        String token = dotenv.get("DISCORD_TOKEN");
        if (token == null || token.isEmpty()) {
            System.out.println("Token non trouvé dans le fichier .env");
            return;
        }


        JDA jda = JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .enableIntents(GatewayIntent.GUILD_MESSAGES)
                .addEventListeners(new SlashCommandListener())
                .addEventListeners(new SlashCommandAutoComplete())
                .addEventListeners(new MessageListener())
                .build();

        CommandListUpdateAction commands = jda.updateCommands();

        commands.addCommands(
                Commands.slash("play", "Joue une musique depuis la library")
                        .addOption(STRING, "musique", "Nom de la musique à jouer", true, true)
                        .addOption(BOOLEAN, "loop", "Activer la répétition de la musique", false),
                Commands.slash("stop", "Arrête la musique et déconnecte le bot"),
                Commands.slash("loop", "Active ou désactive la répétition de la musique en cours"),
                Commands.slash("download", "Télécharge une musique depuis un URL Youtube")
                        .addOption(STRING, "url", "URL de la vidéo Youtube", true)
        );

        commands.queue();
    }
}