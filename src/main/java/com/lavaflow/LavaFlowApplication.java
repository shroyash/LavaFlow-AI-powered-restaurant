
package com.lavaflow;

import com.lavaflow.common.config.EnvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LavaFlowApplication {

    public static void main(String[] args) {

        EnvLoader.loadEnv();
        SpringApplication.run(LavaFlowApplication.class, args);
    }
}
