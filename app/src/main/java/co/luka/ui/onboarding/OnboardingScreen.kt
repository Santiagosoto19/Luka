package co.luka.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import co.luka.R
import co.luka.data.repository.CatalogRepository
import co.luka.data.settings.SettingsStore
import co.luka.ui.access.notificationAccessEnabled
import co.luka.ui.access.openNotificationAccess
import co.luka.ui.components.LukaCard
import co.luka.ui.components.PrimaryButton
import co.luka.ui.theme.LocalLukaColors
import co.luka.ui.theme.Space
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settings: SettingsStore,
    private val catalog: CatalogRepository,
) : ViewModel() {
    val sources = catalog.observeSources().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggle(id: Long, enabled: Boolean) {
        viewModelScope.launch {
            val source = sources.value.firstOrNull { it.id == id } ?: return@launch
            catalog.updateSource(source.copy(monitorNotifications = enabled))
        }
    }

    fun finish() { viewModelScope.launch { settings.setOnboardingDone(true) } }
}

@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel = hiltViewModel()) {
    val sources by viewModel.sources.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var listening by remember { mutableStateOf(notificationAccessEnabled(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) listening = notificationAccessEnabled(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.s20, vertical = Space.s32),
            verticalArrangement = Arrangement.spacedBy(Space.s20),
        ) {
            Image(
                painter = painterResource(R.drawable.logo_luka),
                contentDescription = "Luka",
                modifier = Modifier.fillMaxWidth().height(148.dp),
                contentScale = ContentScale.Fit,
            )
            Text(
                "Luka lee notificaciones, no tus claves",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            )
            Text(
                "Solo mira las notificaciones de las apps que elijas. No entra a Nequi, Nu ni Bancolombia, no pide contraseñas y no mueve dinero. Los datos se quedan en este teléfono.",
                style = MaterialTheme.typography.bodyLarge,
            )
            PrimaryButton(
                text = if (listening) "Acceso a notificaciones activo" else "Abrir acceso a notificaciones",
                onClick = {
                    openNotificationAccess(context)
                    listening = notificationAccessEnabled(context)
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Apps que Luka puede leer",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
            sources.filter { it.kind == "app" }.forEach { source ->
                LukaCard(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Space.s12),
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.s4)) {
                            Text(source.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                source.packageName.orEmpty(),
                                color = LocalLukaColors.current.muted,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Switch(
                            checked = source.monitorNotifications,
                            onCheckedChange = { viewModel.toggle(source.id, it) },
                        )
                    }
                }
            }
            PrimaryButton(text = "Listo", onClick = viewModel::finish, modifier = Modifier.fillMaxWidth())
        }
    }
}
