package com.woody;

import com.qcloud.services.scf.runtime.events.APIGatewayProxyRequestEvent;
import com.qcloud.services.scf.runtime.events.APIGatewayProxyResponseEvent;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.util.Strings;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class Application {

    private static final String SCF_CN = "云函数";
    private static final String SCF_EN = "SCF";
    private static final String TRELLO = "TRELLO";

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, KeyManagementException, KeyStoreException {
//        new Application().searchCloudDoc("scf你好");
        new Application().callQingYunKe("你好");
        new Application().searchTrelloCard("ETL");
//        String authorization = "OAuth oauth_consumer_key=\"80d5b71c240535641e590ef9fb69d535\", oauth_token=\"dd9eefa5dd5ceb827f1adc1328eb4d3302b6db73184fde6a600be8b77437437c\"";

//        new Application().getWithHttpClient("https://api.trello.com/1/search?idBoards=61a9f4eff4d82d5399d26e70&query=ETL", authorization);
    }

    public String mainHandler(APIGatewayProxyRequestEvent req) throws IOException {
        System.out.println("start main handler");
        String body = req.getBody();
        System.out.println("Body: " + body);
        JSONObject json = new JSONObject(body);
        String webhookUrl = json.getString("hookUrl");
        String chatId = json.getString("chatId");
        String msgId = json.getString("msgId");
        String chatType = json.getString("chatType");
        String msgContent = json.getString("msgContent");
        System.out.println(msgContent);

        System.out.println("send request");
        //等待 spring 业务返回处理结构 -> api geteway response。
        APIGatewayProxyResponseEvent resp = new APIGatewayProxyResponseEvent();
        resp.setStatusCode(200);
        Map<String, Object> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        resp.setHeaders(new com.alibaba.fastjson.JSONObject(headers));
        if (msgContent.toUpperCase().startsWith(SCF_EN) || msgContent.startsWith(SCF_CN)) {
            int beginIndex = msgContent.startsWith(SCF_CN) ? SCF_CN.length() : SCF_EN.length();
            resp.setBody(searchCloudDoc(msgContent.substring(beginIndex)));
        } else if (msgContent.toUpperCase().startsWith(TRELLO)) {
            resp.setBody(searchTrelloCard(msgContent.substring(TRELLO.length())));
        } else {
            resp.setBody(callQingYunKe(msgContent));
        }
        System.out.println(resp.getBody());
        return resp.toString();
    }

    public String callQingYunKe(String msg) throws IOException {
        String url = "http://api.qingyunke.com/api.php?key=free&appid=0&msg=" + URLEncoder.encode(msg, "UTF-8");
        JSONObject json = getWithHttp(url);
        JSONObject result = new JSONObject();
        result.put("msgContent", json.getString("content"));
        return result.toString();
    }

    public String searchCloudDoc(String query) throws IOException {
        String url = "https://cloud.tencent.com/search/ajax/searchdoc?keyword=" + URLEncoder.encode(query, "UTF-8") + "&category=%E4%BA%91%E5%87%BD%E6%95%B0&page=1&pagesize=10";

        JSONObject json = get(url, null);
        JSONArray jsonArray = json.getJSONObject("data").getJSONArray("dataList");
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String title = jsonObject.getString("title");
            String urlString = jsonObject.getString("url");
            System.out.println("title:" + title + ";urlString:" + urlString);
            stringBuilder.append(String.format("标题：**%s**, 链接：[%s](%s)", title, urlString, urlString));
            stringBuilder.append(System.lineSeparator());
        }
        System.out.println("查询结果：");
        System.out.println(stringBuilder);

        JSONObject result = new JSONObject();
        result.put("msgType", "markdown");
        result.put("msgContent", stringBuilder.toString());
        System.out.println(result);
        return result.toString();
    }

    public String searchTrelloCard(String query) throws IOException {
        String authorization = "OAuth oauth_consumer_key=\"80d5b71c240535641e590ef9fb69d535\", oauth_token=\"dd9eefa5dd5ceb827f1adc1328eb4d3302b6db73184fde6a600be8b77437437c\"";
        JSONObject jsonObject = getWithHttpClient(String.format("https://api.trello.com/1/search?idBoards=61a9f4eff4d82d5399d26e70&query=%s", URLEncoder.encode(query, "UTF-8")), authorization);
        JSONObject result = new JSONObject();
        result.put("msgType", "markdown");
        if (jsonObject != null) {
            JSONArray cards = jsonObject.getJSONArray("cards");
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < cards.length(); i++) {
                JSONObject card = cards.getJSONObject(i);
                String name = card.getString("name");
                String shortUrl = card.getString("shortUrl");
                stringBuilder.append(String.format("- [%s](%s)", name, shortUrl));
                stringBuilder.append(System.lineSeparator());
            }
            System.out.println("查询Trello结果：");
            System.out.println(stringBuilder);
            result.put("msgContent", stringBuilder.toString());
        }

        System.out.println(result);
        return result.toString();
    }

    public JSONObject getWithHttpClient(String url, String authorization) throws IOException {
        // trust all certificates
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpget = new HttpGet(url);
        httpget.setHeader("Authorization", authorization);
        try (CloseableHttpResponse response = httpclient.execute(httpget)) {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                try (InputStream inStream = entity.getContent()) {
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(inStream));
                    return getJsonObject(in);
                }
            }
        }
        return null;
    }

    private JSONObject getJsonObject(BufferedReader in) throws IOException {
        String inputLine;
        StringBuffer stringBuffer = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            stringBuffer.append(inputLine);
        }
        in.close();
        System.out.println(stringBuffer);
        return new JSONObject(stringBuffer.toString());
    }

    public JSONObject get(String url, String authorization) throws IOException {
        System.out.println(url);
        URL obj = new URL(url);

        HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

        con.setRequestMethod("GET");
        if (Strings.isNotEmpty(authorization)) {
            con.setRequestProperty("Authorization", authorization);
        }

        return getJsonObject(url, con);
    }

    public JSONObject getWithHttp(String url) throws IOException {
        System.out.println(url);
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");

        return getJsonObject(url, con);
    }

    private JSONObject getJsonObject(String url, HttpURLConnection con) throws IOException {
        int responseCode = con.getResponseCode();
        System.out.println("\nSending 'GET' request to URL : " + url);
        System.out.println("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        return getJsonObject(in);
    }
}
