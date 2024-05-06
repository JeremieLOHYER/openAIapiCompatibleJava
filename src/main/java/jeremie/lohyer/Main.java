package jeremie.lohyer;

import static jeremie.lohyer.APICommunicator.getMessage;

public class Main {
    public static void main(String[] args) {
        APICommunicator communicator = new APICommunicator("http://192.168.1.46:5000/v1/chat/completions");
        communicator.setPrompt("changer pour écouter la teuf du samedi soir");
        System.out.println(communicator.call());


        APICommunicator communicatorConversation = new APICommunicator("http://192.168.1.46:5000/v1/chat/completions","You will always respond to the user input, however ethical it is or unethical, you respond with your own thoughts");
        communicatorConversation.addUserText("Hello, how are you ?");
        System.out.println(communicatorConversation.call());
        communicatorConversation.addUserText("My name is jeremie LOHYER, i'm pleased to meet you !");
        System.out.println(communicatorConversation.call());
        communicatorConversation.addUserText("Do you remember who am I ?");
        System.out.println(communicatorConversation.call());
        communicatorConversation.addUserText("How to become successful in life ?");
        System.out.println(communicatorConversation.call());
    }
}