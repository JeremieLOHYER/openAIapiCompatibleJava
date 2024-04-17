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
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;

public class APICommunicator {

    private HttpClient client;
    private String webAddress;

    private static String preprompt = "you will do json formating on the next data coming. as following :  {'action': 'pausing','song': 'null'}" +
            "first, you will give the info if it's an action of 'playing', 'pausing' or 'stopping' a song. " +
            "then if it's an action of 'playing', you will tell what song is it telling to play, or you say 'null'. with the key word 'song' " +
                    "here you go : ";

    private String prompt;

    public APICommunicator(String webAddress) {
        client = HttpClient.newBuilder().build();
        this.webAddress = webAddress;
        this.prompt = "ecouter shrek";
    }

    public APICommunicator setPrompt(String prompt) {
        this.prompt = prompt;
        return this;
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

        String jsonContent =
                "{" +
                "    \"model\": \"dolphin-2.1-mistral-7b\"," +
                "    \"messages\": [" +
                "        {\n" +
                "            \"role\": \"user\"," +
                "            \"content\": \"" + preprompt + " : " + prompt + "\"" +
                "        }" +
                "    ]," +
                "    \"temperature\": 0.7" +
                "}";
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