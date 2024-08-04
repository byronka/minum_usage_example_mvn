package com.renomad.sampledomain;

import com.renomad.auth.AuthUtils;
import com.renomad.minum.state.Constants;
import com.renomad.minum.state.Context;
import com.renomad.minum.logging.ILogger;
import com.renomad.minum.templating.TemplateProcessor;
import com.renomad.minum.utils.FileUtils;
import com.renomad.minum.utils.LRUCache;
import com.renomad.minum.utils.StacktraceUtils;
import com.renomad.minum.web.IRequest;
import com.renomad.minum.web.IResponse;
import com.renomad.minum.web.Response;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

import static com.renomad.minum.web.StatusLine.StatusCode.*;

public class ListPhotos {

    private final TemplateProcessor listPhotosTemplateProcessor;
    private final ILogger logger;
    private final Path dbDir;

    private final UploadPhoto up;
    private final AuthUtils auth;
    private final Map<String, byte[]> lruCache;
    private final Constants constants;

    public ListPhotos(Context context, UploadPhoto up, AuthUtils auth) {
        this.logger = context.getLogger();
        this.constants = context.getConstants();
        this.dbDir = Path.of(constants.dbDirectory);
        listPhotosTemplateProcessor = TemplateProcessor.buildProcessor(new FileUtils(context.getLogger(), context.getConstants()).readTextFile("src/main/webapp/templates/listphotos/list_photos_template.html"));
        this.up = up;
        this.auth = auth;
        this.lruCache = LRUCache.getLruCache();
    }

    public IResponse ListPhotosPage(IRequest r) {

        String photoHtml = up.getPhotographs().stream().map(x ->
        """
        <li>
            <img src="photo?name=%s" alt="photo alt text" title="photo title text" />
            <p>%s</p>
            <p>%s</p>
        </li>
        """.formatted(x.getPhotoUrl(), x.getShortDescription(), x.getDescription())).collect(Collectors.joining ("\n"));

        String navBar = auth.processAuth(r).isAuthenticated() ? "<p><a href=\"logout\">Logout</a></p><p><a href=\"upload\">Upload</a></p>" : "";

        String listPhotosHtml = listPhotosTemplateProcessor.renderTemplate(Map.of(
                "nav_bar", navBar,
                "photo_html", photoHtml
        ));
        return Response.htmlOk(listPhotosHtml);
    }

    /**
     * Like you would think - a way to read a photo from disk to put on the wire
     */
    public IResponse grabPhoto(IRequest r) {
        String filename = r.getRequestLine().queryString().get("name");
        logger.logAudit(() -> r.getRemoteRequester() + " is looking for a photo named " + filename);

        // if the name query is null or blank, return 404
        if (filename == null || filename.isBlank()) {
            return Response.buildLeanResponse(CODE_404_NOT_FOUND);
        }

        // first, is it already in our cache?
        if (lruCache.containsKey(filename)) {
            logger.logDebug(() -> "Found " + filename + " in the cache. Serving.");
            return Response.buildResponse(CODE_200_OK,
                    Map.of(
                            "Cache-Control","max-age=604800",
                            "Content-Type","image/jpeg"
                    ),
                    lruCache.get(filename));
        }
        // first let's check to see whether the file is even there.
        Path photoPath = dbDir.resolve("photo_files").resolve(filename);
        boolean doesFileExist = Files.exists(photoPath);

        // if it's not there, return 404
        if (! doesFileExist) {
            logger.logDebug(() -> "filename of " + filename + " does not exist in the directory");
            return Response.buildLeanResponse(CODE_404_NOT_FOUND);
        }

        // otherwise, read the bytes
        logger.logDebug(() -> "about to read file at " + photoPath);

        try (RandomAccessFile reader = new RandomAccessFile(photoPath.toString(), "r");
             FileChannel channel = reader.getChannel();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            int bufferSize = 1024;
            if (bufferSize > channel.size()) {
                bufferSize = (int) channel.size();
            }
            ByteBuffer buff = ByteBuffer.allocate(bufferSize);

            while (channel.read(buff) > 0) {
                out.write(buff.array(), 0, buff.position());
                buff.clear();
            }

            byte[] bytes = out.toByteArray();
            if (bytes.length == 0) {
                logger.logDebug(() -> filename + " photo filesize was 0.  Sending 404");
                return Response.buildLeanResponse(CODE_404_NOT_FOUND);
            } else {
                String s = filename + "photo filesize was " + bytes.length + " bytes.";
                logger.logDebug(() -> s);

                logger.logDebug(() -> "Storing " + filename + " in the cache");
                lruCache.put(filename, bytes);

                return Response.buildResponse(CODE_200_OK,
                        Map.of(
                                "Cache-Control","max-age=604800",
                                "Content-Type","image/jpeg"
                        ),
                        bytes);

            }
        } catch (IOException e){
            logger.logAsyncError(() -> "There was an issue reading a file at " + photoPath + ". " + StacktraceUtils.stackTraceToString(e));
            return Response.buildLeanResponse(CODE_500_INTERNAL_SERVER_ERROR);
        }


    }

}
