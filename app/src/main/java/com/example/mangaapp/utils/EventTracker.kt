package com.example.mangaapp.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.mangaapp.repository.MangaRepository
import org.json.JSONArray
import org.json.JSONObject

object EventTracker {

    private const val PREF_NAME = "event_tracker_prefs"
    private const val KEY_EVENTS = "tracked_events"
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Ghi nhận sự kiện lưu cục bộ (4.1)
     */
    fun logEvent(eventName: String, params: Map<String, String> = emptyMap()) {
        if (!::prefs.isInitialized) {
            Log.w("EventTracker", "EventTracker chua duoc init!")
            return
        }

        val eventObj = JSONObject()
        eventObj.put("eventName", eventName)
        eventObj.put("timestamp", System.currentTimeMillis())
        
        val paramsObj = JSONObject()
        params.forEach { (k, v) -> paramsObj.put(k, v) }
        eventObj.put("params", paramsObj)

        // Đọc danh sách cũ và thêm vào
        val currentEventsJson = prefs.getString(KEY_EVENTS, "[]") ?: "[]"
        val eventsArray = JSONArray(currentEventsJson)
        eventsArray.put(eventObj)

        prefs.edit().putString(KEY_EVENTS, eventsArray.toString()).apply()
        Log.d("EventTracker", "Da luu cuc bo event: $eventName")
    }

    /**
     * Đồng bộ lên server (4.1)
     */
    fun syncEventsToBackend() {
        if (!::prefs.isInitialized) return
        val currentEventsJson = prefs.getString(KEY_EVENTS, "[]") ?: "[]"
        val eventsArray = JSONArray(currentEventsJson)

        if (eventsArray.length() == 0) {
            Log.d("EventTracker", "Khong co event nao de sync.")
            return
        }

        // Tưởng tượng gọi gọi API backend hoặc Firebase tại đây
        // VD: MangaRepository.syncLogs(eventsArray)
        // Hiện tại chỉ log ra console để mô phỏng Server Call
        Log.d("EventTracker", "Sync len backend ${eventsArray.length()} events...")

        // Sau khi báo thành công, xoá cục bộ để chuẩn bị đợt sync tiếp theo
        prefs.edit().remove(KEY_EVENTS).apply()
        Log.d("EventTracker", "Da sync thanh cong va xoa local events.")
    }
}
