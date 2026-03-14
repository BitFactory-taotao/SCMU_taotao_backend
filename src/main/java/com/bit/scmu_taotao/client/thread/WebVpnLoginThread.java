package com.bit.scmu_taotao.client.thread;

import com.bit.scmu_taotao.client.HttpResponseHandler;
import com.bit.scmu_taotao.client.HttpResponseHandlerImpl;
import com.bit.scmu_taotao.client.HttpResponseResult;
import com.bit.scmu_taotao.service.EasyCookieSpecProvider;
import com.bit.scmu_taotao.service.RedisService;
import com.bit.scmu_taotao.util.common.KeyDescription;
import com.bit.scmu_taotao.util.LoginProcessMessage;
import com.bit.scmu_taotao.util.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Lookup;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.cookie.CookieSpecProvider;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
@Slf4j
public class WebVpnLoginThread extends Thread {
    private final String userId;
    private final String password;
    private final CookieStore cookieStore;
    private final RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(10000)      // 连接超时 10s
            .setSocketTimeout(15000)       // 读取超时 15s
            .setConnectionRequestTimeout(10000) // 从池中获取连接超时 10s
            .setRedirectsEnabled(true)
            .setMaxRedirects(5)
            .setCircularRedirectsAllowed(true)
            .setCookieSpec("easy").build();
    private final String requestUrl;
    private final RedisService redisService;
    private final String UserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36 Edg/135.0.0.0";
    private final HttpResponseHandler httpResponseHandler = new HttpResponseHandlerImpl();

    //构造方法
    public WebVpnLoginThread(String userId, String password, CookieStore cookieStore, String requestUrl, RedisService redisService) {
        this.userId = userId;
        this.password = password;
        this.cookieStore = cookieStore == null ? new BasicCookieStore() : cookieStore;
        this.requestUrl = requestUrl;
        this.redisService = redisService;
    }

    //创建httpClient
    private CloseableHttpClient createHttpClient() {
        //创建 Cookie 规范注册表，RegistryBuilder->构建注册表的工具类。
        Lookup<CookieSpecProvider> res = RegistryBuilder.
                <CookieSpecProvider>create().
                register("easy", new EasyCookieSpecProvider())
                .build();
        return HttpClients.custom()
                .setDefaultCookieStore(cookieStore)
                .setDefaultRequestConfig(requestConfig)
                .setDefaultCookieSpecRegistry(res)
                .setUserAgent(UserAgent)
                .build();
    }

    //传html拿  authenticityToken->  create_vpn_login_data  ->  createLoginFormEntity ->createLoginRequest
    //创建登录表单数据
    private List<NameValuePair> createVpnLoginData(String authenticityToken) {
        NameValuePair[] pairs = {
                new BasicNameValuePair("user[login]", userId),
                new BasicNameValuePair("user[password]", password),
                new BasicNameValuePair("user[dymatice_code]", "unknown"),
                new BasicNameValuePair("user[otp_with_capcha]", "false"),
                new BasicNameValuePair("authenticity_token", authenticityToken),
                new BasicNameValuePair("commit", "登录 Login"),
                new BasicNameValuePair("utf8", "✓")
        };
        //一定要确定好登录的键值对名称问题   加强注意!!!!
        return new ArrayList<NameValuePair>(Arrays.asList(pairs));
    }

    //创建登录表单实体
    private UrlEncodedFormEntity createLoginFormEntity(String html) {
        Document doc = Jsoup.parse(html);
        String authenticityToken = doc.select("input[name=authenticity_token]").val();
        return new UrlEncodedFormEntity(createVpnLoginData(authenticityToken), StandardCharsets.UTF_8);
    }

    //创建登录HttpPost请求,
    private HttpPost createLoginRequest(String html) {
        HttpPost httpPost = new HttpPost(requestUrl + "/users/sign_in");
        httpPost.setEntity(createLoginFormEntity(html));
        httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
        return httpPost;
    }

    /*    @Override
        public void run() {
            try(CloseableHttpClient httpClient = createHttpClient()){
                // TODO fakeIp原来用的openId，改为用username可以吗
                String fakeIp = (String) redisService.get(KeyDescription.FAKEIP + username);
                //先发GET拿HTML(httpResponseResult.getContent())
                HttpGet httpGet = new HttpGet(requestUrl  + "/users/sign_in");
                httpGet.setHeader("X-Forwarded-For",fakeIp);
    //            log.info("000000000000"+ httpGet.getFirstHeader("x-forword-for"));
                HttpResponse httpResponse = httpClient.execute(httpGet);
                HttpResponseResult httpResponseResult = httpResponseHandler.parse(httpResponse);
                if ( httpResponseResult.getCode() != 200 && httpResponseResult.getCode() != 302 ) {
                    throw new LoginException(LoginProcessMessage.IP_BLOCK);
                }
                //GET没问题后发POST
                HttpPost httpPost = createLoginRequest(httpResponseResult.getContent());
                httpPost.setHeader("X-Forwarded-For",fakeIp);
                httpClient.execute(httpPost);
                //发完POST刷新Cookie
                httpGet.setURI(URI.create(requestUrl + "/vpn_key/update"));
                httpGet.setHeader("X-Forwarded-For",fakeIp);
                httpClient.execute(httpGet);
                //流式实现获取名为_webvpn_key的Cookie
                if (cookieStore.getCookies().stream().anyMatch(cookie -> cookie.getName().equals("_webvpn_key"))) {
                    //序列化Cookie存入Redis
                    redisService.set(KeyDescription.SSFW + openid, CookieSerializtion.serialize(cookieStore));
                    // TODO 登录后返回token和爬取到的用户名
                    return;
                }
                throw new LoginException(LoginProcessMessage.LOGIN_FAILURE);
            }catch (IOException e){
                throw new RuntimeException(e);
            }
        }*/
    //同步登录，与重写异步run方法在代码实现上无太大差别
    public Result syncLogin() {
        log.info("开始同步登录:{}", userId);
        try (CloseableHttpClient httpClient = createHttpClient()) {
            String fakeIp = null;
            if (redisService != null) {
                fakeIp = (String) redisService.get(KeyDescription.FAKEIP + userId);
            }
            //先发GET拿HTML(httpResponseResult.getContent())
            HttpGet httpGet = new HttpGet(requestUrl + "/users/sign_in");
            if (fakeIp != null) {
                httpGet.setHeader("X-Forwarded-For", fakeIp);
            }
            HttpResponseResult httpResponseResult;
            try (CloseableHttpResponse resp = httpClient.execute(httpGet)) {
                //判断是否被封IP
                httpResponseResult = httpResponseHandler.parse(resp);
            }
            if (httpResponseResult.getCode() != 200) {
                throw new LoginException(LoginProcessMessage.IP_BLOCK);
            }
            log.info("GET请求成功，开始POST请求");
            //GET没问题后发POST
            HttpPost httpPost = createLoginRequest(httpResponseResult.getContent());
            if (fakeIp != null) {
                httpPost.setHeader("X-Forwarded-For", fakeIp);
            }
            try (CloseableHttpResponse resp = httpClient.execute(httpPost)) {
                // 虽然这里不解析内容，但必须消费实体以释放连接
                EntityUtils.consume(resp.getEntity());
            }
            log.info("POST请求成功，开始刷新Cookie");
            //发完POST刷新Cookie
            httpGet.setURI(URI.create(requestUrl + "/vpn_key/update"));
            try (CloseableHttpResponse resp = httpClient.execute(httpGet)) {
                EntityUtils.consume(resp.getEntity());
            }
            log.info("Cookie刷新成功，开始判断登录状态");
            //流式实现获取名为_webvpn_key的Cookie
            //判断是否登录成功
            boolean loginSuccess = cookieStore.getCookies().stream()
                    .anyMatch(c -> "_webvpn_key".equals(c.getName()));
            if (!loginSuccess) {
                throw new LoginException(LoginProcessMessage.LOGIN_FAILURE);
            }
            log.info("登录成功，开始获取用户名");
            // TODO 爬取用户名
            String username = getRealUserName(httpClient);
            log.info("获取用户名成功，用户名为:{}", username);
            return Result.ok(username);
        } catch (IOException e) {
            log.error("syncLogin: IO 异常", e);
            throw new RuntimeException(e);
        } catch (LoginException e) {
            log.error("登录失败:{}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("syncLogin: 未知异常", e);
            throw new RuntimeException(e);
        }
    }

    private String getRealUserName(CloseableHttpClient httpClient) throws IOException {
        /**
         * 获取真实用户名的私有方法
         * @param httpClient 可关闭的 HTTP 客户端，用于发送网络请求
         * @return 解析后的真实用户名
         * @throws IOException 当网络请求或解析过程中出现错误时抛出
         */
        log.info("[getRealUserName] 开始获取用户名，requestUrl: {}", requestUrl);
        // 1. 访问 WebVPN 首页
        HttpGet homePage = new HttpGet(requestUrl + "/");
        log.info("[getRealUserName] 准备发送 GET 请求到：{}", requestUrl + "/");
        log.debug("[getRealUserName] CookieStore 中的 Cookies: {}", cookieStore.getCookies());

        HttpResponseResult result;
        try (CloseableHttpResponse resp = httpClient.execute(homePage)) {
            log.info("[getRealUserName] HTTP 请求执行完成，状态码：{}", resp.getStatusLine().getStatusCode());
            result = httpResponseHandler.parse(resp);
        } catch (IOException e) {
            log.error("[getRealUserName] HTTP 请求执行失败，URL: {}, 异常类型：{}", requestUrl + "/", e.getClass().getName(), e);
            throw e;
        } catch (Exception e) {
            log.error("[getRealUserName] HTTP 请求执行时发生未知异常，URL: {}", requestUrl + "/", e);
            throw new RuntimeException(e);
        }

        if (result.getCode() != 200) {
            log.error("[getRealUserName] 状态码非 200: {}, URL: {}", result.getCode(), requestUrl);
            throw new RuntimeException("获取用户信息页面失败，状态码：" + result.getCode());
        }

        try {
            // 1. 解析 HTML
            Document doc = Jsoup.parse(result.getContent());
            log.info("[getRealUserName] HTML 解析成功，文档标题：{}", doc.title());
            // 2. 使用选择器定位元素
            // 我们取 class 为 dropdown-toggle 的 a 标签
            Element nameElement = doc.selectFirst("a.dropdown-toggle");

            if (nameElement != null) {
                // ownText() 只获取当前节点的文本，不包括子节点（如 <b> 标签）
                // 这能有效过滤掉那个用于显示小箭头的 <b class="caret"></b>
                String userName = nameElement.ownText().trim();
                log.info("[getRealUserName] 找到用户名元素，用户名为：{}", userName);
                return userName;
            } else {
                log.warn("[getRealUserName] 未找到 a.dropdown-toggle 元素，HTML 内容前 500 字符：{}",
                    result.getContent().length() > 500 ? result.getContent().substring(0, 500) : result.getContent());
            }
        } catch (Exception e) {
            log.error("[getRealUserName] 解析 HTML 时发生异常", e);
            e.printStackTrace();
        }
        return "未找到姓名";
    }
}

