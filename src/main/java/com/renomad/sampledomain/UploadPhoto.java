package com.renomad.sampledomain;

import com.renomad.minum.state.Constants;
import com.renomad.minum.state.Context;
import com.renomad.auth.AuthResult;
import com.renomad.auth.AuthUtils;
import com.renomad.minum.database.Db;
import com.renomad.minum.logging.ILogger;
import com.renomad.sampledomain.photo.Photograph;
import com.renomad.minum.utils.FileUtils;
import com.renomad.minum.utils.StacktraceUtils;
import com.renomad.minum.web.Request;
import com.renomad.minum.web.Response;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static com.renomad.minum.web.StatusLine.StatusCode.CODE_401_UNAUTHORIZED;
import static com.renomad.minum.web.StatusLine.StatusCode.CODE_500_INTERNAL_SERVER_ERROR;


public class UploadPhoto {

    private final String uploadPhotoTemplateHtml;
    private final Db<Photograph> db;
    private final ILogger logger;
    private final Path dbDir;
    private final AuthUtils auth;
    private final Constants constants;

    public UploadPhoto(Db<Photograph> db, AuthUtils auth, Context context) {
        this.constants = context.getConstants();
        this.auth = auth;
        this.logger = context.getLogger();
        this.dbDir = Path.of(constants.dbDirectory);

        uploadPhotoTemplateHtml = new FileUtils(context.getLogger(), context.getConstants()).readTextFile("src/main/webapp/templates/uploadphoto/upload_photo_template.html");
        this.db = db;
    }

    public Response uploadPage(Request r) {
        AuthResult authResult = auth.processAuth(r);
        if (! authResult.isAuthenticated()) {
            return new Response(CODE_401_UNAUTHORIZED);
        }
        return Response.htmlOk(uploadPhotoTemplateHtml);
    }

    public Response uploadPageReceivePost(Request request) {
        AuthResult authResult = auth.processAuth(request);
        if (! authResult.isAuthenticated()) {
            return new Response(CODE_401_UNAUTHORIZED);
        }
        var photoBytes = request.body().asBytes("image_uploads");
        var shortDescription = request.body().asString("short_description");
        var description = request.body().asString("long_description");

        var newFilename = UUID.nameUUIDFromBytes(photoBytes).toString();
        final var newPhotograph = new Photograph(0, newFilename, shortDescription, description);
        Path photoDirectory = dbDir.resolve("photo_files");
        Path photoPath = photoDirectory.resolve(newFilename);
        try {
            logger.logDebug(() -> "Creating a directory for photo_files");
            boolean directoryExists = Files.exists(photoDirectory);
            logger.logDebug(() -> "Directory: " + photoDirectory + ". Already exists: " + directoryExists);
            if (!directoryExists) {
                logger.logDebug(() -> "Creating directory, since it does not already exist: " + photoDirectory);
                Files.createDirectories(photoDirectory);
                logger.logDebug(() -> "Directory: " + photoDirectory + " created");
            }

            Files.write(photoPath, photoBytes);
        } catch (IOException e) {
            logger.logAsyncError(() -> StacktraceUtils.stackTraceToString(e));
            return new Response(CODE_500_INTERNAL_SERVER_ERROR, e.toString());
        }
        db.write(newPhotograph);
        return Response.redirectTo("photos");
    }

    public List<Photograph> getPhotographs() {
        return db.values().stream().toList();
    }

}
