package com.renomad.sampledomain;

import com.renomad.auth.AuthUtils;
import com.renomad.minum.database.Db;
import com.renomad.minum.templating.TemplateProcessor;
import com.renomad.minum.utils.FileUtils;
import com.renomad.minum.utils.StringUtils;
import com.renomad.minum.web.Request;
import com.renomad.minum.web.Response;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.renomad.minum.web.StatusLine.StatusCode.*;


public class SampleDomain {

    private final Db<PersonName> db;
    private final AuthUtils auth;
    private final TemplateProcessor nameEntryTemplate;
    private final String authHomepage;
    private final String unauthHomepage;

    public SampleDomain(Db<PersonName> diskData, AuthUtils auth, FileUtils fileUtils) {
        this.db = diskData;
        this.auth = auth;
        nameEntryTemplate = TemplateProcessor.buildProcessor(fileUtils.readTextFile("src/main/webapp/templates/sampledomain/name_entry.html"));
        authHomepage = fileUtils.readTextFile("src/main/webapp/templates/sampledomain/auth_homepage.html");
        unauthHomepage = fileUtils.readTextFile("src/main/webapp/templates/sampledomain/unauth_homepage.html");
    }

    public Response formEntry(Request r) {
        final var authResult = auth.processAuth(r);
        if (! authResult.isAuthenticated()) {
            return Response.buildLeanResponse(CODE_401_UNAUTHORIZED);
        }
        final String names = db
                .values().stream().sorted(Comparator.comparingLong(PersonName::getIndex))
                .map(x -> "<li>" + StringUtils.safeHtml(x.getFullname()) + "</li>\n")
                .collect(Collectors.joining());

        return Response.htmlOk(nameEntryTemplate.renderTemplate(Map.of("names", names)));
    }

    public Response testform(Request r) {
        final var authResult = auth.processAuth(r);
        if (! authResult.isAuthenticated()) {
            return Response.buildLeanResponse(CODE_401_UNAUTHORIZED);
        }

        final var nameEntry = r.body().asString("name_entry");

        db.write(new PersonName(0L, nameEntry));
        return Response.buildLeanResponse(CODE_303_SEE_OTHER, Map.of("Location","formentry"));
    }

    /**
     * This is an example of a homepage for a domain.  Here we examine
     * whether the user is authenticated.  If not, we request them to
     * log in.  If already, then we show some features and the log-out link.
     */
    public Response sampleDomainIndex(Request request) {
        final var authResult = auth.processAuth(request);
        if (! authResult.isAuthenticated()) {
            return Response.htmlOk(unauthHomepage);
        } else {
            return Response.htmlOk(authHomepage);
        }

    }

    /**
     * a GET request, at /hello?name=foo
     * <p>
     *     Replies "hello foo"
     * </p>
     */
    public Response helloName(Request request) {
        String name = request.requestLine().queryString().get("name");
        return Response.htmlOk("hello " + name);
    }

}
