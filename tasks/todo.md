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

## User Info Subcommand Feature
- [x] Add `/fperm user info <player>` subcommand with beautiful, formatted output listing UUID, Primary Group (Weight), Prefix, Assigned Groups, and individual permission node tags
- [x] Integrate `/fperm user info` subcommand tab-completion inside `FpermTabCompleter`
- [x] Compile and verify using `bash gradlew build`

## Bug Fixes & Refinements - Session Expired & Default Group
- [x] Add `Cache-Control` and `Pragma` headers in `WebEditorServer.java`'s `sendResponse` to disable browser cache on API requests.
- [x] Refactor `ApiDataHandler` and `ApiSaveHandler` in `WebEditorServer.java` to catch `Throwable` instead of `Exception`, logging all severe tracebacks.
- [x] Add try-catch protection around the stream sorting block inside `PermissionService.java`'s `getRegisteredPermissionsSorted()`.
- [x] Refactor `getPlayerGroups(Player)` and `getPrimaryGroup(Player)` fallbacks in `FoliaPerms.java` to fully resolve groupless players as `"default"`.
- [x] Guarantee `"default"` group exists in the groups cache map on `load()` and `loadAsync()` in `PermissionService.java`.
- [x] Ensure that `PlayerListener.java` always triggers permission attachment refresh on join, even if assigning the default group.
- [x] Incorporate `/fperm user info <player>` into the `/fperm help` output in `FpermCommand.java`.
- [x] Compile and verify using `bash gradlew build`.

## Bug Fixes & Refinements - Web Editor Sync & Username Displays
- [x] Implement group members weight and prefix copy constructor properties in `saveAsync()` inside `PermissionService.java`.
- [x] Reconcile bidirectional player-group relationships on `load()` inside `PermissionService.java`.
- [x] Reconcile bidirectional player-group relationships on `loadAsync()` inside `PermissionService.java`.
- [x] Resolve and serialize offline player usernames inside `ApiDataHandler` in `WebEditorServer.java`.
- [x] Rework player list rendering to search and display both `<display_name>` and `<UUID>` in `editor.html`.
- [x] Update inline user editor title to display username and UUID in `editor.html`.
- [x] Render group member badges with `<display_name> (<short_uuid>)` in `editor.html`.
- [x] Compile and verify using `bash gradlew build`.
