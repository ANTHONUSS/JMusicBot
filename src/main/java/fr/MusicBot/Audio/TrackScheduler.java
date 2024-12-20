package fr.MusicBot.Audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import fr.MusicBot.LOGs;
import net.dv8tion.jda.api.managers.AudioManager;

public class TrackScheduler extends AudioEventAdapter {
    public final AudioPlayer audioPlayer;
    private final AudioManager audioManager;
    private boolean isLooping;
    private String currentTrackName;

    public TrackScheduler(AudioPlayer audioPlayer, AudioManager audioManager) {
        this.audioPlayer = audioPlayer;
        this.audioManager = audioManager;
    }

    public void setLooping(boolean isLooping) {
        this.isLooping = isLooping;
    }

    public void setCurrentTrackName(String currentTrackName) {this.currentTrackName = currentTrackName;}

    public String getCurrentTrackName() {
        return currentTrackName;
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (endReason == AudioTrackEndReason.FINISHED) {
            if (isLooping) {
                LOGs.sendLog("Musique " + currentTrackName + " lue en boucle", 4);
                audioPlayer.playTrack(track.makeClone());
            } else if (endReason == AudioTrackEndReason.FINISHED && !isLooping) {
                if (audioManager.isConnected()) {
                    audioManager.closeAudioConnection();
                }
            }
        }
    }

}
