package fr.MusicBot;

import java.io.File;
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
            event.reply("impossible de récupérer le festionnaire de piste, Utilisez `/play`").queue();
            return;
        }

        isLooping = !isLooping;
        currentTrackScheduler.setLooping(isLooping);

        event.reply(isLooping
                ? "Loop activé."
                : "Loop désactivé.").queue();
    }

    public void download(SlashCommandInteractionEvent event, String url) {
        File musicFolder = new File("Music");
        if (!musicFolder.exists()) {
            System.out.println("Le dossier \"Music\" n'existe pas");
            return;
        }

        event.deferReply(true).queue();

        new Thread(() -> {
            // Vérification si l'URL est une playlist
            ProcessBuilder playlistCheckProcess = new ProcessBuilder("yt-dlp.exe", "--flat-playlist", url);
            try {
                System.out.println("Vérification de l'URL pour Playlist");
                Process checkProcess = playlistCheckProcess.start();
                int exitCode = checkProcess.waitFor();

                if (exitCode == 0) {
                    // L'URL est une playlist, refuser le téléchargement
                    event.getHook().sendMessage("❌ L'URL fournie est une playlist. Téléchargement refusé.")
                            .setEphemeral(true)
                            .queue();
                    return;
                }
            } catch (Exception e) {
                // Erreur lors de la vérification de la playlist
                event.getHook().sendMessage("❌ Erreur lors de la vérification de l'URL : " + e.getMessage())
                        .setEphemeral(true)
                        .queue();
                return;
            }

            // Construire la commande pour télécharger uniquement l'audio en MP3
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "yt-dlp.exe",
                    "-x",                             // Extraire l'audio uniquement
                    "--audio-format", "mp3",          // Format MP3
                    "--no-playlist",                  // Eviter de télécharger toute une playlist connectée à une vidéo
                    "-o", "Music/%(title)s.%(ext)s",  // Sauvegarde dans le dossier Music
                    url                               // URL de la vidéo
            );

            try {
                event.getHook().sendMessage("⏳ Téléchargement en cours...")
                        .setEphemeral(true)
                        .queue();

                System.out.println("Lancement du téléchargement...");
                // Lancer le téléchargement avec yt-dlp
                Process process = processBuilder.start();

                // Attendre que le téléchargement se termine
                int exitCode = process.waitFor();

                if (exitCode == 0) {
                    // Télécharger avec succès
                    event.getHook().sendMessage("✅ Téléchargement terminé avec succès. Le fichier est sauvegardé dans le dossier **Music/**.")
                            .setEphemeral(true)
                            .queue();
                } else {
                    // Téléchargement échoué
                    event.getHook().sendMessage("❌ Une erreur est survenue lors du téléchargement.")
                            .setEphemeral(true)
                            .queue();
                }
            } catch (Exception e) {
                // Erreur lors de l'exécution de yt-dlp
                event.getHook().sendMessage("❌ Une erreur est survenue pendant l'exécution de la commande : " + e.getMessage())
                        .setEphemeral(true)
                        .queue();
            }
        }).start();
    }

    public static List<String> listMusicFiles() {
        File musicFolder = new File("Music");

        if (!musicFolder.exists() || !musicFolder.isDirectory()) {
            return Collections.emptyList(); // Retourne une liste vide si le dossier n'existe pas ou n'est pas valide
        }

        return Arrays.stream(musicFolder.listFiles())
                .filter(file -> !file.isDirectory()) // Exclure les dossiers
                .filter(file -> file.getName().endsWith(".mp3")) // Filtrer uniquement les fichiers '.mp3'
                .map(File::getName) // Extraire les noms de fichiers
                .toList();
    }
}
