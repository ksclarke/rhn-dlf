
package info.freelibrary.rhndlf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.util.Base64;

/**
 * A test of the LambdaFunctionHandler.
 */
public class LambdaFunctionHandlerTest {

    private static JSONObject myInput;

    /**
     * Creates a JSON input object like one would get from the AWS API Gateway.
     */
    @BeforeClass
    public static void createInput() {
        final HashMap<String, Object> json = new LinkedHashMap<>();
        final HashMap<String, String> authorization = new LinkedHashMap<>();
        final String myUsername = System.getProperty("RHN_USERNAME");
        final String myPassword = System.getProperty("RHN_PASSWORD");

        if (myUsername != null && myPassword != null) {
            final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            final String auth;

            try {
                byteStream.write(myUsername.getBytes(StandardCharsets.UTF_8));
                byteStream.write(":".getBytes(StandardCharsets.UTF_8));
                byteStream.write(myPassword.getBytes(StandardCharsets.UTF_8));
            } catch (final IOException details) {
                Assert.fail(details.getMessage());
            }

            auth = Base64.encodeAsString(byteStream.toByteArray());
            authorization.put("Authorization", "Basic " + auth);

            json.put("headers", authorization);
            json.put("method", "GET");
            json.put("body", new LinkedHashMap<String, String>());

            myInput = new JSONObject(json);
        } else {
            Assert.fail("Username and password not configured correctly");
        }
    }

    /**
     * Tests the Lambda function handler for the RHN Download Facilitator.
     */
    @Test
    public void testLambdaFunctionHandler() {
        final LambdaFunctionHandler handler = new LambdaFunctionHandler();
        final Context context = createContext();

        try {
            handler.handleRequest(myInput, context);
        } catch (final Throwable aThrowable) {
            aThrowable.printStackTrace();
            Assert.fail(aThrowable.getMessage());
        }
    }

    private Context createContext() {
        final TestContext context = new TestContext();
        context.setFunctionName("RHN Download Facilitator");
        return context;
    }

}
