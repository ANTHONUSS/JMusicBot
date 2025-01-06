package fr.MusicBot.Audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import fr.MusicBot.LOGs;
import fr.MusicBot.Listeners.SlashCommandListener;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.managers.AudioManager;

public class TrackScheduler extends AudioEventAdapter {
    public final AudioPlayer audioPlayer;
    private final AudioManager audioManager;
    private boolean isLooping;
    private String currentTrackName;
    private SlashCommandInteractionEvent currentEvent;

    public TrackScheduler(AudioPlayer audioPlayer, AudioManager audioManager) {
        this.audioPlayer = audioPlayer;
        this.audioManager = audioManager;
    }

    public void setLooping(boolean isLooping) {
        this.isLooping = isLooping;
    }

    public void setCurrentTrackName(String currentTrackName) {
        this.currentTrackName = currentTrackName;
    }

    public void setCurrentEvent(SlashCommandInteractionEvent currentEvent) {
        this.currentEvent = currentEvent;
    }

    public String getCurrentTrackName() {
        return currentTrackName;
    }

    public void setCurrentTrackIndex(int currentTrackIndex) {
        SlashCommandListener.currentTrackIndex = currentTrackIndex;
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (endReason == AudioTrackEndReason.FINISHED) {
            if (SlashCommandListener.currentTrackIndex + 1 < SlashCommandListener.currentTrackList.size()) {
                LOGs.sendLog("Prochaine musique lue"
                        + "\nNom : " + currentTrackName
                        + "\nServeur : " + currentEvent.getGuild().getName()
                        + "\nSalon : #" + currentEvent.getChannel().getName(), LOGs.LogType.PLAY);

                SlashCommandListener.currentTrackIndex++;
                audioPlayer.playTrack(SlashCommandListener.currentTrackList.get(SlashCommandListener.currentTrackIndex));
            } else {
                if (isLooping) {
                    LOGs.sendLog("Playlist lue en boucle"
                            + "\nNom : " + currentTrackName
                            + "\nServeur : " + currentEvent.getGuild().getName()
                            + "\nSalon : #" + currentEvent.getChannel().getName(), LOGs.LogType.LOOP);

                    if (SlashCommandListener.currentTrackList.size() == 1) {
                        SlashCommandListener.currentTrackIndex = 0;
                        audioPlayer.playTrack(track.makeClone());
                    } else {
                        SlashCommandListener.currentTrackIndex = 0;
                        audioPlayer.playTrack(SlashCommandListener.currentTrackList.get(SlashCommandListener.currentTrackIndex));
                    }
                } else {
                    if (audioManager.isConnected()) {
                        SlashCommandListener.isPlaying = false;
                        audioManager.closeAudioConnection();
                    }
                }
            }
        }
    }

}
