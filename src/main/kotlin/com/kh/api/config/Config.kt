package com.kh.api.config

import com.kh.occupying.Korail
import com.kh.occupying.KorailProperties
import com.kh.occupying.WebClientWrapper
import com.kh.service.AlarmSender
import com.kh.service.BackgroundExecutor
import com.kh.util.SecretProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.scheduling.annotation.AsyncConfigurer
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor
import java.util.concurrent.ThreadPoolExecutor

@Configuration
class AppConfig {

    @Bean
    fun korail() = Korail(WebClientWrapper(KorailProperties()))

    @Bean
    fun getJavaMailSender(): JavaMailSender {
        val secret = SecretProperties()
        return NaverMailSenderConfig.getSender(
                id = secret.email.id,
                pw = secret.email.pw
        )
    }

    @Bean
    fun alarmSender() = AlarmSender(getJavaMailSender())

    @Bean
    fun asyncService() = BackgroundExecutor(korail(), alarmSender())

}

@Configuration
@EnableAsync
class AsyncConfig : AsyncConfigurer {

    override fun getAsyncExecutor(): Executor? {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 5
        executor.maxPoolSize = 10
        executor.setQueueCapacity(5)
        executor.setRejectedExecutionHandler(ThreadPoolExecutor.CallerRunsPolicy())
        executor.initialize()
        return executor
    }

}