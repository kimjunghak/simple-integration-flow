# Spring Integration

## 통합 플로우

1. 채널`Channel`: 메시지 전달
2. 필터`Filter`: 조건에 맞는 메시지만 선별
3. 변환기`Transformer`: 메시지 값이나 payload의 타입을 변환
4. 라우터`Router`: 여러 채널 중 하나로 전달(보통 메시지 헤더 기반)
5. 분배기`Splitter`: 두 개 이상의 메시지로 분할 -> 각각 다른 채널로 전송
6. 집적기`Aggregator`: 분배기와 상반된 것 -> 다수의 메시지를 하나로 결합
7. 서비스 엑티베이터`Service activator`: 자바 메서드에 메세지를 넘겨준 후 메서드의 반환값을 출력 채널로 전송
8. 채널 어댑터`Channel adapter`: 외부 시스템에 채널을 연결 -> 입력 받거나 쓸 수 있다.
9. 게이트웨이`Gateway`: 인터페이스를 통해 통합 플로우로 데이터 전달


### 분배기`Splitter`

```kotlin
class OrderSplitter {
    fun splitOrderIntoParts(po: PuchaseOrder): Collection<Any> {
        val parts = ArrayList<Any>()
        parts.add(po.billingInfo)
        parts.add(po.lineItems)
        return parts
    }
}

@Bean
@Splitter(inputChannel = "poChannel", outputChannel = "splitOrderChannel")
fun orderSplitter(): OrderSpliiter {
    return OrderSplitter()
}

@Bean
@Router(inputChannel = "splitOrderChannel")
fun splitOrderRouter(): MessageRouter {
    val router = PayloadTypeRouter()
    router.channelMappings[BillingInfo::class.java.name] = "billingInfoChannel"
    router.channelMappings[List::class.java.name] = "lineItemsChannel"
    return router
}

@Bean
@Splitter(inputChannel = "lineItemsChannel", outputChannel = "lineItemChannel")
fun lineItemSplitter(lineItems: List<LineItem>): List<LineItem> {
    return lineItems
}
```