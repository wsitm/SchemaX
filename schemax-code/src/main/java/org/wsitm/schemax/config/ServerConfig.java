package org.wsitm.schemax.config;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.thread.ThreadUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.wsitm.schemax.entity.domain.JdbcInfo;
import org.wsitm.schemax.entity.vo.JdbcInfoVo;
import org.wsitm.schemax.mapper.JdbcInfoMapper;
import org.wsitm.schemax.utils.RdbmsUtil;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 服务配置
 *
 * @author wsitm
 */
@Configuration
public class ServerConfig implements ApplicationListener<ApplicationEvent> {
    private static final Logger log = LoggerFactory.getLogger(ServerConfig.class);

    @Autowired
    private JdbcInfoMapper jdbcInfoMapper;

    @Bean(name = "threadPoolExecutor")
    public ThreadPoolExecutor threadPoolExecutor() {
        return ThreadUtil.newExecutor();
    }


    @Override
    public void onApplicationEvent(@NonNull ApplicationEvent event) {
        if (event instanceof ApplicationStartedEvent) {
            log.info("加载驱动包...");
            // 获取jdbc信息并初始化，加载 jdbc 到类加载器中
            List<JdbcInfoVo> jdbcInfoList = jdbcInfoMapper.selectJdbcInfoList(new JdbcInfo());
            if (CollUtil.isNotEmpty(jdbcInfoList)) {
                for (JdbcInfo jdbcInfo : jdbcInfoList) {
                    RdbmsUtil.loadJdbcJar(jdbcInfo);
                }
            }
        }
//        if (event instanceof ContextClosedEvent) {
//        }
    }

}
