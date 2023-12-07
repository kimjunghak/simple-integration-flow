package integration

import org.springframework.integration.annotation.MessagingGateway
import org.springframework.integration.file.FileHeaders
import org.springframework.messaging.handler.annotation.Header

@MessagingGateway(defaultRequestChannel = "textInChannel")
interface FileWriterGateway {

    fun writeToFile(@Header(FileHeaders.FILENAME) filename: String, data: String)
}