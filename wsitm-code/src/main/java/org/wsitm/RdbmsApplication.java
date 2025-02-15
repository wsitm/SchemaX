package org.wsitm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 启动程序
 *
 * @author wsitm
 */
@SpringBootApplication()
public class RdbmsApplication {
    public static void main(String[] args) {
        // System.setProperty("spring.devtools.restart.enabled", "false");
        SpringApplication.run(RdbmsApplication.class, args);
        System.out.println("^_^  程序启动成功  ^_^ \n" +
                "                                   _/    _/                     \n" +
                "    _/      _/      _/    _/_/_/      _/_/_/_/  _/_/_/  _/_/    \n" +
                "   _/      _/      _/  _/_/      _/    _/      _/    _/    _/   \n" +
                "    _/  _/  _/  _/        _/_/  _/    _/      _/    _/    _/    \n" +
                "     _/      _/      _/_/_/    _/      _/_/  _/    _/    _/     \n" +
                "                                                                ");
    }
}
