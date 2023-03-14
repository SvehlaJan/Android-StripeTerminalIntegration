package tech.svehla.demo.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import tech.svehla.demo.R
import tech.svehla.demo.presentation.model.ReaderVO
import tech.svehla.demo.ui.components.ErrorDialog
import tech.svehla.demo.ui.components.SvehlaTextField

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    uiState: MainVmContract.UiState,
    onUiAction: (MainVmContract.UiAction) -> Unit = {},
) {

    uiState.errorVO?.let { error ->
        ErrorDialog(
            errorVO = error,
            onDismiss = { onUiAction(MainVmContract.UiAction.OnErrorConsumed) },
        )
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        if (uiState.paymentReady) {
            PaymentContent(
                amount = uiState.amount,
                referenceNumber = uiState.referenceNumber,
                amountErrorId = uiState.amountErrorId,
                referenceNumberErrorId = uiState.referenceNumberErrorId,
                showPaymentSuccess = uiState.showPaymentSuccess,
                onAmountChanged = { onUiAction(MainVmContract.UiAction.OnAmountChanged(it)) },
                onReferenceNumberChanged = { onUiAction(MainVmContract.UiAction.OnReferenceNumberChanged(it)) },
                onStartPayment = { onUiAction(MainVmContract.UiAction.OnStartPaymentRequested) },
            )
        } else {
            DiscoveryContent(
                readers = uiState.readers ?: emptyList(),
                onDiscoverReaders = { onUiAction(MainVmContract.UiAction.OnDiscoverReadersRequested) },
                onReaderSelected = { onUiAction(MainVmContract.UiAction.OnReaderSelected(it)) },
            )
        }
    }

    if (uiState.isLoading) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background.copy(alpha = 0.5f))
                .clickable { },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colors.primary,
            )
            uiState.paymentProgress?.let { progress ->
                Text(
                    text = stringResource(id = progress.messageId),
                    style = MaterialTheme.typography.h6,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun PaymentContent(
    modifier: Modifier = Modifier,
    amount: String,
    referenceNumber: String,
    amountErrorId: Int?,
    referenceNumberErrorId: Int?,
    showPaymentSuccess: Boolean,
    onAmountChanged: (String) -> Unit,
    onReferenceNumberChanged: (String) -> Unit,
    onStartPayment: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier = modifier.padding(8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (showPaymentSuccess) {
            Text(
                modifier = Modifier.padding(16.dp),
                text = stringResource(id = R.string.payment_success),
                style = MaterialTheme.typography.h6,
                textAlign = TextAlign.Center,
            )
        }
        SvehlaTextField(
            modifier = Modifier.fillMaxWidth(),
            value = referenceNumber,
            onValueChange = onReferenceNumberChanged,
            label = { Text(text = stringResource(R.string.label_reference_number)) },
            errorId = referenceNumberErrorId,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            singleLine = true,
        )

        SvehlaTextField(
            modifier = Modifier.fillMaxWidth(),
            value = amount,
            onValueChange = onAmountChanged,
            label = { Text(text = stringResource(R.string.label_amount)) },
            errorId = amountErrorId,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                onStartPayment()
                focusManager.clearFocus()
            }),
            singleLine = true,
        )

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onStartPayment,
        ) {
            Text(text = stringResource(R.string.button_start_payment))
        }
    }
}

@Composable
private fun DiscoveryContent(
    modifier: Modifier = Modifier,
    readers: List<ReaderVO> = emptyList(),
    onReaderSelected: (ReaderVO) -> Unit = {},
    onDiscoverReaders: () -> Unit = {},
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (readers.isEmpty()) {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                onClick = onDiscoverReaders
            ) {
                Text(text = stringResource(R.string.button_discover_readers))
            }
        } else {
            LazyColumn() {
                items(readers.size) { index ->
                    val reader = readers[index]
                    ReaderTile(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        reader = reader,
                        onClick = { onReaderSelected(reader) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun ReaderTile(
    modifier: Modifier = Modifier,
    reader: ReaderVO,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier,
        elevation = 8.dp,
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(
                text = reader.label ?: "Unknown reader",
                style = MaterialTheme.typography.h6,
            )
            reader.serialNumber?.let { serialNumber ->
                Text(
                    text = "Serial no.: $serialNumber",
                    style = MaterialTheme.typography.body2,
                )
            }
            reader.ipAddress?.let { ipAddress ->
                Text(
                    text = "IP address: $ipAddress",
                    style = MaterialTheme.typography.body2,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MainScreen(
        uiState = MainVmContract.UiState(
            readers = listOf(
                ReaderVO(
                    serialNumber = "123456789",
                    label = "Reader 1",
                    ipAddress = "192.168.0.100",
                ),
            )
        ),
    )
}

@Preview(showBackground = true)
@Composable
fun PaymentContentPreview() {
    PaymentContent(
        amount = "100",
        referenceNumber = "123456789",
        amountErrorId = null,
        referenceNumberErrorId = null,
        showPaymentSuccess = false,
        onAmountChanged = {},
        onReferenceNumberChanged = {},
        onStartPayment = {},
    )
}

@Preview(showBackground = true)
@Composable
fun DiscoveryContentPreview() {
    DiscoveryContent(
        readers = listOf(
            ReaderVO(
                serialNumber = "123456789",
                label = "Reader 1",
                ipAddress = "192.168.0.100",
            ),
        ),
        onReaderSelected = {},
        onDiscoverReaders = {},
    )
}