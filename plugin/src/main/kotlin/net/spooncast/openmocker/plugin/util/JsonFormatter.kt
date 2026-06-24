package net.spooncast.openmocker.plugin.util

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser

/**
 * JSON 문자열을 들여쓰기된 형태로 정렬한다. 외부 런타임 의존성 0 원칙에 따라 플랫폼 번들 Gson 만
 * 사용한다(플러그인의 다른 JSON 처리와 동일).
 *
 * 입력이 유효한 JSON 이 아니거나 비어 있으면 원문을 그대로 돌려준다 — Body/payload 에는 plain
 * text·XML 등 JSON 이 아닌 값도 들어올 수 있어, 정렬 실패가 입력을 훼손하지 않게 한다.
 */
object JsonFormatter {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun pretty(raw: String): String {
        if (raw.isBlank()) return raw
        return try {
            val element = JsonParser.parseString(raw)
            if (element.isJsonNull) raw else gson.toJson(element)
        } catch (e: Exception) {
            raw
        }
    }
}
