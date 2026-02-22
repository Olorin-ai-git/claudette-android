# UI Screens Phase 5 - Implementation Plan

## Date: 2026-02-21

## Files to Create (10 total)

### Snippet Drawer

1. `SnippetDrawerScreen.kt` - Bottom sheet snippet drawer with search, categories, add custom
2. `SnippetDrawerViewModel.kt` - ViewModel with filtered snippets, add/delete

### File Browser

3. `RemoteFileBrowserScreen.kt` - Breadcrumb nav, file/dir listing, navigation
4. `RemoteFileBrowserViewModel.kt` - Directory listing, navigation, SFTP connection

### File Editor

5. `RemoteFileEditorScreen.kt` - Monospace editor, save/close, loading/error states
6. `RemoteFileEditorViewModel.kt` - File load/save via SFTP, size limit check

### Agent Visualizer

7. `AgentVisualizerScreen.kt` - Tree view of agent hierarchy with status indicators

### Resources

8. `ClaudeResourcesScreen.kt` - Grouped resource listing with tap-to-invoke

### Settings

9. `ClaudeMDDashboardScreen.kt` - Claude settings.json viewer/editor with hooks
10. `HooksAutomationScreen.kt` - Hook CRUD with type selector, command, matcher

## Patterns Used

- Material3 components throughout
- ClaudetteTheme colors (dark-only)
- Timber via LoggerFactory for logging
- StateFlow + collectAsState() for state
- hiltViewModel() for DI
- FontFamily.Monospace for terminal aesthetic
- RoundedCornerShape(12.dp) for cards
