package com.phdevtech.cicloserp

import android.app.Application
import com.phdevtech.cicloserp.data.CiclosRepository
import com.phdevtech.cicloserp.data.SessionStore
import com.phdevtech.cicloserp.data.api.ApiClient

/** Dependências do app (sem framework de injeção: o app é pequeno). */
class CiclosApp : Application() {
    val repository: CiclosRepository by lazy { CiclosRepository(ApiClient(), SessionStore(this)) }
}
