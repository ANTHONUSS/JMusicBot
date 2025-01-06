package fr.MusicBot;

public class LOGs {
    public static final String DEFAULT = "\u001B[0m";   //Reset the color
    public static final String RED = "\u001B[31m";      //Errors
    public static final String YELLOW = "\u001B[33m";   //Music downloads
    public static final String GREEN = "\u001B[32m";    //Music play
    public static final String PURPLE = "\u001B[35m";   //music stops
    public static final String CYAN = "\u001B[36m";     //loop
    public static final String BLUE = "\u001B[34m";     //"feur" reply
    public static final String PINK = "\033[38;5;213m"; //debug reply

    public enum LogType {
        ERROR, DOWNLOAD, PLAY, STOP, LOOP, FEUR, DEBUG
    }

    public static void sendLog(String message, LogType logType) {
        String color;
        String enterMessage;
        switch (logType) {
            case ERROR -> {
                color = RED;
                enterMessage = "ERROR ==> ";
            }
            case DOWNLOAD -> {
                color = YELLOW;
                enterMessage = "DOWNLOAD ==> ";
            }
            case PLAY -> {
                color = GREEN;
                enterMessage = "PLAY ==> ";
            }
            case STOP -> {
                color = PURPLE;
                enterMessage = "STOP ==> ";
            }
            case LOOP -> {
                color = CYAN;
                enterMessage = "LOOP ==> ";
            }
            case FEUR -> {
                color = BLUE;
                enterMessage = "FEUR ==> ";
            }
            case DEBUG -> {
                color = PINK;
                enterMessage = "DEBUG ==> ";
            }
            default -> {
                System.out.println(RED + "Mauvais logType entré : " + logType + DEFAULT);
                return;
            }
        }

        System.out.println(color + enterMessage + message + DEFAULT);
    }
}
