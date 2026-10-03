package com.taskboard.monitoring;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.AppenderBase;
import com.taskboard.monitoring.LogDtos.LogEntry;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Logback appender를 Spring 빈으로 만들어 루트 로거에 직접 연결.
 * logback.xml에 선언하면 LogService를 주입할 수 없어 코드로 등록
 */
@Component
@RequiredArgsConstructor
public class LogCaptureAppender extends AppenderBase<ILoggingEvent> implements InitializingBean, DisposableBean {

    private final LogService logService;

    @Override
    public void afterPropertiesSet() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        setContext(loggerContext);
        setName("taskboard-log-capture");
        start();
        loggerContext.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(this);
    }

    @Override
    protected void append(ILoggingEvent event) {
        logService.record(toEntry(event));
    }

    @Override
    public void destroy() {
        ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger(Logger.ROOT_LOGGER_NAME).detachAppender(this);
        stop();
    }

    private LogEntry toEntry(ILoggingEvent event) {
        IThrowableProxy throwable = event.getThrowableProxy();
        return new LogEntry(
                event.getLevel().toString(),
                event.getLoggerName(),
                event.getThreadName(),
                event.getFormattedMessage(),
                throwable != null ? ThrowableProxyUtil.asString(throwable) : null,
                LocalDateTime.ofInstant(Instant.ofEpochMilli(event.getTimeStamp()), ZoneId.systemDefault()));
    }
}
