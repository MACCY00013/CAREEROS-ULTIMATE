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
import androidx.compose.material3.Card
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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

private enum class AppTab(val label: String, val icon: String) {
    HOME("Home", "⌂"), SKILLS("Skills", "✓"), CAREER("Career", "◎"), JOBS("Jobs", "▣"), MORE("More", "•••")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = CareerDatabase.get(this)
        setContent {
            var draft by remember { mutableStateOf("") }
            var skills by remember { mutableStateOf(emptyList<SkillEntity>()) }
            var selectedTab by remember { mutableStateOf(AppTab.HOME) }
            var trackerItems by remember { mutableStateOf(listOf("Wishlist", "Applied", "Interviewing", "Offer", "Rejected")) }
            val scope = rememberCoroutineScope()
            LaunchedEffect(Unit) { skills = database.skillDao().all() }
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        when (selectedTab) {
                            AppTab.HOME -> HomeScreen(skills.size, trackerItems.size - 5)
                            AppTab.SKILLS -> SkillsScreen(skills, draft, { draft = it }, {
                                if (draft.isNotBlank()) {
                                    val skill = SkillEntity(draft.trim())
                                    draft = ""
                                    scope.launch {
                                        database.skillDao().save(skill)
                                        skills = skills.filterNot { it.name == skill.name } + skill
                                        scheduleSync()
                                    }
                                }
                            })
                            AppTab.CAREER -> CareerScreen()
                            AppTab.JOBS -> JobsScreen()
                            AppTab.MORE -> MoreScreen(trackerItems) { trackerItems = trackerItems + "New application" }
                        }
                        NavigationBar {
                            AppTab.entries.forEach { tab ->
                                NavigationBarItem(selected = selectedTab == tab, onClick = { selectedTab = tab }, icon = { Text(tab.icon) }, label = { Text(tab.label) })
                            }
                        }
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun HomeScreen(skillCount: Int, applicationCount: Int) {
        LazyColumn(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Text("CareerOS", style = MaterialTheme.typography.displaySmall); Text("Your career workspace, online or offline.") }
            item { SummaryCard("Skills in your vault", skillCount.toString(), "Keep building proof of your strengths.") }
            item { SummaryCard("Applications", applicationCount.toString(), "Track every opportunity from wishlist to offer.") }
            item { SummaryCard("Next move", "Choose a target role", "Career paths turn your skills into practical steps.") }
            item { Text("Sync runs automatically when Wi-Fi or cellular data is available.", style = MaterialTheme.typography.bodySmall) }
        }
    }

    @androidx.compose.runtime.Composable
    private fun SkillsScreen(skills: List<SkillEntity>, draft: String, onDraftChange: (String) -> Unit, onAdd: () -> Unit) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Skills Vault", style = MaterialTheme.typography.headlineLarge)
            Text("Add and edit skills offline. Pending records sync in the background.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = draft, onValueChange = onDraftChange, label = { Text("Add a skill") }, modifier = Modifier.weight(1f))
                Button(onClick = onAdd) { Text("Add") }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(skills, key = { it.name }) { skill -> Card(modifier = Modifier.fillMaxWidth()) { Text("${skill.name}\n${skill.level} · ${skill.syncStatus}", modifier = Modifier.padding(16.dp)) } }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun CareerScreen() {
        val careers = listOf("Backend Engineer" to "Python · REST APIs · SQL · Docker", "Cloud Security Engineer" to "IAM · AWS · Kubernetes", "Data Analyst" to "SQL · Data analytics · Power BI", "AI/ML Engineer" to "Python · Machine learning")
        LazyColumn(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Career Paths", style = MaterialTheme.typography.headlineLarge); Text("Roles aligned to your growing capability.") }
            items(careers) { (title, skills) -> SummaryCard(title, "Target role", skills) }
        }
    }

    @androidx.compose.runtime.Composable
    private fun JobsScreen() {
        val jobs = listOf("Associate Software Engineer · Accenture" to "Bengaluru", "Cloud Support Associate · AWS" to "Hyderabad", "IAM Engineer · Okta" to "Remote")
        LazyColumn(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Live Job Feed", style = MaterialTheme.typography.headlineLarge); Text("Cached listings remain visible offline.") }
            items(jobs) { (title, location) -> SummaryCard(title, location, "☆  Bookmark this opportunity") }
        }
    }

    @androidx.compose.runtime.Composable
    private fun MoreScreen(items: List<String>, onAddApplication: () -> Unit) {
        var selectedMore by remember { mutableStateOf("Applications") }
        var resumeText by remember { mutableStateOf("") }
        var prompt by remember { mutableStateOf("") }
        LazyColumn(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("More Workspace", style = MaterialTheme.typography.headlineLarge) }
            item { Button(onClick = { selectedMore = "Applications" }) { Text("Application Tracker") }; Button(onClick = { selectedMore = "Resume & ATS" }) { Text("Resume & ATS") }; Button(onClick = { selectedMore = "AI Hub" }) { Text("AI Hub") }; Button(onClick = { selectedMore = "Profile & Settings" }) { Text("Profile & Settings") } }
            item {
                when (selectedMore) {
                    "Applications" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("Application Tracker", style = MaterialTheme.typography.titleLarge); Button(onClick = onAddApplication) { Text("Add application") }; items.forEach { Text("• $it") } }
                    "Resume & ATS" -> Column { Text("Resume & ATS", style = MaterialTheme.typography.titleLarge); OutlinedTextField(resumeText, { resumeText = it }, label = { Text("Resume notes") }, modifier = Modifier.fillMaxWidth()); Button(onClick = {}) { Text("Run ATS check when online") } }
                    "AI Hub" -> Column { Text("AI Hub", style = MaterialTheme.typography.titleLarge); Text("Prompts queue offline and process when connected."); OutlinedTextField(prompt, { prompt = it }, label = { Text("Ask your career copilot") }, modifier = Modifier.fillMaxWidth()); Button(onClick = {}) { Text("Queue prompt") } }
                    else -> Column { Text("Profile & Settings", style = MaterialTheme.typography.titleLarge); Text("Saket Yadav · Maccy Creations"); Text("Legal, privacy, sync, and account controls") }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun SummaryCard(title: String, value: String, detail: String) {
        Card(modifier = Modifier.fillMaxWidth()) { Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(value, style = MaterialTheme.typography.headlineSmall); Text(detail, style = MaterialTheme.typography.bodySmall) } }
    }

    private fun scheduleSync() {
        val request = OneTimeWorkRequestBuilder<SkillSyncWorker>().setConstraints(
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
        ).build()
        WorkManager.getInstance(this).enqueueUniqueWork("careeros-skill-sync", ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }
}