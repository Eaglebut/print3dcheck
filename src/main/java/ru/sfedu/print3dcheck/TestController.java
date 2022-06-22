package ru.sfedu.print3dcheck;

import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@RestController
@RequestMapping("/api/printstatus")
@Log4j2
public class TestController {

    private static final int BUFFER_SIZE = 1024;
    private static final long TIMEOUT = 2000;
    private static final String PYTHON_PATH = "C:\\Python39\\python.exe";
    private static final String SCRIPT_PATH = "Z:\\projects\\print3dcheck\\neural\\main.py";
    private static final String WORK_DIR = "Z:\\projects\\print3dcheck\\neural";
    private static final String GET_PICTURE_URL = "http://192.168.0.29:4747/cam/1/frame.jpg";
    private static final String START_STREAM_URL = "http://192.168.0.29:4747/video/";
    private static final String PICTURE_PATH = "neural/test.jpg";

    @GetMapping(path = "/getpicture")
    public void getPicture(HttpServletResponse response) throws IOException {
        try (
                BufferedInputStream input = new BufferedInputStream(new URL(GET_PICTURE_URL).openStream());
                ServletOutputStream output = response.getOutputStream()
        ) {
            getPictureFromIpCam(input, output);
        } catch (IOException e) {
            log.error(e);
            throw e;
        }
    }

    @GetMapping
    public String testPrint() throws IOException {
        startIpCamVideoLoading(TIMEOUT);
        try (
                BufferedInputStream input = new BufferedInputStream(new URL(GET_PICTURE_URL).openStream());
                FileOutputStream outFile = new FileOutputStream(PICTURE_PATH)
        ) {
            getPictureFromIpCam(input, outFile);
            Process process = startCheck();
            Scanner inputScan = new Scanner(process.getInputStream());
            String result = "";
            if (inputScan.hasNext()){
                result = inputScan.next();
                log.info(result);
            }
            Gson gson = new Gson();
            return gson.toJson(new Result(Integer.parseInt(result)));
        } catch (IOException e) {
            log.error(e);
            throw e;
        }
    }

    private void startIpCamVideoLoading(long timout) {
        new Thread(() -> {
            try (
                    BufferedInputStream in =
                            new BufferedInputStream(new URL(START_STREAM_URL).openStream())
            ) {
                byte[] dataBuffer = new byte[BUFFER_SIZE];
                long startTime = System.currentTimeMillis();
                while (
                        in.read(dataBuffer, 0, BUFFER_SIZE) != -1 &&
                                System.currentTimeMillis() - startTime < timout
                );
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

    private Process startCheck() throws IOException {
        List<String> command = new ArrayList<>();
        command.add(PYTHON_PATH);
        command.add(SCRIPT_PATH);
        File workingDir = new File(WORK_DIR);
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        processBuilder.directory(workingDir);
        Process process;
        process = processBuilder.start();
        return process;
    }
    @Getter
    @Setter
    @AllArgsConstructor
    private static class Result {
        private Integer status;
    }
}
