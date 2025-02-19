package com.example.firedatabase_assis.home_page

import android.util.Log
import com.example.firedatabase_assis.postgres.CommentDto
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.lang.reflect.Type
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

class CommentWebSocketManager(
    private val serverUrl: String,
    private val viewModel: CommentsViewModel,
    private val scope: CoroutineScope
) {
    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var isConnecting = false
    private val maxReconnectAttempts = 5
    private var reconnectAttempts = 0
    private val baseReconnectDelayMs = 2000L

    private val pendingMessages = ArrayBlockingQueue<ClientMessage>(100)

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val gson = GsonBuilder().setLenient()
        .registerTypeAdapter(ServerMessage::class.java, object : JsonDeserializer<ServerMessage> {
            override fun deserialize(
                json: JsonElement,
                typeOfT: Type,
                context: JsonDeserializationContext
            ): ServerMessage? {
                try {
                    val jsonObject = json.asJsonObject
                    when (jsonObject.get("type")?.asString) {
                        "NEW_ROOT_COMMENT" -> {
                            val comment = context.deserialize<CommentDto>(
                                jsonObject.get("comment"),
                                CommentDto::class.java
                            )
                            return ServerMessage.NewRootComment(comment)
                        }

                        "NEW_REPLY" -> {
                            val comment = context.deserialize<CommentDto>(
                                jsonObject.get("comment"),
                                CommentDto::class.java
                            )
                            val parentId = jsonObject.get("parentId").asInt
                            return ServerMessage.NewReply(comment, parentId)
                        }

                        "REPLY_COUNT_UPDATE" -> {
                            val commentId = jsonObject.get("commentId").asInt
                            val newCount = jsonObject.get("newCount").asInt
                            return ServerMessage.ReplyCountUpdate(commentId, newCount)
                        }

                        "SUBSCRIPTION_UPDATED" -> {
                            Log.d(
                                "WebSocket",
                                "Subscription updated: ${jsonObject.get("count")?.asInt} sections"
                            )
                            return null
                        }

                        "ERROR" -> {
                            Log.e(
                                "WebSocket",
                                "Server error: ${jsonObject.get("message")?.asString}"
                            )
                            return null
                        }

                        else -> {
                            Log.w(
                                "WebSocket",
                                "Unknown message type: ${jsonObject.get("type")?.asString}"
                            )
                            return null
                        }
                    }
                } catch (e: Exception) {
                    Log.e("WebSocket", "Error deserializing message: $json", e)
                    return null
                }
            }
        })
        .create()

    enum class ConnectionState {
        CONNECTED,
        DISCONNECTED,
        CONNECTING,
        RECONNECTING
    }

    fun connect() {
        if (isConnecting || _connectionState.value == ConnectionState.CONNECTED) {
            return
        }

        isConnecting = true
        _connectionState.value = ConnectionState.CONNECTING

        val client = OkHttpClient.Builder()
            .pingInterval(30, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url(serverUrl)
            .build()

        try {
            webSocket = client.newWebSocket(request, createWebSocketListener())
        } catch (e: Exception) {
            Log.e("WebSocket", "Error creating WebSocket", e)
            handleConnectionFailure()
        }
    }

    private fun createWebSocketListener() = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            scope.launch {
                Log.d("WebSocket", "Connection opened")
                isConnecting = false
                reconnectAttempts = 0
                _connectionState.value = ConnectionState.CONNECTED
                processPendingMessages()
                // Resubscribe to previously expanded sections
                updateSubscriptions(viewModel.visibleReplySections.value)
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            scope.launch {
                Log.d("WebSocket", "Received message: $text")
                try {
                    val message = gson.fromJson(text, ServerMessage::class.java)
                    message?.let { handleServerMessage(it) }
                } catch (e: Exception) {
                    Log.e("WebSocket", "Error parsing message: $text", e)
                }
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e("WebSocket", "Connection failed with exception: ${t.javaClass.simpleName}", t)
            Log.e("WebSocket", "Exception message: ${t.message}")
            t.stackTrace.firstOrNull()?.let {
                Log.e(
                    "WebSocket",
                    "First point of failure: ${it.className}.${it.methodName} line ${it.lineNumber}"
                )
            }

            response?.let { res ->
                Log.e(
                    "WebSocket", """
                    Response details:
                    Code: ${res.code}
                    Message: ${res.message}
                    Protocol: ${res.protocol}
                    Headers: ${res.headers}
                    Is successful: ${res.isSuccessful}
                """.trimIndent()
                )
            }

            handleConnectionFailure()
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.d("WebSocket", "Connection closing. Code: $code, Reason: $reason")
            webSocket.close(1000, null)
            handleConnectionFailure()
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d("WebSocket", "Connection closed. Code: $code, Reason: $reason")
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    private fun handleConnectionFailure() {
        if (_connectionState.value != ConnectionState.RECONNECTING) {
            reconnect()
        }
    }

    private fun reconnect() {
        reconnectJob?.cancel()

        if (reconnectAttempts >= maxReconnectAttempts) {
            _connectionState.value = ConnectionState.DISCONNECTED
            reconnectAttempts = 0
            return
        }

        _connectionState.value = ConnectionState.RECONNECTING

        reconnectJob = scope.launch {
            val delayMs = baseReconnectDelayMs * (1 shl reconnectAttempts)
            delay(delayMs)
            reconnectAttempts++
            connect()
        }
    }

    private fun handleServerMessage(message: ServerMessage) {
        when (message) {
            is ServerMessage.NewRootComment -> {
                viewModel.addNewRootComment(message.comment)
            }

            is ServerMessage.NewReply -> {
                if (viewModel.visibleReplySections.value.contains(message.parentId)) {
                    viewModel.addNewReply(message.comment)
                }
            }

            is ServerMessage.ReplyCountUpdate -> {
                val currentCounts = viewModel._replyCountsMap.value.toMutableMap()
                currentCounts[message.commentId] = message.newCount
                viewModel._replyCountsMap.value = currentCounts
            }
        }
    }

    fun updateSubscriptions(expandedSections: Set<Int>) {
        val message = mapOf(
            "expandedSections" to expandedSections.toList()
        )
        if (_connectionState.value == ConnectionState.CONNECTED) {
            val jsonMessage = gson.toJson(message)
            Log.d("WebSocket", "Sending subscription update: $jsonMessage")
            webSocket?.send(jsonMessage)
        } else {
            pendingMessages.offer(ClientMessage.UpdateSubscription(expandedSections))
        }
    }

    private fun processPendingMessages() {
        while (pendingMessages.isNotEmpty()) {
            val message = pendingMessages.poll()
            message?.let {
                updateSubscriptions((it as ClientMessage.UpdateSubscription).expandedSections)
            }
        }
    }

    fun disconnect() {
        scope.launch {
            try {
                reconnectJob?.cancel()
                webSocket?.close(1000, "User left")
            } catch (e: Exception) {
                Log.e("WebSocket", "Error closing connection", e)
            } finally {
                webSocket = null
                _connectionState.value = ConnectionState.DISCONNECTED
                pendingMessages.clear()
                isConnecting = false
                reconnectAttempts = 0
            }
        }
    }
}