# MediQueue Database Schema & Seed Notes

## Schema Design Principles
The schema stores operational flow timestamps only:
- **No clinical records**: No diagnoses, symptoms, treatments, medications, or test values.
- **Anonymized Patient Registry**: Patients are identified solely by a pseudonym `patient_id` (e.g. `PAT-UUID`), an `age_group` category (`0-17`, `18-35`, `36-50`, `51-65`, `65+`), and gender.
- **Operational Workflow Model**:
  Each patient visit triggers a sequence of stages:
  1. `REGISTRATION` (General Admissions / Front Desk)
  2. `CONSULTATION` (Specialty Department e.g. Cardiology, Orthopedics, Pediatrics, OPD)
  3. `DIAGNOSTIC` (Radiology or Pathology Lab, if referred)
  4. `PHARMACY` (Hospital Central Pharmacy)
  5. `DISCHARGE` (Billing and Patient Exit)

## Timestamp Lifecycle
- `arrival_ts`: Timestamp when the patient joins the queue for that stage.
- `service_start_ts`: Timestamp when healthcare personnel begin serving the patient.
- `service_end_ts`: Timestamp when the stage concludes and the patient transitions.
- **Wait Time** = `service_start_ts` − `arrival_ts`
- **Service Duration** = `service_end_ts` − `service_start_ts`
- **Transition Delay** = Stage(N).`arrival_ts` − Stage(N-1).`service_end_ts`

## Injected Operational Anomalies
1. **Recurring Congestion Points**:
   - Radiology (Dept 5) on Mondays 10:00–12:00: MRI/CT queue spike, wait times exceed 75 minutes.
   - Pharmacy (Dept 7) Daily 17:00–19:00: Evening discharge prescription surge, wait times exceed 60 minutes.
   - Orthopedics (Dept 3) Wednesdays 09:00–11:00: Plaster/walk-in clinic peak.
2. **Dirty Data (~2.5%)**:
   - Missing timestamps (null `service_start_ts` or `service_end_ts`).
   - Chronological inversions (`service_end_ts` < `service_start_ts` or `service_start_ts` < `arrival_ts`).
   - Duplicate stage records.
