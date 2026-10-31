package co.luka.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import co.luka.data.db.CaptureEntity
import co.luka.data.repository.CaptureRepository
import co.luka.data.repository.CatalogRepository
import co.luka.data.repository.ManualEntry
import co.luka.ui.components.LukaCard
import co.luka.ui.components.PrimaryButton
import co.luka.ui.components.SecondaryButton
import co.luka.ui.movements.MovementForm
import co.luka.ui.movements.localDateFromPicker
import co.luka.ui.theme.LocalLukaColors
import co.luka.ui.theme.LukaSheetShape
import co.luka.ui.theme.Space
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val captures: CaptureRepository,
    catalog: CatalogRepository,
) : ViewModel() {
    val pending = captures.observeReview().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val categories = catalog.observeCategories(false).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val sources = catalog.observeSources().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    var error by mutableStateOf<String?>(null)
        private set

    fun complete(capture: CaptureEntity, entry: ManualEntry, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                captures.complete(capture.id, entry.copy(sourceId = entry.sourceId ?: capture.sourceId))
                error = null
                onDone()
            } catch (exception: IllegalArgumentException) {
                error = exception.message
            }
        }
    }

    fun ignore(id: Long) { viewModelScope.launch { captures.ignore(id) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(viewModel: ReviewViewModel = hiltViewModel()) {
    val pending by viewModel.pending.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val sources by viewModel.sources.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<CaptureEntity?>(null) }
    var showDate by remember { mutableStateOf(false) }
    var pendingDate by remember { mutableStateOf<((LocalDate) -> Unit)?>(null) }
    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = Space.s20, end = Space.s20, top = Space.s20, bottom = Space.s32),
        verticalArrangement = Arrangement.spacedBy(Space.s12),
    ) {
        item {
            Text("Por revisar", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
        }
        if (pending.isEmpty()) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(top = Space.s32),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Space.s8),
                ) {
                    Box(
                        Modifier.size(72.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.Inbox, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    }
                    Text("Nada por revisar", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), textAlign = TextAlign.Center)
                    Text(
                        "Las notificaciones que no se entiendan aparecen aquí.",
                        color = LocalLukaColors.current.muted,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        items(pending, key = { it.id }) { capture ->
            LukaCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Space.s12)) {
                    Text(capture.body.ifBlank { capture.title }, style = MaterialTheme.typography.bodyLarge)
                    PrimaryButton("Completar", onClick = { selected = capture }, modifier = Modifier.fillMaxWidth())
                    SecondaryButton("Ignorar", onClick = { viewModel.ignore(capture.id) }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
    if (showDate) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    localDateFromPicker(state.selectedDateMillis)?.let { pendingDate?.invoke(it) }
                    showDate = false
                }) { Text("Listo") }
            },
        ) { DatePicker(state = state) }
    }
    selected?.let { capture ->
        ModalBottomSheet(
            onDismissRequest = { selected = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = LukaSheetShape,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ) {
            Text(capture.body, modifier = Modifier.padding(horizontal = Space.s20), style = MaterialTheme.typography.bodyMedium)
            MovementForm(
                categories = categories,
                sources = sources,
                error = viewModel.error,
                initialOccurredAt = capture.postedAt,
                initialSourceId = capture.sourceId,
                onPickDate = { _, picked ->
                    pendingDate = picked
                    showDate = true
                },
                onSave = { entry -> viewModel.complete(capture, entry) { selected = null } },
            )
        }
    }
}
