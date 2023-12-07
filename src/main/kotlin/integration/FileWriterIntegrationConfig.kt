package integration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.integration.annotation.Filter
import org.springframework.integration.annotation.InboundChannelAdapter
import org.springframework.integration.annotation.Poller
import org.springframework.integration.annotation.Router
import org.springframework.integration.annotation.ServiceActivator
import org.springframework.integration.annotation.Splitter
import org.springframework.integration.annotation.Transformer
import org.springframework.integration.channel.DirectChannel
import org.springframework.integration.core.GenericTransformer
import org.springframework.integration.core.MessageSource
import org.springframework.integration.dsl.IntegrationFlow
import org.springframework.integration.dsl.MessageChannels
import org.springframework.integration.file.FileReadingMessageSource
import org.springframework.integration.file.FileWritingMessageHandler
import org.springframework.integration.file.dsl.Files
import org.springframework.integration.file.filters.SimplePatternFileListFilter
import org.springframework.integration.file.support.FileExistsMode
import org.springframework.integration.router.AbstractMessageRouter
import org.springframework.integration.router.MessageRouter
import org.springframework.integration.router.PayloadTypeRouter
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.support.GenericMessage
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

@Configuration
class FileWriterIntegrationConfig {

    //    별도 채널 생성, @ServiceActivator(inputChannel = "orderChannel")
//    @Bean
//    fun orderChannel(): MessageChannel {
//        return PublishSubscribeChannel()
//    }

    @Bean
    @Transformer(inputChannel = "textInChannel", outputChannel = "fileWriterChannel")
    fun upperCaseTransformer(): GenericTransformer<String, String> {
        return GenericTransformer { text -> text.uppercase() }
    }

    @Bean
    @ServiceActivator(inputChannel = "fileWriterChannel", poller = Poller(fixedRate = "1000"))
    fun fileWriter(): FileWritingMessageHandler {
        val handler = FileWritingMessageHandler(File("/tmp/integration/files"))
        handler.setExpectReply(false)
        handler.setFileExistsMode(FileExistsMode.APPEND)
        handler.setAppendNewLine(true)
        return handler
    }

//    @Filter(inputChannel = "numberChannel", outputChannel = "evenChannel")
//    fun evenNumberFilter(number: Int): Boolean {
//        return number % 2 == 0
//    }

    @Bean
    @Router(inputChannel = "numberChannel")
    fun evenOddRouter(): AbstractMessageRouter {
        return object : AbstractMessageRouter() {
            override fun determineTargetChannels(message: Message<*>): Collection<MessageChannel> {
                val number = message.payload as Int
                return if (number % 2 == 0) {
                    listOf(evenChannel())
                } else {
                    listOf(oddChannel())
                }
            }
        }
    }

    @Bean
    fun evenChannel(): MessageChannel {
        return DirectChannel()
    }

    @Bean
    fun oddChannel(): MessageChannel {
        return DirectChannel()
    }

    @Bean
    @InboundChannelAdapter(poller = Poller(fixedRate = "1000"), channel = "numberChannel")
    fun numberSource(source: AtomicInteger): MessageSource<Int> {
        return MessageSource { GenericMessage(source.getAndIncrement()) }
    }

    @Bean
    fun source(): AtomicInteger {
        return AtomicInteger()
    }

    @Bean
    @InboundChannelAdapter(channel = "file-channel", poller = Poller(fixedDelay = "1000"))
    fun fileReadingMessageSource(): MessageSource<File> {
        val sourceReader = FileReadingMessageSource()
        sourceReader.setDirectory(File("INPUT_DIR"))
        sourceReader.setFilter(SimplePatternFileListFilter("FILE_PATTERN"))
        return sourceReader
    }

    @Profile("javadsl")
    @Bean
    fun fileWriterFlow(): IntegrationFlow {
        return IntegrationFlow
            .from(MessageChannels.direct("textInChannel"))
            .transform<String, String> { it.uppercase() }
            .channel(MessageChannels.direct("fileWriterChannel"))
            .handle(
                Files.outboundAdapter(File("/tmp/integration/files"))
                    .fileExistsMode(FileExistsMode.APPEND)
                    .appendNewLine(true)
            )
            .get()
    }

//    @Profile("javadsl")
//    @Bean
//    fun evenNumberFlow(): IntegrationFlow {
//        return IntegrationFlow
//            .from(MessageChannels.direct("numberChannel"))
//            .filter<Int> { it % 2 == 0 }
//            .channel(MessageChannels.direct("evenChannel"))
//            .get()
//    }

    @Profile("javadsl")
    @Bean
    fun numberRoutingFlow(): IntegrationFlow {
        return IntegrationFlow
            .from(MessageChannels.direct("numberRoutingChannel"))
            .route<Int, String>({n -> if (n % 2 == 0) "EVEN" else "ODD"}) { mapping -> mapping
                .subFlowMapping("EVEN") { it.channel(evenChannel()) }
                .subFlowMapping("ODD") { it.channel(oddChannel()) } }
            .get()
    }
}