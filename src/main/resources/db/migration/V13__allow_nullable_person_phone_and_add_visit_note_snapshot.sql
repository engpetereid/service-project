-- ===========================================================================
-- V13: Allow nullable person phone, add max_note_score_snapshot to visit_record,
-- and add missing performance indexes.
-- ===========================================================================

-- 1. Allow nullable phone for young students who only have guardian phones
ALTER TABLE person MODIFY phone VARCHAR(20) NULL;

-- 2. Add max_note_score_snapshot to visit_record for historical accuracy
ALTER TABLE visit_record ADD COLUMN max_note_score_snapshot INT NOT NULL DEFAULT 21;

-- 3. Composite performance indexes for queries & statistics
CREATE INDEX idx_visit_record_year_student ON visit_record(academic_year_id, student_id);
CREATE INDEX idx_attendance_record_student_present ON attendance_record(student_id, present);
CREATE INDEX idx_confession_record_student_year ON confession_record(student_id, academic_year_id);
CREATE INDEX idx_student_placement_year_servant_status ON student_placement(academic_year_id, servant_id, status);
