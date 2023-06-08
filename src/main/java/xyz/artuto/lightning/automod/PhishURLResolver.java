package xyz.artuto.lightning.automod;

import com.jagrosh.vortex.utils.FixedCache;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class PhishURLResolver
{
    private final FixedCache<String, Boolean> cache;
    private final Logger logger;
    private final OkHttpClient httpClient;

    public PhishURLResolver()
    {
        this.cache = new FixedCache<>(1000);
        this.logger = LoggerFactory.getLogger(PhishURLResolver.class);
        this.httpClient = new OkHttpClient();
    }

    public boolean isPhishUrl(String url)
    {
        url = url.toLowerCase().trim();

        if(cache.contains(url))
            return cache.get(url);

        try
        {
            logger.info("Looking up url {} on phisherman.gg", url);
            boolean result = checkWithApi(makeRequest(url));
            cache.put(url, result);

            logger.info("Url {} was reported as {} by phisherman.gg", url, result ? "blocked" : "safe/unlisted");
            return result;
        }
        catch(Exception e)
        {
            logger.error("Failed to lookup url {} on phisherman.gg:", url, e);
            return false;
        }
    }

    private boolean checkWithApi(Request req) throws IOException
    {
        try(Response res = httpClient.newCall(req).execute())
        {
            if(!res.isSuccessful())
                throw new IOException("Unsuccessful response: " + res.code());

            ResponseBody body = res.body();
            if(body == null)
                throw new IOException("Request body is null!");

            return Boolean.parseBoolean(body.string());
        }
    }

    private Request makeRequest(String url)
    {
        return new Request.Builder()
                .url("https://api.phisherman.gg/v1/domains/" + url)
                .addHeader("User-Agent", "Lightning (+https://github.com/Artuto/Lightning / 264499432538505217)")
                .build();
    }
}
