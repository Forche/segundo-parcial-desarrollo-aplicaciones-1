package ar.jornadas.ui

import android.app.Application
import androidx.lifecycle.*
import ar.jornadas.Servicios
import ar.jornadas.data.Charla
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProgramaModel(app: Application) : AndroidViewModel(app) {
    private val servicios = Servicios(app)
    val mesa = servicios.mesa
    private val repo = servicios.agenda

    /** null = Room todavía no respondió; distinto de una lista vacía (programa sin charlas). */
    val charlas: StateFlow<List<Charla>?> =
        repo.programa.stateIn<List<Charla>?>(viewModelScope, SharingStarted.Eagerly, null)
    val hayEdicion: StateFlow<Boolean> =
        repo.hayEdicion.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val dia = MutableStateFlow(0)
    private val _falloCarga = MutableStateFlow(false)
    val falloCarga: StateFlow<Boolean> = _falloCarga.asStateFlow()
    val cargando = MutableStateFlow(false)

    fun recargar() {
        viewModelScope.launch {
            cargando.value = true
            try {
                repo.sincronizar()
                _falloCarga.value = false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _falloCarga.value = true
            } finally {
                cargando.value = false
            }
        }
    }

    fun marcar(id: String) { viewModelScope.launch { repo.cambiarFavorita(id) } }
}
