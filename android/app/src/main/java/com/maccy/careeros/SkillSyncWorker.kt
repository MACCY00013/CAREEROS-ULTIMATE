package com.maccy.careeros

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.net.HttpURLConnection
import java.net.URL

class SkillSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val dao = CareerDatabase.get(applicationContext).skillDao()
        return try {
            dao.pending().forEach { skill ->
                val connection = URL("${BuildConfig.API_BASE_URL}/api/v1/skills/sync").openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 10_000
                connection.readTimeout = 10_000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { output ->
                    output.write("{\"name\":\"${skill.name.replace("\"", "\\\"")}\",\"level\":\"${skill.level}\"}".toByteArray())
                }
                if (connection.responseCode in 200..299) dao.markSynced(skill.name) else return Result.retry()
                connection.disconnect()
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}