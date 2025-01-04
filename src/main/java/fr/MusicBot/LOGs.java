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

    public static void sendLog(String message, int logType) {
        String color;
        String enterMessage;
        switch (logType) {
            case 0 -> {
                color = RED;
                enterMessage = "ERROR ==> ";
            }
            case 1 -> {
                color = YELLOW;
                enterMessage = "DOWNLOAD ==> ";
            }
            case 2 -> {
                color = GREEN;
                enterMessage = "PLAY ==> ";
            }
            case 3 -> {
                color = PURPLE;
                enterMessage = "STOP ==> ";
            }
            case 4 -> {
                color = CYAN;
                enterMessage = "LOOP ==> ";
            }
            case 5 -> {
                color = BLUE;
                enterMessage = "FEUR ==> ";
            }
            case 6 -> {
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
