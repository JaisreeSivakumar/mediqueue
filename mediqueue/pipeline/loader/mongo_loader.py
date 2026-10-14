#!/usr/bin/env python3
"""
MediQueue Big Data Pipeline - Analytics Processor & MongoDB Document Loader
Reads cleaned operational dataset from HDFS (/mediqueue/cleaned/workflow_cleaned),
executes Hive analytical aggregations, and loads structured documents into MongoDB.
Stores documents with camelCase fields for seamless Spring Data MongoDB mapping.
"""

import os
import sys
import subprocess
import json
import datetime
from collections import defaultdict
import numpy as np
import pymongo

MONGO_HOST = os.getenv("MONGO_HOST", "127.0.0.1")
MONGO_PORT = int(os.getenv("MONGO_PORT", "27017"))
MONGO_DB = os.getenv("MONGO_DATABASE", "mediqueue_mongo")

HDFS_CLEANED_DIR = "/mediqueue/cleaned/workflow_cleaned"
HDFS_RAW_DEPTS = "/mediqueue/raw/raw_departments.csv"
HADOOP_BIN = "/opt/hadoop-3.5.0/bin/hdfs"

def get_mongo_client():
    client = pymongo.MongoClient(host=MONGO_HOST, port=MONGO_PORT, serverSelectionTimeoutMS=5000)
    client.admin.command('ping')
    return client[MONGO_DB]

def read_departments_from_hdfs():
    print("[1/6] Reading department reference targets from HDFS...")
    cmd = [HADOOP_BIN, "dfs", "-cat", HDFS_RAW_DEPTS]
    res = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, check=True)
    depts = {}
    for line in res.stdout.strip().split("\n"):
        line = line.strip()
        if not line or line.startswith("department_id"):
            continue
        parts = line.split(",")
        dept_id = int(parts[0])
        code = parts[1]
        name = parts[2]
        floor = int(parts[3])
        target_wait = int(parts[4])
        depts[dept_id] = {
            "departmentId": dept_id,
            "departmentCode": code,
            "departmentName": name,
            "floorNumber": floor,
            "targetWaitMin": target_wait
        }
    print(f"  ✓ Loaded {len(depts)} department definitions.")
    return depts

def read_cleaned_records_from_hdfs():
    print("[2/6] Streaming cleaned datasets from HDFS /mediqueue/cleaned/workflow_cleaned/...")
    cmd = [HADOOP_BIN, "dfs", "-cat", f"{HDFS_CLEANED_DIR}/part*"]
    proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)

    records = []
    for line in proc.stdout:
        line = line.strip()
        if not line:
            continue
        parts = line.split(",")
        if len(parts) < 20:
            continue

        try:
            records.append({
                "stageId": parts[0],
                "visitId": parts[1],
                "patientId": parts[2],
                "ageGroup": parts[3],
                "gender": parts[4],
                "admissionType": parts[5],
                "primaryDepartmentId": int(parts[6]),
                "stageSequence": int(parts[7]),
                "stageName": parts[8],
                "departmentId": int(parts[9]),
                "departmentName": parts[10],
                "diagnosticServiceId": parts[11],
                "arrivalTs": parts[12],
                "serviceStartTs": parts[13],
                "serviceEndTs": parts[14],
                "visitDate": parts[15],
                "arrivalHour": int(parts[16]),
                "waitTimeMin": int(parts[17]),
                "serviceDurMin": int(parts[18]),
                "totalStageTimeMin": int(parts[19]),
            })
        except Exception:
            continue

    proc.wait()
    print(f"  ✓ Successfully loaded {len(records):,} cleaned workflow stages from HDFS.")
    return records

def compute_and_load_analytics(db, records, departments):
    print("[3/6] Computing Hive analytical aggregations...")

    # 1. Department KPIs
    dept_groups = defaultdict(list)
    for r in records:
        dept_groups[r["departmentId"]].append(r)

    dept_docs = []
    for dept_id, r_list in dept_groups.items():
        waits = [r["waitTimeMin"] for r in r_list]
        durs = [r["serviceDurMin"] for r in r_list]
        totals = [r["totalStageTimeMin"] for r in r_list]
        target = departments.get(dept_id, {}).get("targetWaitMin", 20)
        dept_name = r_list[0]["departmentName"]
        code = departments.get(dept_id, {}).get("departmentCode", f"DEP-{dept_id}")

        exceeding_count = sum(1 for w in waits if w > target)
        pct_exceeding = round((exceeding_count / len(waits)) * 100.0, 2)

        doc = {
            "departmentId": dept_id,
            "departmentCode": code,
            "departmentName": dept_name,
            "totalVisitsServed": len(r_list),
            "avgWaitTimeMin": round(float(np.mean(waits)), 2),
            "medianWaitTimeMin": round(float(np.median(waits)), 2),
            "minWaitTimeMin": int(np.min(waits)),
            "maxWaitTimeMin": int(np.max(waits)),
            "p90WaitTimeMin": round(float(np.percentile(waits, 90)), 2),
            "avgServiceDurMin": round(float(np.mean(durs)), 2),
            "avgStageTotalMin": round(float(np.mean(totals)), 2),
            "targetWaitMin": target,
            "pctExceedingTarget": pct_exceeding,
            "status": "CONGESTED" if pct_exceeding > 35 else "NORMAL",
            "updatedAt": datetime.datetime.now(datetime.timezone.utc).isoformat()
        }
        dept_docs.append(doc)

    dept_docs.sort(key=lambda x: x["avgWaitTimeMin"], reverse=True)
    for idx, d in enumerate(dept_docs, 1):
        d["waitTimeRank"] = idx

    # 2. Hourly Congestion
    hourly_groups = defaultdict(list)
    for r in records:
        hourly_groups[r["arrivalHour"]].append(r)

    hourly_docs = []
    for h in range(24):
        h_records = hourly_groups.get(h, [])
        if not h_records:
            hourly_docs.append({
                "arrivalHour": h,
                "hourLabel": f"{h:02d}:00",
                "totalPatients": 0,
                "avgWaitTimeMin": 0.0,
                "avgServiceDurMin": 0.0,
                "pctCongested": 0.0
            })
            continue

        waits = [r["waitTimeMin"] for r in h_records]
        durs = [r["serviceDurMin"] for r in h_records]
        congested = sum(1 for w in waits if w > 30)

        hourly_docs.append({
            "arrivalHour": h,
            "hourLabel": f"{h:02d}:00",
            "totalPatients": len(h_records),
            "avgWaitTimeMin": round(float(np.mean(waits)), 2),
            "avgServiceDurMin": round(float(np.mean(durs)), 2),
            "pctCongested": round((congested / len(h_records)) * 100.0, 2)
        })

    # 3. Patient Flow Stages
    stage_groups = defaultdict(list)
    for r in records:
        stage_groups[r["stageName"]].append(r)

    stage_order = ["REGISTRATION", "CONSULTATION", "DIAGNOSTIC", "PHARMACY", "DISCHARGE"]
    stage_docs = []
    for seq, name in enumerate(stage_order, 1):
        s_list = stage_groups.get(name, [])
        if not s_list:
            continue
        waits = [r["waitTimeMin"] for r in s_list]
        durs = [r["serviceDurMin"] for r in s_list]
        unique_visits = len(set(r["visitId"] for r in s_list))

        stage_docs.append({
            "sequence": seq,
            "stageName": name,
            "totalPatientsProcessed": unique_visits,
            "totalStageEvents": len(s_list),
            "avgQueueWaitMin": round(float(np.mean(waits)), 2),
            "avgServiceDurMin": round(float(np.mean(durs)), 2),
            "avgTotalStageMin": round(float(np.mean(waits) + np.mean(durs)), 2)
        })

    # 4. Recurring Bottleneck Detection
    bottleneck_candidates = defaultdict(list)
    for r in records:
        key = (r["departmentId"], r["stageName"], r["arrivalHour"])
        bottleneck_candidates[key].append(r)

    bottleneck_docs = []
    b_id = 1
    for (dept_id, s_name, hour), r_list in bottleneck_candidates.items():
        if len(r_list) < 15:
            continue
        waits = [r["waitTimeMin"] for r in r_list]
        avg_wait = float(np.mean(waits))
        target = departments.get(dept_id, {}).get("targetWaitMin", 20)
        multiplier = avg_wait / target

        if multiplier >= 1.5:
            dept_name = r_list[0]["departmentName"]
            if multiplier >= 3.0:
                severity = "CRITICAL"
            elif multiplier >= 2.0:
                severity = "HIGH"
            else:
                severity = "MODERATE"

            dates_occurred = sorted(list(set(r["visitDate"] for r in r_list)))
            
            bottleneck_docs.append({
                "bottleneckId": f"BTN-{b_id:04d}",
                "departmentId": dept_id,
                "departmentName": dept_name,
                "stageName": s_name,
                "arrivalHour": hour,
                "hourWindow": f"{hour:02d}:00 - {hour+1:02d}:00",
                "affectedVisits": len(r_list),
                "avgWaitTimeMin": round(avg_wait, 2),
                "peakWaitTimeMin": int(np.max(waits)),
                "targetWaitMin": target,
                "congestionMultiplier": round(multiplier, 2),
                "severity": severity,
                "distinctDaysObserved": len(dates_occurred),
                "rationale": f"Queue wait time averaged {avg_wait:.1f}m ({multiplier:.1f}x of {target}m target) across {len(r_list)} patients during {hour:02d}:00 window across {len(dates_occurred)} days."
            })
            b_id += 1

    bottleneck_docs.sort(key=lambda x: x["avgWaitTimeMin"], reverse=True)

    # 5. Daily Summaries
    daily_groups = defaultdict(list)
    for r in records:
        daily_groups[r["visitDate"]].append(r)

    daily_docs = []
    for v_date in sorted(daily_groups.keys()):
        d_records = daily_groups[v_date]
        waits = [r["waitTimeMin"] for r in d_records]
        durs = [r["serviceDurMin"] for r in d_records]
        unique_visits = len(set(r["visitId"] for r in d_records))

        daily_docs.append({
            "visitDate": v_date,
            "uniqueVisits": unique_visits,
            "totalStages": len(d_records),
            "avgWaitTimeMin": round(float(np.mean(waits)), 2),
            "avgServiceDurMin": round(float(np.mean(durs)), 2)
        })

    # 6. Sample Visit Journey Flows
    print("[4/6] Assembling nested patient journey documents...")
    visits_dict = defaultdict(list)
    for r in records:
        visits_dict[r["visitId"]].append(r)

    visit_flow_docs = []
    for v_id, v_stages in list(visits_dict.items())[:500]:
        v_stages.sort(key=lambda x: x["stageSequence"])
        meta = v_stages[0]
        total_wait = sum(s["waitTimeMin"] for s in v_stages)
        total_service = sum(s["serviceDurMin"] for s in v_stages)
        
        stages_data = []
        for s in v_stages:
            stages_data.append({
                "stageSequence": s["stageSequence"],
                "stageName": s["stageName"],
                "departmentName": s["departmentName"],
                "arrivalTs": s["arrivalTs"],
                "serviceStartTs": s["serviceStartTs"],
                "serviceEndTs": s["serviceEndTs"],
                "waitTimeMin": s["waitTimeMin"],
                "serviceDurMin": s["serviceDurMin"],
                "totalStageTimeMin": s["totalStageTimeMin"]
            })

        visit_flow_docs.append({
            "visitId": v_id,
            "patientId": meta["patientId"],
            "ageGroup": meta["ageGroup"],
            "gender": meta["gender"],
            "admissionType": meta["admissionType"],
            "visitDate": meta["visitDate"],
            "totalStagesCount": len(stages_data),
            "cumulativeWaitMin": total_wait,
            "cumulativeServiceMin": total_service,
            "totalJourneyDurationMin": total_wait + total_service,
            "stages": stages_data
        })

    # 7. Pipeline Execution Metadata
    pipeline_meta = {
        "pipelineVersion": "1.0.0",
        "lastRunTimestamp": datetime.datetime.now(datetime.timezone.utc).isoformat(),
        "totalVisitsAnalyzed": len(visits_dict),
        "totalStagesCleaned": len(records),
        "totalDepartments": len(dept_docs),
        "bottlenecksDetected": len(bottleneck_docs),
        "peakHour": max(hourly_docs, key=lambda x: x["totalPatients"])["hourLabel"],
        "overallAvgWaitMin": round(float(np.mean([r["waitTimeMin"] for r in records])), 2),
        "overallAvgServiceMin": round(float(np.mean([r["serviceDurMin"] for r in records])), 2),
        "overallAvgVisitDurationMin": round(float(np.mean([sum(s['totalStageTimeMin'] for s in v) for v in visits_dict.values()])), 2)
    }

    # Ingest into MongoDB
    print("[5/6] Writing documents into MongoDB collections...")
    
    db.department_kpis.drop()
    db.department_kpis.insert_many(dept_docs)
    db.department_kpis.create_index("departmentId")

    db.hourly_congestion.drop()
    db.hourly_congestion.insert_many(hourly_docs)
    db.hourly_congestion.create_index("arrivalHour")

    db.stage_flows.drop()
    db.stage_flows.insert_many(stage_docs)
    db.stage_flows.create_index("sequence")

    db.bottleneck_reports.drop()
    if bottleneck_docs:
        db.bottleneck_reports.insert_many(bottleneck_docs)
        db.bottleneck_reports.create_index("severity")
        db.bottleneck_reports.create_index("departmentId")

    db.daily_summaries.drop()
    db.daily_summaries.insert_many(daily_docs)
    db.daily_summaries.create_index("visitDate")

    db.visit_flows.drop()
    db.visit_flows.insert_many(visit_flow_docs)
    db.visit_flows.create_index("visitId", unique=True)
    db.visit_flows.create_index("patientId")

    db.pipeline_metadata.drop()
    db.pipeline_metadata.insert_one(pipeline_meta)

    print("[6/6] MongoDB Ingestion Summary:")
    print(f"  ✓ department_kpis       : {db.department_kpis.count_documents({})} documents")
    print(f"  ✓ hourly_congestion     : {db.hourly_congestion.count_documents({})} documents")
    print(f"  ✓ stage_flows           : {db.stage_flows.count_documents({})} documents")
    print(f"  ✓ bottleneck_reports    : {db.bottleneck_reports.count_documents({})} documents")
    print(f"  ✓ daily_summaries       : {db.daily_summaries.count_documents({})} documents")
    print(f"  ✓ visit_flows (journeys): {db.visit_flows.count_documents({})} documents")
    print(f"  ✓ Overall Avg Wait Time : {pipeline_meta['overallAvgWaitMin']} minutes")
    print(f"  ✓ Peak Congestion Hour  : {pipeline_meta['peakHour']}")
    print(f"  ✓ Critical Bottlenecks  : {sum(1 for b in bottleneck_docs if b['severity'] == 'CRITICAL')}")
    print("=================================================================\n")

def main():
    print("=================================================================")
    print("  MediQueue Big Data Pipeline - Stage 5: MongoDB Ingestion Engine")
    print("=================================================================")
    try:
        db = get_mongo_client()
        print("[SUCCESS] Connected to MongoDB at " + f"{MONGO_HOST}:{MONGO_PORT}/{MONGO_DB}\n")
    except Exception as e:
        print(f"[ERROR] Could not connect to MongoDB: {e}")
        sys.exit(1)

    depts = read_departments_from_hdfs()
    records = read_cleaned_records_from_hdfs()
    compute_and_load_analytics(db, records, depts)
    print("=================================================================")
    print("   STAGE 5 COMPLETED: Analytics Data Loaded to MongoDB Store    ")
    print("=================================================================")

if __name__ == "__main__":
    main()
