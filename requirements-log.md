# AU Notes - Requirements & Bug Fixes Specification

## PART A: HOME SCREEN & UI FIXES
1. **Home Screen Rectangle Size**: Note display cards/rectangles are too large; reduce to normal compact, balanced size.
2. **Remove Bulk Select -> Telegram Long-Press Action Bar / Menu**:
   - Remove "Bulk Select" button/text.
   - Long-press on note/folder triggers context menu/action sheet (like Telegram):
     - Reorder
     - Delete
     - Export as ZIP
     - Lock
     - All options with clean SVG/Vector icons.
3. **Splash Screen**: Add a sleek animated splash screen with the new app icon and smooth transition into workspace.
4. **Day Mode & Auto-Scroll Bug**:
   - Day (Light) mode text color contrast fix (text was invisible/washed out in light mode).
   - Write mode auto-scroll: prevent text from going behind the bottom toolbar while typing. Keep cursor and text visible above keyboard & toolbar.
5. **Search Fix (Deep Search)**:
   - Add "Deep Search" toggle.
   - Normal Search = Title only.
   - Deep Search = Searches inside Note content as well.
6. **Text Change**: In Settings, change "aupad preference" to "Settings".

## PART B: SIDEBAR CHANGES
1. Remove "Collections" and "Logs" from sidebar navigation.
2. Rename "Storage File Editor" to "File Editor".
3. **Storage / File Manage Permission**: Request Storage/Manage External Storage permission on first app launch like a native file manager app.
4. **File Editor Flow**:
   - Beautiful rounded card explaining what File Editor does.
   - On tapping "Understood", open full phone file explorer view (device storage explorer) with search bar and file extractor.
5. Move "About The App" below "File Editor" and "Contacts".

## PART C: DEVELOPER OPTIONS
- Stack duplicate accounts vertically (one below another) with verified blue tick badge and tap-to-redirect:
  - GitHub: anxul-and-ups
  - YouTube: http://lrx.anshul
  - Gmail: educationaltalks1@gmail.com, animalsa154@gmail.com
  - Keep existing Telegram and Instagram.

## PART D: NAME GENERATOR (UPDATED)
1. Add "Name Generator" below About.
2. Opens in-app WebView for https://nickfinder.com/ without URL bar or Chrome history.
3. **Copy Detection & Dual Mode Drawer/Sidebar**:
   - Detects clipboard/copied name from website instantly.
   - Right-side slide drawer/panel with 2 tabs/options:
     a) "Generate Names" (Website screen)
     b) "Your Names" (Permanently saved list of all copied names; tap to expand full screen)
   - "Select Names to Save in Notes" action on both screens to export chosen names directly into a new/existing Note.

## PART E: COMMAND MODE (VOICE CONTROL)
- Add "Command Mode" below Name Generator.
- Full voice navigation/control using SpeechRecognizer & TTS:
  - "create new notes", "delete notes", "open notes", "summary notes", "create and delete folders", "api previews".
- If note/folder is locked -> voice responds "Access Denied" + audio cue (`wrong.mp3`).
- PIN gate for sensitive voice commands (e.g., "gemini api do mujhe" checks lock -> asks for PIN -> grants on correct PIN).

## PART F: WRITE MODE FIXES
1. **Table Fix**: "Insert Table" opens dedicated full table editor screen matching user layout screenshot.
2. **Chatbot Icons**: Keep only 1 floating chatbot icon. Replace icon with user-provided PNG asset (on both Home Screen & Note Edit Screen).
3. **Chatbot Bug Fix**: Write mode chatbot must have real live context of current note text, capable of rewriting, editing, summarizing, and cleaning.
4. **Plus (+) Icon & OCR**: In floating chatbot, add (+) button to attach .txt, PDF, or Image. Run OCR / text extraction and execute commands like "Paste poem from this PDF into note".
5. **Image Editing**: In-note images can be moved, cropped, resized, and renamed directly on the write screen.
6. **Read Mode & PDF Export Bug**: Fix images not rendering in Read Mode, and PDF export only showing file names instead of actual images.
7. **Alarm Feature (Clock Icon Replacement)**:
   - Replace "insert timestamp" behavior with a professional Alarm / Reminder feature.
   - Fires real notification & alarm sound even if app is closed.
   - Ringtone selector: System ringtones + "Device Files" (scans all audio files on device).
   - Custom alarm title.

## PART G: EXPORT SYSTEM FIX
- Export dialog with 2 distinct options:
  1. Export (Save to Internal Storage / Documents folder)
  2. Share (Android Share Sheet)
- Editable file extension before export.
- Folder export: Zipped, with `.txt` extension if specified.

## PART H: API KEY SECTION FIX
1. "Inbuilt API" selected -> API key hidden completely.
2. "Use your own API key" selected -> API key input visible with Paste clipboard button.
3. Fix model listing error.

## PART I: SETTINGS & SECURITY FIX
1. Security Question & Passcode can currently change without entering existing PIN -> Fix immediately with PIN verification guard.
2. New "Security Area" in Settings:
   - Protected by PIN / Biometric (Fingerprint).
   - Contains: Change Lock, Hidden Notes, Hidden Folders.
   - Notes/Folders moved here when hidden, restored to original location on unhide.
   - Remove legacy hidden mechanism.
3. Fix Note Lock failure + add sleek Lock/Unlock animation.

## PART J: ICONS & FINAL ASSETS
- Integrate new app icon, SVG icons, and MP3 files (`wrong.mp3`, etc.) provided by user.

## 2026-09-25 implementation progress
- Security PIN no longer has universal 1234 fallback; first use enters Create PIN -> Confirm PIN -> Security Question/Answer setup.
- PIN recovery remains guarded by security answer; wrong PIN attempts are tracked globally and the supplied wrong.mp3 plays on the third consecutive failed attempt.
- Hidden-folder preference store added; hidden folders are excluded from Home and surfaced in Security Area for restore.
- Custom folders can be created; Add Folder remains the final item in the folder strip.
- Table editor cleaned to match supplied reference: blank cells, no row/column option handles, no autogenerated Header/Row labels.
- Export rewritten to public Documents/AU Notes storage via MediaStore on Android 10+; PDF renderer now creates multiple pages and embeds image attachments from their stored URIs.
- Rich text editor now uses range-aware style spans during editing with visual transformation; Bold/Italic/Underline/Strikethrough/Code/Color are selection-based and future-typing styles are applied only to inserted ranges.
- Supplied AU app icon and assistant icon copied into app resources; supplied wrong.mp3 copied to res/raw/wrong.mp3.
- GitHub Actions debug build workflow remains push-triggered for main/master and uploads APK artifact.

## 2026-09-25 continued batch
- Replaced fake image OCR placeholder in NoteEditorScreen with real Google ML Kit Latin text recognition.
- Added OCR for PDF pages by rendering pages with PdfRenderer and running ML Kit OCR on up to 8 pages.
- Added Write Mode JSON Mode toggle with syntax-highlighted JSON preview (keys, strings, numbers, literals, punctuation).
- Replaced Write Mode fingerprint attachment icon with provided media SVG and expanded picker to images, videos, PDF, text, JSON and supported documents.
- Write Mode AI assistant now auto-inserts generated content when the user explicitly asks to paste/insert/add it to the current note; manual Paste into Note remains available.
- Added ML Kit text-recognition dependency.

## Continuation pass — 2026-09-25
- Continued supplied-SVG integration across screens where matching SVGs exist: copy, edit, delete, send, folder, lock, eye/eye-off.
- Kept Security Area top-bar + / − controls as actual Add/Remove controls because the supplied SVG set has no plus/minus equivalents; avoided semantically incorrect icon substitutions.
- Kept non-equivalent Material icons (navigation, chevron, alarm, table, microphone, search, etc.) where no supplied SVG exists.
- Verified GitHub Actions push workflow targets main/master and uploads a debug APK artifact; release workflow remains tag-based.
- Reviewed export implementation: Android 10+ uses Documents/AU Notes through MediaStore; PDF writer renders styled text, tables, and image attachments across pages; non-PDF exports show an attachment compatibility warning.

## Final hardening pass — 2026-09-25
- Home note cards no longer expose the Favourite action; the card action is now the supplied Copy SVG and copies the full note content to the Android clipboard.
- Protected folder actions are gated: hidden/reorder/delete/export operations cannot be performed while a locked folder remains locked in the current session.
- Command Mode search/summary no longer reveal titles/content from locked, hidden, or locked-folder notes; open/delete paths treat protected folders as protected too.
- AI chatbot note search and destructive/move actions now exclude protected notes/folders and re-check protection at action-confirmation time.
- Read Mode image attachments now render `content://` and `file://` URIs correctly instead of treating content URIs as filesystem paths.
- Read Mode now has a top Copy action after saving/opening a note.
- Alarm setup now supports a custom alarm title; device audio discovery includes MediaStore audio beyond only `IS_MUSIC` tracks; reboot restoration reschedules a pending alarm.
- Folder ZIP export now produces a real `.zip` file and saves it to public `Documents/AU Notes` through MediaStore on Android 10+; protected notes are excluded and locked folders require unlock before export.
- File Editor now navigates the real shared-storage directory tree when all-files access is available, with parent-folder navigation and directory opening.
- File Editor's Create action writes into the currently browsed device-storage directory.
- Name Generator now has a Select Names to Save in Notes flow from both Generate and Your Names screens, with new-note or existing-note targets.
- Removed remaining emoji glyphs from application UI/status strings.
- Splash subtitle changed to `Your Premium Workspace` as requested.
