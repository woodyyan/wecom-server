package com.woody;

import com.qcloud.services.scf.runtime.events.APIGatewayProxyRequestEvent;
import com.qcloud.services.scf.runtime.events.APIGatewayProxyResponseEvent;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.nlp.v20190408.NlpClient;
import com.tencentcloudapi.nlp.v20190408.models.ChatBotRequest;
import com.tencentcloudapi.nlp.v20190408.models.ChatBotResponse;
import org.apache.logging.log4j.util.Strings;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import static com.woody.Configs.SECRET_ID;
import static com.woody.Configs.SECRET_KEY;

public class Application {

    private static final String SCF_CN = "云函数";
    private static final String SCF_EN = "SCF";
    private static final String TRELLO = "TRELLO";
    public static final String EM_BEGIN = "<em>";
    public static final String EM_END = "</em>";

    public static void main(String[] args) throws IOException {
        new Application().searchCloudDoc("收费");
//        new Application().callQingYunKe("你好");
//        new Application().searchTrelloCard("ETL");
//        System.out.println(new Application().chatBot("你好"));
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
        APIGatewayProxyResponseEvent resp = new APIGatewayProxyResponseEvent();
        resp.setStatusCode(200);
        Map<String, Object> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        resp.setHeaders(new com.alibaba.fastjson.JSONObject(headers));
        if (msgContent.toUpperCase().startsWith(SCF_EN) || msgContent.startsWith(SCF_CN)) {
            int beginIndex = msgContent.startsWith(SCF_CN) ? SCF_CN.length() : SCF_EN.length();
            resp.setBody(searchCloudDoc(msgContent.substring(beginIndex)));
        } else {
            resp.setBody(chatBot(msgContent));
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
            String content = jsonObject.getString("content");
            System.out.println("title:" + title + ";urlString:" + urlString);
            System.out.println(content);
            stringBuilder.append(String.format("[%s](%s) \n > %s \n", title, urlString, replaceAllFont(content)));
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

    private String replaceAllFont(String content) {
        if (content.contains(EM_BEGIN)) {
            content = replaceFont(content);
            return replaceAllFont(content);
        } else {
            return content;
        }
    }

    private String replaceFont(String content) {
        int begin = content.indexOf(EM_BEGIN);
        int end = content.indexOf(EM_END);
        if (begin < 0 || end < 0) {
            return content;
        }
        String emString = content.substring(begin, end + EM_END.length());
        String fontString = emString.replace(EM_BEGIN, "<font color=red>");
        fontString = fontString.replace(EM_END, "</font>");
        String replace = content.replace(emString, fontString);
        if (replace.contains("。")) {
            int index = replace.indexOf("。");
            replace = replace.substring(0, index + 1);
        }
        return replace;
    }

    public String chatBot(String message) {
        try {
            // 实例化一个认证对象，入参需要传入腾讯云账户secretId，secretKey,此处还需注意密钥对的保密
            // 密钥可前往https://console.cloud.tencent.com/cam/capi网站进行获取
            Credential cred = new Credential(SECRET_ID, SECRET_KEY);
            // 实例化一个http选项，可选的，没有特殊需求可以跳过
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("nlp.tencentcloudapi.com");
            // 实例化一个client选项，可选的，没有特殊需求可以跳过
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            // 实例化要请求产品的client对象,clientProfile是可选的
            NlpClient client = new NlpClient(cred, "ap-guangzhou", clientProfile);
            // 实例化一个请求对象,每个接口都会对应一个request对象
            ChatBotRequest req = new ChatBotRequest();
            req.setQuery(message);
            // 返回的resp是一个ChatBotResponse的实例，与请求对象对应
            ChatBotResponse resp = client.ChatBot(req);
            // 输出json格式的字符串回包
            JSONObject responseJson = new JSONObject(ChatBotResponse.toJsonString(resp));
            System.out.println(responseJson);
            JSONObject result = new JSONObject();
            result.put("msgContent", responseJson.getString("Reply"));
            return result.toString();
        } catch (TencentCloudSDKException e) {
            System.out.println(e.getMessage());
        }
        return "";
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
