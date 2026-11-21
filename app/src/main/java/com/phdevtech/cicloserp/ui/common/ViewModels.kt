package com.phdevtech.cicloserp.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.phdevtech.cicloserp.CiclosApp
import com.phdevtech.cicloserp.data.CiclosRepository

@Composable
fun repository(): CiclosRepository = (LocalContext.current.applicationContext as CiclosApp).repository

/** ViewModel criado com o repositório do app ([key] separa instâncias, ex.: uma por OS). */
@Composable
inline fun <reified VM : ViewModel> appViewModel(key: String? = null, crossinline create: (CiclosRepository) -> VM): VM {
    val repo = repository()
    return viewModel(key = key, factory = viewModelFactory { initializer { create(repo) } })
}
