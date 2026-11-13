package ar.jornadas.data

import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import java.util.concurrent.atomic.AtomicBoolean

class AgendaRepository(
    private val api: ProgramaApi,
    private val dao: CarteleraDao,
    private val prefs: SharedPreferences,
) {
    val programa: Flow<List<Charla>> = dao.observar()

    private val edicionDescargada = MutableStateFlow(prefs.getBoolean(CLAVE_EDICION, false))

    /**
     * Una v4 exitosa deja la tabla vacía y sigue siendo una edición válida, por eso hace falta la marca.
     * Las charlas guardadas también cuentan: cubren instalaciones previas que no tienen la marca.
     */
    val hayEdicion: Flow<Boolean> =
        combine(edicionDescargada, programa) { marca, charlas -> marca || charlas.isNotEmpty() }

    suspend fun sincronizar() {
        val fichas = api.consultar().map {
            Charla(it.id, it.title, it.track, it.day, it.time, it.room, it.speaker, it.summary)
        }
        dao.reemplazarPrograma(fichas)
        prefs.edit().putBoolean(CLAVE_EDICION, true).apply()
        edicionDescargada.value = true
    }

    suspend fun cambiarFavorita(id: String) = dao.alternar(id)

    private val cargaAutomaticaHecha = AtomicBoolean(false)

    /** true sólo la primera vez en el proceso: el repositorio vive lo que vive el proceso. */
    fun reclamarCargaAutomatica(): Boolean = cargaAutomaticaHecha.compareAndSet(false, true)

    private companion object {
        const val CLAVE_EDICION = "edicion_descargada"
    }
}
