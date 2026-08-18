# Implementation Plan - TaxiGoalApp Update

Update the TaxiGoalApp with comprehensive features including a main screen for shift management, a calendar for history, and PDF import for bank statements.

## Proposed Changes

### Build Configuration

#### [MODIFY] [build.gradle.kts](file:///C:/Projects/Gemini/app/build.gradle.kts)
- Ensure `pdfbox-android` library is included in `dependencies`.

### App Manifest

#### [MODIFY] [AndroidManifest.xml](file:///C:/Projects/Gemini/app/src/main/AndroidManifest.xml)
- Register `CalendarActivity` and `PdfImportActivity`.

### Main Screen (Income & Fuel Calculation)

#### [MODIFY] [MainActivity.kt](file:///C:/Projects/Gemini/app/src/main/java/com/example/taxigoalapp/MainActivity.kt)
- Implement auto-saving of drafts.
- Add quick add buttons for mileage (+10, +50, +100 km).
- Calculate fuel cost based on 85 ₸/l.
- Add shift confirmation logic.

#### [MODIFY] [activity_main.xml](file:///C:/Projects/Gemini/app/src/main/res/layout/activity_main.xml)
- Implement a dark Yandex Pro style layout.
- Add widgets for income, fuel, quick mileage, and net profit.

### Calendar Screen (History & Edit)

#### [MODIFY] [CalendarActivity.kt](file:///C:/Projects/Gemini/app/src/main/java/com/example/taxigoalapp/CalendarActivity.kt)
- Support editing and deleting shifts for any day.
- Add navigation to the PDF import screen.

#### [MODIFY] [activity_calendar.xml](file:///C:/Projects/Gemini/app/src/main/res/layout/activity_calendar.xml)
- Add "IMPORT FROM PDF" button.
- Ensure proper calendar layout.

#### [MODIFY] [item_calendar_day.xml](file:///C:/Projects/Gemini/app/src/main/res/layout/item_calendar_day.xml)
- Layout for individual calendar day cells.

### PDF Import Screen

#### [MODIFY] [PdfImportActivity.kt](file:///C:/Projects/Gemini/app/src/main/java/com/example/taxigoalapp/PdfImportActivity.kt)
- Implement PDF parsing for bank statements (income/expenses).
- Add preview and "Save to Goal" functionality.

#### [MODIFY] [activity_pdf_import.xml](file:///C:/Projects/Gemini/app/src/main/res/layout/activity_pdf_import.xml)
- Layout for file selection and transaction preview.

## Verification Plan

### Automated Tests
- Run `app:assembleDebug` to ensure the project builds successfully.

### Manual Verification
- Verify layout rendering using `render_compose_preview` (if applicable) or manual inspection of XML.
- Check activity navigation and logic in the code.
