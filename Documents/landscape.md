# Landscape Adaptation Plan (UI Audit + Execution Plan)

## Scope
This is a planning document only (no code changes) for adapting the current ReelEd UI to landscape orientation across:
- Main app flows (onboarding + parent dashboard)
- Overlay quiz experience (foreground service Compose overlay)

## Current UI Audit Summary

### 1) Global layout patterns observed
- Most screens are built as portrait-first `Column` stacks with fixed vertical rhythm (`Spacer`, fixed `height`, fixed hero sizes).
- Many onboarding screens combine: top header, central hero/illustration, bottom CTA bar. In landscape this can compress vertical space and push content/CTAs off-screen.
- Dashboard uses single-column cards with mobile-height assumptions; landscape has unused horizontal space.
- Overlay cards use full-screen + weighted vertical sections (`weight(0.25f/0.75f)`, etc.), large text sizes, and fixed touch target dimensions.

### 2) Overlay-specific findings
- Overlay window itself is `MATCH_PARENT x MATCH_PARENT` and can support landscape already, but content inside is portrait-biased.
- `QuizCardRouter` currently prioritizes vertical stacking:
  - timer at top
  - train area `weight(0.25f)`
  - card area `weight(0.75f)`
  - mascot pinned with fixed offsets/sizes
- Card-level fixed sizes are likely problematic in landscape for small-height devices:
  - Very large type (e.g., 72sp, 44sp, 120sp check mark)
  - Fixed component heights (e.g., 64dp buttons, 300dp illustration blocks, 160dp chart area)
  - 2-row matching layouts that become short in height when landscape + keyboard/gesture insets are present

### 3) Onboarding findings
- Multiple onboarding screens use large fixed hero blocks (`size(280.dp)`, `height(300.dp)`, etc.).
- Some screens are scrollable already (`verticalScroll`), but many remain fixed and may clip in landscape.
- Footer action bars are frequently fixed-height and assume enough vertical remainder.

### 4) Dashboard findings
- `ParentDashboardScreen` uses `LazyColumn` for dashboard tab and `Column` for controls/settings.
- Cards (`TodaySummaryCard`, `WeekBarCard`, `SubjectBreakdownCard`, `RecentAttemptsCard`) are visually good but remain stacked in one column in all orientations.
- Landscape can support a two-pane or two-column card composition with stronger information density.

## Landscape Design Targets

### A) Breakpoint strategy
Use width/height-aware adaptation rather than orientation checks alone:
- `Compact`: narrow width or very short height (keep mostly single-column + scrolling)
- `Medium`: landscape phones / small tablets (two-zone layouts where possible)
- `Expanded`: tablets/large landscape (two-pane patterns, larger touch targets retained)

Recommended gating dimensions:
- Primary split trigger: `maxWidth >= 840.dp`
- Height-constrained trigger: `maxHeight <= 480.dp` (apply compact vertical mode)

### B) Shared layout rules
- Replace fixed hero heights with bounded responsive sizes (`fillMaxHeight(fraction)`, `heightIn(min,max)`).
- Prefer `Row` + weighted panes in landscape for content + controls.
- Keep primary CTA always visible; move non-critical decorative content behind scroll or collapse.
- Keep minimum tap sizes >= 48dp and maintain readable minimum text scale for children-focused UI.

## Screen-by-Screen Adaptation Plan

## 1) Overlay Shell (`QuizCardRouter`)
Goal: preserve fast quiz completion in low vertical space.

Plan:
- Introduce a landscape container mode:
  - Left pane: prompt/instructions + dynamic card content
  - Right pane: interactive answer region OR mascot/train depending on card type
- In low-height mode:
  - Reduce train section prominence (inline or collapsible)
  - Move timer to compact top strip with reduced vertical padding
  - Reposition mascot to non-overlapping corner with adaptive size
- Add safe inset handling around edges so overlay controls don’t collide with system gestures/notches.

## 2) Tap Choice Card
Current risk: giant prompt text and tall option rows reduce usable space in landscape.

Plan:
- Use two-region layout in landscape:
  - Left: question + instruction
  - Right: answer buttons in 2x2 grid (or stacked if options < 4)
- Dynamic typography scaling based on container size.
- Cap option button height with adaptive min/max rather than portrait-centric values.

## 3) Tap-Tap Match Card
Current risk: top/bottom rows consume height; labels can become cramped.

Plan:
- In landscape switch to side-by-side matching columns:
  - Left-side tiles and right-side tiles with clear central connection affordance/state
- Reduce tile font size responsively and enforce line wrapping rules.
- Keep match feedback icons and states visible without reducing tile hit targets below 48dp.

## 4) Drag-Drop Match Card
Current risk: random chip placement + central target circle can conflict with short vertical bounds.

Plan:
- Use bounded “arena” with adaptive center radius and chip spacing calculated from current constraints.
- In short-height landscape, switch to structured ring/row placement instead of pure random placement.
- Keep the target slot and draggable chips fully visible without overlap clipping.

## 5) Fill-Blank Card
Current risk: sentence + chip bank + button stack can exceed height quickly.

Plan:
- Landscape split:
  - Left: sentence + blank
  - Right: chip bank + check action + feedback
- Convert feedback box to inline compact banner in low-height mode.
- Keep chip flow scrollable within bounded pane if choices are many.

## 6) Draw Match Card
Current risk: drawing area shrinks too much; accuracy model depends on canvas size.

Plan:
- Enforce minimum drawing surface dimensions in landscape before allowing submit evaluation.
- If below threshold, change to guided compact mode:
  - Smaller glyph
  - Wider horizontal canvas
  - Adjust score tracker tolerance relative to canvas dimensions.

## 7) Onboarding Screens
Goal: maintain onboarding momentum with no clipped controls.

Plan pattern for all onboarding screens:
- Use a reusable `OnboardingScaffoldResponsive` pattern:
  - Portrait: current stacked structure
  - Landscape: two-pane (illustration left, content/actions right) with persistent CTA area
- Make non-critical decorative elements optional/collapsible in height-constrained mode.
- Ensure all onboarding pages either scroll or fit with bottom CTA visible.

Priority screens with biggest landscape risk:
- `PermissionUsageScreen`
- `PermissionOverlayScreen`
- `PermissionNotifScreen`
- `PinSetupScreen`
- `OnboardingSuccessScreen`

## 8) Parent Dashboard
Goal: improve density and readability in landscape.

Plan:
- Switch dashboard tab content to adaptive grid/composition in medium/expanded widths:
  - Column A: Today Summary + Weekly Activity
  - Column B: Subject Breakdown + Recent Attempts
- Keep controls/settings in constrained max-width containers to avoid stretched lines.
- Evaluate moving bottom nav to side rail in expanded landscape (optional phase 2).

## Implementation Strategy (Phased)

### Phase 1: Infrastructure + shared responsive primitives
- Add adaptive helpers for width/height class detection.
- Add reusable responsive scaffolds for onboarding and overlay cards.
- Add baseline inset/padding tokens for landscape.

### Phase 2: Overlay-first adaptation (highest user impact)
- Update `QuizCardRouter` shell layout.
- Adapt all quiz cards for low-height landscape behavior.
- Validate strict-mode interaction still works as designed.

### Phase 3: Onboarding adaptation
- Apply two-pane responsive structure page-by-page.
- Guarantee CTA visibility and no clipping across landscape phone sizes.

### Phase 4: Dashboard adaptation
- Convert single-column dashboard into adaptive two-column composition.
- Tune chart/card dimensions for medium and expanded widths.

### Phase 5: Polish + accessibility + QA hardening
- Typography scaling audit for readability.
- Touch target and focus traversal audit.
- Motion/animation timing tune for reduced spatial jitter in landscape.

## QA Plan

## Device/viewport coverage
- Phone small landscape (approx 640x360 dp class)
- Phone medium landscape (approx 732x412 dp class)
- Fold/large phone landscape
- Tablet landscape (>= 840dp width)

## Functional checks
- Overlay interactions remain blocked/enabled correctly in strict/non-strict modes.
- Timer visibility and progress correctness across all card types.
- No clipped CTA buttons in onboarding.
- Dashboard cards remain readable and non-overlapping.

## Visual checks
- No text truncation for long localized strings.
- No overlap between mascot/train/timer/question UI.
- Chip/tile components maintain touch target sizes.

## Risks and Mitigations
- Risk: card-specific logic coupled tightly to current portrait hierarchy.
  - Mitigation: introduce shared responsive wrappers first, then migrate card internals incrementally.
- Risk: scoring/gesture behavior regresses in draw/drag cards due to resized canvases.
  - Mitigation: add dimension-dependent thresholds and run manual gesture regression suite.
- Risk: onboarding redesign drifts from current brand visuals.
  - Mitigation: preserve current visual language, only refactor structure and sizing rules.

## Definition of Done for Landscape MVP
- All onboarding screens are usable in landscape with visible primary action and no clipped critical content.
- All 5 quiz card types are playable in landscape without interaction regressions.
- Parent dashboard uses landscape space effectively (not a stretched portrait stack).
- QA passes on at least one small phone, one medium phone, and one tablet landscape profile.
