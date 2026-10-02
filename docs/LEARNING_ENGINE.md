# LENTERA 2.0 — ADAPTIVE LEARNING & MASTERY MODEL

## 1. Mastery Score Calculation (0.0 to 1.0)
For each concept $c$, LENTERA calculates a weighted mastery score $M_c$:

$$M_c = w_{acc} \cdot \left(\frac{\text{correct\_attempts}}{\text{total\_attempts}}\right) + w_{rec} \cdot e^{-\lambda \cdot \Delta t} + w_{diff} \cdot D_{avg}$$

Where:
* $w_{acc} = 0.5$ (Accuracy weight)
* $w_{rec} = 0.3$ (Recency decay weight, half-life = 7 days)
* $w_{diff} = 0.2$ (Difficulty scaling weight)

## 2. Adaptive Learning Loop
1. **Quiz Evaluation**: Answer evaluation (MCQs, True/False, Fill in blanks, and Numerical tolerance-based checking).
2. **Weak Concept Identification**: Concepts with mastery $M_c < 0.60$ are flagged as weak.
3. **Remedial Recommendations**: Generates a targeted 5-minute remedial lesson focusing specifically on identified misconceptions.
4. **Re-Testing & Mastery Update**: After completing remedial practice, mastery score is updated in Room DB and reflected on the Progress screen.
