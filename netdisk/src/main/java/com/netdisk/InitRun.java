package com.netdisk;

import com.netdisk.component.RedisComponent;
import com.netdisk.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.sql.DataSource;

    /**
     * Spring Boot 应用启动时的初始化检查组件
     */
@Component("initRun")
public class InitRun implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(InitRun.class);

    @Resource
    private DataSource dataSource;

    @Resource
    private RedisComponent redisComponent;

    @Override
    public void run(ApplicationArguments args) {
        try {
            dataSource.getConnection();
            redisComponent.getSysSettingsDto();
            logger.error("服务启动成功，可以开始愉快的开发了");
        } catch (Exception e) {
            logger.error("数据库或者redis设置失败，请检查配置");
            throw new BusinessException("服务启动失败");
        }
    }
}
