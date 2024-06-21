package com.fazii.finalapp

data class PermissionState(
    val hasPermission: Boolean,
    val launchPermissionRequest: () -> Unit
)
