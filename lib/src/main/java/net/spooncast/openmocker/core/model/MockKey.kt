package net.spooncast.openmocker.core.model

/**
 * Mock 요청을 식별하기 위한 키 클래스
 * HTTP 메서드와 경로를 조합하여 고유한 요청을 식별합니다.
 *
 * @param method HTTP 메서드 (GET, POST, PUT, DELETE 등)
 * @param path 요청 경로 (예: "/api/users", "/api/posts/123")
 */
data class MockKey(
    val method: String,
    val path: String
) {
    init {
        require(method.isNotBlank()) { "Method cannot be blank" }
        require(path.isNotBlank()) { "Path cannot be blank" }
        require(method.matches(Regex("^[A-Z]+$"))) { "Method must be uppercase letters only" }
        require(path.startsWith("/")) { "Path must start with '/'" }
    }

    /**
     * MockKey를 문자열로 표현합니다.
     * 형식: "METHOD /path"
     */
    override fun toString(): String = "$method $path"
}