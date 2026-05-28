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
