package jeremie.lohyer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.util.Callback;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.function.Function;

import static jeremie.lohyer.ContentBuilder.*;

public class APICommunicator {

    private final String webAddress;

    private final String model = "dolphin-2.1-mistral-7b";

    private String preprompt;

    private String prompt = "";

    private List<ContentClass> conversation;

    private String temperature;

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

    public APICommunicator changePreprompt(String preprompt) {
        this.preprompt = preprompt;
        return this;
    }

    public APICommunicator setPrompt(String prompt) {
        this.prompt = prompt;
        return this;
    }

    public APICommunicator addUserText(String text) {
        return addText("User", text);
    }

    public APICommunicator addLLMText(String text) {
        return addText("assistant", text);
    }

    public APICommunicator addText(String role, String text) {
        if (conversation == null) {
            conversation = new ArrayList<>();
            this.conversation.add(new ContentClass(
                    new Content[]{
                            new ContentText("role","System"),
                            new ContentText("content",preprompt),
                    }
            ));
        }
        this.conversation.add(new ContentClass(
                new Content[]{
                        new ContentText("role",role),
                        new ContentText("content",text),
                }
        ));
        return this;
    }

    public void call(Executor executor, Function<String, Void> callBack) {
        executor.execute(() -> callBack.apply(asyncCall()));
    }

    public void call(boolean additive, Function<String, Void> callBack) {
        new Thread(() -> callBack.apply(asyncCall(additive))).start();
    }

    protected String asyncCall() {
        return asyncCall(true);
    }

    protected String asyncCall(boolean additive) {
        HttpURLConnection connection;
        try {
            connection = (HttpURLConnection) new URL(webAddress).openConnection();
            connection.setRequestMethod("POST");
        } catch (Exception e) {
            return "request échouée";
        }
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        ContentBuilder contentBuilder = new ContentBuilder();
        ContentClass[] contentClass;

        if (conversation != null) {
            contentClass = new ContentClass[conversation.size()];
            conversation.toArray(contentClass);
        } else {
            contentClass = new ContentClass[]{
                new ContentClass(new Content[]{
                    new ContentText("role", "user"),
                    new ContentText("content", preprompt + " " + prompt)
                })
            };
        }
        contentBuilder.addContent(
            new ContentClass(
                new Content[]{
                    new ContentText("model",model),
                    new ContentArray("messages", contentClass),
                    new ContentText("temperature",this.temperature)
                }
            ), 0
        );

        String jsonContent = contentBuilder.build();

        System.out.println(jsonContent);


        byte[] postData = jsonContent.getBytes(StandardCharsets.UTF_8);

        try {
            OutputStream stream = connection.getOutputStream();
            if (stream == null) {
                return "output stream is null";
            } else {
                stream.write(postData);
            }
        } catch (IOException e) {
            return "output stream echoue";
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
                return "line échouée";
            }

            connection.disconnect();


            if (additive) {
                addLLMText(getMessage(response.toString()));
            }

            return getMessage(response.toString());
        } catch (IOException e) {
            System.out.println("exception : " + e.getMessage());
            InputStream error = connection.getErrorStream();
            if (error != null) {
                in = new BufferedReader(new InputStreamReader(error));
                String inputLine;
                try {
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                } catch (IOException e1) {
                    return "line échouée";
                }

                connection.disconnect();
            } else {
                response.append(e.getMessage());
            }

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