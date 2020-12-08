package hudson.plugins.tics;

import java.io.PrintStream;
import java.net.ConnectException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import hudson.plugins.tics.MeasureApiSuccessResponse.Baseline;
import hudson.plugins.tics.MeasureApiSuccessResponse.Run;
import hudson.plugins.tics.MeasureApiSuccessResponse.TqiVersion;

public class MeasureApiCall extends AbstractApiCall {
    public static final TypeToken<MeasureApiSuccessResponse<Number>> RESPONSE_NUMBER_TYPETOKEN = new TypeToken<MeasureApiSuccessResponse<Number>>(){/**/};
    public static final TypeToken<MeasureApiSuccessResponse<Double>> RESPONSE_DOUBLE_TYPETOKEN = new TypeToken<MeasureApiSuccessResponse<Double>>(){/**/};
    public static final TypeToken<MeasureApiSuccessResponse<TqiVersion>> RESPONSE_TQIVERSION_TYPETOKEN = new TypeToken<MeasureApiSuccessResponse<TqiVersion>>(){/**/};
    public static final TypeToken<MeasureApiSuccessResponse<List<Run>>> RESPONSE_RUNS_TYPETOKEN = new TypeToken<MeasureApiSuccessResponse<List<Run>>>(){/**/};
    public static final TypeToken<MeasureApiSuccessResponse<List<Baseline>>> RESPONSE_BASELINES_TYPETOKEN = new TypeToken<MeasureApiSuccessResponse<List<Baseline>>>(){/**/};

    private final PrintStream logger;
    private final String measureApiUrl;

    public static class MeasureApiCallException extends Exception {
        private final String message;

        public MeasureApiCallException(final String message) {
            super(message);
            this.message = message;
        }

        @Override
        public String getMessage() {
            return message;
        }
    }

    public MeasureApiCall(final PrintStream logger, final String measureApiUrl, final Optional<StandardUsernamePasswordCredentials> credentials) {
        super("[Measure API]", logger, credentials);
        Preconditions.checkState(measureApiUrl.endsWith("/Measure"));
        this.logger = logger;
        this.measureApiUrl = measureApiUrl;
    }

    public <T> T execute(final TypeToken<T> typeToken, final String paths, final String metrics) throws MeasureApiCallException {
        URIBuilder builder;
        try {
            builder = new URIBuilder(this.measureApiUrl)
                .setParameter("nodes", paths)
                .setParameter("metrics", metrics);
        } catch (final URISyntaxException e) {
            throw new MeasureApiCallException("Invalid URL: " + e.getMessage());
        }
        final String url;
        try {
            url = builder.build().toString();
        } catch (final URISyntaxException e) {
            throw new MeasureApiCallException("Invalid URL: " + e.getMessage());
        }

        final HttpGet httpGet = new HttpGet(url);
        // WARNING: we cannot send the X-Requested-With header to get proper Json response in case of error,
        // because tiobeweb (up to 7.5 at least) does not use Basic HTTP Authentication if that header is provided.
        // COMMENTED ON PURPOSE: httpGet.addHeader("X-Requested-With", "JenkinsPlugin"); // X-Requested-With header indicates that tiobeweb API should return Json when an error occurred. Unfortunately, this does not work in 7.4 when wrong section was provided
        logger.println(TicsPublisher.LOGGING_PREFIX + httpGet.toString());

        final String body;
        try (CloseableHttpClient httpclient = this.createHttpClient();
                CloseableHttpResponse response = httpclient.execute(httpGet);
                ) {
                body = EntityUtils.toString(response.getEntity());
                this.throwIfStatusNotOk(response, body);
        } catch (final ConnectException e) {
            throw new MeasureApiCallException(e.getMessage());
        } catch (final Exception e) {
            throw new MeasureApiCallException(e.toString() /* Includes exception name for more information*/);
        }

        try {
            final T t = new Gson().<T>fromJson(body, typeToken.getType());
            return t;
        } catch(final JsonSyntaxException ex) {
            throw new RuntimeException("Error parsing json: " + body, ex);
        }
    }


}
