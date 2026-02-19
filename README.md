# 🚀 RIFT – Real-Time Intelligent Fraud Tracker

## 📌 Overview

RIFT is a Graph-Based Financial Crime Detection Engine developed for the **RIFT 2026 Hackathon – Graph Theory / Financial Crime Detection Track**.

The system analyzes transaction data and detects complex money muling networks using:

- Graph Theory
- Ensemble Machine Learning
- Explainable AI (XAI)
- Temporal Pattern Analysis

RIFT is designed to detect fraud patterns that traditional database queries fail to identify.

---

## 🏗 System Architecture

CSV Upload  
↓  
Transaction Parsing  
↓  
Directed Graph Construction  
↓  
Pattern Detection Engines  
&nbsp;&nbsp;&nbsp;&nbsp;• Cycle Detection (DFS-based)  
&nbsp;&nbsp;&nbsp;&nbsp;• Smurfing Detection (Fan-In / Fan-Out)  
&nbsp;&nbsp;&nbsp;&nbsp;• Layered Network Detection  
&nbsp;&nbsp;&nbsp;&nbsp;• Temporal Pattern Analysis  
↓  
Ensemble Machine Learning Risk Scoring  
&nbsp;&nbsp;&nbsp;&nbsp;• Isolation Forest  
&nbsp;&nbsp;&nbsp;&nbsp;• Local Outlier Factor (LOF)  
&nbsp;&nbsp;&nbsp;&nbsp;• Custom Risk Predictor  
↓  
Explainable AI (XAI) Layer  
↓  
Interactive Graph Dashboard

---

## 🔎 Detection Methodology

### 🔁 Circular Fund Routing
Detects transaction cycles of length 3–5 using Depth-First Search (DFS).  
All accounts in detected cycles are grouped into fraud rings.

### 💰 Smurfing (Fan-In / Fan-Out)
Identifies:
- Abnormal aggregation (many → one)
- Rapid dispersion (one → many)

### 🕸 Layered Shell Networks
Detects multi-hop transaction chains used to obscure the source of funds.

### ⏱ Temporal Pattern Analysis
Identifies:
- High transaction velocity
- Night-time activity
- Weekend spikes
- Rapid round-tripping behavior

---

## 🤖 Ensemble Machine Learning

RIFT combines multiple lightweight ML models:

- Isolation Forest (anomaly detection)
- Local Outlier Factor (density-based outlier detection)
- Custom Risk Predictor (feature-weight model)

Final risk score is generated using weighted ensemble learning.

Each account receives:
- Suspicion score (0–100)
- Model confidence score
- Pattern contribution breakdown

---

## 🔬 Explainable AI (XAI)

Unlike black-box systems, RIFT provides transparent explanations.

Each flagged account includes:

- Risk contribution breakdown (pattern-based %)
- Ring association details
- Model agreement confidence
- Human-readable fraud explanation
- Feature-level anomaly insights

This ensures interpretability in financial crime detection.

---

## 📊 Interactive Dashboard Features

- Graph visualization of transaction network
- Suspicious accounts highlighted (color + size scaling)
- Fraud ring isolation
- Risk-based node classification
- Real-time transaction monitor simulation
- XAI explanation panel

---

## ⚡ Performance

- Processing time: ~1–2 seconds (small datasets)
- Designed for up to 10K+ transactions
- Modular and scalable detection engines
- Parallelizable architecture

---

## 🛠 Tech Stack

### Backend
- Java
- Spring Boot
- Lombok
- Apache Commons CSV

### Algorithms
- DFS-based cycle detection
- Graph traversal algorithms
- Temporal analytics engine
- Ensemble ML scoring system

### Frontend
- HTML
- JavaScript
- Interactive graph rendering

---

## 📂 Input Format

The system accepts a CSV file with the following exact structure:

Required Columns:

- transaction_id (String)
- sender_id (String)
- receiver_id (String)
- amount (Float)
- timestamp (YYYY-MM-DD HH:MM:SS)

Example:
transaction_id,sender_id,receiver_id,amount,timestamp
TX1,A,B,1000,2026-02-19 10:00:00
TX2,B,C,2000,2026-02-19 10:05:00
TX3,C,A,1500,2026-02-19 10:10:00




