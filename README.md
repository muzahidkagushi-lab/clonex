# CloneX – App Cloner (Architecture & Cloning Engine Guide)

CloneX is a single-activity **Jetpack Compose + Material 3 + MVVM** Android application with full **Bengali (`values-bn`)** and **English (`values`)** localization, local Room persistence, Jetpack DataStore preferences, and a clean virtualization abstraction (`CloneEngine`).

## Package Structure

- `com.example.domain.model` — Immutable domain models (`InstalledApp`, `CloneItem`, `AppSettings`, `BackupPayload`)
- `com.example.engine` — `CloneEngine` interface, `MockCloneEngine` reference implementation, and `BootCompletedReceiver`
- `com.example.data.local` — Room database (`CloneDatabase`, `CloneDao`, `CloneEntity`)
- `com.example.data.preferences` — Jetpack DataStore (`SettingsDataStore`)
- `com.example.data.repository` — `InstalledAppsRepository` (`PackageManager` queries) & `CloneRepository` (CRUD, validation, JSON backup/restore)
- `com.example.util` — `LocaleHelper`, `Formatters`, `ShortcutHelper`, `SecurityHelper`
- `com.example.ui` — `theme`, `viewmodel`, `components`, `screens`, `navigation`

## Plugging In a Real App-Virtualization Engine

All virtualization lifecycle calls are isolated behind `com.example.engine.CloneEngine`:

```kotlin
interface CloneEngine {
    val runningCloneIds: StateFlow<Set<String>>
    val activeSandboxClone: StateFlow<CloneItem?>
    suspend fun createClone(...): CloneItem
    suspend fun launchClone(clone: CloneItem): LaunchCloneResult
    suspend fun stopClone(cloneId: String)
    suspend fun deleteClone(cloneId: String)
    suspend fun clearData(cloneId: String)
    suspend fun clearCache(cloneId: String): Long
    suspend fun getStorageUsage(cloneId: String): CloneStorageUsage
    suspend fun onBootCompleted(clonesToStart: List<CloneItem>)
}
```

### Integration Steps

1. **Create a new implementation** (e.g., `VirtualCloneEngine(context): CloneEngine`).
2. **Map each `CloneItem.id` (UUID) to a virtual user slot** (`userId: Int`) inside your chosen virtualization backend (such as BlackBox, VirtualApp, or Android Managed Profiles).
3. **Replace the `TODO (Virtualization Hook)` markers** documented in `MockCloneEngine.kt`:
   - `createClone`: Install/register the target package into the isolated user space (`installPackageAsUser(pkg, userId)`).
   - `launchClone`: Launch the virtualized APK (`launchApk(pkg, userId)`).
   - `stopClone`: Terminate the virtualized user process (`stopPackage(pkg, userId)`).
   - `clearData` / `deleteClone`: Wipe the virtualized user data and remove `filesDir/clones/<uuid>/`.
4. **Swap the instance in `MainViewModel.kt`** (`private val cloneEngine: CloneEngine = VirtualCloneEngine(application)`). No UI, ViewModel, or Room schema changes are required.
