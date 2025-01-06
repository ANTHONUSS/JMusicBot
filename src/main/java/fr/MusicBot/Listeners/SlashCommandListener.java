package fr.MusicBot.Listeners;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.*;
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
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.managers.AudioManager;

public class SlashCommandListener extends ListenerAdapter {
    private final AudioPlayerManager playerManager;
    private TrackScheduler currentTrackScheduler;
    private boolean isLooping = false;
    public static boolean isPlaying = false;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    public static final List<AudioTrack> currentTrackList = new ArrayList<>();
    public static int currentTrackIndex;
    private AudioPlayer audioPlayer;

    public SlashCommandListener() {
        this.playerManager = new DefaultAudioPlayerManager();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "play" -> {
                String selectedMusic = event.getOption("musique", OptionMapping::getAsString);

                boolean loopEnabled = event.getOption("loop") != null && Objects.requireNonNull(event.getOption("loop")).getAsBoolean();

                play(event, selectedMusic, loopEnabled);
            }
            case "stop" -> {
                stop(event);
            }
            case "loop" -> {
                toggleLoop(event);
            }
            case "download" -> {
                String choosedURL = event.getOption("url", OptionMapping::getAsString);

                download(event, choosedURL);
            }
            case "list" -> {
                list(event);
            }
            case "queue" -> {
                String selectedMusic = event.getOption("musique", OptionMapping::getAsString);

                queue(event, selectedMusic);
            }
            case "list-queue" -> {
                listQueue(event);
            }
            case "next" -> {
                next(event);
            }
            case "previous" -> {
                previous(event);
            }
            case "remove" -> {
                String selectedMusic = event.getOption("playlist-musique", OptionMapping::getAsString);
                remove(event, selectedMusic);
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
        audioPlayer = playerManager.createPlayer();
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
                isPlaying = true;
                LOGs.sendLog("Musique jouée"
                        + "\nNom : " + musicName
                        + "\nServeur : " + event.getGuild().getName()
                        + "\nSalon : #" + event.getChannel().getName()
                        + "\nUser : @" + event.getUser().getName()
                        + "\nEn boucle : " + isLooping, LOGs.LogType.PLAY);
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
            currentTrackList.clear();
            isPlaying = false;
            event.reply("Musique arrêtée.").queue();
            LOGs.sendLog("Musique arrêtée"
                            + "\nServeur : " + event.getGuild().getName()
                            + "\nSalon : #" + event.getChannel().getName()
                            + "\nUser : " + event.getUser().getName(),
                    LOGs.LogType.STOP);
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
                LOGs.LogType.LOOP);
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
            LOGs.sendLog("Musique " + musicName + " en cours de téléchargement par @" + event.getUser().getName(), LOGs.LogType.DOWNLOAD);


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
                    LOGs.sendLog(line, LOGs.LogType.DOWNLOAD);
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
                            + "\nUser : " + event.getUser().getName(), LOGs.LogType.DOWNLOAD);
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

    private static void list(SlashCommandInteractionEvent event) {
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
        if (!isPlaying) {
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
                event.reply("La musique **" + getFileName(audioTrack.getInfo().uri) + "** a été rajoutée à la Playlist").queue();
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

    private void listQueue(SlashCommandInteractionEvent event) {
        if (currentTrackList.isEmpty()) {
            event.reply("La playlist est vide")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        List<String> playlist = currentTrackList.stream()
                .map(track -> getFileName(track.getInfo().uri))
                .toList();

        StringBuilder builder = new StringBuilder("Musiques de la playlist actuelle :\n");
        for (int i = 0; i < playlist.size(); i++)
            builder.append("- **").append(playlist.get(i)).append("**\n");

        event.reply(builder.toString())
                .setEphemeral(true)
                .queue();
    }

    private void next(SlashCommandInteractionEvent event) {
        if (currentTrackList.isEmpty()) {
            event.reply("La playlist est vide")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (currentTrackList.size() == 1) {
            event.reply("La playlist ne contient qu'un élément, utilisez /play à la place.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (currentTrackIndex == currentTrackList.size() - 1 && !isLooping) {
            event.reply("Il n'y a pas de musique suivante")
                    .setEphemeral(true)
                    .queue();
            return;
        } else if (currentTrackIndex == currentTrackList.size() - 1 && isLooping) currentTrackIndex = 0;
        else currentTrackIndex++;

        event.reply("Prochaine musique dans la playlist jouée.").queue();

        audioPlayer.playTrack(currentTrackList.get(currentTrackIndex));
    }

    private void previous(SlashCommandInteractionEvent event) {
        if (currentTrackList.isEmpty()) {
            event.reply("La playlist est vide")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (currentTrackList.size() == 1) {
            event.reply("La playlist ne contient qu'un élément, utilisez /play à la place.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (currentTrackIndex == 0 && !isLooping) {
            event.reply("Il n'y a pas de musique précédente")
                    .setEphemeral(true)
                    .queue();
            return;
        } else if (currentTrackIndex == 0 && isLooping) currentTrackIndex = currentTrackList.size() - 1;
        else currentTrackIndex--;

        event.reply("Musique précédente dans la playlist jouée.").queue();
        audioPlayer.playTrack(currentTrackList.get(currentTrackIndex));
    }

    private void remove(SlashCommandInteractionEvent event, String selectedMusic) {
        for (AudioTrack track : currentTrackList) {
            if (getFileName(track.getInfo().uri).equals(selectedMusic)) {
                currentTrackList.remove(track);
                currentTrackIndex--;
                break;
            }
        }

        event.reply("La musique **" + selectedMusic + "** à été retirée").queue();
    }

    public static String getFileName(String uri) {
        File file = new File(uri);
        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf(".");
        return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
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

        if (currentPage < 1) currentPage = 1;
        if (currentPage > totalPages) currentPage = totalPages;

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
