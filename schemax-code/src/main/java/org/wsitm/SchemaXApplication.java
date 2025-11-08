package org.wsitm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 启动程序
 *
 * @author wsitm
 */
@SpringBootApplication(exclude = {
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class
})
public class SchemaXApplication {
    public static void main(String[] args) {
        // System.setProperty("spring.devtools.restart.enabled", "false");
        SpringApplication.run(SchemaXApplication.class, args);
        System.out.println("^_^  程序启动成功  ^_^ \n" +
                "                                                                                   \n" +
                "      _/_/_/            _/                                            _/      _/   \n" +
                "   _/          _/_/_/  _/_/_/      _/_/    _/_/_/  _/_/      _/_/_/    _/  _/      \n" +
                "    _/_/    _/        _/    _/  _/_/_/_/  _/    _/    _/  _/    _/      _/         \n" +
                "       _/  _/        _/    _/  _/        _/    _/    _/  _/    _/    _/  _/        \n" +
                "_/_/_/      _/_/_/  _/    _/    _/_/_/  _/    _/    _/    _/_/_/  _/      _/       \n" +
                "                                                                                   \n" +
                "                                                                                   \n");
    }
}
