package info.an0therdev;

import okhttp3.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) {
        try{
            String token = "token";
            String group_id = "group_id";
            new Bot(token, group_id);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}

class Bot{

    final private Random random;
    final private VkHandler handler;
    final private String token;
    final private String group_id;
    final private String version = "5.80";

    public Bot(String token, String group_id) throws Exception{
        random = new Random();
        this.token = token;
        this.group_id = group_id;
        handler = new VkHandler(this);
        handler.start();
        handler.join();
    }

    public String getToken(){
        return token;
    }

    public String getGroup_id(){
        return group_id;
    }

    public String getVersion(){
        return version;
    }

    public String getRandom_id(){
        return String.valueOf(random.nextInt(Integer.MAX_VALUE)+1);
    }

    public static class Listener{

        public static String getResponse(String request){
            if(request.contains("Инфо")){
                return "200";
            }
            return "результат: 0";
        }

    }

}

class VkHandler extends Thread{

    final private OkHttpClient httpClient;
    final private LongPollServer server;
    final private JSONParser parser;
    final private Bot bot;

    public VkHandler(Bot bot) throws Exception{
        this.bot = bot;
        httpClient = new OkHttpClient.Builder().readTimeout(30, TimeUnit.SECONDS).build();
        parser = new JSONParser();
        server = getLongPollServer();
    }

    @Override
    public void run(){
        try{
            ArrayList queue;
            while (true){
                server.connect();
                queue = server.getMessages();
                for(Message msg : (Message[]) queue.toArray(new Message[queue.size()])){
                    System.out.println(msg.text);
                    String res = Bot.Listener.getResponse(msg.getText());
                    Request request = new Request.Builder().url("https://api.vk.com/method/messages.send?message="+res+"&v="+bot.getVersion()+"&access_token="+bot.getToken()+"&user_id="+msg.getUser()+"&random_id="+bot.getRandom_id()).build();
                    httpClient.newCall(request).execute();
                }
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private LongPollServer getLongPollServer() throws IOException, ParseException {
        Request request = new Request.Builder().url("https://api.vk.com/method/groups.getLongPollServer?v="+bot.getVersion()+"&access_token="+bot.getToken()+"&group_id="+bot.getGroup_id()).build();
        try(Response res = httpClient.newCall(request).execute()){
            JSONObject obj = (JSONObject) ((JSONObject) parser.parse(res.body().string())).get("response");
            //System.out.println(obj.toJSONString());
            return new LongPollServer(obj.get("key").toString(), obj.get("server").toString(), obj.get("ts").toString());
        }
    }

    private class LongPollServer{

        final private String key;
        final private String server;
        private String ts;
        final private ArrayList<Message> messages;

        public LongPollServer(String key, String server, String ts){
            this.key = key;
            this.server = server;
            this.ts = ts;
            messages = new ArrayList<>();
        }

        public void connect() throws IOException, ParseException {
            messages.clear();
            Request request = new Request.Builder().url(server+"?act=a_check&key="+key+"&ts="+ts+"&wait=25").build();
            try(Response res = httpClient.newCall(request).execute()){
                JSONObject obj = (JSONObject) parser.parse(res.body().string());
                if(obj == null){
                    return;
                }
                for(Object update_obj : (JSONArray)obj.get("updates")){
                    JSONObject update = (JSONObject) update_obj;
                    if(update.get("type").equals("message_new")){
                        //System.out.println(update.toJSONString());
                        messages.add(new Message(((JSONObject)update.get("object")).get("text").toString(), ((JSONObject)update.get("object")).get("from_id").toString()));
                    }
                }
                ts = obj.get("ts").toString();
            }
        }

        public ArrayList<Message> getMessages(){
            return messages;
        }

    }

    private class Message{

        final private String text;
        final private String user;

        public Message(String text, String user){
            this.text = text;
            this.user = user;
        }

        public String getText(){
            return text;
        }

        public String getUser(){
            return user;
        }

    }

}
