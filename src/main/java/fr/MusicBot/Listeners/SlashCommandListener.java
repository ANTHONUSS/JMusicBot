package fr.MusicBot.Listeners;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import fr.MusicBot.Audio.AudioPlayerSendHandler;
import fr.MusicBot.Audio.TrackScheduler;
import fr.MusicBot.LOGs;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;

public class SlashCommandListener extends ListenerAdapter {
    private final AudioPlayerManager playerManager;
    private TrackScheduler currentTrackScheduler;
    private boolean isLooping = false;

    public SlashCommandListener() {
        this.playerManager = new DefaultAudioPlayerManager();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "play" -> {
                String selectedMusic = event.getOption("musique", interactionOption -> interactionOption.getAsString());

                boolean loopEnabled = event.getOption("loop") != null && event.getOption("loop").getAsBoolean();

                play(event, selectedMusic, loopEnabled);
            }
            case "stop" -> {
                stop(event);
            }
            case "loop" -> {
                toggleLoop(event);
            }
            case "download" -> {
                String choosedURL = event.getOption("url", interactionOption -> interactionOption.getAsString());

                download(event, choosedURL);
            }
            default -> {
                return;
            }
        }
    }

    public void play(SlashCommandInteractionEvent event, String selectedMusic, boolean loopEnabled) {
        if (event.getMember().getVoiceState().getChannel() == null) {
            event.reply("Vous n'êtes connecté à aucun salon vocal.").queue();
            return;
        }
        VoiceChannel channel = (VoiceChannel) event.getMember().getVoiceState().getChannel();

        Guild guild = event.getGuild();
        if (guild == null) {
            event.reply("Cette commande doit être exécutée dans un serveur.").queue();
            return;
        }

        File musicFile = new File("Music/" + selectedMusic);
        if (!musicFile.exists()) {
            event.reply("Le fichier sélectionné n'existe pas : " + selectedMusic).queue();
            return;
        }

        playerManager.registerSourceManager(new LocalAudioSourceManager());
        AudioSourceManagers.registerRemoteSources(playerManager);

        AudioManager audioManager = guild.getAudioManager();
        AudioPlayer audioPlayer = playerManager.createPlayer();
        currentTrackScheduler = new TrackScheduler(audioPlayer, audioManager);

        currentTrackScheduler.setLooping(loopEnabled);
        isLooping = loopEnabled;

        audioPlayer.addListener(currentTrackScheduler);

        playerManager.loadItem(musicFile.getAbsolutePath(), new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack audioTrack) {
                event.reply("Lecture de **" + musicFile.getName() + "**"
                        + (loopEnabled ? " (en boucle)" : "")).queue();
                audioPlayer.playTrack(audioTrack);
                LOGs.sendLog("Musique " + musicFile.getName() + " lue." + (loopEnabled ? " (en boucle)" : ""), 2);
            }

            @Override
            public void playlistLoaded(AudioPlaylist audioPlaylist) {
                trackLoaded(audioPlaylist.getTracks().get(0));
            }

            @Override
            public void noMatches() {
                event.reply("La piste n'a pas pu être trouvée ou est incompatible.").queue();
            }

            @Override
            public void loadFailed(FriendlyException e) {
                event.reply("Erreur lors du chargement de l'audio : " + e.getMessage()).queue();
            }
        });

        audioManager.setSendingHandler(new AudioPlayerSendHandler(audioPlayer));
        audioManager.openAudioConnection(channel);
    }

    public void stop(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        AudioManager audioManager = guild.getAudioManager();
        if (audioManager.isConnected()) {
            audioManager.closeAudioConnection();
            event.reply("Musique arrêtée.").queue();
            LOGs.sendLog("Musique arrêtée.", 3);
        } else {
            event.reply("Aucune musique en cours.").queue();
        }
    }

    public void toggleLoop(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        AudioManager audioManager = guild.getAudioManager();
        if (!audioManager.isConnected()) {
            event.reply("Aucune musique n'est en cours.").queue();
            return;
        }

        if (currentTrackScheduler == null) {
            event.reply("impossible de récupérer le gestionnaire de piste, Utilisez `/play`").queue();
            return;
        }

        isLooping = !isLooping;
        currentTrackScheduler.setLooping(isLooping);

        event.reply(isLooping
                ? "Loop activé."
                : "Loop désactivé.").queue();

        LOGs.sendLog(isLooping
                ? "Loop activé."
                : "Loop désactivé.",4);
    }

    public void download(SlashCommandInteractionEvent event, String url) {
        File musicFolder = new File("Music");
        if (!musicFolder.exists()) {
            event.reply("Une erreur est survenue lors du téléchargement : Music directory does not exist.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        event.deferReply(true).queue();

        ProcessBuilder processBuilder = new ProcessBuilder(
                "yt-dlp.exe",
                "-x",
                "--audio-format", "mp3",
                "--no-playlist",
                "-o", "Music/%(title)s.%(ext)s",
                url
        );

        event.getHook().sendMessage("Téléchargement en cours...")
                .setEphemeral(true)
                .queue();
        LOGs.sendLog("Musique en cours de téléchargement", 1);
        try {
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String line;

            String musicName = "";

            while ((line = reader.readLine()) != null) {
                System.out.println("yt-dlp: " + line);

                if (line.contains("[ExtractAudio]") && line.contains("Destination")) {
                    musicName = line.split("Destination: ")[1].trim();
                    musicName = new File(musicName).getName();

                    musicName = musicName.substring(0, musicName.lastIndexOf("."));
                }
            }

            while ((line = errorReader.readLine()) != null) {
                System.err.println("yt-dlp: " + line);
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                event.getHook().sendMessage("La musique **"+musicName+"** à été téléchargée.")
                        .queue();
                LOGs.sendLog("Musique téléchargée : "+musicName, 1);
            } else {
                event.getHook().sendMessage("Une erreur est survenue lors du téléchargement")
                        .setEphemeral(true)
                        .queue();
            }
        } catch (Exception e) {
            event.getHook().sendMessage("Une erreur est survenue lors du téléchargement : " + e.getMessage())
                        .setEphemeral(true)
                        .queue();
        }


    }

    public static List<String> listMusicFiles() {
        File musicFolder = new File("Music");

        if (!musicFolder.exists() || !musicFolder.isDirectory()) {
            return Collections.emptyList(); // Retourne une liste vide si le dossier n'existe pas ou n'est pas valide
        }

        return Arrays.stream(musicFolder.listFiles())
                .filter(file -> !file.isDirectory()) // Exclure les dossiers
                .map(File::getName) // Filtrer uniquement les fichiers '.mp3'
                .filter(name -> name.endsWith(".mp3")) // Extraire les noms de fichiers
                .toList();
    }
}
