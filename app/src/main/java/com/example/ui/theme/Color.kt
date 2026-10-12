package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Raspberry Pi brand colors & Tech Palette
val RaspberryRed = Color(0xFFC51A4A) // Keep this
val TealAccent = Color(0xFF0284C7)   // Rename CyberCyan to TealAccent
val SuccessEmerald = Color(0xFF10B981) // Rename EmeraldSuccess to SuccessEmerald
val ElectricBlue = Color(0xFF38BDF8)  // Rename CyberCyanLight to ElectricBlue
val RedAlert = Color(0xFFEF4444)      // Rename RoseError to RedAlert
val AmberWarning = Color(0xFFF59E0B)  // Keep AmberWarning

// Surfaces / Nav Palette
val NavyDarkest = Color(0xFF070D1E)
val NavyDark = Color(0xFF0B132B)
val NavySurface = Color(0xFF131D3B)
val NavySurfaceElevated = Color(0xFF1C2A4F)
val NavyBorder = Color(0xFF283A6B)

// Typography
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

// Need these too based on component errors
val TealMuted = Color(0xFF48E5C2)

// Compatibility Aliases
val CyberCyan = TealAccent
val EmeraldSuccess = SuccessEmerald
val RoseError = RedAlert
val CyberCyanLight = ElectricBlue
val TealDark = Color(0xFF00A896) // Added to fix Unresolved reference 'TealDark'
