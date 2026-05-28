# FoliaPerms Tasks - Custom IP & Folia Schedulers

## Custom IP Checklist
- [x] Refactor `FpermCommand.java` to dynamically check if the configured host is custom
- [x] Render *only* the custom clickable link if configured, hiding local loopback or local subnet IPs
- [x] Update `WebEditorServer.java` to bind to `0.0.0.0` for non-loopback hosts to avoid `BindException` on VPS environments
- [x] Build and verify compilation correctness

## Folia Region Scheduler Transition
- [x] Upgrade `build.gradle` Paper API compile dependency to `1.20.4-R0.1-SNAPSHOT`
- [x] Refactor `refreshAllAttachments()` in `FoliaPerms.java` to be region-safe for each online player
- [x] Refactor async/sync tasks in `PermissionService.java` to use Folia's native region/async schedulers
- [x] Refactor save refresh task in `WebEditorServer.java` to use Folia's global region scheduler
- [x] Compile and verify using `bash gradlew build`

## Default Group Assignment & Fallback
- [x] Automatically assign group named "default" to first-time or groupless players on join
- [x] Implement implicit "default" group fallback in `PermissionService` (both `hasPermission` and `getAllowedPermissions`)
- [x] Compile and verify using `bash gradlew build`

## Unregistered Permission Resolution & Permissible Injection
- [x] Include direct and group permissions (unregistered nodes) in `getAllowedPermissions`
- [x] Inject custom `FoliaPermissible` dynamically into the player entity using reflection
- [x] Refactor dynamic reflection injection to locate the permissible field by interface type rather than name for robust Paper/Folia version-independent compatibility
- [x] Compile and verify using `bash gradlew build`

## LuckPerms-Style Web Editor Transformation
- [x] Refactor HTML structure of `editor.html` into a side-by-side grid (Left Sidebar & Right Main Panel)
- [x] Implement tabbed navigation ("Groups" vs. "Players") inside the Left Sidebar with searches and create buttons
- [x] Replace modal-based overlays with a dynamic inline editor panel in the Right Main Panel
- [x] Create a premium placeholder view shown when no group or player is selected
- [x] Design an "Add Permission" input bar with built-in autocomplete matching LuckPerms aesthetics
- [x] Render a highly clean, flat table/list of permissions with instant delete buttons
- [x] Support metadata rows (Prefix and Weight inputs) inline in the editor header for groups
- [x] Support member/group associations (assigned groups for users, members list for groups) directly inline
- [x] Ensure 100% feature parity with original Javascript state management (token-based save state)
- [x] Verify build compiles flawlessly via `bash gradlew build`

## Web Editor Minimalist Redesign
- [x] Rework `editor.html` design: remove all gradients, apply Zinc flat theme, exact borders, clean font hierarchy, premium SaaS modal styles
- [x] Compile and verify using `bash gradlew build`

## Group Weight, Prefix & Placeholders
- [x] Integrate weight/prefix properties into `GroupData` and YAML/SQL storage backends
- [x] Build weight-based prefix and group resolution in `PermissionService`
- [x] Create and register the native `FoliaPermsExpansion` PlaceholderAPI hook
- [x] Incorporate weight/prefix configurations inside Web Editor server and UI modal inputs
- [x] Compile and verify using `bash gradlew build`

## Bug Fixes
- [x] Fix PlaceholderAPI hook not registering due to STARTUP load order (register dynamically on PluginEnableEvent)
## Implementation & Verification Review

### 1. Group Weight & Prefix Data Hierarchy
- **Properties**: Supported `weight` (int) and `prefix` (String) inside `GroupData`.
- **YAML Storage**: Handled non-destructive key loading and writing in `YamlStorage.java`.
- **SQL Storage**: Dynamically run schema migration `ALTER TABLE foliaperms_groups ADD COLUMN weight INT DEFAULT 0` and `prefix VARCHAR(255) DEFAULT ''` inside a safe SQLite/MySQL block in `SqlPermStorage.java`.

### 2. Service Resolvers & PlaceholderAPI Hooks
- **Prefix Resolution**: Exposes `PermissionService.getPlayerPrefix(UUID)` resolving weight-prioritized prefix with a fallback to the `default` group.
- **Primary Group**: Exposes `PermissionService.getPlayerPrimaryGroup(UUID)` resolving group with the highest weight.
- **PlaceholderAPI Hooks**: Created and dynamically registered `FoliaPermsExpansion` to parse `%foliaperms_prefix%`, `%foliaperms_primary_group%`, and `%foliaperms_group%`.

### 3. Web Editor Inputs & Live Synchronization
- **UI Element**: Rendered minimalist Zinc-themed Group Prefix & Weight inputs inside `group-meta-section` of the modal body in `editor.html`.
- **Live Sync**: Integrated active listener `updateGroupMeta()` executing real-time local model updates and flat tag list rerendering.
- **JSON Serialization**: Configured `WebEditorServer.java` payload parser to read and persist weight/prefix inputs.
- **Aesthetic**: Slate-colored minimal flat borders, no gradients, matching the premium Obsidian aesthetic.

### 4. Build Compilation
- Executed `bash gradlew build` and confirmed the artifact compiles and packages flawlessly.

