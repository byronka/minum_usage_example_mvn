package com.renomad;

import com.renomad.minum.Context;
import com.renomad.minum.logging.ILogger;
import com.renomad.minum.logging.TestLogger;
import com.renomad.minum.utils.MyThread;
import com.renomad.minum.web.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static com.renomad.minum.testing.RegexUtils.find;
import static com.renomad.minum.testing.TestFramework.assertEquals;
import static com.renomad.minum.testing.TestFramework.assertTrue;
import static com.renomad.minum.web.StatusLine.StatusCode.*;

/**
 * This test is called after the testing framework has started
 * the whole system.  We can now talk to the server like a regular user.
 */
public class FunctionalTests {

    private final TestLogger logger;
    private final Context context;
    private final FunctionalTesting ft;

    public FunctionalTests(Context context) {
        this.logger = (TestLogger)context.getLogger();
        this.context = context;
        this.ft = new FunctionalTesting(context);
    }

    public void test() throws Exception {
        logger.test("Check we can customize mime types");
        context.getFullSystem().getWebFramework().addMimeForSuffix(".png", "image/png");

        /* Request a static png image that needed a mime type we just provided */
        assertEquals(ft.get("moon.png").statusLine().status(), _200_OK);
        assertEquals(ft.get("moon.png").headers().valueByKey("content-type"), List.of("image/png"));

        logger.test("grab the photos page unauthenticated. We should be able to view the photos.");
        assertEquals(ft.get("photos").statusLine().status(), _200_OK);

        logger.test("go to the page for registering a user, while unauthenticated.");
        assertEquals(ft.get("register").statusLine().status(), _200_OK);

        logger.test("register a user");
        var registrationResponse = ft.post("registeruser", "username=foo&password=bar");
        assertEquals(registrationResponse.statusLine().status(), _303_SEE_OTHER);
        assertEquals(registrationResponse.headers().valueByKey("location"), List.of("login"));

        logger.test("Go to the login page, unauthenticated");
        assertEquals(ft.get("login").statusLine().status(), _200_OK);

        logger.test("login as the user we registered");
        var response = ft.post("loginuser", "username=foo&password=bar");
        var cookieValue = String.join(";", response.headers().valueByKey("set-cookie"));

        logger.test("try visiting the registration page while authenticated (should get redirected)");
        List<String> authHeader = List.of("Cookie: " + cookieValue);
        var registrationResponseAuthd = ft.post("registeruser", "username=foo&password=bar", authHeader);
        assertEquals(registrationResponseAuthd.statusLine().status(), _303_SEE_OTHER);
        assertEquals(registrationResponseAuthd.headers().valueByKey("location"), List.of("index"));

        logger.test("try visiting the login page while authenticated (should get redirected)");
        assertEquals(ft.get("login", authHeader).statusLine().status(), _303_SEE_OTHER);

        logger.test("visit the page for uploading photos, authenticated");
        ft.get("upload", authHeader);

        logger.test("upload some content, authenticated");
        ft.post("upload", "image_uploads=123&short_descriptionbar=&long_description=foofoo", authHeader);

        logger.test("check out what's on the photos page now, unauthenticated");
        var response2 = ft.get("photos");
        var htmlResponse = response2.body().asString();
        String photoSrc = find("photo\\?name=[a-z0-9\\-]*", htmlResponse);

        logger.test("look at the contents of a particular photo, unauthenticated");
        ft.get(photoSrc, authHeader);

        logger.test("check out what's on the sample domain page, authenticated");
        assertTrue(ft.get("index", authHeader).body().asString().contains("Enter a name"));
        assertTrue(ft.get("formEntry", authHeader).body().asString().contains("Name Entry"));
        assertEquals(ft.post("testform", "name_entry=abc", authHeader).statusLine().status(), _303_SEE_OTHER);

        logger.test("logout");
        assertEquals(ft.get("logout", authHeader).statusLine().status(), _303_SEE_OTHER);

        logger.test("if we try to upload a photo unauth, we're prevented");
        assertEquals(ft.post("upload", "foo=bar").statusLine().status(), _401_UNAUTHORIZED);

        logger.test("if we try to upload a name on the sampledomain auth, we're prevented");
        assertEquals(ft.post("testform", "foo=bar").statusLine().status(), _401_UNAUTHORIZED);

        // *********** ERROR HANDLING SECTION *****************

        logger.test("if we try sending too many characters on a line, it should block us");
        ft.get("a".repeat(context.getConstants().MAX_READ_LINE_SIZE_BYTES + 1));

        // remember, we're the client, we don't have immediate access to the server here.  So,
        // we have to wait for it to get through some processing before we check.
        MyThread.sleep(50);
        String failureMsg = ((TestLogger)logger).findFirstMessageThatContains("in readLine", 15);
        assertEquals(failureMsg, "in readLine, client sent more bytes than allowed.  Current max: 500");

    }

}
