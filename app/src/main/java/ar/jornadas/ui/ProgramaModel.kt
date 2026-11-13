package ar.jornadas.ui

import android.app.Application
import androidx.lifecycle.*
import ar.jornadas.Servicios
import ar.jornadas.data.Charla
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProgramaModel(app: Application, private val estado: SavedStateHandle) : AndroidViewModel(app) {
    private val servicios = Servicios(app)
    val mesa = servicios.mesa
    private val repo = servicios.agenda

    /** null = Room todavía no respondió; distinto de una lista vacía (programa sin charlas). */
    val charlas: StateFlow<List<Charla>?> =
        repo.programa.stateIn<List<Charla>?>(viewModelScope, SharingStarted.Eagerly, null)
    val hayEdicion: StateFlow<Boolean> =
        repo.hayEdicion.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // Filtros en el SavedStateHandle: sobreviven a rotación y a muerte del proceso;
    // una tarea nueva arranca sin ellos.
    val dia: StateFlow<Int> = estado.getStateFlow(CLAVE_DIA, 0)
    val tema: StateFlow<String> = estado.getStateFlow(CLAVE_TEMA, TODOS_LOS_TEMAS)
    fun elegirDia(valor: Int) { estado[CLAVE_DIA] = valor }
    fun elegirTema(valor: String) { estado[CLAVE_TEMA] = valor }

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

    companion object {
        const val TODOS_LOS_TEMAS = "Todos"
        private const val CLAVE_DIA = "filtro_dia"
        private const val CLAVE_TEMA = "filtro_tema"
    }
}
