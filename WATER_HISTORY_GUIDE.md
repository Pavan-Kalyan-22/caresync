# CareSync Water History & Intake Guide 💧

> **NOTICE / DISCLAIMER:**  
> The CareSync Water History and Hydration system is an application-level **wellness product tool** designed to help users track their daily fluid consumption against estimated targets. It is **NOT** a medical prescription, clinical diagnostic tool, or clinical fluid management system.

---

## 1. Overview

The Water History module enables authenticated CareSync users to:
1. **Log water consumption**: Record individual drinking events in milliliters.
2. **Review 7-day hydration history**: Inspect their water consumption, daily targets, and progress percentages across the current day and the previous 6 calendar days.

---

## 2. Architecture & Data Flow

```text
HTTP Client (Web / Mobile)
   │
   ├── POST /api/v1/water/log (Amount in ml)
   │     │
   │     ▼
   │   WaterController
   │     │ Extracts authenticated user email from JWT
   │     ▼
   │   WaterServiceImpl
   │     │ Validates input (1 - 5000 ml)
   │     │ Persists WaterIntake entity in MySQL
   │     ▼
   │   WaterIntakeRepository (table: water_intakes)
   │
   └── GET /api/v1/water/history?city=... | lat=...&lon=...
         │
         ▼
       WaterController
         │ Extracts authenticated user email from JWT
         ▼
       WaterServiceImpl
         │ 1. Loads user records for [today - 6 days, today + 1 day)
         │ 2. Aggregates consumed ml by calendar day
         │ 3. Obtains today's target (weather-adjusted if location supplied, else baseline)
         │ 4. Computes historical days' targets using user profile baseline
         │ 5. Computes uncapped progress percentage: (consumed / target) * 100
         ▼
       WaterHistoryResponse (Exactly 7 days, newest day first)
```

---

## 3. Database & Entity Design

### Table: `water_intakes`

```sql
CREATE TABLE water_intakes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    amount_ml INT NOT NULL,
    logged_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_water_intakes_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_water_intakes_user_logged_at (user_id, logged_at)
);
```

### Key Design Decisions:
- **Unidirectional Relationship**: `WaterIntake` references `User` via `@ManyToOne`. The `User` entity is not modified.
- **No Stored Daily Target**: Daily targets are derived dynamically from the Smart Hydration rules rather than redundantly stored in every water intake record.
- **Audit Logging**: `loggedAt` records the consumption timestamp; `createdAt` tracks the DB persistence instant.

---

## 4. Daily Target & Historical Limitation

### Relationship with Smart Hydration:
The daily hydration target reuses the pure business logic and centralized constants in `HydrationCalculationEngine`:

$$\text{Base Target} = \text{Clamp}\Big(\text{Weight (kg)} \times 35.0\text{ ml/kg} + \text{Activity Adjustment},\; 1500\text{ ml},\; 4500\text{ ml}\Big)$$

### Today's Target vs. Previous 6 Days' Target:
1. **Today (`date == today`)**:
   - If location parameters (`city` or `lat`+`lon`) are supplied, the target dynamically incorporates live ambient temperature and humidity via `HydrationService`.
   - If location parameters are omitted (or external weather is unavailable), today uses the user's profile baseline target.
2. **Previous 6 Days (`date < today`)**:
   - **Architectural Notice & Limitation**: Historical weather data is not stored in CareSync or retrievable from the free external weather tier.
   - To prevent fabricating past weather conditions or falsely applying today's weather backward in time, previous days calculate the user's authentic **Profile Baseline Target** ($\text{Weight} \times 35.0\text{ ml/kg} + \text{Activity Adjustment}$, clamped between $1500\text{ ml}$ and $4500\text{ ml}$).

---

## 5. Aggregation & Progress Calculation

### Calendar Day Definition:
- A calendar day spans from `00:00:00.000` to `23:59:59.999`.
- Queries filter via a half-open interval: `[today.minusDays(6).atStartOfDay(), today.plusDays(1).atStartOfDay())`.
- Grouping is performed by `record.getLoggedAt().toLocalDate()`.

### Progress Percentage:
$$\text{Progress Percentage} = \frac{\text{Consumed (ml)}}{\text{Daily Target (ml)}} \times 100$$

- **Uncapped**: If consumed water exceeds the daily target, the progress percentage correctly exceeds 100% (e.g., $3000\text{ ml} / 2500\text{ ml} \times 100 = 120.0\%$).
- **Precision**: Rounded to 2 decimal places using `RoundingMode.HALF_UP`.
- **Zero Days**: If no water was logged on a calendar day, `consumedMl: 0` and `progressPercentage: 0.0`. The day is still included in the 7-day output.

---

## 6. REST API Endpoints

### 1. Log Water Intake
`POST /api/v1/water/log`

#### Headers:
| Header | Value | Required |
| :--- | :--- | :---: |
| `Authorization` | `Bearer <JWT_ACCESS_TOKEN>` | Yes |
| `Content-Type` | `application/json` | Yes |

#### Request Body:
```json
{
  "amountMl": 500
}
```

#### Validation:
- `amountMl`: Required, integer between `1` and `5000` ml.

#### Response (`201 Created`):
```json
{
  "success": true,
  "message": "Water intake logged successfully",
  "data": {
    "id": 1,
    "amountMl": 500,
    "loggedAt": "2026-09-09T14:30:00"
  },
  "timestamp": "2026-09-09T14:30:00.123456"
}
```

---

### 2. Get 7-Day Water History
`GET /api/v1/water/history`

#### Headers:
| Header | Value | Required |
| :--- | :--- | :---: |
| `Authorization` | `Bearer <JWT_ACCESS_TOKEN>` | Yes |

#### Query Parameters:
| Parameter | Type | Example | Description |
| :--- | :--- | :--- | :--- |
| `city` | String | `Bengaluru` | Optional city name for today's weather adjustment |
| `lat` | Double | `12.9716` | Optional latitude coordinate for today's weather |
| `lon` | Double | `77.5946` | Optional longitude coordinate for today's weather |

#### Response (`200 OK`):
```json
{
  "success": true,
  "message": "Water history retrieved successfully",
  "data": {
    "days": [
      {
        "date": "2026-09-09",
        "dailyTargetMl": 2950,
        "consumedMl": 2100,
        "progressPercentage": 71.19
      },
      {
        "date": "2026-09-08",
        "dailyTargetMl": 2450,
        "consumedMl": 1800,
        "progressPercentage": 73.47
      },
      {
        "date": "2026-09-07",
        "dailyTargetMl": 2450,
        "consumedMl": 0,
        "progressPercentage": 0.0
      },
      {
        "date": "2026-09-06",
        "dailyTargetMl": 2450,
        "consumedMl": 2700,
        "progressPercentage": 110.2
      },
      {
        "date": "2026-09-05",
        "dailyTargetMl": 2450,
        "consumedMl": 0,
        "progressPercentage": 0.0
      },
      {
        "date": "2026-09-04",
        "dailyTargetMl": 2450,
        "consumedMl": 1500,
        "progressPercentage": 61.22
      },
      {
        "date": "2026-09-03",
        "dailyTargetMl": 2450,
        "consumedMl": 0,
        "progressPercentage": 0.0
      }
    ]
  },
  "timestamp": "2026-09-09T20:25:00.123456"
}
```

---

## 7. Security & User Isolation

- **Authentication**: Strict JWT authentication. Unauthenticated requests receive HTTP 401 Unauthorized via `JwtAuthenticationEntryPoint`.
- **User Isolation**: All queries filter strictly by the authenticated `User` (`findByUserAndLoggedAtBetweenOrderByLoggedAtDesc`).
- **Zero Authorization Leaks**: No `userId` parameter is accepted from the client. User A can never inspect or log records for User B.
