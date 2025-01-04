package fr.MusicBot.Listeners;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.managers.AudioManager;

public class SlashCommandListener extends ListenerAdapter {
    private final AudioPlayerManager playerManager;
    private TrackScheduler currentTrackScheduler;
    private boolean isLooping = false;
    public static boolean isPlaying = false;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    public static final List<AudioTrack> currentTrackList = new ArrayList<>();

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
            case "list" -> {
                list(event);
            }
            case "queue" -> {
                String selectedMusic = event.getOption("musique", interactionOption -> interactionOption.getAsString());

                queue(event, selectedMusic);
            }
        }
    }

    private void play(SlashCommandInteractionEvent event, String selectedMusic, boolean loopEnabled) {
        if (event.getMember().getVoiceState().getChannel() == null) {
            event.reply("Vous n'êtes connecté à aucun salon vocal.").queue();
            return;
        }
        VoiceChannel channel = (VoiceChannel) event.getMember().getVoiceState().getChannel();

        Guild guild = event.getGuild();

        selectedMusic += ".mp3";
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
        currentTrackScheduler.setCurrentTrackName(selectedMusic);
        currentTrackScheduler.setCurrentEvent(event);
        isLooping = loopEnabled;

        audioPlayer.addListener(currentTrackScheduler);

        String musicName = musicFile.getName().replace(".mp3", "");

        playerManager.loadItem(musicFile.getAbsolutePath(), new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack audioTrack) {
                event.reply("Lecture de **" + musicName + "**"
                        + (loopEnabled ? " (en boucle)" : "")).queue();

                currentTrackList.clear();
                currentTrackScheduler.setCurrentTrackIndex(0);
                currentTrackList.add(audioTrack);
                audioPlayer.playTrack(audioTrack);
                isPlaying=true;
                LOGs.sendLog("Musique jouée"
                        + "\nNom : " + musicName
                        + "\nServeur : " + event.getGuild().getName()
                        + "\nSalon : #" + event.getChannel().getName()
                        + "\nUser : @" + event.getUser().getName()
                        + "\nEn boucle : " + isLooping, 2);
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

    private void stop(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        AudioManager audioManager = guild.getAudioManager();
        if (audioManager.isConnected()) {
            audioManager.closeAudioConnection();
            isPlaying = false;
            event.reply("Musique arrêtée.").queue();
            LOGs.sendLog("Musique arrêtée"
                            + "\nServeur : " + event.getGuild().getName()
                            + "\nSalon : #" + event.getChannel().getName()
                            + "\nUser : " + event.getUser().getName(),
                    3);
        } else {
            event.reply("Aucune musique en cours.").queue();
        }
    }

    private void toggleLoop(SlashCommandInteractionEvent event) {
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

        if (currentTrackScheduler.getCurrentTrackName() == null || currentTrackScheduler.getCurrentTrackName().isEmpty()) {
            currentTrackScheduler.setCurrentTrackName(currentTrackScheduler.audioPlayer.getPlayingTrack().getInfo().title);
        }
        isLooping = !isLooping;
        currentTrackScheduler.setLooping(isLooping);
        currentTrackScheduler.setCurrentEvent(event);

        event.reply(isLooping
                ? "Loop activé."
                : "Loop désactivé.").queue();

        LOGs.sendLog((isLooping ? "Loop activé" : "Loop désactivé")
                        + "\nNom : " + currentTrackScheduler.getCurrentTrackName()
                        + "\nServeur : " + event.getGuild().getName()
                        + "\nSalon : #" + event.getChannel().getName()
                        + "\nUser : @" + event.getUser().getName(),
                4);
    }

    private void download(SlashCommandInteractionEvent event, String url) {
        File musicFolder = new File("Music");
        if (!musicFolder.exists()) {
            event.reply("Une erreur est survenue lors du téléchargement : Music directory does not exist.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        event.deferReply(true).queue();

        executorService.submit(() -> {

            String musicName = null;

            try {
                ProcessBuilder titleProcessBuilder = new ProcessBuilder(
                        "yt-dlp.exe",
                        "--get-title",
                        url
                );
                Process titleProcess = titleProcessBuilder.start();

                BufferedReader titleReader = new BufferedReader(new InputStreamReader(titleProcess.getInputStream()));
                musicName = titleReader.readLine();
                titleProcess.waitFor();

                if (musicName == null || musicName.isEmpty())
                    musicName = "Titre inconnu";
            } catch (Exception e) {
                event.getHook().sendMessage("Impossible de récupérer le nom de la musique.")
                        .setEphemeral(true)
                        .queue();
                return;
            }

            event.getHook().sendMessage("Téléchargement de **" + musicName + "** en cours...")
                    .setEphemeral(true)
                    .queue();
            LOGs.sendLog("Musique " + musicName + " en cours de téléchargement par @" + event.getUser().getName(), 1);


            ProcessBuilder processBuilder = new ProcessBuilder(
                    "yt-dlp.exe",
                    "-x",
                    "--audio-format", "mp3",
                    "--no-playlist",
                    "-o", "Music/%(title)s.%(ext)s",
                    url
            );

            try {
                Process process = processBuilder.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                String line;

                while ((line = reader.readLine()) != null) {
                    LOGs.sendLog(line, 1);
                }

                while ((line = errorReader.readLine()) != null) {
                    System.err.println("yt-dlp: " + line);
                }

                int exitCode = process.waitFor();

                if (exitCode == 0) {
                    event.getHook().deleteOriginal().queue();
                    event.getChannel().sendMessage("La musique **" + musicName + "** à été téléchargée.")
                            .queue();
                    LOGs.sendLog("Musique téléchargée"
                            + "\nNom : " + musicName
                            + "\nUser : " + event.getUser().getName(), 1);
                } else {
                    event.getHook().editOriginal("Une erreur est survenue lors du téléchargement")
                            .queue();
                }
            } catch (Exception e) {
                event.getHook().editOriginal("Une erreur est survenue lors du téléchargement : " + e.getMessage())
                        .queue();
            }

        });

    }

    private static void list(SlashCommandInteractionEvent event){
        ButtonInteractionListener.currentEvent = event;

        event.reply(makeList(1))
                .addActionRow(
                        Button.primary("previous_page_1", "⬅️ Page précédente").asDisabled(),
                        Button.primary("next_page_1", "➡️ Page suivante")
                )
                .setEphemeral(true)
                .queue();
    }

    private void queue(SlashCommandInteractionEvent event, String selectedMusic) {
        if(!isPlaying){
            event.reply("Aucune musique n'est jouée")
                    .setEphemeral(true)
                    .queue();
            return;
        }
        selectedMusic += ".mp3";
        File musicFile = new File("Music/" + selectedMusic);

        playerManager.loadItem(musicFile.getAbsolutePath(), new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack audioTrack) {
                currentTrackList.add(audioTrack);
                event.reply("La musique **" + audioTrack.getInfo().title + "** a été rajoutée à la Playlist").queue();
            }

            @Override
            public void playlistLoaded(AudioPlaylist audioPlaylist) {
                trackLoaded(audioPlaylist.getTracks().get(0));
            }

            @Override
            public void noMatches() {
                event.reply("La piste n'a pas pu être trouvée ou est incompatible.")
                        .setEphemeral(true)
                        .queue();
            }

            @Override
            public void loadFailed(FriendlyException e) {
                event.reply("Erreur lors du chargement de l'audio : " + e.getMessage())
                        .setEphemeral(true)
                        .queue();
            }
        });
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
                .map(name -> name.replace(".mp3", ""))
                .toList();
    }

    public static String makeList(int currentPage) {
        List<String> musicList = listMusicFiles();
        int totalPages = (int) Math.ceil((double) musicList.size() / 10);

        if (currentPage < 1) currentPage=1;
        if (currentPage > totalPages) currentPage=totalPages;

        List<String> pageContent = musicList.stream()
                .skip((currentPage - 1) * 10L)
                .limit(10)
                .toList();

        StringBuilder musicsBuilder = new StringBuilder();
        musicsBuilder.append("**Liste des musiques disponibles (" + currentPage + "/" + totalPages + ") : **\n\n");
        for (String music : pageContent) {
            musicsBuilder.append("- ").append(music).append("\n");
        }

        return musicsBuilder.toString();
    }
}
