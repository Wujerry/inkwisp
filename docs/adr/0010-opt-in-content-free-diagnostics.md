# Keep diagnostics opt-in and content-free

InkWisp will not collect behavioral analytics. Crash reporting is disabled by default and may be enabled explicitly by the user; reports are limited to application version, Android version, broad device category, and scrubbed stack information that the user can preview. Document content and paths, filenames, credentials, model request or response bodies, and writing activity must never enter diagnostics, and locally retained diagnostic data can be deleted in one action. Any future SDK must satisfy this boundary before adoption.
