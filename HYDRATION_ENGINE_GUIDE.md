# CareSync Smart Hydration Engine Guide 💧

> **NOTICE / DISCLAIMER:**  
> The CareSync Smart Hydration Calculation Engine is an application-level **wellness product heuristic** designed to encourage healthy daily fluid intake. It is **NOT** a medical prescription, clinical diagnostic tool, or universal medical standard. Individuals with medical conditions (such as kidney disease, congestive heart failure, or electrolyte disorders) must follow advice from their qualified medical professional.

> [!NOTE]
> For the canonical single source of truth regarding mathematical formulas, activity classifications, environmental adjustments, and boundary rules, refer to [`HYDRATION_FORMULAS.md`](file:///c:/Users/pavan/OneDrive/Desktop/HealthCare/HYDRATION_FORMULAS.md).

---

## 1. Overview

The Smart Hydration Engine calculates a personalized daily fluid intake target (in milliliters and litres) for an authenticated user by synthesizing:
1. **User Profile Biometrics:** Body weight (in kg) and occupational activity level.
2. **Real-time Environmental Context:** Local ambient temperature (°C) and relative humidity (%).

The calculation is deterministic, transparent, testable, and isolated in a dedicated domain component (`HydrationCalculationEngine`).

---

## 2. Architecture & Data Flow

```text
HTTP Client (Web / Mobile)
   │
   │ GET /api/v1/hydration/recommendation?city=... | lat=...&lon=...
   │ Header: Authorization: Bearer <JWT>
   ▼
HydrationController
   │ Extracts authenticated user's email from JWT Authentication principal
   │ Validates presence of city OR (lat + lon) query parameters
   ▼
HydrationServiceImpl
   │ 1. Queries UserRepository for authenticated User entity
   │ 2. Validates user profile (weight required, age >= 18)
   │ 3. Invokes WeatherService.getCurrentWeather(...) for live weather
   │ 4. Passes User + WeatherResponse to HydrationCalculationEngine
   ▼
HydrationCalculationEngine
   │ Pure, deterministic domain component
   │ Computes: Base (35 ml/kg) + Activity + Temperature + Humidity
   │ Enforces application guardrails: [1500 ml, 4500 ml]
   ▼
HydrationResponse
   │ Returned in standard CareSync ApiResponse<HydrationResponse> (HTTP 200)
```

---

## 3. Calculation Formula & Rules

### Core Formula

$$\text{Daily Water Target (ml)} = \text{Clamp}\Big(\text{Base Requirement} + \text{Activity Adj} + \text{Temp Adj} + \text{Humidity Adj},\; \text{Min}=1500,\; \text{Max}=4500\Big)$$

$$\text{Daily Water Target (Litres)} = \frac{\text{Daily Water Target (ml)}}{1000.0}$$

---

### Step 1: Base Water Requirement (Body Weight Heuristic)
* **Rule:** $35.0\text{ ml}$ per kilogram of body weight.
* **Constant:** `BASE_WATER_ML_PER_KG = 35.0`
* **Formula:** $\text{Base Requirement (ml)} = \text{Round}(\text{weight in kg} \times 35.0)$.
* *Audience:* Tailored for adult users ($18+$ years). No age-based multipliers or gender adjustments are used.

---

### Step 2: Activity / Occupation Adjustment Heuristic
Because the `User` profile contains `occupation` but no explicit physical activity field, occupations are classified into four activity tiers using keyword pattern matching:

| Activity Tier | Approved Occupations | Adjustment (ml) |
| :--- | :--- | :---: |
| **`SEDENTARY`** | Desk jobs, Software Engineer, Developer, Student, Accountant, Driver, Office Worker, Manager, Clerk, or null/unrecognized | **`+0 ml`** |
| **`LIGHTLY_ACTIVE`** | Teacher, Doctor, Nurse, Retail, Sales, Chef, Pharmacist | **`+300 ml`** |
| **`MODERATELY_ACTIVE`** | Field worker, Delivery, Farmer, Factory Worker, Police, Fitness Trainer | **`+600 ml`** |
| **`VERY_ACTIVE`** | Construction worker, Athlete, Military, Miner, Manual Laborer | **`+900 ml`** |

---

### Step 3: Ambient Temperature Adjustment Heuristic
Increased ambient heat elevates perspiration and insensible fluid loss above room comfort temperatures:

| Temperature Range | Description | Adjustment (ml) |
| :--- | :--- | :---: |
| **$T < 25^\circ\text{C}$** | Normal / Temperate room temperature | **`+0 ml`** |
| **$25^\circ\text{C} \le T < 30^\circ\text{C}$** | Warm | **`+250 ml`** |
| **$30^\circ\text{C} \le T < 35^\circ\text{C}$** | Hot | **`+500 ml`** |
| **$T \ge 35^\circ\text{C}$** | Very Hot / Extreme | **`+750 ml`** |

---

### Step 4: Ambient Humidity Adjustment Heuristic
* **Hot & Humid ($T \ge 28^\circ\text{C}$ and $\text{Humidity} \ge 70\%$):** High humidity impedes evaporative cooling, triggering heavier sweat production $\rightarrow \mathbf{+200\text{ ml}}$.
* **Dry Heat ($T \ge 25^\circ\text{C}$ and $\text{Humidity} \le 30\%$):** Rapid sweat evaporation accelerates unnoticed insensible dehydration $\rightarrow \mathbf{+150\text{ ml}}$.
* **Normal / Moderate conditions:** **`+0 ml`**.

---

### Step 5: Application Guardrails (Clamping Limits)
Application-level boundaries designed to avoid unrealistic or extreme recommendations:
* **Application Floor (`MIN_DAILY_TARGET_ML`):** `1500 ml` (1.5 Litres).
* **Application Ceiling (`MAX_DAILY_TARGET_ML`):** `4500 ml` (4.5 Litres).
* $\text{Final Target} = \max(1500, \min(4500, \text{Raw Target}))$.

---

## 4. Example Calculations

### Example A: Desk Worker in Moderate Climate
* **User:** 70 kg, Software Engineer (Sedentary)
* **Weather:** 22°C, 50% humidity
* **Base:** $70 \times 35.0 = 2450\text{ ml}$
* **Activity:** $+0\text{ ml}$
* **Temperature:** $+0\text{ ml}$
* **Humidity:** $+0\text{ ml}$
* **Total:** $2450\text{ ml}$ ($2.45\text{ Litres}$)

### Example B: Teacher in Hot Climate
* **User:** 60 kg, Teacher (Lightly Active)
* **Weather:** 32°C, 55% humidity
* **Base:** $60 \times 35.0 = 2100\text{ ml}$
* **Activity:** $+300\text{ ml}$
* **Temperature:** $+500\text{ ml}$
* **Humidity:** $+0\text{ ml}$
* **Total:** $2100 + 300 + 500 = 2900\text{ ml}$ ($2.90\text{ Litres}$)

### Example C: Field Worker in Hot & Humid Climate
* **User:** 80 kg, Delivery Driver (Moderately Active)
* **Weather:** 33°C, 75% humidity
* **Base:** $80 \times 35.0 = 2800\text{ ml}$
* **Activity:** $+600\text{ ml}$
* **Temperature:** $+500\text{ ml}$
* **Humidity:** $+200\text{ ml}$
* **Total:** $2800 + 600 + 500 + 200 = 4100\text{ ml}$ ($4.10\text{ Litres}$)

### Example D: Extreme Case Guardrail Clamping
* **User:** 110 kg, Construction Worker (Very Active)
* **Weather:** 38°C, 80% humidity
* **Raw Calculation:** Base $3850$ + Activity $900$ + Temp $750$ + Humidity $200$ = $5700\text{ ml}$
* **Clamped Result:** Clamped to application ceiling of **$4500\text{ ml}$ ($4.50\text{ Litres}$)**.

---

## 5. REST API Documentation

### Endpoint
```http
GET /api/v1/hydration/recommendation
```

### Headers
| Header | Type | Value | Required |
| :--- | :--- | :--- | :---: |
| `Authorization` | String | `Bearer <JWT_ACCESS_TOKEN>` | Yes |

### Query Parameters
| Parameter | Type | Example | Description |
| :--- | :--- | :--- | :--- |
| `city` | String | `Bengaluru` | City name for weather lookup. |
| `lat` | Double | `12.9716` | Latitude coordinate ($-90$ to $+90$). |
| `lon` | Double | `77.5946` | Longitude coordinate ($-180$ to $+180$). |

*Note: Either `city` OR (`lat` AND `lon`) must be provided. Country is not used as a fallback.*

### HTTP Status Codes
* **`200 OK`**: Calculation successful.
* **`400 BAD REQUEST`**: Missing location, missing user weight, underage user ($< 18$), or invalid inputs.
* **`401 UNAUTHORIZED`**: Missing, expired, or invalid JWT.
* **`404 NOT FOUND`**: User or location not found.
* **`503 SERVICE UNAVAILABLE`**: External weather provider unavailable.

### Sample Success Response
```json
{
  "success": true,
  "message": "Hydration recommendation calculated successfully",
  "data": {
    "dailyWaterTargetMl": 2950,
    "dailyWaterTargetLitres": 2.95,
    "baseRequirementMl": 2450,
    "activityAdjustmentMl": 0,
    "temperatureAdjustmentMl": 500,
    "humidityAdjustmentMl": 0,
    "weightKg": 70.0,
    "occupation": "Software Engineer",
    "activityLevel": "SEDENTARY",
    "temperatureCelsius": 31.5,
    "humidityPercent": 60,
    "weatherCondition": "Clouds",
    "location": "Bengaluru",
    "explanation": "Base requirement of 2450 ml (35 ml/kg), elevated ambient temperature of 31.5°C (+500 ml). Recommendation is an application wellness guide.",
    "calculatedAt": "2026-09-08T20:45:00"
  },
  "timestamp": "2026-09-08T20:45:00.123456"
}
```
