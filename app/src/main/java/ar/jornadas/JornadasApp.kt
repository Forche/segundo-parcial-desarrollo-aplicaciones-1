package ar.jornadas

import android.app.Application

/** Una instancia por proceso: dueña de las dependencias que tienen que vivir lo que vive el proceso. */
class JornadasApp : Application() {
    val servicios by lazy { Servicios(this) }
}
