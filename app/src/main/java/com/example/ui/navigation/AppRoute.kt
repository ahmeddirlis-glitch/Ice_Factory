package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute {
    val key: String

    @Serializable data object Dashboard : AppRoute { override val key = "dashboard" }
    @Serializable data object UsersRoles : AppRoute { override val key = "users" }
    @Serializable data object Attendance : AppRoute { override val key = "attendance" }
    @Serializable data object FaceVerification : AppRoute { override val key = "face_verification" }
    @Serializable data object Production : AppRoute { override val key = "production" }
    @Serializable data object Inventory : AppRoute { override val key = "inventory" }
    @Serializable data object Sales : AppRoute { override val key = "sales" }
    @Serializable data object Customers : AppRoute { override val key = "customers" }
    @Serializable data object Deliveries : AppRoute { override val key = "deliveries" }
    @Serializable data object Expenses : AppRoute { override val key = "expenses" }
    @Serializable data object Salary : AppRoute { override val key = "salary" }
    @Serializable data object Utilities : AppRoute { override val key = "utilities" }
    @Serializable data object Maintenance : AppRoute { override val key = "maintenance" }
    @Serializable data object Suppliers : AppRoute { override val key = "suppliers" }
    @Serializable data object Incidents : AppRoute { override val key = "incidents" }
    @Serializable data object Reports : AppRoute { override val key = "reports" }
    @Serializable data object Notices : AppRoute { override val key = "notices" }
    @Serializable data object Settings : AppRoute { override val key = "settings" }
    @Serializable data object About : AppRoute { override val key = "about" }
    @Serializable data object Updates : AppRoute { override val key = "updates" }
    @Serializable data object Profile : AppRoute { override val key = "profile" }

    companion object {
        fun fromKey(key: String): AppRoute = when (key.lowercase()) {
            "dashboard" -> Dashboard
            "users" -> UsersRoles
            "attendance" -> Attendance
            "face_verification" -> FaceVerification
            "production" -> Production
            "inventory" -> Inventory
            "sales" -> Sales
            "customers" -> Customers
            "deliveries" -> Deliveries
            "expenses" -> Expenses
            "salary" -> Salary
            "utilities" -> Utilities
            "maintenance" -> Maintenance
            "suppliers" -> Suppliers
            "incidents" -> Incidents
            "reports" -> Reports
            "notices" -> Notices
            "settings" -> Settings
            "about" -> About
            "updates" -> Updates
            "profile" -> Profile
            else -> Dashboard
        }

        val ownerOnlyKeys = setOf("salary", "reports", "settings")
        val staffManagerKeys = setOf("users")
        val staffAllowedKeys = setOf(
            "dashboard",
            "attendance",
            "face_verification",
            "salary",
            "notices",
            "incidents",
            "profile",
            "settings",
            "about",
            "updates"
        )

        fun isAllowedFor(role: com.example.data.model.UserRole, routeKey: String): Boolean {
            return when (role) {
                com.example.data.model.UserRole.OWNER -> true
                com.example.data.model.UserRole.ADMIN -> routeKey != "reports" && routeKey != "salary"
                com.example.data.model.UserRole.MANAGER -> routeKey == Settings.key || (routeKey !in ownerOnlyKeys && routeKey !in staffManagerKeys)
                com.example.data.model.UserRole.STAFF -> routeKey in staffAllowedKeys
            }
        }
    }
}
