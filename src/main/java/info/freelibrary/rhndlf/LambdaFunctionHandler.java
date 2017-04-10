
package info.freelibrary.rhndlf;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.ListIterator;

import org.json.simple.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

/**
 * A AWS Lambda script to facilitate downloading RHN ISOs by DevOps processes.
 *
 * @author Kevin S. Clarke <ksclarke@ksclarke.io>
 */
public class LambdaFunctionHandler implements RequestHandler<JSONObject, Location> {

    private static final String DOWNLOAD_URL =
            "https://access.redhat.com/downloads/content/69/ver=/rhel---6/6.8/x86_64/product-software";

    private static final String DOWNLOAD_ISO = "rhel-server-6.8-x86_64-dvd.iso";

    private final int TIMEOUT = 60 * 1000;

    public Location handleRequest(final JSONObject aInput, final Context aContext) {
        final LambdaLogger logger = aContext.getLogger();
        final String[] auth = getAuthentication(aInput, aContext);

        try {
            final ListIterator<Element> linkIterator;

            Document page;
            Element form;
            Connection http;

            // Get the login form
            page = Jsoup.connect(DOWNLOAD_URL).timeout(TIMEOUT).get();
            form = page.select("form#kc-form-login").first();

            if (form == null) {
                logger.log(page.toString());
                throw new RuntimeException("Failed to retrieve login form");
            }

            // Login to RHN
            http = Jsoup.connect(form.attr("action")).timeout(TIMEOUT);
            http.data("username", auth[0]);
            http.data("password", auth[1]);
            http.userAgent("WebKit");
            page = http.post();
            form = page.select("form").first();

            if (form == null) {
                logger.log(page.toString());
                throw new RuntimeException("Failed to retrieve SAML form");
            }

            // A successful response returns a new follow-up form that we need to submit
            http = Jsoup.connect(form.attr("action")).timeout(TIMEOUT);
            http.data("SAMLResponse", form.child(0).attr("VALUE"));
            http.data("RelayState", form.child(1).attr("VALUE"));
            page = http.post();

            // Grab all the ISO links from the downloads page
            linkIterator = page.select(".isoLink").listIterator();

            while (linkIterator.hasNext()) {
                final String link = linkIterator.next().attr("href");

                if (link.contains(DOWNLOAD_ISO)) {
                    logger.log("Found download location: " + link);
                    return new Location(link);
                }
            }
        } catch (final IOException details) {
            throw new RuntimeException(details);
        }

        throw new RuntimeException("Script did not find a download URL");
    }

    @SuppressWarnings("unchecked")
    private String[] getAuthentication(final JSONObject aInput, final Context aContext) {
        final JSONObject jsonAuth = new JSONObject((LinkedHashMap<String, ?>) aInput.get("headers"));
        final String encodedAuthString = ((String) jsonAuth.get("Authorization")).substring(6); // "Basic "
        final byte[] encodedBytes = encodedAuthString.getBytes(StandardCharsets.UTF_8);
        final byte[] unencodedBytes = Base64.getDecoder().decode(encodedBytes);
        final String unencodedString = new String(unencodedBytes, StandardCharsets.UTF_8);
        final String[] authentication = unencodedString.split(":");

        // A valid authentication with have a username separate from a password with a colon
        if (authentication.length != 2) {
            throw new RuntimeException("Unexpected authentication syntax: " + unencodedString);
        } else {
            aContext.getLogger().log("Attempting login as: " + authentication[0]);
        }

        return authentication;
    }
}
