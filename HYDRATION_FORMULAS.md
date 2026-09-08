# Smart Hydration Calculation Formula

> **Canonical Source of Truth**  
> This document is the definitive single source of truth for all mathematical formulas, heuristic weights, environmental adjustments, validation constraints, and business rules implemented in the CareSync Smart Hydration Calculation Engine (`HydrationCalculationEngine`).

---

## 1. Purpose

The Smart Hydration Calculation Engine provides an **application-level wellness product heuristic** designed to encourage healthy daily fluid intake for users based on their weight, daily routine, and ambient environmental conditions.

> [!IMPORTANT]
> This calculation is **NOT** a medical prescription, clinical diagnosis, or universal medical standard. It is a software product estimation heuristic inspired by weight-based hydration estimation approaches. Individuals with medical conditions (such as chronic kidney disease, congestive heart failure, or electrolyte imbalances) or those under clinical fluid restriction must strictly follow recommendations from their qualified healthcare professional.

---

## 2. Base Requirement

The foundational daily hydration baseline is calculated directly from user body weight:

$$\text{Base Requirement (ml)} = \text{Round}\big(\text{Weight (kg)} \times 35.0\text{ ml/kg}\big)$$

* **Factor:** $35.0\text{ ml/kg}$ (defined in code as `BASE_WATER_ML_PER_KG = 35.0`).
* **Heuristic Context:** $35.0\text{ ml/kg}$ is a configurable product heuristic inspired by commonly referenced adult hydration estimation practices.
* **Demographics:** Tailored strictly for adult users ($18+$ years).
* **Adjustments Excluded:** There is **no gender adjustment** and **no age-based multiplier**.

---

## 3. Adult User Rule

Hydration calculation is supported exclusively for adult users:

* **Minimum Supported Age:** $18$ years (`MIN_ADULT_AGE = 18`). Profiles under 18 receive an HTTP 400 Bad Request error.
* **Maximum Validation Age:** $120$ years (`MAX_VALID_AGE = 120`). Profiles with age $> 120$ receive an HTTP 400 Bad Request error.

---

## 4. Activity Adjustment

Physical activity is inferred from the user's `occupation` profile field. Occupations are evaluated case-insensitively using keyword matching into four discrete activity tiers:

| Activity Level | Adjustment (ml) | Constant | Approved Occupation Keywords |
| :--- | :---: | :--- | :--- |
| **`SEDENTARY`** | **`+0 ml`** | `ACTIVITY_SEDENTARY_ML = 0` | `desk jobs`, `software engineer`, `developer`, `student`, `accountant`, `driver`, `office worker`, `manager`, `clerk`, and any **null, empty, or unrecognized occupation** (safe default). |
| **`LIGHTLY_ACTIVE`** | **`+300 ml`** | `ACTIVITY_LIGHT_ML = 300` | `teacher`, `doctor`, `nurse`, `retail`, `sales`, `chef`, `pharmacist` |
| **`MODERATELY_ACTIVE`** | **`+600 ml`** | `ACTIVITY_MODERATE_ML = 600` | `field worker`, `delivery`, `farmer`, `factory worker`, `police`, `fitness trainer` |
| **`VERY_ACTIVE`** | **`+900 ml`** | `ACTIVITY_VERY_ACTIVE_ML = 900` | `construction worker`, `athlete`, `military`, `miner`, `manual laborer` |

* **Default Behavior:** If `occupation` is `null`, blank, or matches none of the keywords (such as previously removed synonyms like `electrician`, `carpenter`, `waiter`, `bartender`, `plumber`, `mechanic`, `warehouse`, etc.), it safely defaults to `SEDENTARY` (`+0 ml`).

---

## 5. Temperature Adjustment

Ambient temperature ($T$ in $^\circ\text{C}$) elevates perspiration and fluid loss above room comfort:

| Ambient Temperature Range | Adjustment (ml) | Constant |
| :--- | :---: | :--- |
| **$T < 25.0^\circ\text{C}$** | **`+0 ml`** | Baseline room temperature |
| **$25.0^\circ\text{C} \le T < 30.0^\circ\text{C}$** | **`+250 ml`** | `TEMP_ADJUSTMENT_WARM_ML = 250` |
| **$30.0^\circ\text{C} \le T < 35.0^\circ\text{C}$** | **`+500 ml`** | `TEMP_ADJUSTMENT_HOT_ML = 500` |
| **$T \ge 35.0^\circ\text{C}$** | **`+750 ml`** | `TEMP_ADJUSTMENT_EXTREME_ML = 750` |

### Boundary Values:
* `24.99°C` $\rightarrow$ `+0 ml`
* `25.00°C` $\rightarrow$ `+250 ml`
* `29.99°C` $\rightarrow$ `+250 ml`
* `30.00°C` $\rightarrow$ `+500 ml`
* `34.99°C` $\rightarrow$ `+500 ml`
* `35.00°C` $\rightarrow$ `+750 ml`

---

## 6. Humidity Adjustment

Relative humidity ($H$ in $\%$) alters thermoregulatory sweat evaporation:

| Condition | Criteria | Adjustment (ml) | Constant |
| :--- | :--- | :---: | :--- |
| **Hot & Humid** | $T \ge 28.0^\circ\text{C}$ **AND** $H \ge 70\%$ | **`+200 ml`** | `HUMIDITY_ADJUSTMENT_HOT_HUMID_ML = 200` |
| **Dry Heat** | $T \ge 25.0^\circ\text{C}$ **AND** $H \le 30\%$ | **`+150 ml`** | `HUMIDITY_ADJUSTMENT_DRY_HEAT_ML = 150` |
| **All Other Conditions** | Neither of the above conditions met | **`+0 ml`** | Normal / Moderate conditions |

### Boundary Values:
* $T = 28.0^\circ\text{C}, H = 70\%$ $\rightarrow$ Hot & Humid (`+200 ml`)
* $T = 28.0^\circ\text{C}, H = 69\%$ $\rightarrow$ Not Hot & Humid (`+0 ml`)
* $T = 27.99^\circ\text{C}, H = 70\%$ $\rightarrow$ Not Hot & Humid (`+0 ml`)
* $T = 25.0^\circ\text{C}, H = 30\%$ $\rightarrow$ Dry Heat (`+150 ml`)
* $T = 25.0^\circ\text{C}, H = 31\%$ $\rightarrow$ Not Dry Heat (`+0 ml`)
* $T = 24.99^\circ\text{C}, H = 30\%$ $\rightarrow$ Not Dry Heat (`+0 ml`)

---

## 7. Combined Formula

The unconstrained target volume sums all additive heuristic factors:

$$\text{Raw Target (ml)} = \text{Base Requirement} + \text{Activity Adjustment} + \text{Temperature Adjustment} + \text{Humidity Adjustment}$$

---

## 8. Application Guardrails

To protect against unrealistic or extreme values, the raw target is clamped within application boundaries:

$$\text{Final Target (ml)} = \max\Big(1500,\; \min\big(4500,\; \text{Raw Target (ml)}\big)\Big)$$

* **Application Floor (`MIN_DAILY_TARGET_ML`):** `1500 ml`
* **Application Ceiling (`MAX_DAILY_TARGET_ML`):** `4500 ml`

> [!WARNING]
> The minimum of $1500\text{ ml}$ and maximum of $4500\text{ ml}$ are **application-level product boundaries** intended to ensure sensible application output. They are **NOT** universal medical safety limits.

---

## 9. Litres Conversion

The final target is converted to litres and rounded to two decimal places:

$$\text{Litres} = \frac{\text{Final Target (ml)}}{1000}$$

* **Java Implementation:**
  ```java
  BigDecimal.valueOf(clampedTargetMl)
      .divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP)
      .doubleValue();
  ```
* **Rounding Mode:** `RoundingMode.HALF_UP` to two decimal places (e.g., $2950\text{ ml} \rightarrow 2.95\text{ L}$).

---

## 10. Validation Rules

All calculation inputs are rigorously validated before formula evaluation:

| Metric | Valid Range / Rule | Error Response |
| :--- | :--- | :--- |
| **User Weight** | Mandatory, $20.0\text{ kg} \le W \le 300.0\text{ kg}$ | HTTP 400 Bad Request |
| **User Age** | $18 \le \text{Age} \le 120$ (if present) | HTTP 400 Bad Request |
| **Location Query** | Must supply `city` OR both `lat` AND `lon` | HTTP 400 Bad Request |
| **Location Precedence** | Coordinates (`lat` + `lon`) take precedence over `city` | Automatic precedence |
| **Country Fallback** | **NO country fallback is permitted** | HTTP 400 if city & coords missing |
| **City Name** | Non-blank, maximum 100 characters | HTTP 400 Bad Request |
| **Latitude** | $-90.0 \le \text{lat} \le 90.0$ | HTTP 400 Bad Request |
| **Longitude** | $-180.0 \le \text{lon} \le 180.0$ | HTTP 400 Bad Request |
| **Temperature** | $-50.0^\circ\text{C} \le T \le 60.0^\circ\text{C}$ | HTTP 400 Bad Request |
| **Humidity** | $0\% \le H \le 100\%$ | HTTP 400 Bad Request |

---

## 11. Worked Examples

### Example 1: Desk Worker in Mild Weather
* **User:** 70 kg, Software Engineer (`SEDENTARY`)
* **Weather:** $22^\circ\text{C}$, $50\%$ humidity
* **Base Requirement:** $70 \times 35.0 = 2450\text{ ml}$
* **Activity Adjustment:** $+0\text{ ml}$
* **Temperature Adjustment:** $+0\text{ ml}$ ($22^\circ\text{C} < 25^\circ\text{C}$)
* **Humidity Adjustment:** $+0\text{ ml}$
* **Raw Target:** $2450 + 0 + 0 + 0 = 2450\text{ ml}$
* **Clamping:** $2450\text{ ml} \in [1500, 4500] \rightarrow 2450\text{ ml}$
* **Litres:** $\mathbf{2.45\text{ L}}$

---

### Example 2: Desk Worker in Hot & Humid Weather
* **User:** 70 kg, Software Engineer (`SEDENTARY`)
* **Weather:** $32^\circ\text{C}$, $75\%$ humidity
* **Base Requirement:** $70 \times 35.0 = 2450\text{ ml}$
* **Activity Adjustment:** $+0\text{ ml}$
* **Temperature Adjustment:** $+500\text{ ml}$ ($30^\circ\text{C} \le 32^\circ\text{C} < 35^\circ\text{C}$)
* **Humidity Adjustment:** $+200\text{ ml}$ ($32^\circ\text{C} \ge 28^\circ\text{C}$ AND $75\% \ge 70\%$)
* **Raw Target:** $2450 + 0 + 500 + 200 = 3150\text{ ml}$
* **Clamping:** $3150\text{ ml} \in [1500, 4500] \rightarrow 3150\text{ ml}$
* **Litres:** $\mathbf{3.15\text{ L}}$

---

### Example 3: Active Worker in Dry Heat
* **User:** 70 kg, Delivery Driver (`MODERATELY_ACTIVE`)
* **Weather:** $30^\circ\text{C}$, $25\%$ humidity
* **Base Requirement:** $70 \times 35.0 = 2450\text{ ml}$
* **Activity Adjustment:** $+600\text{ ml}$
* **Temperature Adjustment:** $+500\text{ ml}$ ($30^\circ\text{C} \le 30^\circ\text{C} < 35^\circ\text{C}$)
* **Humidity Adjustment:** $+150\text{ ml}$ ($30^\circ\text{C} \ge 25^\circ\text{C}$ AND $25\% \le 30\%$)
* **Raw Target:** $2450 + 600 + 500 + 150 = 3700\text{ ml}$
* **Clamping:** $3700\text{ ml} \in [1500, 4500] \rightarrow 3700\text{ ml}$
* **Litres:** $\mathbf{3.70\text{ L}}$

---

### Example 4: Lower Boundary Clamping
* **User:** 35 kg, Student (`SEDENTARY`)
* **Weather:** $18^\circ\text{C}$, $40\%$ humidity
* **Base Requirement:** $35 \times 35.0 = 1225\text{ ml}$
* **Adjustments:** Activity $+0\text{ ml}$, Temp $+0\text{ ml}$, Humidity $+0\text{ ml}$
* **Raw Target:** $1225\text{ ml}$
* **Clamping:** $\max(1500, 1225) = \mathbf{1500\text{ ml}}$
* **Litres:** $\mathbf{1.50\text{ L}}$ *(clamped to product lower bound)*

---

### Example 5: Upper Boundary Clamping
* **User:** 110 kg, Construction Worker (`VERY_ACTIVE`)
* **Weather:** $38^\circ\text{C}$, $75\%$ humidity
* **Base Requirement:** $110 \times 35.0 = 3850\text{ ml}$
* **Activity Adjustment:** $+900\text{ ml}$
* **Temperature Adjustment:** $+750\text{ ml}$ ($38^\circ\text{C} \ge 35^\circ\text{C}$)
* **Humidity Adjustment:** $+200\text{ ml}$ ($38^\circ\text{C} \ge 28^\circ\text{C}$ AND $75\% \ge 70\%$)
* **Raw Target:** $3850 + 900 + 750 + 200 = 5700\text{ ml}$
* **Clamping:** $\min(4500, 5700) = \mathbf{4500\text{ ml}}$
* **Litres:** $\mathbf{4.50\text{ L}}$ *(clamped to product upper bound)*

---

### Example 6: Temperature Threshold Boundaries
* $T = 24.99^\circ\text{C} \rightarrow +0\text{ ml}$
* $T = 25.00^\circ\text{C} \rightarrow +250\text{ ml}$
* $T = 29.99^\circ\text{C} \rightarrow +250\text{ ml}$
* $T = 30.00^\circ\text{C} \rightarrow +500\text{ ml}$
* $T = 34.99^\circ\text{C} \rightarrow +500\text{ ml}$
* $T = 35.00^\circ\text{C} \rightarrow +750\text{ ml}$

---

### Example 7: Humidity Threshold Boundaries
* $T = 28.0^\circ\text{C}, H = 70\% \rightarrow \mathbf{+200\text{ ml}}$ (Hot & Humid active)
* $T = 28.0^\circ\text{C}, H = 69\% \rightarrow \mathbf{+0\text{ ml}}$ (Below high humidity threshold)
* $T = 27.99^\circ\text{C}, H = 70\% \rightarrow \mathbf{+0\text{ ml}}$ (Below temperature threshold for humid heat)
* $T = 25.0^\circ\text{C}, H = 30\% \rightarrow \mathbf{+150\text{ ml}}$ (Dry Heat active)
* $T = 25.0^\circ\text{C}, H = 31\% \rightarrow \mathbf{+0\text{ ml}}$ (Above low humidity threshold)
* $T = 24.99^\circ\text{C}, H = 30\% \rightarrow \mathbf{+0\text{ ml}}$ (Below temperature threshold for dry heat)
