package com.woody;

import com.qcloud.services.scf.runtime.events.APIGatewayProxyRequestEvent;
import com.qcloud.services.scf.runtime.events.APIGatewayProxyResponseEvent;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Application {

    public Application() {
        List<String> replyList = Arrays.asList("抱歉我现在有事不在 等一下也不想理你",
                "您好 发送一元红包自动解锁聊天模式",
                "您好 我现在不无聊 希望无聊时您再找我",
                "您的小可爱正八百里加急赶往你的聊天界面",
                "你好 我是小王 现在认我做爹你就是小王子",
                "魔仙堡打盹中醒了回你",
                "咕噜咕噜魔仙堡专线在为你接通",
                "不要烦我噢 我在冒泡泡Oooooo",
                "老爹古董店 有事请留言",
                "俺没回就是去拔萝卜了Ooo",
                "我不喜欢回消息 感觉我上辈子就是个免打扰",
                "人工服务请按1",
                "目前心动已售完 欢迎下次光临",
                "去宇宙摘星星啦 马上回来",
                "别找我 有事打钱",
                "你好 我是自动回复 您的聊天对象暂时不在",
                "您可以和我聊天 但是我也只会这一句",
                "恭喜你解锁我这只小可爱",
                "我买几个橘子去 你就在此地 不要走动",
                "我去当喜之郎了 回来带太空人给你",
                "对方正尝试与您连接 请稍等 当前进度1%",
                "唉呀妈呀脑瓜疼 脑瓜疼 没钱交网费脑瓜疼",
                "欢迎使用沙雕服务热线",
                "回复技能冷却中",
                "您的消息已送达 对方收到 就是不回",
                "对不起 您所联系的用户太过优秀",
                "已被腾讯删除 想了解详情请咨询10086",
                "稍等我 待会我用方天画戟给你削苹果吃",
                "请输入520次我爱你召唤本人",
                "没回信息就是在放羊 一直没回就是羊丢了",
                "你的小宝贝不在 你爹在",
                "这边因泄露了蟹堡王的祖传秘方 海洋监管局已将她抓获 待释放之时她会主动与您联系",
                "嗯 你继续说 我听着呢",
                "你是夏日限定的美好，会在赏味期内回复",
                "在赶来与你赴约的路上",
                "喂，这里是比基尼海滩的蟹堡王餐厅，我正在煎放在超级蟹黄堡里的肉饼，有事请找章鱼哥嘟嘟嘟嘟嘟",
                "我和xx去当太空人了，回来给你抓外星人",
                "在学习的海洋里溺死了",
                "我和黑山老妖去后山讨论吃唐僧的事情，有事回来再说。",
                "你大声点！我听不见！",
                "不回就是在峡谷 一直不回就是被峡谷埋了",
                "不回就是在吃鸡 一直不回就是被鸡吃了",
                "我去宇宙了 回来摘星星给你",
                "404 NOT FOUND",
                "在妈妈肚子里，已经迫不及待等我出生了吗？出生了本小姐会回你哒。",
                "你好 我们老大正在拯救银河系",
                "打完怪兽就回来 稍等一下你就会见到他了",
                "主人说要想知道她的踪迹需要一包番茄味薯片",
                "鱼儿们 塘主出去撒网了 回来宠幸你们",
                "有什么事晚上再说，幼儿园还没放学",
                "客官请稍等，主子正在路上",
                "没回消息就是在要饭",
                "请大喊三声美女 我会马上出现 如没反应",
                "说明不真诚 请再喊三声 以此类推 谢谢",
                "你的好友已下线，请先转账后联系",
                "对不起，对方太可爱，有事情排队预约",
                "我在晒太阳 别打扰我",
                "有内鬼 现在不方便回复",
                "对方已中毒 发送我爱你即可解毒",
                "对方网络不稳定，请稍后重试",
                "等会再下凡见见尔等凡夫俗子",
                "闭关修炼中");
    }

    public static void main(String[] args) throws IOException {
        new Application().callQingYunKe("你好");
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
        String responseBody = "{\n" +
                "    \"msgContent\": \"%s\"\n" +
                "}";
        resp.setBody(String.format(responseBody, callQingYunKe(msgContent)));
        System.out.println(responseBody);
        return resp.toString();
    }

    public String callQingYunKe(String msg) throws IOException {
        String url = "http://api.qingyunke.com/api.php?key=free&appid=0&msg=" + msg;

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
        JSONObject json = new JSONObject(response.toString());
        return json.getString("content");
    }
}
