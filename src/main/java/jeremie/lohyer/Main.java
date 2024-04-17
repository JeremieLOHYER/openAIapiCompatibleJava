package jeremie.lohyer;

import static jeremie.lohyer.APICommunicator.getMessage;

public class Main {
    public static void main(String[] args) {
        APICommunicator communicator = new APICommunicator("http://192.168.1.46:5000/v1/chat/completions");
        System.out.println(communicator.call());
    }
}