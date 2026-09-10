# CareSync Healthcare Dashboard API Guide 📊

> **NOTICE / DISCLAIMER:**  
> The CareSync Healthcare Dashboard is an application-level **wellness and hydration guidance system**. The recommended water targets, remaining intake values, and environmental alerts are software product heuristics and **NOT** clinical medical prescriptions, healthcare diagnoses, or universal medical advice.

---

## 1. Overview

The Healthcare Dashboard API (`GET /api/v1/dashboard`) serves as the central orchestration and composition endpoint for CareSync. It aggregates:
1. **User Profile**: Authenticated user identity and name.
2. **Current Ambient Weather**: Live temperature, humidity, and weather condition from OpenWeatherMap.
3. **Smart Hydration Target**: Real-time, weather-adjusted daily water intake target.
4. **Today's Water Tracking**: Aggregated water consumed today, remaining water to reach the target, and uncapped progress percentage.
5. **Contextual Alerts**: Deterministic environmental and hydration reminders.

---

## 2. Architecture & Data Flow

The Dashboard is a **pure composition/read layer**. It creates no new database tables and duplicates no business logic.

```text
HTTP Client (Web / Mobile)
       │
       │ GET /api/v1/dashboard?city=... | lat=...&lon=...
       │ Header: Authorization: Bearer <JWT>
       ▼
DashboardController
       │ Extracts authenticated email from JWT principal
       ▼
DashboardServiceImpl
       │
       ├── 1. UserRepository: Loads authenticated User profile
       │
       ├── 2. HydrationService: Obtains live weather & daily hydration target
       │      (Orchestrates WeatherService & HydrationCalculationEngine)
       │
       ├── 3. WaterIntakeRepository: Queries today's water consumption [startOfDay, endOfDay)
       │
       └── 4. DashboardService: Computes remaining water, progress percentage, & alerts
              │
              ▼
       DashboardResponse (Enveloped in ApiResponse<DashboardResponse>)
```

---

## 3. REST API Specification

### Endpoint
```http
GET /api/v1/dashboard
```

### Security
- **Authentication**: JWT Bearer token required in the `Authorization` header.
- **Authorization**: The authenticated JWT user determines the dashboard owner. No `userId` request parameter is accepted or trusted.

### Query Parameters
| Parameter | Type | Example | Description |
| :--- | :--- | :--- | :--- |
| `city` | String | `Bengaluru` | City name for current weather lookup |
| `lat` | Double | `12.9716` | Latitude coordinate ($-90$ to $+90$) |
| `lon` | Double | `77.5946` | Longitude coordinate ($-180$ to $+180$) |

### Location Rules & Precedence
- **Location Requirement**: The dashboard requires location context. Either `city` OR (`lat` + `lon`) must be provided.
- **Precedence**: If both `city` and coordinates (`lat` + `lon`) are provided, **coordinates take precedence**.
- **Incomplete Coordinates**: Providing `lat` without `lon` (or vice versa) returns **HTTP 400 Bad Request**.
- **Missing Location**: Omitting all location parameters returns **HTTP 400 Bad Request**.

---

## 4. Response Structure

### Sample Success Response (`200 OK`)
```json
{
  "success": true,
  "message": "Dashboard retrieved successfully",
  "data": {
    "user": {
      "name": "Pavan Kalyan"
    },
    "weather": {
      "location": "Bengaluru",
      "temperature": 32.0,
      "humidity": 65,
      "weatherCondition": "Clear"
    },
    "hydration": {
      "dailyTargetMl": 2950,
      "dailyTargetLitres": 2.95,
      "consumedMl": 1500,
      "remainingMl": 1450,
      "progressPercentage": 50.85,
      "isWeatherAdjusted": true
    },
    "alerts": [
      {
        "type": "WARM_TEMPERATURE",
        "severity": "INFO",
        "message": "Warm ambient temperature of 32.0°C observed. Target adjusted to compensate for heat."
      }
    ],
    "calculatedAt": "2026-09-10T14:30:00"
  },
  "timestamp": "2026-09-10T14:30:00.123456"
}
```

---

## 5. Calculations & Rules

### 1. Daily Hydration Target Reuse
The Dashboard directly reuses the approved Smart Hydration calculation via `HydrationService`:
- $\text{Base Requirement} = \text{Weight} \times 35.0\text{ ml/kg}$ (for adults $18+$).
- Activity adjustment classified from user occupation (`SEDENTARY` $+0$, `LIGHTLY_ACTIVE` $+300$, `MODERATELY_ACTIVE` $+600$, `VERY_ACTIVE` $+900$).
- Ambient temperature adjustment ($<25^\circ\text{C}$ $+0$, $25-30^\circ\text{C}$ $+250$, $30-35^\circ\text{C}$ $+500$, $\ge 35^\circ\text{C}$ $+750$).
- Ambient humidity adjustment (Hot & Humid $+200$, Dry Heat $+150$, Other $+0$).
- Application product boundaries: Clamped between $[1500\text{ ml}, 4500\text{ ml}]$.

### 2. Today's Consumed Water
- Defines today's calendar window using an injectable `Clock`: $[00:00:00.000, 23:59:59.999]$.
- Sums all water consumption records logged by the authenticated user today:
  $$\text{consumedMl} = \sum \text{amountMl}$$
- If no records are found, `consumedMl: 0`.

### 3. Remaining Water
$$\text{remainingMl} = \max(\text{dailyTargetMl} - \text{consumedMl},\; 0)$$
- **Non-Negative Guarantee**: Remaining water can never be negative.
- **Example**: Target $2500\text{ ml}$, Consumed $3000\text{ ml} \rightarrow \text{remainingMl} = 0$.

### 4. Progress Percentage
$$\text{progressPercentage} = \frac{\text{consumedMl}}{\text{dailyTargetMl}} \times 100$$
- Scaled to 2 decimal places using `RoundingMode.HALF_UP`.
- **Uncapped**: Progress can exceed 100.0% when user surpasses their target (e.g., $3000\text{ ml} / 2500\text{ ml} \rightarrow 120.00\%$).
- If `consumedMl == 0` or `dailyTargetMl <= 0`, returns `0.0`.

---

## 6. Deterministic Alert Rules

Alerts provide transparent, non-clinical feedback based on deterministic criteria:

| Alert Type | Trigger Condition | Severity | Description / Message |
| :--- | :--- | :---: | :--- |
| **`HIGH_TEMPERATURE`** | Temperature $\ge 35.0^\circ\text{C}$ | `WARNING` | High ambient temperature of X°C observed. Perspiration rate increases fluid loss; remember to drink water regularly. |
| **`WARM_TEMPERATURE`** | $30.0^\circ\text{C} \le \text{Temperature} < 35.0^\circ\text{C}$ | `INFO` | Warm ambient temperature of X°C observed. Target adjusted to compensate for heat. |
| **`HIGH_HUMIDITY`** | Temperature $\ge 28.0^\circ\text{C}$ **AND** Humidity $\ge 70\%$ | `WARNING` | High humidity (X%) combined with warm temperature restricts sweat evaporation. Extra fluid intake recommended. |
| **`HYDRATION_GOAL_REACHED`**| Consumed $\ge$ Target | `INFO` | Daily hydration goal achieved! Great job staying hydrated today. |
| **`HYDRATION_REMINDER`** | Consumed $== 0$ | `INFO` | You have not logged any water yet today. Remember to stay hydrated. |
| **`WEATHER_UNAVAILABLE`** | External weather provider failure | `INFO` | Current weather is unavailable. Showing your baseline hydration target. |

---

## 7. Weather Failure Fallback & `isWeatherAdjusted`

If the external weather provider (OpenWeatherMap) experiences an outage, network timeout, or rate-limiting:
1. **The Dashboard does not fail**: Core CareSync metrics (user greeting, water consumed, remaining target, progress) remain fully available.
2. **Fallback Target**: Populates the hydration target with the user's authentic **Profile Baseline Target** ($\text{Weight} \times 35.0\text{ ml/kg} + \text{Activity Adjustment}$, clamped between $1500\text{ ml}$ and $4500\text{ ml}$).
3. **`isWeatherAdjusted`**: Set to `false` (in normal operation with live weather, set to `true`).
4. **Weather Section**: Set to `null`.
5. **Alert**: An informational alert of type `WEATHER_UNAVAILABLE` is added to inform the user that the target reflects baseline requirements.

---

## 8. HTTP Status Codes

| Status Code | Reason |
| :--- | :--- |
| **`200 OK`** | Dashboard successfully retrieved. |
| **`400 BAD REQUEST`** | Missing location, incomplete coordinates, incomplete profile (missing weight), or underage user ($< 18$). |
| **`401 UNAUTHORIZED`** | Missing, invalid, or expired JWT Bearer token. |
| **`404 NOT FOUND`** | Authenticated user profile not found or inactive in database. |
