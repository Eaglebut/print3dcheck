package ru.sfedu.print3dcheck;

import lombok.extern.log4j.Log4j2;
import org.apache.tomcat.util.buf.ByteBufferUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URL;

@RestController
@RequestMapping("/api/testprint")
@Log4j2
public class TestController {

    private static final int BUFFER_SIZE = 1024;
    private static final long TIMEOUT = 2000;

    @GetMapping
    public void testPrint(HttpServletResponse response) {
        startIpCamVideoLoading(TIMEOUT);
        try (
                BufferedInputStream input = new BufferedInputStream(new URL("http://192.168.0.29:4747/cam/1/frame.jpg").openStream());
                ServletOutputStream output = response.getOutputStream();
        ) {
            getPictureFromIpCam(input, output);
        } catch (IOException e) {
            log.error(e);
        }
    }

    private void startIpCamVideoLoading(long timout) {
        new Thread(() -> {
            try (
                    BufferedInputStream in = new BufferedInputStream(new URL("http://192.168.0.29:4747/video/").openStream())
            ) {
                byte[] dataBuffer = new byte[BUFFER_SIZE];
                long startTime = System.currentTimeMillis();
                while (in.read(dataBuffer, 0, BUFFER_SIZE) != -1 && System.currentTimeMillis() - startTime < timout) ;
            } catch (IOException e) {
                log.error(e);
            }
        }).start();
    }

    private void getPictureFromIpCam(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] dataBuffer = new byte[BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = inputStream.read(dataBuffer, 0, BUFFER_SIZE)) != -1) {
            outputStream.write(dataBuffer, 0, bytesRead);
        }
    }
}
