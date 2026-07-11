import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
data class GenerateContentRequest(
    val tools: List<Tool>? = null
)

@Serializable
data class Tool(
    val googleSearch: GoogleSearch? = null
)

@Serializable
data class GoogleSearch(val ignore: String? = null)

fun main() {
    val req = GenerateContentRequest(tools = listOf(Tool(googleSearch = GoogleSearch())))
    println(Json.encodeToString(req))
}
