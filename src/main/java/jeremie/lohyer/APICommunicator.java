package jeremie.lohyer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class APICommunicator {

    private final String webAddress;

    private final String model = "dolphin-2.1-mistral-7b";

    private String preprompt = "you will do json formating on the next data coming. as following :  {'action': 'pausing','song': 'null'}" +
            "first, you will give the info if it's an action of 'playing', 'pausing' or 'stopping' a song. " +
            "then if it's an action of 'playing', you will tell what song is it telling to play, or you say 'null'. with the key word 'song' " +
                    "here you go : ";

    private String prompt = "";

    private List<ContentBuilder.ContentClass> conversation;

    private String temperature = "0.1";

    public APICommunicator(String webAddress, String preprompt, String temperature) {
        this.webAddress = webAddress;
        this.preprompt = preprompt;
        this.temperature = temperature;
    }

    public APICommunicator(String webAddress, String preprompt) {
        this.webAddress = webAddress;
        this.preprompt = preprompt;
        this.temperature = "0.7";
    }

    public APICommunicator(String webAddress) {
        this.webAddress = webAddress;
    }

    public APICommunicator setPrompt(String prompt) {
        this.prompt = prompt;
        return this;
    }

    public APICommunicator addUserText(String text) {
        if (conversation == null) {
            conversation = new ArrayList<>();
            this.conversation.add(new ContentBuilder.ContentClass(
                    new ContentBuilder.Content[]{
                            new ContentBuilder.ContentText("role","user"),
                            new ContentBuilder.ContentText("content",preprompt),
                    }
            ));
        }
        this.conversation.add(new ContentBuilder.ContentClass(
                new ContentBuilder.Content[]{
                        new ContentBuilder.ContentText("role","user"),
                        new ContentBuilder.ContentText("content",text),
                }
        ));
        return this;
    }

    private void addLLMText(String text) {
        if (conversation != null) {
            this.conversation.add(new ContentBuilder.ContentClass(
                    new ContentBuilder.Content[]{
                            new ContentBuilder.ContentText("role","assistant"),
                            new ContentBuilder.ContentText("content",text),
                    }
            ));
        }
    }

    public String call() {
        URL url = null;
        try {
            url = new URL(webAddress);
        } catch (MalformedURLException e) {
            System.out.println("url échoué");
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            System.out.println("connection échouée");
        }
        try {
            connection.setRequestMethod("POST");
        } catch (ProtocolException e) {
            System.out.println("request échouée");
        }
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        ContentBuilder contentBuilder = new ContentBuilder();

        ContentBuilder.ContentClass[] contentClass;

        if (conversation != null) {
            contentClass = conversation.toArray(ContentBuilder.ContentClass[]::new);
        } else {
            contentClass = new ContentBuilder.ContentClass[]{
                new ContentBuilder.ContentClass(new ContentBuilder.Content[]{
                    new ContentBuilder.ContentText("role", "user"),
                    new ContentBuilder.ContentText("content", preprompt + " " + prompt)
                })
            };
        }

        contentBuilder.addContent(
            new ContentBuilder.ContentClass(
                new ContentBuilder.Content[]{
                    new ContentBuilder.ContentText("model",model),
                    new ContentBuilder.ContentArray("messages", contentClass),
                    new ContentBuilder.ContentText("temperature",this.temperature)
                }
            ), 0
        );

        String jsonContent = contentBuilder.build();

        System.out.println(jsonContent);


        byte[] postData = jsonContent.getBytes(StandardCharsets.UTF_8);
        try {
            connection.getOutputStream().write(postData);
        } catch (IOException e) {
            System.out.println("output stream echoue");
        }


        StringBuilder response = new StringBuilder();
        BufferedReader in;
        try {
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            try {
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
            } catch (IOException e) {
                System.out.println("line échouée");
            }

            connection.disconnect();



            addLLMText(getMessage(response.toString()));

            return getMessage(response.toString());
        } catch (IOException e) {
            in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            String inputLine;
            try {
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
            } catch (IOException e1) {
                System.out.println("line échouée");
            }

            connection.disconnect();

            return response.toString();
        }

    }

    public static String getMessage(String json) {
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);

        // Get the 'message' field from the JSON
        JsonObject messageObject = jsonObject.getAsJsonArray("choices")
                .get(0)
                .getAsJsonObject()
                .getAsJsonObject("message");
        String message = messageObject.get("content").getAsString();

        return message;
    }

}