-- ============================================================
-- SUPABASE SCHEMA — AI Quiz Overlay MVP
-- Paste this entire block into Bolt.
-- Run in Supabase SQL Editor in this exact order.
-- ============================================================


-- ── 1. TESTERS ───────────────────────────────────────────────
-- One row per device install. tester_id is a UUID generated
-- on first app open and stored in DataStore. No name or email.

create table testers (
  id                        uuid primary key default gen_random_uuid(),
  tester_id                 text not null unique,
  nickname                  text,
  app_version               text,
  overlay_permission_granted boolean default false,
  usage_access_granted      boolean default false,
  battery_opt_disabled      boolean default false,
  created_at                timestamptz default now()
);


-- ── 2. QUIZ QUESTIONS ────────────────────────────────────────
-- Master question bank. Android app fetches active rows only.
-- payload_json holds the full type-specific payload as JSON string.
-- card_type must match exactly: TAP_CHOICE | TAP_TAP_MATCH |
-- DRAG_DROP_MATCH | FILL_BLANK

create table quiz_questions (
  id                   text primary key,
  card_type            text not null
                         check (card_type in (
                           'TAP_CHOICE',
                           'TAP_TAP_MATCH',
                           'DRAG_DROP_MATCH',
                           'FILL_BLANK'
                         )),
  subject              text not null
                         check (subject in (
                           'math', 'english', 'science', 'general'
                         )),
  difficulty           integer not null check (difficulty between 1 and 3),
  question_text        text not null,
  instruction_label    text not null,
  media_url            text,
  payload_json         text not null,
  timer_seconds        integer not null default 20,
  strict_mode          boolean not null default false,
  show_correct_on_wrong boolean not null default true,
  active               boolean not null default true,
  created_at           timestamptz default now()
);


-- ── 3. QUIZ ATTEMPTS ─────────────────────────────────────────
-- One row per quiz shown to a child. Written locally in Room
-- first, then synced here by SyncWorker.
-- source_app is the package name e.g. com.instagram.android

create table quiz_attempts (
  id                   text primary key,
  tester_id            text not null references testers(tester_id),
  question_id          text not null references quiz_questions(id),
  shown_at             timestamptz not null,
  answered_at          timestamptz,
  selected_option_id   text,
  is_correct           boolean not null default false,
  was_dismissed        boolean not null default false,
  was_timer_expired    boolean not null default false,
  response_time_ms     bigint not null default 0,
  source_app           text not null,
  created_at           timestamptz default now()
);


-- ── 4. OVERLAY SESSIONS ──────────────────────────────────────
-- One row per continuous Foreground Service run.
-- started_at = service onStartCommand, ended_at = onDestroy.

create table overlay_sessions (
  id                   uuid primary key default gen_random_uuid(),
  tester_id            text not null references testers(tester_id),
  started_at           timestamptz not null,
  ended_at             timestamptz,
  source_app           text,
  total_quizzes_shown  integer default 0,
  total_answered       integer default 0,
  total_dismissed      integer default 0,
  total_timer_expired  integer default 0,
  created_at           timestamptz default now()
);


-- ── 5. EVENT LOGS ────────────────────────────────────────────
-- Lightweight structured event stream.
-- event_type values: quiz_shown | quiz_answered | quiz_dismissed |
-- quiz_timer_expired | overlay_started | overlay_stopped |
-- parent_pause_started | parent_pause_ended | pin_failed |
-- app_opened | permission_granted

create table event_logs (
  id            text primary key,
  tester_id     text not null references testers(tester_id),
  event_type    text not null,
  payload_json  text,
  created_at    timestamptz not null
);


-- ── 6. PARENT PAUSE LOG ──────────────────────────────────────
-- Tracks every time a parent triggered a quick pause.
-- Used on dashboard to show parent vs child usage patterns.
-- access_point: notification | overlay_card

create table parent_pauses (
  id              uuid primary key default gen_random_uuid(),
  tester_id       text not null references testers(tester_id),
  paused_at       timestamptz not null,
  resume_at       timestamptz not null,
  actual_resumed_at timestamptz,
  duration_minutes integer not null check (duration_minutes in (30, 60, 120)),
  access_point    text not null check (access_point in ('notification', 'overlay_card')),
  resumed_early   boolean default false,
  created_at      timestamptz default now()
);


-- ── 7. FEEDBACK ──────────────────────────────────────────────
-- One row per tester feedback submission from the app.

create table feedback (
  id          uuid primary key default gen_random_uuid(),
  tester_id   text not null references testers(tester_id),
  rating      integer check (rating between 1 and 5),
  comment     text,
  app_version text,
  created_at  timestamptz default now()
);


-- ── INDEXES ──────────────────────────────────────────────────
-- These cover the most common query patterns from SyncWorker
-- and any Supabase dashboard queries you run manually.

create index idx_attempts_tester     on quiz_attempts(tester_id);
create index idx_attempts_shown_at   on quiz_attempts(shown_at);
create index idx_attempts_question   on quiz_attempts(question_id);
create index idx_events_tester       on event_logs(tester_id);
create index idx_events_type         on event_logs(event_type);
create index idx_events_created      on event_logs(created_at);
create index idx_sessions_tester     on overlay_sessions(tester_id);
create index idx_pauses_tester       on parent_pauses(tester_id);
create index idx_questions_active    on quiz_questions(active, subject, difficulty);


-- ── ROW LEVEL SECURITY ───────────────────────────────────────
-- MVP uses direct app → Supabase REST with the anon key.
-- RLS is enabled but policy is open for MVP since there is no
-- auth system. Tighten before any public release.

alter table testers          enable row level security;
alter table quiz_questions   enable row level security;
alter table quiz_attempts    enable row level security;
alter table overlay_sessions enable row level security;
alter table event_logs       enable row level security;
alter table parent_pauses    enable row level security;
alter table feedback         enable row level security;

create policy "anon_all" on testers          for all using (true) with check (true);
create policy "anon_all" on quiz_questions   for all using (true) with check (true);
create policy "anon_all" on quiz_attempts    for all using (true) with check (true);
create policy "anon_all" on overlay_sessions for all using (true) with check (true);
create policy "anon_all" on event_logs       for all using (true) with check (true);
create policy "anon_all" on parent_pauses    for all using (true) with check (true);
create policy "anon_all" on feedback         for all using (true) with check (true);


-- ── SAMPLE SEED DATA — 5 QUESTIONS (one per type + extras) ───
-- Bolt: use these as templates to generate the full 50–100 set.
-- Ensure payload_json is valid JSON with no line breaks inside.

insert into quiz_questions
  (id, card_type, subject, difficulty, question_text, instruction_label, payload_json, timer_seconds, strict_mode, show_correct_on_wrong)
values

('q_001', 'TAP_CHOICE', 'math', 1,
 'What is 6 × 7?', 'Tap the correct answer',
 '{"options":[{"id":"A","label":"42","is_correct":true},{"id":"B","label":"36","is_correct":false},{"id":"C","label":"48","is_correct":false},{"id":"D","label":"54","is_correct":false}]}',
 20, false, true),

('q_002', 'TAP_CHOICE', 'general', 1,
 'Which planet is closest to the Sun?', 'Tap the correct answer',
 '{"options":[{"id":"A","label":"Venus","is_correct":false},{"id":"B","label":"Earth","is_correct":false},{"id":"C","label":"Mercury","is_correct":true},{"id":"D","label":"Mars","is_correct":false}]}',
 20, false, true),

('q_003', 'TAP_TAP_MATCH', 'english', 1,
 'Match each word to its meaning', 'Tap left then tap right to match',
 '{"pairs":[{"left_id":"L1","left_label":"Dog","right_id":"R3","right_label":"Animal"},{"left_id":"L2","left_label":"Run","right_id":"R1","right_label":"Move fast"},{"left_id":"L3","left_label":"Big","right_id":"R4","right_label":"Large"},{"left_id":"L4","left_label":"Eat","right_id":"R2","right_label":"Food"}],"right_order_shuffled":["R2","R4","R1","R3"]}',
 0, false, true),

('q_004', 'DRAG_DROP_MATCH', 'math', 2,
 'Put these numbers in order smallest to largest', 'Drag each number to the right slot',
 '{"chips":[{"chip_id":"C1","label":"2"},{"chip_id":"C2","label":"4"},{"chip_id":"C3","label":"7"},{"chip_id":"C4","label":"9"}],"slots":[{"slot_id":"S1","slot_label":"Smallest","correct_chip_id":"C1"},{"slot_id":"S2","slot_label":"2nd","correct_chip_id":"C2"},{"slot_id":"S3","slot_label":"3rd","correct_chip_id":"C3"},{"slot_id":"S4","slot_label":"Largest","correct_chip_id":"C4"}]}',
 0, true, true),

('q_005', 'FILL_BLANK', 'science', 1,
 'The ___ is the closest star to Earth.', 'Tap a word to fill the blank',
 '{"sentence_template":"The ___ is the closest star to Earth.","word_bank":[{"chip_id":"W1","label":"Moon","is_correct":false},{"chip_id":"W2","label":"Sun","is_correct":true},{"chip_id":"W3","label":"Mars","is_correct":false},{"chip_id":"W4","label":"Earth","is_correct":false}]}',
 20, false, true);