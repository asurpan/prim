# Fix Build Error in MainActivity.kt

The project is failing to build due to a syntax error in `MainActivity.kt` at line 1206. Additionally, there are missing Opt-In annotations for CameraX Experimental APIs.

## User Review Required

> [!IMPORTANT]
> I am replacing the `if` statement used as an expression (with `?:`) with a proper `if-else` statement to ensure the Kotlin compiler correctly parses the block. I am also adding `@OptIn` to `onCreate` to resolve experimental API warnings.

## Proposed Changes

### UI & Camera Integration

#### [MODIFY] [MainActivity.kt](file:///C:/Users/Jose/AndroidStudioProjects/PRIM/app/src/main/java/com/sagon/prim/MainActivity.kt)

1.  **Fix Syntax Error in `QRReaderDialog`**:
    *   Change the `mediaImage?.let { ... } ?: proxy.close()` block (which appeared to be `if (mediaImage != null) { ... } ?: proxy.close()` in some compiler passes) to a clean `if-else` structure.
    *   Reformat the `AndroidView` call to use explicit parameters on separate lines to avoid "Unexpected tokens" confusion.
2.  **Add Opt-In Annotations**:
    *   Add `@OptIn(ExperimentalGetImage::class)` to `MainActivity.onCreate` to satisfy requirements of `MainScreen`.
    *   Cleanup existing `@OptIn` usage in `QRReaderDialog`.

---

## Verification Plan

### Automated Tests
- Run `./gradlew :app:compileDebugKotlin` to verify the syntax error is resolved.

### Manual Verification
- Deploy the app to a device to ensure the QR scanner still functions correctly.
