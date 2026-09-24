package com.maccy.careeros

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = CareerDatabase.get(this)
        setContent {
            var draft by remember { mutableStateOf("") }
            var skills by remember { mutableStateOf(emptyList<SkillEntity>()) }
            val scope = rememberCoroutineScope()
            LaunchedEffect(Unit) { skills = database.skillDao().all() }
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("CareerOS", style = MaterialTheme.typography.displaySmall)
                        Text("Your career workspace, even offline.")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(value = draft, onValueChange = { draft = it }, label = { Text("Add a skill") }, modifier = Modifier.weight(1f))
                            Button(onClick = {
                                if (draft.isNotBlank()) {
                                    val skill = SkillEntity(draft.trim())
                                    draft = ""
                                    scope.launch {
                                        database.skillDao().save(skill)
                                        skills = skills.filterNot { it.name == skill.name } + skill
                                        scheduleSync()
                                    }
                                }
                            }) { Text("Add") }
                        }
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(skills, key = { it.name }) { skill -> Text("${skill.name} · ${skill.syncStatus}") }
                        }
                    }
                }
            }
        }
    }

    private fun scheduleSync() {
        val request = OneTimeWorkRequestBuilder<SkillSyncWorker>().setConstraints(
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
        ).build()
        WorkManager.getInstance(this).enqueueUniqueWork("careeros-skill-sync", ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }
}