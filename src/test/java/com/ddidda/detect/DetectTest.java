package com.ddidda.detect;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class DetectTest {

    @Test
    public void detectTest(){
        log.info("detectTest");
    }

    private MimeTypeDetector detector = new MimeTypeDetector();

    @Test
    public void testGlobLiteral() {
        log.info("detect begin" );
        String mime = detectMimeType("b-jpg.img");
        log.info("type: {}", mime );

    }

    @Test
    public void testGlobExtension() {
        assertEquals("text/plain", detectMimeType("abc.txt"));
        assertEquals("image/x-win-bitmap", detectMimeType("x.cur"));
        assertEquals("application/vnd.ms-tnef", detectMimeType("winmail.dat"));
        assertEquals("text/x-troff-mm", detectMimeType("abc.mm"));
        assertEquals("video/x-anim", detectMimeType("abc.anim5"));
        assertEquals("video/x-anim", detectMimeType("abc.animj"));
        assertEquals("application/x-compress", detectMimeType("README.Z"));
        assertEquals("application/vnd.ms-outlook", detectMimeType("t.pst"));
    }

    @Test
    public void testGlobFilename() {
        assertEquals("text/x-readme", detectMimeType("README"));
        assertEquals("text/x-readme", detectMimeType("READMEFILE"));
        assertEquals("text/x-readme", detectMimeType("READMEanim3"));
        assertEquals("text/x-log", detectMimeType("README.log"));
        assertEquals("text/x-readme", detectMimeType("README.file"));
    }

    @Test
    public void testOctetStream() {
        assertEquals("application/octet-stream", detectMimeType("empty"));
        assertEquals("application/octet-stream", detectMimeType("octet-stream"));
    }

    @Test
    public void testMultipleExtensions() {
        assertEquals("application/x-java-archive", detectMimeType("e.1.3.jar"));
    }

    @Test
    public void testMagic() {
        assertEquals("application/xml", detectMimeType("e[xml]"));
    }

    @Test
    public void testMagicIndent() {
        // "a\n" will match image/x-pcx if rules are treated as OR instead of AND.
        assertEquals("text/plain", detectMimeType("a"));
    }

    @Test
    public void testText() {
        assertEquals("text/plain", detectMimeType("plaintext"));
        assertEquals("text/plain", detectMimeType("textfiles/utf-8"));
        assertEquals("text/plain", detectMimeType("textfiles/windows-1255"));
    }

    @Test
    public void testMatchletSearchIsThorough() {
        // returns application/octet-stream if the entire matchlet range is not searched
        assertEquals("application/x-matroska", detectMimeType("mkv-video-header"));
    }

    @Test
    public void testRespectsMagicFileOrdering() {
        // MIME candidates are found in this order for this file: "application/ogg", "audio/ogg", "video/ogg" (note, the superclass comes first)
        // however, if a HashSet is used internally, the iterable order will be something like: "audio/ogg", "application/ogg", "video/ogg"
        // and "audio/ogg" is returned for video as well as audio (not good)
        assertEquals("application/ogg", detectMimeType("ogv-video-header"));
    }

    private String detectMimeType(String resourceName) {
        try (InputStream is = getClass().getResourceAsStream("/test/" + resourceName)) {
            return detector.detectMimeType(resourceName, is);
        } catch (GetBytesException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testEmptyFile() throws IOException, GetBytesException {
        File f = File.createTempFile("mime-type-test", ".weird");
        f.deleteOnExit();

        try (FileWriter fw = new FileWriter(f)) {
            // empty file
        }
        assertEquals("application/octet-stream", detector.detectMimeType(f));
    }

    @Test
    public void testFile() throws IOException, GetBytesException {
        File f = File.createTempFile("mime-type-test", ".weird");
        f.deleteOnExit();

        try (FileWriter fw = new FileWriter(f)) {
            fw.append("foo bar baz");
        }
        assertEquals("text/plain", detector.detectMimeType(f));
    }

    @Test
    public void testPath() throws IOException, GetBytesException {
        File f = File.createTempFile("mime-type-test", ".weird");
        f.deleteOnExit();

        try (FileWriter fw = new FileWriter(f)) {
            fw.append("foo bar baz");
        }
        assertEquals("text/plain", detector.detectMimeType(f.toPath()));
    }

    @Test
    public void testCallback() throws GetBytesException {
        Callable<byte[]> getBytes = new Callable<byte[]>() {
            public byte[] call() throws UnsupportedEncodingException {
                return "foo bar baz".getBytes("utf-8");
            }
        };

        assertEquals("text/plain", detector.detectMimeType("mime-type-test.weird", getBytes));
    }

    @Test
    public void testAsync() throws IOException, InterruptedException, ExecutionException {
        byte[] bytes = "foo bar baz".getBytes("utf-8");

        Supplier<CompletionStage<byte[]>> getBytes = () -> {
            return CompletableFuture.completedFuture(bytes);
        };

        assertEquals("text/plain", detector.detectMimeTypeAsync("mime-type-test.weird", getBytes).toCompletableFuture().get());
    }

    @Test
    public void testAsyncGetBytesException() throws IOException, InterruptedException, ExecutionException {
        Supplier<CompletionStage<byte[]>> getBytes = () -> {
            CompletableFuture<byte[]> future = new CompletableFuture<byte[]>();
            future.completeExceptionally(new GetBytesException(new IOException("oops")));
            return future;
        };

        try {
            detector.detectMimeTypeAsync("mime-type-test.weird", getBytes).toCompletableFuture().get();
            fail("That should have thrown an exception");
        } catch (ExecutionException ex) {
            assertEquals(GetBytesException.class, ex.getCause().getClass());
        }
    }
}
