package hudson.plugins.tics;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import hudson.model.TaskListener;
import hudson.plugins.tics.MeasureApiCall.MeasureApiCallException;

public class InstallTicsApiCall extends AbstractApiCall {

    private static final String LOGGING_PREFIX = "[TICS Install]";
    private final String installTicsUrl;

    public InstallTicsApiCall(final String installTicsUrl, Optional<Pair<String, String>> credentials, final TaskListener listener) {
        super(LOGGING_PREFIX, listener.getLogger(), credentials);
        this.installTicsUrl = installTicsUrl;
    }

    public String retrieveInstallTics() {
        final String url = this.installTicsUrl;
        final String response = this.performHttpRequest(url);

        final InstallTicsApiResponse resp = new Gson().fromJson(response, InstallTicsApiResponse.class);

        if (resp.links == null || Strings.isNullOrEmpty(resp.links.installTics)) {
            throw new IllegalArgumentException(LOGGING_PREFIX + "Cannot determine Install TICS API url.");
        }

        return resp.links.installTics;
    }

    private String performHttpRequest(final String url) {
        final HttpGet httpGet = new HttpGet(url);
        try (final CloseableHttpClient httpclient = this.createHttpClient();
                final CloseableHttpResponse response = httpclient.execute(httpGet);) {
            final String body = EntityUtils.toString(response.getEntity());

            this.throwIfStatusNotOk(response, body);
            return body;
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException | IOException | MeasureApiCallException ex) {
            throw new RuntimeException("Error while performing API request to " + url, ex);
        }
    }

}
