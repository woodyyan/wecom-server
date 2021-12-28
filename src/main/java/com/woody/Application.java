package com.woody;

import com.qcloud.services.scf.runtime.events.APIGatewayProxyRequestEvent;
import com.qcloud.services.scf.runtime.events.APIGatewayProxyResponseEvent;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class Application {

    private static final String SCF_CN = "云函数";
    private static final String SCF_EN = "SCF";

    public static void main(String[] args) throws IOException {
        new Application().searchCloudDoc("你好");
//        new Application().callQingYunKe("你好");
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
        if (msgContent.startsWith(SCF_EN) || msgContent.startsWith(SCF_CN)) {
            int beginIndex = msgContent.startsWith(SCF_EN) ? SCF_EN.length() : SCF_CN.length();
            resp.setBody(searchCloudDoc(msgContent.substring(beginIndex)));
        } else {
            resp.setBody(callQingYunKe(msgContent));
        }
        System.out.println(resp.getBody());
        return resp.toString();
    }

    public String callQingYunKe(String msg) throws IOException {
        String url = "http://api.qingyunke.com/api.php?key=free&appid=0&msg=" + URLEncoder.encode(msg, "UTF-8");
        JSONObject json = get(url);
        JSONObject result = new JSONObject();
        result.put("msgContent", json.getString("content"));
        return result.toString();
    }

    public String searchCloudDoc(String query) throws IOException {
        String url = "https://cloud.tencent.com/search/ajax/searchdoc?keyword=" + URLEncoder.encode(query, "UTF-8") + "&category=%E4%BA%91%E5%87%BD%E6%95%B0&page=1&pagesize=10";

        JSONObject json = get(url);
        JSONArray jsonArray = json.getJSONObject("data").getJSONArray("dataList");
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String title = jsonObject.getString("title");
            String urlString = jsonObject.getString("url");
            System.out.println("title:" + title + ";urlString:" + urlString);
            stringBuilder.append(String.format("标题：%s, 链接：%s", title, urlString));
            stringBuilder.append(System.lineSeparator());
        }
        System.out.println("查询结果：");
        System.out.println(stringBuilder);

        JSONObject result = new JSONObject();
        result.put("msgContent", stringBuilder.toString());
        System.out.println(result);
        return result.toString();
    }

    public JSONObject get(String url) throws IOException {
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        int responseCode = con.getResponseCode();
        System.out.println("\nSending 'GET' request to URL : " + url);
        System.out.println("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        System.out.println(response);
        return new JSONObject(response.toString());
    }
}
