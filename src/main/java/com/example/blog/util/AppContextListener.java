package com.example.blog.util;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * 웹앱 시작/종료 시 커넥션 풀을 초기화/정리한다.
 */
@WebListener
public class AppContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        Database.init();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        Database.close();
    }
}
