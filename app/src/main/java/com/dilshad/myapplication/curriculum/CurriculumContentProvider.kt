package com.dilshad.myapplication.curriculum

import com.dilshad.myapplication.content.CachedQuizEntity
import com.dilshad.myapplication.content.CachedQuizQuestionEntity
import com.dilshad.myapplication.content.ContentChunkEntity
import com.dilshad.myapplication.content.ContentPackEntity
import com.dilshad.myapplication.domain.rag.CurriculumCorpus
import com.google.gson.Gson
import java.util.UUID

data class ChapterContent(
    val chapterNumber: String,
    val title: String,
    val subject: String,
    val classLevel: Int,
    val readTimeMinutes: Int = 12,
    val overview: String,
    val keyFormulas: List<Pair<String, String>>,
    val keyConcepts: List<Pair<String, String>>,
    val realWorldExamples: List<String>,
    val hindiSummary: String,
    val odiaSummary: String,
    val boardQuestions: List<Triple<String, String, String>> // Question, Answer, Explanation
)

object CurriculumContentProvider {
    private val gson = Gson()

    val chapters: List<ChapterContent> = listOf(
        // ==================== CLASS 10 SCIENCE ====================
        ChapterContent(
            chapterNumber = "1",
            title = "Chemical Reactions and Equations",
            subject = "Science",
            classLevel = 10,
            readTimeMinutes = 15,
            overview = """
                A chemical reaction is a process where one or more substances (reactants) transform into new substances (products) with entirely different chemical properties. During a reaction, chemical bonds between atoms are broken and new bonds are formed.
                
                Evidence of a chemical reaction includes change in state, change in colour, evolution of a gas, or change in temperature.
                
                The Law of Conservation of Mass states that mass can neither be created nor destroyed in a chemical reaction. Therefore, the total number of atoms of each element on the reactant side must equal the total number of atoms on the product side. Balancing chemical equations is achieved using the hit-and-trial method by adjusting stoichiometric coefficients.
                
                Major Types of Reactions:
                1. Combination Reaction: Two or more reactants combine to form a single product. Example: Burning of coal (C + O₂ → CO₂) or formation of slaked lime (CaO + H₂O → Ca(OH)₂ + heat).
                2. Decomposition Reaction: A single compound breaks down into two or more simpler substances. It requires energy in the form of heat (thermal: CaCO₃ → CaO + CO₂), electricity (electrolytic: 2H₂O → 2H₂ + O₂), or light (photochemical: 2AgCl → 2Ag + Cl₂).
                3. Displacement Reaction: A more reactive element displaces a less reactive element from its aqueous salt solution. Example: Fe(s) + CuSO₄(aq, blue) → FeSO₄(aq, pale green) + Cu(s).
                4. Double Displacement Reaction: Mutual exchange of ions between two reactants, often producing an insoluble precipitate. Example: Na₂SO₄(aq) + BaCl₂(aq) → BaSO₄(s, white precipitate) + 2NaCl(aq).
                5. Redox Reaction: Oxidation (gain of oxygen / loss of electrons) and Reduction (loss of oxygen / gain of electrons) occur simultaneously. Example: CuO + H₂ → Cu + H₂O (CuO is reduced to Cu, H₂ is oxidized to H₂O).
                
                Corrosion: Degradation of metals due to atmospheric oxygen and moisture (e.g. rusting of iron forming hydrated ferric oxide Fe₂O₃·xH₂O). Prevented by galvanisation, painting, and alloying.
                Rancidity: Oxidation of fats and oils in food resulting in unpleasant taste and odour. Prevented by flush packaging with nitrogen gas, refrigeration, and antioxidants.
            """.trimIndent(),
            keyFormulas = listOf(
                "Combination" to "CaO + H₂O → Ca(OH)₂ + Heat (Slaked Lime)",
                "Thermal Decomposition" to "CaCO₃(s) + Heat → CaO(s) + CO₂(g)",
                "Displacement" to "Fe(s) + CuSO₄(aq) → FeSO₄(aq) + Cu(s)",
                "Double Displacement" to "Na₂SO₄(aq) + BaCl₂(aq) → BaSO₄(s)↓ + 2NaCl(aq)",
                "Redox Oxidation" to "Gain of Oxygen / Loss of Hydrogen / Loss of electrons",
                "Redox Reduction" to "Loss of Oxygen / Gain of Hydrogen / Gain of electrons"
            ),
            keyConcepts = listOf(
                "Balanced Chemical Equation" to "Number of atoms of each element remains identical on reactant and product sides.",
                "Exothermic vs Endothermic" to "Exothermic releases heat (respiration, combustion). Endothermic absorbs heat (photosynthesis, thermal decomposition).",
                "Precipitation" to "Formation of an insoluble solid when two ionic solutions are combined.",
                "Corrosion Prevention" to "Galvanisation (zinc coating), chrome plating, oiling, and painting."
            ),
            realWorldExamples = listOf(
                "Whitewashing walls with calcium hydroxide which reacts with air CO₂ over 2-3 days to form shiny CaCO₃.",
                "Black-and-white photography utilizing silver bromide decomposition under sunlight (2AgBr → 2Ag + Br₂).",
                "Chips bags puffed with unreactive nitrogen gas to prevent rancidity."
            ),
            hindiSummary = "रासायनिक अभिक्रियाएं और समीकरण: जब दो या दो से अधिक पदार्थ मिलकर नए गुणधर्म वाले पदार्थ बनाते हैं, तो इसे रासायनिक अभिक्रिया कहते हैं। द्रव्यमान संरक्षण के नियम के कारण समीकरण को संतुलित किया जाता है। मुख्य प्रकार: संयोजन, वियोजन, विस्थापन, द्विविस्थापन और रेडॉक्स।",
            odiaSummary = "ରାସାୟନିକ ପ୍ରତିକ୍ରିୟା ଏବଂ ସମୀକରଣ: ପ୍ରତିକ୍ରିୟାରେ ଅଣୁଗୁଡ଼ିକ ମଧ୍ୟରେ ବନ୍ଧ ଭାଙ୍ଗି ନୂଆ ବନ୍ଧ ସୃଷ୍ଟି ହୁଏ। ବସ୍ତୁତ୍ୱ ସଂରକ୍ଷଣ ନିୟମ ଅନୁସାରେ ସମୀକରଣ ସନ୍ତୁଳିତ କରାଯାଏ। ମୁଖ୍ୟ ପ୍ରକାର: ସଂଶ୍ଳେଷଣ, ବିଘଟନ, ବିସ୍ଥାପନ, ଦ୍ୱି-ବିସ୍ଥାପନ ଓ ରେଡକ୍ସ।",
            boardQuestions = listOf(
                Triple(
                    "Why should a magnesium ribbon be cleaned before burning in air?",
                    "Magnesium ribbon is cleaned with sandpaper before burning to remove the protective layer of basic magnesium carbonate/oxide formed by reaction with moist air, allowing clean contact with oxygen.",
                    "Magnesium is a reactive metal that slowly reacts with atmospheric oxygen to form a thin non-reactive oxide crust."
                ),
                Triple(
                    "Why is respiration considered an exothermic reaction?",
                    "During digestion, food carbohydrates break down into glucose. In cell respiration, glucose combines with oxygen (C₆H₁₂O₆ + 6O₂ → 6CO₂ + 6H₂O + Energy) releasing ATP energy.",
                    "Because energy is released to power vital life processes, respiration is classified as an exothermic reaction."
                )
            )
        ),

        ChapterContent(
            chapterNumber = "2",
            title = "Acids, Bases and Salts",
            subject = "Science",
            classLevel = 10,
            readTimeMinutes = 14,
            overview = """
                Acids are substances that produce hydrogen ions H⁺ (or hydronium ions H₃O⁺) in aqueous solution. They taste sour, conduct electricity in solution, and turn blue litmus paper red. Common mineral acids include hydrochloric acid (HCl), sulphuric acid (H₂SO₄), and nitric acid (HNO₃). Organic acids include acetic acid in vinegar and citric acid in citrus fruits.
                
                Bases are substances that produce hydroxide ions OH⁻ in aqueous solution. They taste bitter, feel slippery or soapy to touch, and turn red litmus paper blue. Water-soluble bases are known as alkalis (e.g. NaOH, KOH, Ca(OH)₂).
                
                Neutralization Reaction:
                When an acid reacts with a base, they cancel each other's effects, producing a salt and water:
                Acid + Base → Salt + Water (e.g. HCl + NaOH → NaCl + H₂O)
                
                The pH Scale:
                Measures the concentration of hydrogen ions in a solution from 0 to 14.
                • pH < 7: Acidic solution (lower pH = stronger acid)
                • pH = 7: Neutral solution (pure distilled water)
                • pH > 7: Basic / Alkaline solution (higher pH = stronger base)
                
                Importance of pH in Everyday Life:
                1. Living organisms operate within a narrow pH range of 7.0 to 7.8.
                2. Acid rain occurs when rainwater pH drops below 5.6, harming aquatic ecosystems.
                3. Stomach produces HCl (pH ~ 1.5-2.0) to digest food. Excess acid causes pain treated with antacids (Mg(OH)₂ milk of magnesia or NaHCO₃).
                4. Tooth decay begins when mouth pH falls below 5.5, dissolving calcium hydroxyapatite enamel.
                
                Important Commercial Salts:
                1. Bleaching Powder (CaOCl₂): Produced by the action of chlorine on dry slaked lime Ca(OH)₂. Used as a disinfectant and bleaching agent.
                2. Baking Soda (NaHCO₃): Sodium hydrogen carbonate, used in cooking, baking powder (mixed with mild edible acid like tartaric acid), and fire extinguishers.
                3. Washing Soda (Na₂CO₃·10H₂O): Recrystallized sodium carbonate with 10 water molecules of crystallization. Used in glass/soap manufacture and removing permanent water hardness.
                4. Plaster of Paris (CaSO₄·½H₂O): Obtained by heating gypsum (CaSO₄·2H₂O) at 373 K. Sets into hard gypsum when mixed with water; used for setting fractured bones and statues.
            """.trimIndent(),
            keyFormulas = listOf(
                "Neutralization" to "Acid + Base → Salt + Water  (H⁺ + OH⁻ → H₂O)",
                "pH Definition" to "pH = -log₁₀[H⁺]  (Acid < 7, Neutral = 7, Base > 7)",
                "Bleaching Powder" to "Ca(OH)₂ + Cl₂ → CaOCl₂ + H₂O",
                "Baking Soda" to "NaCl + H₂O + CO₂ + NH₃ → NH₄Cl + NaHCO₃",
                "Plaster of Paris" to "CaSO₄·2H₂O (373 K) → CaSO₄·½H₂O + 1½H₂O"
            ),
            keyConcepts = listOf(
                "Hydronium Ion" to "H⁺ cannot exist alone in water; it associates with H₂O to form H₃O⁺.",
                "Water of Crystallization" to "Fixed number of water molecules chemically bonded in one formula unit of salt (e.g. CuSO₄·5H₂O blue vitriol).",
                "Strong vs Weak Acids" to "Strong acids dissociate completely in water (HCl); weak acids dissociate partially (CH₃COOH)."
            ),
            realWorldExamples = listOf(
                "Using calamine lotion (zinc carbonate) on wasp/ant stings to neutralize formic acid.",
                "Toothpaste being slightly alkaline to neutralize tooth-decaying mouth acids.",
                "Farmers adding slaked lime (CaO or Ca(OH)₂) to acidic soil to restore optimal crop pH."
            ),
            hindiSummary = "अम्ल, क्षारक और लवण: अम्ल जलीय विलयन में H⁺ आयन देते हैं (pH < 7) और नीले लिटमस को लाल करते हैं। क्षारक OH⁻ आयन देते हैं (pH > 7)। उदासीनीकरण में अम्ल और क्षारक मिलकर लवण और जल बनाते हैं। बेकिंग सोडा, धावन सोडा, विरंजक चूर्ण और प्लास्टर ऑफ पेरिस महत्वपूर्ण औद्योगिक लवण हैं।",
            odiaSummary = "ଅମ୍ଳ, କ୍ଷାରକ ଓ ଲବଣ: ଅମ୍ଳ H⁺ ଆୟନ ଏବଂ କ୍ଷାରକ OH⁻ ଆୟନ ପ୍ରଦାନ କରେ। pH ସ୍କେଲ ୦ ରୁ ୧୪ ମଧ୍ୟରେ ଅମ୍ଳୀୟତା ବା କ୍ଷାରକୀୟତା ମାପିଥାଏ। ଉଦାସୀନୀକରଣ ପ୍ରକ୍ରିୟାରେ ଲବଣ ଓ ଜଳ ଉତ୍ପନ୍ନ ହୁଏ।",
            boardQuestions = listOf(
                Triple(
                    "Why does an aqueous solution of an acid conduct electricity?",
                    "An aqueous solution of an acid conducts electricity because acids dissociate into free mobile hydrogen ions (H₃O⁺) and corresponding anions, which carry electric charge through the solution.",
                    "Dry HCl gas does not conduct electricity; moisture is required for ionization."
                ),
                Triple(
                    "Why does dry HCl gas not change the colour of dry litmus paper?",
                    "Dry HCl gas does not contain hydrogen ions (H⁺) because ionization into H⁺ ions only occurs in the presence of water. Without H⁺ ions, acidic properties cannot manifest.",
                    "Acidic properties are strictly caused by hydronium ions in aqueous media."
                )
            )
        ),

        ChapterContent(
            chapterNumber = "9",
            title = "Light - Reflection and Refraction",
            subject = "Science",
            classLevel = 10,
            readTimeMinutes = 18,
            overview = """
                Light is an electromagnetic radiation that travels along rectilinear paths (straight lines) with a speed of c = 3 × 10⁸ m/s in vacuum.
                
                Laws of Reflection:
                1. The angle of incidence (∠i) is strictly equal to the angle of reflection (∠r): ∠i = ∠r.
                2. The incident ray, the normal at the point of incidence, and the reflected ray all lie in the same geometric plane.
                
                Spherical Mirrors:
                • Concave Mirror: Reflecting surface curved inwards (converging). Used in headlights, shaving mirrors, dentist tools, and solar furnaces. Forms real and inverted images for all positions beyond focus F; forms a virtual, erect, and magnified image when object is between pole P and focus F.
                • Convex Mirror: Reflecting surface curved outwards (diverging). Always forms virtual, erect, and diminished images, providing a wide field of view. Used as vehicle rear-view mirrors.
                
                Sign Convention & Mirror Formula:
                • Distances measured towards left of pole are negative (u is always negative).
                • Concave mirror: focal length f is negative. Convex mirror: focal length f is positive.
                • Mirror Formula: 1/f = 1/v + 1/u
                • Linear Magnification: m = h'/h = -v/u
                
                Refraction of Light:
                The phenomenon of bending of light ray when passing obliquely from one transparent medium to another due to a change in the speed of light.
                • Rarer to Denser (air to glass): bends towards the normal.
                • Denser to Rarer (glass to air): bends away from the normal.
                
                Snell's Law of Refraction:
                The ratio of sine of angle of incidence to sine of angle of refraction is a constant for a given pair of media:
                sin(i) / sin(r) = constant = n₂₁ (refractive index of medium 2 with respect to 1).
                Absolute refractive index: n = c / v.
                
                Spherical Lenses:
                • Convex Lens: Thicker at centre, converging. Real images except when object is between optical centre O and focus F₁ (virtual and magnified). Focal length f is positive.
                • Concave Lens: Thinner at centre, diverging. Always forms virtual, erect, and diminished images. Focal length f is negative.
                • Lens Formula: 1/f = 1/v - 1/u
                • Magnification for Lenses: m = h'/h = +v/u
                • Power of Lens (P): P = 1 / f (f in metres), unit Dioptre (D). Convex has +D, concave has -D.
            """.trimIndent(),
            keyFormulas = listOf(
                "Mirror Formula" to "1/f = 1/v + 1/u  •  R = 2f",
                "Mirror Magnification" to "m = h'/h = -v/u",
                "Snell's Law" to "n₂₁ = sin(i) / sin(r) = v₁ / v₂",
                "Absolute Refractive Index" to "n = c / v  (c = 3 × 10⁸ m/s)",
                "Lens Formula" to "1/f = 1/v - 1/u",
                "Lens Magnification" to "m = h'/h = +v/u",
                "Power of Lens" to "P = 1 / f(in meters)  [Unit: Dioptre, D]"
            ),
            keyConcepts = listOf(
                "Virtual vs Real Image" to "Real images are formed by actual intersection of rays and can be cast on a screen. Virtual images are formed by perceived extension and cannot be captured on screen.",
                "Lateral Inversion" to "Right side of object appears left side in flat plane mirror.",
                "Optical Density" to "A medium with higher refractive index is optically denser, slowing light down."
            ),
            realWorldExamples = listOf(
                "A pencil placed in a tumbler of water appears bent at the air-water boundary due to refraction.",
                "Bottom of a swimming pool appearing raised and shallower than its true geometric depth.",
                "Rear-view wing mirrors in cars labeled 'Objects in mirror are closer than they appear' (convex mirror)."
            ),
            hindiSummary = "प्रकाश - परावर्तन तथा अपवर्तन: परावर्तन के नियम: ∠i = ∠r। दर्पण सूत्र: 1/f = 1/v + 1/u। लेंस सूत्र: 1/f = 1/v - 1/u। स्नेल का नियम: sin(i)/sin(r) = n। लेंस की क्षमता P = 1/f (मीटर में) डायोप्टर (D) में मापी जाती है। उत्तल लेंस अभिसारी (+D) और अवतल अपसारी (-D) होता है।",
            odiaSummary = "ଆଲୋକ - ପ୍ରତିଫଳନ ଏବଂ ପ୍ରତିସରଣ: ପ୍ରତିଫଳନ ନିୟମ ∠i = ∠r। ଦର୍ପଣ ସୂତ୍ର 1/f = 1/v + 1/u ଏବଂ ଲେନ୍ସ ସୂତ୍ର 1/f = 1/v - 1/u। ସ୍ନେଲଙ୍କ ନିୟମ sin(i)/sin(r) = n। ଲେନ୍ସର କ୍ଷମତା P = 1/f (ମିଟର) ଡାୟୋପ୍ଟର (D) ରେ ପ୍ରକାଶିତ।",
            boardQuestions = listOf(
                Triple(
                    "Why do we prefer a convex mirror as a rear-view mirror in vehicles?",
                    "Convex mirrors are preferred as vehicle rear-view mirrors because: 1) They always produce an erect though diminished image. 2) They curve outwards, providing a significantly wider field of view compared to plane mirrors, enabling the driver to monitor large traffic areas behind.",
                    "A plane mirror would only show a narrow angle of trailing traffic."
                ),
                Triple(
                    "Find the focal length of a lens of power -2.0 D. What type of lens is this?",
                    "Focal length f = 1 / P = 1 / (-2.0 D) = -0.5 m = -50 cm. Because the focal length and power are negative, this is a Concave (diverging) lens.",
                    "Negative power signifies a concave lens used for correcting myopia."
                )
            )
        ),

        ChapterContent(
            chapterNumber = "11",
            title = "Electricity",
            subject = "Science",
            classLevel = 10,
            readTimeMinutes = 16,
            overview = """
                Electricity is one of the most versatile forms of energy in the modern world.
                
                Electric Current (I):
                The rate of flow of electric charges through a conductor's cross-section:
                I = Q / t
                Measured in Amperes (A), where 1 Ampere = 1 Coulomb / second. Electric current flows from positive terminal to negative terminal, which is conventional current (opposite to electron drift).
                
                Electric Potential Difference (V):
                The work done (W) in moving a unit positive electric charge (Q) from one point to another:
                V = W / Q
                Measured in Volts (V), where 1 Volt = 1 Joule / Coulomb. Measured using a high-resistance Voltmeter connected in parallel.
                
                Ohm's Law:
                At constant temperature, the electric current flowing through a metallic conductor is directly proportional to the potential difference applied across its ends:
                V ∝ I  =>  V = I × R
                where R is the electrical resistance of the conductor, measured in Ohms (Ω).
                
                Factors affecting Resistance:
                1. Length (L): Resistance is directly proportional to length (R ∝ L).
                2. Cross-sectional Area (A): Resistance is inversely proportional to area (R ∝ 1/A).
                3. Material nature: Represented by electrical resistivity (ρ):
                R = ρ × (L / A)
                Resistivity unit is Ohm-metre (Ω·m). Metals (copper, aluminium) have very low resistivity (~10⁻⁸ Ω·m), making them excellent conductors. Alloys (nichrome) have high resistivity and high melting points, used in heating elements. Insulators (rubber, glass) have resistivity ~10¹² to 10¹⁷ Ω·m.
                
                Combination of Resistors:
                • Series Connection: Resistors connected end-to-end. Current I is identical through all components. Total resistance: Rs = R₁ + R₂ + R₃.
                • Parallel Connection: Resistors connected across common voltage nodes. Potential difference V is identical across each resistor. Equivalent resistance: 1/Rp = 1/R₁ + 1/R₂ + 1/R₃. For two resistors: Rp = (R₁ × R₂) / (R₁ + R₂). Parallel wiring prevents one faulty bulb from breaking the entire house circuit.
                
                Joule's Law of Heating & Electric Power:
                Heat produced in a resistor: H = I² × R × t = V × I × t = (V² / R) × t.
                Electric Power: Rate of electrical energy consumption: P = V × I = I² × R = V² / R (measured in Watts, W).
                Commercial Unit: 1 Kilowatt-hour (kWh, 1 Unit) = 1000 W × 3600 s = 3.6 × 10⁶ Joules (J).
            """.trimIndent(),
            keyFormulas = listOf(
                "Electric Current" to "I = Q / t  (1 A = 1 C/s)",
                "Potential Difference" to "V = W / Q  (1 V = 1 J/C)",
                "Ohm's Law" to "V = I × R  (R = V / I)",
                "Resistance & Resistivity" to "R = ρ × (L / A)",
                "Series Resistance" to "Rs = R₁ + R₂ + R₃ + ...",
                "Parallel Resistance" to "1/Rp = 1/R₁ + 1/R₂ + ...  •  Rp = (R₁R₂)/(R₁+R₂)",
                "Joule's Law of Heating" to "H = I² × R × t = V × I × t",
                "Electric Power" to "P = V × I = I² × R = V² / R  [Unit: Watt]",
                "Commercial Energy Unit" to "1 kWh = 1 Unit = 3.6 × 10⁶ J"
            ),
            keyConcepts = listOf(
                "Ammeter vs Voltmeter" to "Ammeter has low resistance and connects in series. Voltmeter has high resistance and connects in parallel.",
                "Fuse Wire" to "A safety device made of tin-lead alloy with low melting point; breaks circuit during overcurrent via Joule heating.",
                "Domestic Parallel Circuit" to "Each appliance receives 220V supply and can be operated independently."
            ),
            realWorldExamples = listOf(
                "Electric geysers and toasters using nichrome alloy coils which glow red hot without oxidizing.",
                "Electric power metering units in households measuring consumption in kWh.",
                "Copper wires used for transmission lines due to minimal resistivity (low heat loss)."
            ),
            hindiSummary = "विद्युत: विद्युत धारा I = Q/t, विभवान्तर V = W/Q। ओम का नियम: V = I * R। प्रतिरोध R = ρ*L/A। श्रेणीक्रम में Rs = R1 + R2, समांतर क्रम में 1/Rp = 1/R1 + 1/R2। जूल का तापन नियम H = I^2*R*t। शक्ति P = V*I। 1 यूनिट (kWh) = 3.6 × 10^6 जूल।",
            odiaSummary = "ବିଦ୍ୟୁତ: ବିଦ୍ୟୁତ ସ୍ରୋତ I = Q/t, ବିଭବାନ୍ତର V = W/Q। ଓମ୍‌ଙ୍କ ନିୟମ V = I * R। ପଂକ୍ତି ସଂଯୋଗରେ Rs = R1 + R2 ଏବଂ ସମାନ୍ତରରେ 1/Rp = 1/R1 + 1/R2। ଜୁଲ୍‌ଙ୍କ ତାପନ ନିୟମ H = I^2*R*t। ୧ ୟୁନିଟ = ୩.୬ × ୧୦^୬ ଜୁଲ୍।",
            boardQuestions = listOf(
                Triple(
                    "Why are coils of electric toasters and electric irons made of an alloy rather than a pure metal?",
                    "Coils of heating appliances are made of alloys (like Nichrome) because: 1) The resistivity of an alloy is generally higher than that of its constituent pure metals. 2) Alloys do not oxidize (burn) readily at high temperatures, ensuring longevity.",
                    "Pure metals melt or oxidize quickly when heated continuously."
                ),
                Triple(
                    "Why does the cord of an electric heater not glow while the heating element does?",
                    "The heating element is made of high-resistance alloy (Nichrome), generating massive heat via Joule heating (H = I²Rt) causing it to glow red. In contrast, the connecting cord is made of thick copper with negligible resistance, producing barely any heat.",
                    "Heat generated is directly proportional to resistance for a constant series current."
                )
            )
        ),

        // ==================== CLASS 10 MATHEMATICS ====================
        ChapterContent(
            chapterNumber = "1",
            title = "Real Numbers",
            subject = "Mathematics",
            classLevel = 10,
            readTimeMinutes = 13,
            overview = """
                Real numbers include all rational numbers (numbers expressible as p/q where p, q are integers and q ≠ 0) and irrational numbers (numbers with non-terminating and non-recurring decimal expansions).
                
                The Fundamental Theorem of Arithmetic:
                Every composite number can be uniquely expressed (factorised) as a product of prime numbers, apart from the order in which the prime factors occur.
                For example: 120 = 2³ × 3 × 5.
                
                Computing HCF and LCM using Prime Factorisation:
                • HCF (Highest Common Factor): Product of the smallest power of each common prime factor involved in the numbers.
                • LCM (Least Common Multiple): Product of the greatest power of each prime factor involved in the numbers.
                • Relationship for any two positive integers a and b:
                HCF(a, b) × LCM(a, b) = a × b
                (Note: This formula holds strictly for two numbers, not three numbers).
                
                Revisiting Irrational Numbers:
                Proof by Contradiction is used to establish that √2, √3, √5, etc., are irrational numbers.
                Theorem: If p is a prime number and p divides a², then p divides a, where a is a positive integer.
                Using this theorem, assuming √2 = a/b (in co-prime lowest terms) leads to a contradiction that both a and b share a common factor 2. Hence √2 is irrational.
                Sum or difference of a rational and an irrational number is always irrational (e.g. 3 + √5).
            """.trimIndent(),
            keyFormulas = listOf(
                "HCF and LCM Relation" to "HCF(a, b) × LCM(a, b) = a × b",
                "Fundamental Theorem" to "Every composite number = p₁^a × p₂^b × ... (unique prime factors)",
                "Prime Divisibility Theorem" to "If prime p divides a², then p divides a"
            ),
            keyConcepts = listOf(
                "Co-prime Numbers" to "Two integers a and b are co-prime if their HCF is 1 (no common prime factor).",
                "Rational Decimals" to "Terminating if prime factors of denominator q are of form 2^n × 5^m; non-terminating repeating otherwise.",
                "Irrational Decimal" to "Non-terminating and non-repeating decimal expansion (e.g. √2 = 1.4142135...)."
            ),
            realWorldExamples = listOf(
                "Determining when three runners around a circular track meet again at the starting point (LCM).",
                "Cutting pieces of ribbons of different lengths into equal maximum length pieces without leftover (HCF)."
            ),
            hindiSummary = "वास्तविक संख्याएं: अंकगणित की आधारभूत प्रमेय के अनुसार प्रत्येक भाज्य संख्या को अभाज्य गुणनफल के रूप में लिखा जा सकता है। दो संख्याओं के लिए HCF(a, b) * LCM(a, b) = a * b। √2, √3 अपरिमेय संख्याएं हैं।",
            odiaSummary = "ବାସ୍ତବ ସଂଖ୍ୟା: ପାଟିଗଣିତର ମୌଳିକ ପ୍ରମେୟ ଅନୁଯାୟୀ ପ୍ରତ୍ୟେକ ଯୌଗିକ ସଂଖ୍ୟା ମୌଳିକ ଗୁଣନୀୟକରେ ପ୍ରକାଶ କରାଯାଏ। HCF * LCM = a * b। √୨ ଓ √୩ ହେଉଛନ୍ତି ଅପରିମେୟ ସଂଖ୍ୟା।",
            boardQuestions = listOf(
                Triple(
                    "Prove that √5 is an irrational number.",
                    "Assume √5 is rational, so √5 = a/b where a, b are co-prime integers (HCF=1). Squaring gives 5 = a²/b² => a² = 5b². Thus 5 divides a², so 5 divides a (a = 5c). Then 25c² = 5b² => b² = 5c², meaning 5 divides b. This contradicts co-primality of a and b. Hence √5 is irrational.",
                    "Classical proof by contradiction using the prime divisibility theorem."
                ),
                Triple(
                    "Given that HCF(306, 657) = 9, find LCM(306, 657).",
                    "Using HCF(a, b) × LCM(a, b) = a × b:\n9 × LCM = 306 × 657\nLCM = (306 × 657) / 9 = 34 × 657 = 22,338.",
                    "Direct application of the two-number product identity."
                )
            )
        ),

        ChapterContent(
            chapterNumber = "4",
            title = "Quadratic Equations",
            subject = "Mathematics",
            classLevel = 10,
            readTimeMinutes = 14,
            overview = """
                A quadratic equation in variable x is an equation of the second degree in the standard form:
                ax² + bx + c = 0, where a, b, c are real numbers and a ≠ 0.
                
                The values of x that satisfy the equation are known as roots of the quadratic equation (or zeroes of the quadratic polynomial). Every quadratic equation has at most two roots.
                
                Methods of Solving Quadratic Equations:
                1. Factorisation (Splitting the middle term): Express ax² + bx + c as the product of two linear factors (px + q)(rx + s) = 0.
                2. Quadratic Formula (Sridharacharya's Method):
                x = [-b ± √(b² - 4ac)] / (2a)
                
                The Discriminant (D) and Nature of Roots:
                The quantity D = b² - 4ac is called the discriminant. It uniquely determines the nature of the roots:
                1. If D > 0: Two distinct real roots: x = (-b + √D) / (2a) and x = (-b - √D) / (2a).
                2. If D = 0: Two equal real roots (coincident roots): x = -b / (2a).
                3. If D < 0: No real roots (roots are complex or imaginary).
                
                Relationship between Roots and Coefficients:
                If α and β are the roots of ax² + bx + c = 0:
                • Sum of roots: α + β = -b / a
                • Product of roots: α × β = c / a
                The quadratic equation can be written as: x² - (Sum of roots)x + (Product of roots) = 0.
            """.trimIndent(),
            keyFormulas = listOf(
                "Standard Quadratic Form" to "ax² + bx + c = 0  (a ≠ 0)",
                "Discriminant" to "D = b² - 4ac",
                "Quadratic Formula" to "x = [-b ± √(b² - 4ac)] / (2a)",
                "Sum of Roots" to "α + β = -b / a",
                "Product of Roots" to "α × β = c / a"
            ),
            keyConcepts = listOf(
                "Parabolic Trajectory" to "The graph of y = ax² + bx + c is a parabola opening upward (if a > 0) or downward (if a < 0).",
                "Equal Roots Condition" to "For roots to be real and equal, discriminant D = b² - 4ac must equal 0."
            ),
            realWorldExamples = listOf(
                "Calculating flight paths and maximum height of rockets, missiles, and tossed basketballs.",
                "Designing solar parabolic reflectors and suspension bridge cables.",
                "Determining revenue-maximizing pricing in retail economics."
            ),
            hindiSummary = "द्विघात समीकरण: मानक रूप ax² + bx + c = 0। विविक्तकर D = b² - 4ac। श्रीधराचार्य सूत्र: x = [-b ± √D] / (2a)। D > 0 पर दो भिन्न वास्तविक मूल, D = 0 पर दो बराबर मूल, और D < 0 पर कोई वास्तविक मूल नहीं। मूलों का योग = -b/a, गुणनफल = c/a।",
            odiaSummary = "ଦ୍ୱିଘାତ ସମୀକରଣ: ax² + bx + c = 0। ପ୍ରଭେଦକ D = b² - 4ac। ଶ୍ରୀଧରାଚାର୍ଯ୍ୟ ସୂତ୍ର x = [-b ± √D] / (2a)। D > ୦ ହେଲେ ଦୁଇଟି ଭିନ୍ନ ବାସ୍ତବ ମୂଳ, D = ୦ ହେଲେ ସମାନ ମୂଳ ଏବଂ D < ୦ ହେଲେ ବାସ୍ତବ ମୂଳ ନାହିଁ।",
            boardQuestions = listOf(
                Triple(
                    "Find the roots of the equation 2x² - 5x + 3 = 0 using the quadratic formula.",
                    "Here a = 2, b = -5, c = 3.\nDiscriminant D = b² - 4ac = (-5)² - 4(2)(3) = 25 - 24 = 1.\nSince D > 0, roots are real and distinct:\nx = [-(-5) ± √1] / (2 × 2) = (5 ± 1) / 4.\nx₁ = 6/4 = 3/2,  x₂ = 4/4 = 1.\nRoots are x = 3/2 and x = 1.",
                    "Standard quadratic formula computation."
                ),
                Triple(
                    "Find the value of k for which the quadratic equation 2x² + kx + 3 = 0 has two equal roots.",
                    "For equal roots, discriminant D = b² - 4ac = 0.\nHere a = 2, b = k, c = 3.\nD = k² - 4(2)(3) = k² - 24 = 0.\nk² = 24 => k = ±√24 = ±2√6.",
                    "Equal roots condition D = 0."
                )
            )
        ),

        ChapterContent(
            chapterNumber = "8",
            title = "Introduction to Trigonometry",
            subject = "Mathematics",
            classLevel = 10,
            readTimeMinutes = 15,
            overview = """
                Trigonometry (from Greek words 'tri' meaning three, 'gon' meaning sides, and 'metron' meaning measure) deals with the study of relationships between the sides and angles of triangles.
                
                Trigonometric Ratios (in a right-angled triangle ABC right-angled at B):
                For acute angle θ = ∠A:
                • Sine: sin(θ) = Opposite side (BC) / Hypotenuse (AC)
                • Cosine: cos(θ) = Adjacent side (AB) / Hypotenuse (AC)
                • Tangent: tan(θ) = Opposite side / Adjacent side = sin(θ) / cos(θ)
                • Cosecant: cosec(θ) = 1 / sin(θ) = Hypotenuse / Opposite
                • Secant: sec(θ) = 1 / cos(θ) = Hypotenuse / Adjacent
                • Cotangent: cot(θ) = 1 / tan(θ) = Adjacent / Opposite
                
                Values of Trigonometric Ratios for Standard Angles:
                • θ = 0°: sin 0 = 0, cos 0 = 1, tan 0 = 0
                • θ = 30°: sin 30 = 1/2, cos 30 = √3/2, tan 30 = 1/√3
                • θ = 45°: sin 45 = 1/√2, cos 45 = 1/√2, tan 45 = 1
                • θ = 60°: sin 60 = √3/2, cos 60 = 1/2, tan 60 = √3
                • θ = 90°: sin 90 = 1, cos 90 = 0, tan 90 = not defined
                
                Fundamental Trigonometric Identities:
                1. sin²(θ) + cos²(θ) = 1  (for 0° ≤ θ ≤ 90°)
                2. 1 + tan²(θ) = sec²(θ)  (for 0° ≤ θ < 90°)
                3. 1 + cot²(θ) = cosec²(θ)  (for 0° < θ ≤ 90°)
                
                These identities are proven directly using the Pythagoras Theorem in right-angled triangles:
                AB² + BC² = AC²
                Dividing both sides by AC² yields sin²(θ) + cos²(θ) = 1.
            """.trimIndent(),
            keyFormulas = listOf(
                "Identity 1" to "sin²(θ) + cos²(θ) = 1  =>  sin²θ = 1 - cos²θ",
                "Identity 2" to "1 + tan²(θ) = sec²(θ)  =>  sec²θ - tan²θ = 1",
                "Identity 3" to "1 + cot²(θ) = cosec²(θ)  =>  cosec²θ - cot²θ = 1",
                "Tangent Ratio" to "tan(θ) = sin(θ) / cos(θ)",
                "Standard Values" to "sin(30°)=1/2, sin(45°)=1/√2, sin(60°)=√3/2, tan(45°)=1"
            ),
            keyConcepts = listOf(
                "Pythagoras Foundation" to "All 3 fundamental identities originate from AC² = AB² + BC².",
                "Domain Restrictions" to "tan(90°) and sec(90°) are undefined because cos(90°) = 0; cot(0°) and cosec(0°) are undefined because sin(0°) = 0."
            ),
            realWorldExamples = listOf(
                "Surveyors using theodolites to calculate heights of inaccessible mountain peaks or towers.",
                "Navigational positioning in aviation and GPS satellite triangulation.",
                "Architectural roof truss angles."
            ),
            hindiSummary = "त्रिकोणमिति का परिचय: समकोण त्रिभुज में sinθ = लंब/कर्ण, cosθ = आधार/कर्ण, tanθ = लंब/आधार। सर्वसमिकाएं: sin²θ + cos²θ = 1, 1 + tan²θ = sec²θ, 1 + cot²θ = cosec²θ। मानक मान: sin 30° = 1/2, sin 45° = 1/√2, sin 60° = √3/2, tan 45° = 1।",
            odiaSummary = "ତ୍ରିକୋଣମିତି ପରିଚୟ: ସମକୋଣୀ ତ୍ରିଭୁଜରେ sinθ = ଲମ୍ବ/କର୍ଣ୍ଣ, cosθ = ଭୂମି/କର୍ଣ୍ଣ, tanθ = ଲମ୍ବ/ଭୂମି। ମୁଖ୍ୟ ସର୍ବସମିକା: sin²θ + cos²θ = ୧, ୧ + tan²θ = sec²θ। sin(୩୦°) = ୧/୨, tan(୪୫°) = ୧।",
            boardQuestions = listOf(
                Triple(
                    "Evaluate: (sin 30° + tan 45° - cosec 60°) / (sec 30° + cos 60° + cot 45°).",
                    "Numerator = 1/2 + 1 - 2/√3 = 3/2 - 2/√3 = (3√3 - 4) / (2√3).\nDenominator = 2/√3 + 1/2 + 1 = 2/√3 + 3/2 = (4 + 3√3) / (2√3).\nResult = (3√3 - 4) / (3√3 + 4).\nRationalizing: (3√3 - 4)² / ((3√3)² - 4²) = (27 + 16 - 24√3) / (27 - 16) = (43 - 24√3) / 11.",
                    "Direct substitution of standard trigonometric values and rationalization."
                ),
                Triple(
                    "Prove that (sin θ - 2 sin³ θ) / (2 cos³ θ - cos θ) = tan θ.",
                    "LHS = [sin θ (1 - 2 sin² θ)] / [cos θ (2 cos² θ - 1)].\nRecall 1 - 2 sin² θ = cos² θ + sin² θ - 2 sin² θ = cos² θ - sin² θ.\nAnd 2 cos² θ - 1 = 2 cos² θ - (cos² θ + sin² θ) = cos² θ - sin² θ.\nBoth numerator and denominator brackets cancel out identically!\nLHS = sin θ / cos θ = tan θ = RHS.",
                    "Factoring out sin θ and cos θ and applying sin² θ + cos² θ = 1."
                )
            )
        ),

        // ==================== CLASS 9 SCIENCE ====================
        ChapterContent(
            chapterNumber = "8",
            title = "Force and Laws of Motion",
            subject = "Science",
            classLevel = 9,
            readTimeMinutes = 15,
            overview = """
                Force is an external push or pull that changes or tends to change the state of rest, uniform motion, direction, or shape of an object. Force is a vector quantity with SI unit Newton (N).
                
                Balanced vs Unbalanced Forces:
                • Balanced Forces: Net resultant force is zero. They do not change the velocity or direction of an object, though they can alter its shape (e.g. squeezing a rubber ball).
                • Unbalanced Forces: Net force is non-zero, producing acceleration in the object.
                
                Newton's First Law of Motion (Law of Inertia):
                An object continues in its state of rest or of uniform motion in a straight line unless acted upon by an external unbalanced force.
                Inertia is the inherent tendency of an object to resist changes in its state of motion. Mass is the quantitative measure of an object's inertia (greater mass = greater inertia).
                Examples: Passengers falling backward when a stationary bus starts suddenly, or lurching forward when brakes are applied.
                
                Newton's Second Law of Motion:
                The rate of change of momentum of an object is directly proportional to the applied unbalanced force and takes place in the direction of the force.
                Momentum p = mass × velocity (p = m × v, SI unit kg·m/s).
                Rate of change of momentum = d(mv)/dt = m × (v - u)/t = m × a.
                Hence: F = m × a  (Force = mass × acceleration)
                1 Newton is the force that produces an acceleration of 1 m/s² on an object of mass 1 kg.
                
                Newton's Third Law of Motion:
                To every action, there is always an equal and opposite reaction.
                Action and reaction forces act simultaneously on two different interacting bodies, so they never cancel each other out.
                Action = -Reaction  (F_AB = -F_BA)
                
                Conservation of Linear Momentum:
                In an isolated system (no external unbalanced force), the total momentum of interacting bodies before collision equals the total momentum after collision:
                m₁u₁ + m₂u₂ = m₁v₁ + m₂v₂.
            """.trimIndent(),
            keyFormulas = listOf(
                "Momentum" to "p = m × v  [Unit: kg·m/s]",
                "Newton's Second Law" to "F = m × a = m(v - u) / t  [Unit: Newton, N]",
                "Newton's Third Law" to "F_action = -F_reaction",
                "Conservation of Momentum" to "m₁u₁ + m₂u₂ = m₁v₁ + m₂v₂"
            ),
            keyConcepts = listOf(
                "Inertia Types" to "Inertia of rest (dust shaking off blanket), inertia of motion (athlete running before jumping), inertia of direction (car turning sharp corner).",
                "Impulse" to "Large force acting for short time (F × t = change in momentum). A cricketer pulls hands back to increase time t and reduce impact force F."
            ),
            realWorldExamples = listOf(
                "Recoil of a gun when a bullet is fired (momentum conservation).",
                "Rocket propulsion: exhaust gases pushed down with high velocity; rocket accelerates upward.",
                "Swimming: swimmer pushes water backward with hands (action), water pushes swimmer forward (reaction)."
            ),
            hindiSummary = "बल तथा गति के नियम: न्यूटन का पहला नियम (जड़त्व का नियम) - वस्तु अपनी विराम या गति की अवस्था बनाए रखती है। दूसरा नियम - बल = द्रव्यमान × त्वरण (F = m * a)। तीसरा नियम - प्रत्येक क्रिया के बराबर और विपरीत प्रतिक्रिया होती है। संवेग संरक्षण: कुल संवेग स्थिर रहता है।",
            odiaSummary = "ବଳ ଏବଂ ଗତିର ନିୟମ: ପ୍ରଥମ ନିୟମ (ଜଡ଼ତା ନିୟମ)। ଦ୍ୱିତୀୟ ନିୟମ: F = m * a। ତୃତୀୟ ନିୟମ: ପ୍ରତ୍ୟେକ କ୍ରିୟାର ସମାନ ଓ ବିପରୀତ ପ୍ରତିକ୍ରିୟା ଅଛି। ସଂବେଗ ସଂରକ୍ଷଣ ନିୟମ: ସଂଘର୍ଷ ପୂର୍ବ ଓ ପରବର୍ତ୍ତୀ ମୋଟ ସଂବେଗ ସମାନ।",
            boardQuestions = listOf(
                Triple(
                    "Why does a cricket fielder pull his hands backward while catching a fast-moving ball?",
                    "By pulling hands backward, the fielder increases the time interval (t) over which the high momentum of the ball decreases to zero. Since F = Δp / t, increasing time significantly reduces the impact force felt by the hands, preventing pain or injury.",
                    "Direct consequence of Newton's second law: increasing stopping time decreases impulsive force."
                ),
                Triple(
                    "Explain why some leaves may get detached from a tree if we vigorously shake its branch.",
                    "Before shaking, both the branch and the leaves are at rest. When vigorously shaken, the branch immediately comes into motion, but the attached leaves tend to remain at rest due to their inertia of rest. This creates a shear force at the stem, breaking the leaves away.",
                    "Application of Newton's first law (inertia of rest)."
                )
            )
        )
    )

    fun getChapter(classLevel: Int, subject: String, chapterNumberOrTitle: String): ChapterContent? {
        val normalized = chapterNumberOrTitle.trim().lowercase()
        return chapters.firstOrNull { ch ->
            ch.classLevel == classLevel &&
            ch.subject.equals(subject, ignoreCase = true) &&
            (ch.chapterNumber == normalized || ch.title.lowercase().contains(normalized) || normalized.contains(ch.title.lowercase()))
        } ?: chapters.firstOrNull { ch ->
            ch.title.lowercase().contains(normalized) || normalized.contains(ch.title.lowercase())
        }
    }

    fun getAllChaptersForBook(bookId: String): List<ChapterContent> {
        val classNum = Regex("class-(\\d+)").find(bookId)?.groupValues?.get(1)?.toIntOrNull() ?: 10
        val subject = when {
            bookId.contains("science") -> "Science"
            bookId.contains("mathematics") -> "Mathematics"
            else -> "Science"
        }
        val matched = chapters.filter { it.classLevel == classNum && it.subject.equals(subject, ignoreCase = true) }
        return if (matched.isNotEmpty()) matched else chapters.filter { it.classLevel == 10 && it.subject.equals(subject, ignoreCase = true) }
    }

    /**
     * Converts our rich curriculum chapters into Room ContentChunkEntity items
     * to populate offline RAG search and textbook retrieval.
     */
    fun createChunksForBook(packId: String, version: Int, bookId: String): List<ContentChunkEntity> {
        val classNum = Regex("class-(\\d+)").find(bookId)?.groupValues?.get(1)?.toIntOrNull() ?: 10
        val subject = when {
            bookId.contains("science") -> "Science"
            bookId.contains("mathematics") -> "Mathematics"
            else -> "Science"
        }

        val bookChapters = chapters.filter { it.classLevel == classNum && it.subject.equals(subject, ignoreCase = true) }
            .ifEmpty { chapters.filter { it.subject.equals(subject, ignoreCase = true) } }

        val chunks = mutableListOf<ContentChunkEntity>()
        var page = 1

        bookChapters.forEach { ch ->
            // Chunk 1: Overview
            chunks.add(
                ContentChunkEntity(
                    id = "${packId}_ch${ch.chapterNumber}_overview",
                    packId = packId,
                    version = version,
                    chapter = ch.title,
                    section = "Chapter ${ch.chapterNumber} Overview",
                    pageNumber = page++,
                    sourceText = ch.overview,
                    sourceCitation = "NCERT Class ${ch.classLevel} ${ch.subject}, Chapter ${ch.chapterNumber}: ${ch.title}"
                )
            )

            // Chunk 2: Key Formulas & Laws
            if (ch.keyFormulas.isNotEmpty()) {
                val formulasText = ch.keyFormulas.joinToString("\n") { "• ${it.first}: ${it.second}" }
                chunks.add(
                    ContentChunkEntity(
                        id = "${packId}_ch${ch.chapterNumber}_formulas",
                        packId = packId,
                        version = version,
                        chapter = ch.title,
                        section = "Key Formulas and Laws",
                        pageNumber = page++,
                        sourceText = "Key formulas for ${ch.title}:\n$formulasText",
                        sourceCitation = "NCERT Class ${ch.classLevel} ${ch.subject}, Chapter ${ch.chapterNumber} Formulas"
                    )
                )
            }

            // Chunk 3: Key Concepts & Real World Examples
            if (ch.keyConcepts.isNotEmpty() || ch.realWorldExamples.isNotEmpty()) {
                val conceptsText = ch.keyConcepts.joinToString("\n") { "• ${it.first}: ${it.second}" }
                val examplesText = ch.realWorldExamples.joinToString("\n") { "• $it" }
                chunks.add(
                    ContentChunkEntity(
                        id = "${packId}_ch${ch.chapterNumber}_concepts",
                        packId = packId,
                        version = version,
                        chapter = ch.title,
                        section = "Important Concepts and Applications",
                        pageNumber = page++,
                        sourceText = "Concepts:\n$conceptsText\n\nReal-World Applications:\n$examplesText",
                        sourceCitation = "NCERT Class ${ch.classLevel} ${ch.subject}, Chapter ${ch.chapterNumber} Applications"
                    )
                )
            }

            // Chunk 4: Vernacular Regional Summaries
            chunks.add(
                ContentChunkEntity(
                    id = "${packId}_ch${ch.chapterNumber}_vernacular",
                    packId = packId,
                    version = version,
                    chapter = ch.title,
                    section = "Regional Summaries (Hindi & Odia)",
                    pageNumber = page++,
                    sourceText = "Hindi: ${ch.hindiSummary}\nOdia: ${ch.odiaSummary}",
                    sourceCitation = "NCERT Class ${ch.classLevel} ${ch.subject}, Chapter ${ch.chapterNumber} Regional Summary"
                )
            )
        }

        // Also add corpus chunks if relevant
        CurriculumCorpus.chunks.filter { it.subject.equals(subject, ignoreCase = true) }.forEachIndexed { idx, c ->
            chunks.add(
                ContentChunkEntity(
                    id = "${packId}_corpus_${idx + 1}",
                    packId = packId,
                    version = version,
                    chapter = c.chapter,
                    section = c.section,
                    pageNumber = page++,
                    sourceText = "${c.topic}: ${c.content}\nKey Formula: ${c.keyFormula}\nExample: ${c.realWorldExample}",
                    sourceCitation = c.sourceCitation
                )
            )
        }

        return chunks
    }
}
