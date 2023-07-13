package com.renomad.sampledomain;

import com.renomad.auth.AuthResult;
import com.renomad.auth.AuthUtils;
import com.renomad.sampledomain.photo.Photograph;
import minum.Constants;
import minum.Context;
import minum.database.DatabaseDiskPersistenceSimpler;
import minum.logging.ILogger;
import minum.utils.FileUtils;
import minum.utils.StacktraceUtils;
import minum.web.Request;
import minum.web.Response;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static minum.database.DatabaseDiskPersistenceSimpler.calculateNextIndex;
import static minum.web.StatusLine.StatusCode._401_UNAUTHORIZED;
import static minum.web.StatusLine.StatusCode._500_INTERNAL_SERVER_ERROR;


public class UploadPhoto {

    private final String uploadPhotoTemplateHtml;
    private final DatabaseDiskPersistenceSimpler<Photograph> ddps;
    private final AtomicLong newPhotographIndex;
    private final List<Photograph> photographs;
    private final ILogger logger;
    private final Path dbDir;
    private final AuthUtils auth;
    private final Constants constants;

    public UploadPhoto(DatabaseDiskPersistenceSimpler<Photograph> ddps, AuthUtils auth, Context context) {
        this.constants = context.getConstants();
        this.auth = auth;
        this.logger = context.getLogger();
        this.dbDir = Path.of(constants.DB_DIRECTORY);
        uploadPhotoTemplateHtml = FileUtils.readTemplate("uploadphoto/upload_photo_template.html");
        this.ddps = ddps;
        photographs = ddps.readAndDeserialize(Photograph.EMPTY);
        newPhotographIndex = new AtomicLong(calculateNextIndex(photographs));
    }

    public Response uploadPage(Request r) {
        AuthResult authResult = auth.processAuth(r);
        if (! authResult.isAuthenticated()) {
            return new Response(_401_UNAUTHORIZED);
        }
        return Response.htmlOk(uploadPhotoTemplateHtml);
    }

    public Response uploadPageReceivePost(Request request) {
        AuthResult authResult = auth.processAuth(request);
        if (! authResult.isAuthenticated()) {
            return new Response(_401_UNAUTHORIZED);
        }
        var photoBytes = request.body().asBytes("image_uploads");
        var shortDescription = request.body().asString("short_description");
        var description = request.body().asString("long_description");

        var newFilename = UUID.nameUUIDFromBytes(photoBytes).toString();
        final var newPhotograph = new Photograph(newPhotographIndex.getAndIncrement(), newFilename, shortDescription, description);
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
            return new Response(_500_INTERNAL_SERVER_ERROR, e.toString());
        }
        photographs.add(newPhotograph);
        ddps.persistToDisk(newPhotograph);
        return Response.redirectTo("photos");
    }

    public List<Photograph> getPhotographs() {
        return photographs.stream().toList();
    }

}
