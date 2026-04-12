1. Shared Framework (Always Present)
  Every quiz card is wrapped in a common layout that uses the following elements:
   * Background: A modern, clean background with "Learning Mode" styling.
   * Timer Bar (Top): Shows remaining time. In Portrait, it's centered at the top; in Landscape, it's wider across the top.
   * Train Animation: A "Steam Train" engine that appears at the start and persists as a visual anchor.
       * Portrait: Takes up about 25% of the top height.
       * Landscape: Sits in a left sidebar that uses about 30-36% of the width depending on layout mode.
   * Mascot (Right/Sidebar): A friendly character that stays on the screen.
       * Portrait: Floats in the top-right corner.
       * Landscape: Sits in the left sidebar above the train.
   * Parent Button (Bottom Left): A small lock button in the corner that opens the parent PIN screen.
   * Confetti Effect: A full-screen overlay that triggers only on a correct answer.

  ---

  2. Card-Specific Layouts
  The remaining space (75% height in Portrait, about 64-70% width in Landscape) is used by the actual quiz content.

  TapChoiceCard (Multiple Choice)
   * Question Panel: Displays the main question and a smaller instruction label.
       * Portrait: Uses 52% of the content area.
       * Landscape: Uses 42% of the content area on the left.
   * Answers: Rounded colored buttons.
       * Portrait: Stacked vertically (48% height).
       * Landscape: Arranged in a 2x2 grid (58% width).

  TapTapMatchCard (Memory Match)
   * Question/Instruction: Brief text at the top or side.
       * Portrait: Shown at the top.
       * Landscape: Shown in a left column using about 34% of the card width.
   * Matching Tiles:
       * Portrait: Two fixed rows, with one row per side of the match.
       * Landscape: Two fixed columns, with one column per side of the match.

  FillBlankCard (Spelling/Sentence)
   * Prompt Area: Shows the main question and instruction.
   * Sentence Blank: Displays the sentence with a large tappable blank.
   * Word Bank / Submit Area: Word chips and a submit action appear in the lower or right-side interaction area depending on orientation.

  DrawMatchCard (Tracing)
   * Question/Instruction: Text panel.
       * Portrait: 35% weight.
       * Landscape: 34% weight.
   * Drawing Canvas: A large interactive area where a faint letter/shape is shown for tracing.
       * Portrait: 65% weight.
       * Landscape: 66% weight.

  DragDropMatchCard (Sorting)
   * Question/Instruction:
       * Portrait: Shown at the top.
       * Landscape: Shown in a left column using about 34% of the card width.
   * Drag/Drop Play Area:
       * Portrait: Uses the remaining main area below the prompt.
       * Landscape: Uses the right side of the card at about 66% width.
   * Interaction Model: Draggable chips are arranged around a central drop target.

  Summary of Space Usage

  ┌─────────────┬─────────────────────────────┬────────────────────────────────┐
  │ Orientation │ Shared (Train/Mascot/Timer) │ Content (Question/Interaction) │
  ├─────────────┼─────────────────────────────┼────────────────────────────────┤
  │ Portrait    │ ~25-30% Height              │ ~70-75% Height                 │
  │ Landscape   │ ~30-36% Width (Sidebar)     │ ~64-70% Width (Main)           │
  └─────────────┴─────────────────────────────┴────────────────────────────────┘


  Each card is designed so the main interaction area stays in the most accessible part of the
  screen, typically the lower portion in portrait and the right-side main panel in landscape.
