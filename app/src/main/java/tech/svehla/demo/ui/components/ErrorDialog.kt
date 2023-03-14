package tech.svehla.demo.ui.components

import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import tech.svehla.demo.R
import tech.svehla.demo.presentation.model.ErrorVO

@Composable
fun ErrorDialog(
    errorVO: ErrorVO,
    onDismiss: () -> Unit,
) {
    val title = errorVO.titleRes?.let { stringResource(it) }
    val message = errorVO.message ?: stringResource(errorVO.messageRes ?: R.string.error_unknown)
    val actionLabel = errorVO.actionLabelRes?.let { stringResource(it) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.button_ok))
            }
        },
        title = { title?.let { Text(it) } },
        text = { Text(message) },
        dismissButton = {
            actionLabel?.let { label ->
                TextButton(onClick = errorVO.action ?: onDismiss) {
                    Text(label)
                }
            }
        }
    )
}