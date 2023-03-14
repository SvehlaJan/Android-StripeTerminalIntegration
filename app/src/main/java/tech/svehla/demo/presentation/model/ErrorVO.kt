package tech.svehla.demo.presentation.model

import androidx.annotation.StringRes

data class ErrorVO(
    @StringRes val titleRes: Int? = null,
    val message: String? = null,
    @StringRes val messageRes: Int? = null,
    val action: (() -> Unit)? = null,
    @StringRes val actionLabelRes: Int? = null,
)
