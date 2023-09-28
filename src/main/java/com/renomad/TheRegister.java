package com.renomad;

import com.renomad.auth.AuthUtils;
import com.renomad.auth.LoopingSessionReviewing;
import com.renomad.auth.SessionId;
import com.renomad.auth.User;
import com.renomad.minum.web.RequestLine;
import com.renomad.sampledomain.ListPhotos;
import com.renomad.sampledomain.PersonName;
import com.renomad.sampledomain.SampleDomain;
import com.renomad.sampledomain.UploadPhoto;
import com.renomad.sampledomain.photo.Photograph;
import com.renomad.minum.Context;
import com.renomad.minum.database.Db;
import com.renomad.minum.web.Response;
import com.renomad.minum.web.WebFramework;

/**
 * This class is where all code gets registered to work
 * with our web testing.
 * <br><br>
 * example:
 * <pre>{@code
 *     wf.registerPath(RequestLine.Method.GET, "formentry", sd::formEntry);
 * }</pre>
 */
public class TheRegister {

    private final Context context;
    private final WebFramework webFramework;

    public TheRegister(Context context) {
        this.context = context;
        this.webFramework = context.getFullSystem().getWebFramework();
    }

    public void registerDomains() {
        var auth = buildAuthDomain();
        var up = setupUploadPhotos(auth);
        var lp = setupListPhotos(auth, up);
        var sd = setupSampleDomain(auth);

        // homepage
        webFramework.registerPath(RequestLine.Method.GET, "", r -> Response.redirectTo("index.html"));
        webFramework.registerPath(RequestLine.Method.GET, "index", sd::sampleDomainIndex);

        // sample domain stuff
        webFramework.registerPath(RequestLine.Method.GET, "formentry", sd::formEntry);
        webFramework.registerPath(RequestLine.Method.POST, "testform", sd::testform);
        webFramework.registerPath(RequestLine.Method.GET, "hello", sd::helloName);

        // photos stuff
        webFramework.registerPath(RequestLine.Method.GET, "photos", lp::ListPhotosPage);
        webFramework.registerPath(RequestLine.Method.GET, "upload", up::uploadPage);
        webFramework.registerPath(RequestLine.Method.POST, "upload", up::uploadPageReceivePost);
        webFramework.registerPath(RequestLine.Method.GET, "photo", lp::grabPhoto);

        // minum.auth stuff
        webFramework.registerPath(RequestLine.Method.GET, "login", auth::login);
        webFramework.registerPath(RequestLine.Method.GET, "register", auth::register);
        webFramework.registerPath(RequestLine.Method.POST, "registeruser", auth::registerUser);
        webFramework.registerPath(RequestLine.Method.POST, "loginuser", auth::loginUser);
        webFramework.registerPath(RequestLine.Method.GET, "logout", auth::logout);
        webFramework.registerPath(RequestLine.Method.GET, "auth", auth::authPage);

    }

    private SampleDomain setupSampleDomain(AuthUtils auth) {
        Db<PersonName> sampleDomainDb = context.getDb("names", PersonName.EMPTY);
        return new SampleDomain(sampleDomainDb, auth, context.getFileUtils());
    }

    private ListPhotos setupListPhotos(AuthUtils auth, UploadPhoto up) {
        return new ListPhotos(context, up, auth);
    }

    private UploadPhoto setupUploadPhotos(AuthUtils auth) {
        Db<Photograph> photoDb = context.getDb("photos", Photograph.EMPTY);
        return new UploadPhoto(photoDb, auth, context);
    }

    private AuthUtils buildAuthDomain() {
        Db<SessionId> sessionDb = context.getDb("sessions", SessionId.EMPTY);
        Db<User> userDb = context.getDb("users", User.EMPTY);
        var au = new AuthUtils(sessionDb, userDb, context);
        new LoopingSessionReviewing(context, au).initialize();
        au.setSessionLooper();
        return au;
    }
}