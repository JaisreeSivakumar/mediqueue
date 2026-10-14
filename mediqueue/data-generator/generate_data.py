#!/usr/bin/env python3
"""
MediQueue Synthetic Hospital Workflow Data Generator
Phase 1: Generates 150,000+ realistic operational hospital visits across 90+ days.
Operational records only: No PII, age_group only, timestamp flows, dirty rows, and injected bottlenecks.
"""

import os
import sys
import uuid
import random
import datetime
from datetime import timedelta
import pymysql

# Set reproducible seeds
SEED = int(os.getenv("DATA_GENERATOR_SEED", "42"))
random.seed(SEED)

TOTAL_VISITS = int(os.getenv("DATA_GENERATOR_VISITS", "150000"))
DAYS_SPAN = int(os.getenv("DATA_GENERATOR_DAYS", "95"))

# Database connection settings
MYSQL_HOST = os.getenv("MYSQL_HOST", "127.0.0.1")
MYSQL_PORT = int(os.getenv("MYSQL_PORT", "3307"))
MYSQL_DB = os.getenv("MYSQL_DATABASE", "mediqueue_db")
MYSQL_USER = os.getenv("MYSQL_USER", "mediqueue")
MYSQL_PASS = os.getenv("MYSQL_PASSWORD", "mediqueue_pass")

# Reference data: 9 Departments
DEPARTMENTS = [
    (1, "OPD", "Outpatient General Medicine", 1, 15),
    (2, "CARD", "Cardiology", 2, 20),
    (3, "ORTHO", "Orthopedics", 2, 20),
    (4, "PEDS", "Pediatrics", 1, 15),
    (5, "RAD", "Radiology", 0, 25),          # Basement / Ground
    (6, "LAB", "Pathology & Biochemistry", 0, 15),
    (7, "PHARM", "Central Pharmacy", 1, 10),
    (8, "EMERG", "Emergency & Trauma", 1, 10),
    (9, "SURG", "General Surgery Clinic", 3, 25),
]

DIAGNOSTIC_SERVICES = [
    (1, "XRAY", "Standard Digital X-Ray", 5, 15),
    (2, "MRI", "Magnetic Resonance Imaging (MRI)", 5, 40),
    (3, "CT_SCAN", "Computed Tomography (CT)", 5, 30),
    (4, "USG", "Ultrasound Sonography", 5, 20),
    (5, "CBC", "Complete Blood Count", 6, 10),
    (6, "LIPID", "Comprehensive Lipid Panel", 6, 12),
    (7, "ECG", "Electrocardiogram (ECG)", 2, 10),
    (8, "ECHO", "2D Echocardiogram", 2, 25),
]

AGE_GROUPS = ['0-17', '18-35', '36-50', '51-65', '65+']
AGE_WEIGHTS = [0.15, 0.28, 0.27, 0.18, 0.12]
GENDERS = ['M', 'F', 'OTHER']
GENDER_WEIGHTS = [0.49, 0.49, 0.02]

CLINICAL_DEPTS = [1, 2, 3, 4, 8, 9]  # OPD, Card, Ortho, Peds, Emerg, Surg
CLINICAL_WEIGHTS = [0.35, 0.15, 0.18, 0.14, 0.08, 0.10]

# Hour probabilities across 24 hours (simulating peak hours 09:00-12:00 and 16:00-18:00)
# Hospital doors open 07:00, peak 09-12, dip at lunch, second peak 16-18, minimal at night
HOURLY_WEIGHTS = [
    0.005, 0.005, 0.005, 0.005, 0.005, 0.01,   # 00:00 - 05:59
    0.03,  0.06,  0.09,  0.12,  0.13,  0.11,   # 06:00 - 11:59 (Morning Peak 9-12)
    0.06,  0.05,  0.05,  0.06,  0.08,  0.07,   # 12:00 - 17:59 (Afternoon Peak 16-18)
    0.03,  0.02,  0.015, 0.01,  0.005, 0.005   # 18:00 - 23:59
]

def get_connection():
    return pymysql.connect(
        host=MYSQL_HOST,
        port=MYSQL_PORT,
        user=MYSQL_USER,
        password=MYSQL_PASS,
        database=MYSQL_DB,
        charset='utf8mb4',
        autocommit=False
    )

def seed_reference_data(conn):
    print("[-] Seeding departments and diagnostic reference tables...")
    with conn.cursor() as cur:
        cur.execute("SET FOREIGN_KEY_CHECKS = 0;")
        cur.execute("TRUNCATE TABLE visit_stages;")
        cur.execute("TRUNCATE TABLE visits;")
        cur.execute("TRUNCATE TABLE diagnostic_services;")
        cur.execute("TRUNCATE TABLE patients;")
        cur.execute("TRUNCATE TABLE departments;")
        cur.execute("SET FOREIGN_KEY_CHECKS = 1;")

        for d in DEPARTMENTS:
            cur.execute("""
                INSERT INTO departments (department_id, department_code, department_name, floor_number, target_wait_min)
                VALUES (%s, %s, %s, %s, %s)
            """, d)

        for s in DIAGNOSTIC_SERVICES:
            cur.execute("""
                INSERT INTO diagnostic_services (service_id, service_code, service_name, department_id, typical_duration_min)
                VALUES (%s, %s, %s, %s, %s)
            """, s)
    conn.commit()
    print("[+] Departments and diagnostic reference tables seeded successfully.")

def generate_patients(num_patients):
    print(f"[-] Generating {num_patients:,} anonymized patient entities...")
    patients = []
    base_date = datetime.date.today() - timedelta(days=DAYS_SPAN + 180)
    for i in range(1, num_patients + 1):
        pid = f"PAT-{i:07d}"
        age_grp = random.choices(AGE_GROUPS, weights=AGE_WEIGHTS, k=1)[0]
        gender = random.choices(GENDERS, weights=GENDER_WEIGHTS, k=1)[0]
        reg_days = random.randint(0, 180)
        reg_date = base_date + timedelta(days=reg_days)
        patients.append((pid, age_grp, gender, reg_date))
    return patients

def generate_dataset():
    start_time = datetime.datetime.now()
    print(f"=== MediQueue Data Generator Started (Target: {TOTAL_VISITS:,} visits over {DAYS_SPAN} days) ===")
    
    # Establish connection
    try:
        conn = get_connection()
    except Exception as e:
        print(f"[!] Database connection error: {e}")
        sys.exit(1)

    seed_reference_data(conn)

    # 1. Generate unique patients pool (~60% of total visits to simulate returning patients)
    num_unique_patients = int(TOTAL_VISITS * 0.60)
    patients_pool = generate_patients(num_unique_patients)
    
    # Batch insert patients
    print("[-] Inserting patients into MySQL...")
    with conn.cursor() as cur:
        batch_size = 10000
        for i in range(0, len(patients_pool), batch_size):
            chunk = patients_pool[i:i+batch_size]
            cur.executemany("""
                INSERT INTO patients (patient_id, age_group, gender, registered_date)
                VALUES (%s, %s, %s, %s)
            """, chunk)
    conn.commit()
    print(f"[+] {len(patients_pool):,} patients inserted.")

    # 2. Date range calculation
    end_date = datetime.date.today() - timedelta(days=1)
    start_date = end_date - timedelta(days=DAYS_SPAN)

    patient_ids = [p[0] for p in patients_pool]

    # Pre-calculate day dates
    dates_list = [start_date + timedelta(days=d) for d in range(DAYS_SPAN + 1)]
    # Weight weekdays higher than weekends
    date_weights = []
    for d in dates_list:
        if d.weekday() < 5:  # Monday to Friday
            date_weights.append(1.75)
        elif d.weekday() == 5:  # Saturday
            date_weights.append(1.10)
        else:  # Sunday
            date_weights.append(0.55)

    print("[-] Generating visits and visit stages with realistic queues and deliberate bottlenecks...")

    visits_batch = []
    stages_batch = []
    
    stage_counter = 0
    total_dirty_count = 0
    injected_radiology_bottlenecks = 0
    injected_pharmacy_bottlenecks = 0

    hours_choices = list(range(24))

    for v_idx in range(1, TOTAL_VISITS + 1):
        visit_id = f"VST-{v_idx:08d}"
        patient_id = random.choice(patient_ids)
        visit_date = random.choices(dates_list, weights=date_weights, k=1)[0]
        weekday = visit_date.weekday() # 0 = Monday, 6 = Sunday

        primary_dept = random.choices(CLINICAL_DEPTS, weights=CLINICAL_WEIGHTS, k=1)[0]
        adm_type = 'EMERGENCY' if primary_dept == 8 else random.choices(
            ['OPD', 'ROUTINE', 'FOLLOW_UP'], weights=[0.70, 0.20, 0.10], k=1
        )[0]

        # Starting arrival hour based on hourly distribution
        hour = random.choices(hours_choices, weights=HOURLY_WEIGHTS, k=1)[0]
        minute = random.randint(0, 59)
        second = random.randint(0, 59)
        current_arrival = datetime.datetime.combine(visit_date, datetime.time(hour, minute, second))

        # Build workflow sequence
        # Always begins with REGISTRATION (Dept 1) unless Emergency (Dept 8)
        stages_flow = []
        if primary_dept == 8:
            stages_flow.append(('REGISTRATION', 8, None))
            stages_flow.append(('CONSULTATION', 8, None))
        else:
            stages_flow.append(('REGISTRATION', 1, None))
            stages_flow.append(('CONSULTATION', primary_dept, None))

        # Referral logic: 35% Radiology, 45% Lab, 15% both
        has_rad = random.random() < 0.35
        has_lab = random.random() < 0.45
        if has_rad:
            # Diagnostic service in Radiology
            service = random.choice([1, 2, 3, 4])
            stages_flow.append(('DIAGNOSTIC', 5, service))
        if has_lab:
            service = random.choice([5, 6])
            stages_flow.append(('DIAGNOSTIC', 6, service))

        # 85% of visits visit Central Pharmacy
        if random.random() < 0.85:
            stages_flow.append(('PHARMACY', 7, None))

        # 95% complete with Discharge stage
        if random.random() < 0.95:
            stages_flow.append(('DISCHARGE', primary_dept, None))

        total_stages = len(stages_flow)
        visits_batch.append((visit_id, patient_id, visit_date, adm_type, primary_dept, total_stages, 'COMPLETED'))

        # Generate stages timestamps
        stage_seq = 1
        for stage_name, dept_id, diag_service in stages_flow:
            stage_counter += 1
            stage_id = f"STG-{stage_counter:09d}"

            # Base wait time (minutes)
            if dept_id == 1:  # Registration
                base_wait = random.randint(5, 20)
            elif dept_id == 7:  # Pharmacy
                base_wait = random.randint(10, 25)
            elif dept_id in (5, 6):  # Diagnostics
                base_wait = random.randint(15, 30)
            else:  # Consultation
                base_wait = random.randint(12, 28)

            # --- INJECTED BOTTLENECK 1: Radiology on Mondays 10:00 - 12:00 ---
            # Monday is weekday 0
            if dept_id == 5 and weekday == 0 and 10 <= current_arrival.hour < 12:
                # Add heavy queue delay: +55 to 110 minutes
                base_wait += random.randint(55, 110)
                injected_radiology_bottlenecks += 1

            # --- INJECTED BOTTLENECK 2: Central Pharmacy Daily 17:00 - 19:00 ---
            if dept_id == 7 and 17 <= current_arrival.hour < 19:
                # Add evening prescription queue: +40 to 80 minutes
                base_wait += random.randint(40, 80)
                injected_pharmacy_bottlenecks += 1

            # --- INJECTED BOTTLENECK 3: Orthopedics on Wednesdays 09:00 - 11:00 ---
            if dept_id == 3 and weekday == 2 and 9 <= current_arrival.hour < 11:
                base_wait += random.randint(35, 70)

            # Service duration (minutes)
            if stage_name == 'REGISTRATION':
                service_dur = random.randint(3, 8)
            elif stage_name == 'DISCHARGE':
                service_dur = random.randint(4, 10)
            elif stage_name == 'PHARMACY':
                service_dur = random.randint(4, 12)
            elif stage_name == 'DIAGNOSTIC':
                if diag_service in (2, 3):  # MRI or CT
                    service_dur = random.randint(25, 45)
                else:
                    service_dur = random.randint(8, 20)
            else:  # CONSULTATION
                service_dur = random.randint(10, 30)

            arr_ts = current_arrival
            start_ts = arr_ts + timedelta(minutes=base_wait, seconds=random.randint(0, 50))
            end_ts = start_ts + timedelta(minutes=service_dur, seconds=random.randint(0, 50))

            # --- INJECT DIRTY DATA (~2.5% frequency) ---
            is_dirty = random.random() < 0.025
            if is_dirty:
                total_dirty_count += 1
                dirty_kind = random.choice(['NULL_END', 'NULL_START', 'INVERTED', 'DUPLICATE'])
                if dirty_kind == 'NULL_END':
                    end_ts = None
                elif dirty_kind == 'NULL_START':
                    start_ts = None
                elif dirty_kind == 'INVERTED':
                    # End timestamp occurs before start timestamp
                    end_ts = start_ts - timedelta(minutes=random.randint(5, 30))
                elif dirty_kind == 'DUPLICATE':
                    # Add duplicate stage record with duplicate key marker
                    dup_stage_id = f"STG-DUP-{stage_counter:09d}"
                    stages_batch.append((
                        dup_stage_id, visit_id, stage_seq, stage_name, dept_id, diag_service,
                        arr_ts, start_ts, end_ts
                    ))

            stages_batch.append((
                stage_id, visit_id, stage_seq, stage_name, dept_id, diag_service,
                arr_ts, start_ts, end_ts
            ))

            # Next stage arrives a few minutes after previous service ends
            if end_ts is not None:
                transit_time = random.randint(3, 15)  # 3-15 minutes walking/transit
                current_arrival = end_ts + timedelta(minutes=transit_time)
            else:
                current_arrival = arr_ts + timedelta(minutes=30)
            
            stage_seq += 1

        # Periodic batch flush to MySQL
        if len(visits_batch) >= 10000:
            with conn.cursor() as cur:
                cur.executemany("""
                    INSERT INTO visits (visit_id, patient_id, visit_date, admission_type, primary_department_id, total_stages, status)
                    VALUES (%s, %s, %s, %s, %s, %s, %s)
                """, visits_batch)
                cur.executemany("""
                    INSERT INTO visit_stages (stage_id, visit_id, stage_sequence, stage_name, department_id, diagnostic_service_id, arrival_ts, service_start_ts, service_end_ts)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
                """, stages_batch)
            conn.commit()
            print(f"  -> Committed {v_idx:,}/{TOTAL_VISITS:,} visits ({stage_counter:,} stages so far)...")
            visits_batch = []
            stages_batch = []

    # Final flush
    if visits_batch:
        with conn.cursor() as cur:
            cur.executemany("""
                INSERT INTO visits (visit_id, patient_id, visit_date, admission_type, primary_department_id, total_stages, status)
                VALUES (%s, %s, %s, %s, %s, %s, %s)
            """, visits_batch)
            cur.executemany("""
                INSERT INTO visit_stages (stage_id, visit_id, stage_sequence, stage_name, department_id, diagnostic_service_id, arrival_ts, service_start_ts, service_end_ts)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
            """, stages_batch)
        conn.commit()

    conn.close()
    elapsed = (datetime.datetime.now() - start_time).total_seconds()
    print("\n=== Generation Summary ===")
    print(f"Total Visits Generated    : {TOTAL_VISITS:,}")
    print(f"Total Stages Generated    : {stage_counter:,}")
    print(f"Dirty Stage Records Added : {total_dirty_count:,} (~{(total_dirty_count/stage_counter)*100:.2f}%)")
    print(f"Radiology Mon 10-12 Spikes: {injected_radiology_bottlenecks:,}")
    print(f"Pharmacy Daily 17-19 Spikes: {injected_pharmacy_bottlenecks:,}")
    print(f"Execution Elapsed Time    : {elapsed:.2f} seconds")
    print("============================================================\n")

if __name__ == "__main__":
    generate_dataset()
