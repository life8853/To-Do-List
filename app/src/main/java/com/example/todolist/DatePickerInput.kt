package com.example.todolist

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerLayoutType
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import com.example.todolist.ui.theme.ToDoListTheme
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

@SuppressLint("NewApi")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerInput(selectedDateTime: LocalDateTime?, onDateTimeSelected: (LocalDateTime) -> Unit) {
    var showDateDialog by remember { mutableStateOf(false) }
    var showTimeDialog by remember { mutableStateOf(false) }

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val datePickerState = rememberDatePickerState(
        initialDisplayMode = if (isLandscape) DisplayMode.Input else DisplayMode.Picker,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val todayUtc = LocalDate.now(ZoneOffset.UTC)
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant()
                    .toEpochMilli()

                return utcTimeMillis >= todayUtc
            }
        }
    )

    val timePickerState = rememberTimePickerState(is24Hour = true)

    val displayText = selectedDateTime?.let {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")
        it.format(formatter)
    } ?: "No deadline selected"

    OutlinedTextField(
        value = displayText,
        onValueChange = { },
        label = { Text("Deadline") },
        readOnly = true,
        modifier = Modifier
            .fillMaxWidth(),
        trailingIcon = {
            IconButton(onClick = { showDateDialog = true }) {
                Icon(Icons.Default.CalendarMonth, contentDescription = "Select Date")
            }
        }
    )

    if (showDateDialog) {
        DatePickerDialog(
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { showDateDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showDateDialog = false
                    showTimeDialog = true
                }) {
                    Text("Next")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDateDialog = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState, showModeToggle = !isLandscape)
        }
    }

    if (showTimeDialog) {
        TimePickerDialog(
            timePickerState,
            onDismiss = { showTimeDialog = false },
            onConfirm = {
                showTimeDialog = false
                val date = datePickerState.selectedDateMillis?.let {
                    LocalDate.ofInstant(Instant.ofEpochMilli(it), ZoneOffset.UTC)
                }
                val time: LocalTime = LocalTime.of(timePickerState.hour, timePickerState.minute)

                val dateTime = LocalDateTime.of(date, time)
                onDateTimeSelected(dateTime)
            })
    }
}

@SuppressLint("NewApi")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    state: TimePickerState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        text = {
            TimePicker(
                state = state, layoutType = if (isLandscape) {
                    TimePickerLayoutType.Horizontal
                } else {
                    TimePickerLayoutType.Vertical
                }
            )
        }
    )
}


@Preview(showBackground = true)
@Composable
fun DatePickerInputPreview() {
    ToDoListTheme {
        DatePickerInput(null) {}
    }
}