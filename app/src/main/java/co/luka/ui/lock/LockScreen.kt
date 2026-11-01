package co.luka.ui.lock

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModel
import co.luka.R
import co.luka.domain.security.PinHasher
import co.luka.ui.components.PrimaryButton
import co.luka.ui.components.animationsEnabled
import co.luka.ui.theme.LocalLukaColors
import co.luka.ui.theme.Space
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LockViewModel @Inject constructor() : ViewModel() {
    var unlocked by mutableStateOf(false)
        private set

    private var ignoreRelockUntil = 0L

    private val relock = object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
            if (android.os.SystemClock.elapsedRealtime() < ignoreRelockUntil) return
            unlocked = false
        }
    }

    init {
        // ON_STOP del proceso: al ir a segundo plano hay que volver a pedir PIN o huella.
        ProcessLifecycleOwner.get().lifecycle.addObserver(relock)
    }

    override fun onCleared() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(relock)
        super.onCleared()
    }

    fun unlock() {
        unlocked = true
        ignoreRelockUntil = android.os.SystemClock.elapsedRealtime() + 1_200
    }

    fun holdPrompt(visible: Boolean) {
        ignoreRelockUntil = android.os.SystemClock.elapsedRealtime() + if (visible) 60_000 else 1_200
    }
}

@Composable
fun LockScreen(
    pinHash: String,
    pinSalt: String,
    onUnlock: () -> Unit,
    onPromptVisible: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val pinReady = PinHasher.isConfigured(pinSalt, pinHash)
    val biometric = remember(context) { canUseBiometric(context) }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var biometricRequest by remember { mutableIntStateOf(if (pinReady && biometric) 1 else 0) }
    val shake = remember { Animatable(0f) }
    val motion = animationsEnabled()

    LaunchedEffect(biometricRequest) {
        if (biometricRequest == 0) return@LaunchedEffect
        val activity = context as? FragmentActivity
        if (activity == null) {
            error = "Este teléfono no abrió la huella. Usa el PIN."
            return@LaunchedEffect
        }
        onPromptVisible(true)
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onPromptVisible(false)
                    onUnlock()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onPromptVisible(false)
                    if (errorCode == BiometricPrompt.ERROR_LOCKOUT || errorCode == BiometricPrompt.ERROR_LOCKOUT_PERMANENT) {
                        error = "Demasiados intentos. Usa el PIN."
                    }
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Desbloquea Luka")
            .setSubtitle("Confirma con tu huella")
            .setNegativeButtonText("Usar PIN")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()
        try {
            prompt.authenticate(info)
        } catch (_: Exception) {
            onPromptVisible(false)
            error = "Este teléfono no abrió la huella. Usa el PIN."
        }
    }

    LaunchedEffect(error) {
        if (error == null || !motion) return@LaunchedEffect
        shake.snapTo(0f)
        listOf(14f, -14f, 8f, -8f, 0f).forEach { shake.animateTo(it, tween(45)) }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = Space.s24, vertical = Space.s16),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.s12)) {
                Image(
                    painter = painterResource(R.drawable.logo_luka),
                    contentDescription = "Luka",
                    modifier = Modifier.padding(top = Space.s24).height(96.dp),
                    contentScale = ContentScale.Fit,
                )
                Text("Luka está bloqueada", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                Text(
                    if (pinReady) "Huella o PIN" else "Define un PIN en Ajustes para poder entrar.",
                    color = LocalLukaColors.current.muted,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            if (pinReady) {
                PinDots(pin.length, Modifier.offset(x = shake.value.dp))
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
                PinPad(
                    biometric = biometric,
                    onDigit = { digit ->
                        if (pin.length < 8) pin += digit
                        error = null
                    },
                    onDelete = {
                        pin = pin.dropLast(1)
                        error = null
                    },
                    onBiometric = { biometricRequest += 1 },
                )
                PrimaryButton(
                    text = "Entrar",
                    enabled = pin.length >= 4,
                    onClick = {
                        if (PinHasher.accepts(pin, pinSalt, pinHash)) {
                            onUnlock()
                        } else {
                            pin = ""
                            error = "Ese PIN no coincide."
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun PinDots(length: Int, modifier: Modifier = Modifier) {
    val slots = maxOf(4, length).coerceAtMost(8)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(Space.s12)) {
        repeat(slots) { index ->
            val filled = index < length
            Box(
                Modifier
                    .size(14.dp)
                    .background(
                        if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        CircleShape,
                    ),
            )
        }
    }
}

@Composable
private fun PinPad(
    biometric: Boolean,
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onBiometric: () -> Unit,
) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(Space.s12), horizontalAlignment = Alignment.CenterHorizontally) {
        keys.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s20)) {
                row.forEach { key -> PinKey(key) { onDigit(key) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s20), verticalAlignment = Alignment.CenterVertically) {
            if (biometric) {
                PinKey(label = null, description = "Usar huella", icon = Icons.Filled.Fingerprint, onClick = onBiometric)
            } else {
                Spacer(Modifier.size(72.dp))
            }
            PinKey("0") { onDigit("0") }
            PinKey(label = null, description = "Borrar", icon = Icons.AutoMirrored.Filled.Backspace, onClick = onDelete)
        }
    }
}

@Composable
private fun PinKey(
    label: String?,
    description: String? = label,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Box(
        Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = description, tint = MaterialTheme.colorScheme.primary)
        } else {
            Text(
                text = label.orEmpty(),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
            )
        }
    }
}

private fun canUseBiometric(context: android.content.Context): Boolean {
    val result = BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
    return result == BiometricManager.BIOMETRIC_SUCCESS
}

@Composable
fun PinSetupScreen(onSave: (String) -> Unit) {
    var first by remember { mutableStateOf("") }
    var again by remember { mutableStateOf("") }
    var confirming by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val current = if (confirming) again else first
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = Space.s24, vertical = Space.s16),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.s12)) {
                Image(
                    painter = painterResource(R.drawable.logo_luka),
                    contentDescription = "Luka",
                    modifier = Modifier.padding(top = Space.s24).height(96.dp),
                    contentScale = ContentScale.Fit,
                )
                Text(
                    if (confirming) "Repite el PIN" else "Crea tu PIN",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    if (confirming) {
                        "Escríbelo otra vez para guardarlo."
                    } else {
                        "Luka lo pide cada vez que la abras. Si este teléfono tiene huella, también podrás entrar con ella."
                    },
                    color = LocalLukaColors.current.muted,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            PinDots(current.length)
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            PinPad(
                biometric = false,
                onDigit = { digit ->
                    error = null
                    if (confirming) {
                        if (again.length < 8) again += digit
                    } else if (first.length < 8) {
                        first += digit
                    }
                },
                onDelete = {
                    error = null
                    if (confirming) again = again.dropLast(1) else first = first.dropLast(1)
                },
                onBiometric = {},
            )
            PrimaryButton(
                text = if (confirming) "Guardar" else "Continuar",
                enabled = current.length >= 4,
                onClick = {
                    if (!confirming) {
                        confirming = true
                        return@PrimaryButton
                    }
                    if (again != first) {
                        again = ""
                        error = "Los PIN no coinciden."
                        return@PrimaryButton
                    }
                    onSave(first)
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
