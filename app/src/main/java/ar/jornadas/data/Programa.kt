package ar.jornadas.data

import androidx.room.*
import retrofit2.http.GET
import kotlinx.coroutines.flow.Flow

data class FichaRemota(val id: String, val title: String, val track: String, val day: Int,
    val time: String, val room: String, val speaker: String, val summary: String)

interface ProgramaApi {
    @GET("programa") suspend fun consultar(): List<FichaRemota>
}

@Entity(tableName = "charlas")
data class Charla(@PrimaryKey val id: String, val titulo: String, val tema: String, val dia: Int,
    val hora: String, val sala: String, val ponente: String, val resumen: String,
    val favorita: Boolean = false)

@Dao
interface CarteleraDao {
    @Query("SELECT * FROM charlas ORDER BY dia, hora, sala")
    fun observar(): Flow<List<Charla>>

    @Query("SELECT id FROM charlas WHERE favorita = 1")
    suspend fun idsFavoritas(): List<String>

    @Query("DELETE FROM charlas")
    suspend fun vaciar()

    @Insert
    suspend fun insertar(fichas: List<Charla>)

    /**
     * La organización publica instantáneas completas: lo que no viene se retira.
     * Las favoritas se leen dentro de la transacción, después de que llegó la respuesta,
     * para respetar las marcas hechas mientras la petición estaba en curso.
     */
    @Transaction
    suspend fun reemplazarPrograma(fichas: List<Charla>) {
        val favoritas = idsFavoritas().toSet()
        vaciar()
        insertar(fichas.map { it.copy(favorita = it.id in favoritas) })
    }

    @Query("UPDATE charlas SET favorita = NOT favorita WHERE id = :id")
    suspend fun alternar(id: String)
}

@Database(entities = [Charla::class], version = 1, exportSchema = false)
abstract class Archivo : RoomDatabase() {
    abstract fun cartelera(): CarteleraDao
}
