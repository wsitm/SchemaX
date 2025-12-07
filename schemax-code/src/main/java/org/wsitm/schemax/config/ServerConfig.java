package org.wsitm.schemax.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

/**
 * 服务配置
 *
 * @author wsitm
 */
@Configuration
//@MapperScan("org.wsitm.schemax.mapper")
public class ServerConfig implements ApplicationListener<ApplicationEvent> {

    private static final Logger log = LoggerFactory.getLogger(ServerConfig.class);

    @Bean(name = "threadPoolExecutor")
    public java.util.concurrent.ThreadPoolExecutor threadPoolExecutor() {
        return cn.hutool.core.thread.ThreadUtil.newExecutor();
    }


    @Override
    public void onApplicationEvent(@NonNull ApplicationEvent event) {
//        if (event instanceof ApplicationReadyEvent) {
//            ThreadUtil.execute(() -> {
//                log.info("加载驱动包...");
//                // 获取jdbc信息并初始化，加载 jdbc 到类加载器中
//                List<JdbcInfo> jdbcInfoList = RdbmsUtil.getJdbcInfoList();
//                if (CollUtil.isNotEmpty(jdbcInfoList)) {
//                    for (JdbcInfo jdbcInfo : jdbcInfoList) {
//                        RdbmsUtil.loadJdbcJar(jdbcInfo);
//                    }
//                }
//            });
//        }
//        if (event instanceof ContextClosedEvent) {
//            log.info("保存缓存到硬盘...");
//            CacheUtil.getCacheManager().close();
//        }
    }

}
