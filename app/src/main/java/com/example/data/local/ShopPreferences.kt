package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "shop_settings")

data class ShopSettings(
    val shopName: String = "আমার দোকান",
    val shopPhone: String = "",
    val shopAddress: String = "",
    val currencySymbol: String = "৳",
    val isDarkMode: Boolean = false,
    val lastBackupTimestamp: Long = 0L
)

class ShopPreferences(private val context: Context) {
    private object Keys {
        val SHOP_NAME = stringPreferencesKey("shop_name")
        val SHOP_PHONE = stringPreferencesKey("shop_phone")
        val SHOP_ADDRESS = stringPreferencesKey("shop_address")
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val LAST_BACKUP = longPreferencesKey("last_backup_timestamp")
    }

    val settingsFlow: Flow<ShopSettings> = context.dataStore.data.map { prefs ->
        ShopSettings(
            shopName = prefs[Keys.SHOP_NAME]?.takeIf { it.isNotBlank() } ?: "আমার দোকান",
            shopPhone = prefs[Keys.SHOP_PHONE] ?: "",
            shopAddress = prefs[Keys.SHOP_ADDRESS] ?: "",
            currencySymbol = prefs[Keys.CURRENCY_SYMBOL]?.takeIf { it.isNotBlank() } ?: "৳",
            isDarkMode = prefs[Keys.DARK_MODE] ?: false,
            lastBackupTimestamp = prefs[Keys.LAST_BACKUP] ?: 0L
        )
    }

    suspend fun updateShopProfile(name: String, phone: String, address: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SHOP_NAME] = name.trim().ifBlank { "আমার দোকান" }
            prefs[Keys.SHOP_PHONE] = phone.trim()
            prefs[Keys.SHOP_ADDRESS] = address.trim()
        }
    }

    suspend fun updateCurrency(symbol: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CURRENCY_SYMBOL] = symbol.trim().ifBlank { "৳" }
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DARK_MODE] = enabled
        }
    }

    suspend fun updateLastBackupTimestamp(timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_BACKUP] = timestamp
        }
    }

    suspend fun applyRestoredSettings(settings: ShopSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SHOP_NAME] = settings.shopName.ifBlank { "আমার দোকান" }
            prefs[Keys.SHOP_PHONE] = settings.shopPhone
            prefs[Keys.SHOP_ADDRESS] = settings.shopAddress
            prefs[Keys.CURRENCY_SYMBOL] = settings.currencySymbol.ifBlank { "৳" }
            prefs[Keys.DARK_MODE] = settings.isDarkMode
        }
    }
}
