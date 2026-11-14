package com.example.data.education

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class CybersecurityLesson(
    val id: String,
    val number: Int,
    val title: String,
    val category: String,
    val readMinutes: Int,
    val introduction: String,
    val explanation: String,
    val practicalExample: String,
    val recommendations: List<String>,
    val quiz: List<QuizQuestion>
)

object LearningRepository {
    val lessons: List<CybersecurityLesson> = listOf(
        CybersecurityLesson(
            id = "lesson_1",
            number = 1,
            title = "How to identify fake bank messages",
            category = "Banking Scams",
            readMinutes = 3,
            introduction = "Scammers often send urgent SMS or WhatsApp alerts pretending to be your bank to steal login details or OTP codes.",
            explanation = "Real banks never ask for your full PIN, password, or one-time passcode over a text link. Fake bank messages create panic by claiming your account is frozen or an unauthorized transfer occurred.",
            practicalExample = "\"[Alert] Your bank account has been suspended due to unusual activity. Verify immediately at http://bank-verify-secure.xyz or lose access.\"",
            recommendations = listOf(
                "Never click links inside unexpected banking texts.",
                "Open your bank's official mobile app directly or call the number on the back of your card.",
                "Check the sender domain carefully for misspellings or strange endings."
            ),
            quiz = listOf(
                QuizQuestion(
                    question = "What should you do if an SMS says your bank account is locked with a link to unlock it?",
                    options = listOf(
                        "Click the link quickly so you don't lose access",
                        "Open your bank's official app independently to check",
                        "Reply with your account number to confirm"
                    ),
                    correctIndex = 1,
                    explanation = "Always check your account by opening the official bank app directly instead of tapping links in texts."
                )
            )
        ),
        CybersecurityLesson(
            id = "lesson_2",
            number = 2,
            title = "How to avoid WhatsApp scams",
            category = "WhatsApp Scams",
            readMinutes = 3,
            introduction = "WhatsApp scams range from hijacked friend accounts to fake prize notifications and support impersonators.",
            explanation = "Attackers sometimes trick people into sharing a 6-digit WhatsApp verification code so they can take over the account and message friends asking for emergency money.",
            practicalExample = "\"Hi, I accidentally sent my WhatsApp verification code to your phone by mistake. Can you please forward me the 6 digits?\"",
            recommendations = listOf(
                "Enable Two-Step Verification PIN inside WhatsApp Settings > Account.",
                "Never share your 6-digit SMS registration code with anyone, even a friend.",
                "If a friend asks for urgent money on chat, call them on a known voice number first."
            ),
            quiz = listOf(
                QuizQuestion(
                    question = "Why do scammers ask you to send them a 6-digit SMS code?",
                    options = listOf(
                        "To verify your friendship",
                        "To hijack your WhatsApp or banking account",
                        "To improve your signal quality"
                    ),
                    correctIndex = 1,
                    explanation = "Verification codes allow someone to register your account on their own device."
                )
            )
        ),
        CybersecurityLesson(
            id = "lesson_3",
            number = 3,
            title = "How to recognize fake job offers",
            category = "Fake Jobs",
            readMinutes = 4,
            introduction = "Fraudulent recruiters lure job seekers with high-paying remote work or overseas visas requiring upfront fees.",
            explanation = "Legitimate employers do not ask candidates to pay for training kits, visa processing, or 'task unlocks' before starting work. Many task-scam schemes pay small bonuses at first and then demand large deposits.",
            practicalExample = "\"Earn $300 daily liking videos from home! Just deposit $50 to activate your VIP merchant task account.\"",
            recommendations = listOf(
                "Never pay money to get a job or unlock daily tasks.",
                "Verify the recruiter's email domain matches the company's real website.",
                "Be cautious of interviews conducted purely via messaging apps with instant offers."
            ),
            quiz = listOf(
                QuizQuestion(
                    question = "Which sign strongly indicates a fake job offer?",
                    options = listOf(
                        "A scheduled video or in-person interview",
                        "Asking you to pay an upfront registration or deposit fee",
                        "Providing an official corporate HR email"
                    ),
                    correctIndex = 1,
                    explanation = "Real employers pay you; they never charge applicants upfront deposits or activation fees."
                )
            )
        ),
        CybersecurityLesson(
            id = "lesson_4",
            number = 4,
            title = "How investment fraud works",
            category = "Investment Scams",
            readMinutes = 4,
            introduction = "Fake crypto, forex, and stock trading groups promise guaranteed high returns with zero risk.",
            explanation = "Scammers use fabricated trading dashboards showing fake profits to convince victims to deposit larger amounts. When victims try to withdraw, scammers demand 'taxes' or 'unlock fees' and vanish.",
            practicalExample = "\"Guaranteed 35% weekly return on AI crypto trading! Join our VIP Telegram signal group today.\"",
            recommendations = listOf(
                "Treat any promise of 'guaranteed returns' as a major red flag.",
                "Verify financial platforms with your country's official securities regulator.",
                "Never transfer funds to personal wallets or unknown bank accounts for investment apps."
            ),
            quiz = listOf(
                QuizQuestion(
                    question = "What is a classic sign of an investment scam?",
                    options = listOf(
                        "Guaranteed high daily profits with no risk",
                        "Transparent risk disclosures",
                        "Registered regulation details"
                    ),
                    correctIndex = 0,
                    explanation = "All legitimate investments carry risk; guaranteed high profits are a hallmark of fraud."
                )
            )
        ),
        CybersecurityLesson(
            id = "lesson_5",
            number = 5,
            title = "How to identify phishing emails",
            category = "Phishing",
            readMinutes = 3,
            introduction = "Phishing emails imitate trusted companies like Microsoft, PayPal, DHL, or Apple to steal credentials.",
            explanation = "Look past the display name to inspect the actual sender email address and hover over links before clicking. Lookalike domains swap letters like 'rn' for 'm' or use extra words like 'support-billing-alert.com'.",
            practicalExample = "Sender: PayPal Security <notice@paypa1-support-verify.com> — \"Your invoice of $499.00 was processed. Click here to cancel.\"",
            recommendations = listOf(
                "Inspect the full sender email domain, not just the display name.",
                "Do not call support phone numbers listed inside suspicious refund emails.",
                "Log into your account directly via your browser bookmark to check activity."
            ),
            quiz = listOf(
                QuizQuestion(
                    question = "What should you inspect first on a suspicious email claiming to be from PayPal?",
                    options = listOf(
                        "The logo color",
                        "The actual sender email address and link domain",
                        "The font size"
                    ),
                    correctIndex = 1,
                    explanation = "Scammers copy official logos easily, but the actual sender domain often reveals impersonation."
                )
            )
        ),
        CybersecurityLesson(
            id = "lesson_6",
            number = 6,
            title = "How to recognize fake shopping websites",
            category = "Shopping Scams",
            readMinutes = 3,
            introduction = "Fraudulent online stores advertise luxury products at unrealistic discounts through social media ads.",
            explanation = "Warning signs include prices 70-90% below normal retail, missing physical address or return policies, copied reviews, and requests to pay via bank transfer or cryptocurrency instead of protected card checkout.",
            practicalExample = "Brand-new flagship smartphones advertised for $79 with a countdown timer ending in 10 minutes on an unfamiliar '.shop' or '.xyz' site.",
            recommendations = listOf(
                "Check independent reviews outside the store's own website.",
                "Inspect the URL using ScamShield's Website Checker before entering card details.",
                "Avoid stores that only accept wire transfers, gift cards, or crypto."
            ),
            quiz = listOf(
                QuizQuestion(
                    question = "Which payment request on an unfamiliar shopping site is a major red flag?",
                    options = listOf(
                        "Standard credit card checkout",
                        "Direct wire transfer, gift cards, or cryptocurrency only",
                        "Cash on delivery after inspection"
                    ),
                    correctIndex = 1,
                    explanation = "Wire transfers, gift cards, and crypto offer almost no buyer protection or chargeback rights."
                )
            )
        ),
        CybersecurityLesson(
            id = "lesson_7",
            number = 7,
            title = "How to protect passwords",
            category = "Account Security",
            readMinutes = 3,
            introduction = "Reusing the same password across websites lets attackers break into multiple accounts after a single data breach.",
            explanation = "A strong password is long (12+ characters), unique to every website, and stored in a reputable password manager. Enabling passkeys or app-based Two-Factor Authentication adds critical protection.",
            practicalExample = "Using 'Summer2024!' on both a small shopping forum and your primary email allows a breach on the forum to unlock your email and banking resets.",
            recommendations = listOf(
                "Use a unique password for your email and banking accounts.",
                "Turn on Two-Factor Authentication (2FA) wherever available.",
                "Never store passwords in plain text notes or share them in messages."
            ),
            quiz = listOf(
                QuizQuestion(
                    question = "Why is reusing one strong password across all sites risky?",
                    options = listOf(
                        "If one site is breached, attackers can try that password on your email and bank",
                        "Websites automatically delete duplicate passwords",
                        "It slows down Wi-Fi speeds"
                    ),
                    correctIndex = 0,
                    explanation = "Credential stuffing attacks test leaked passwords from one site on banking and email services."
                )
            )
        ),
        CybersecurityLesson(
            id = "lesson_8",
            number = 8,
            title = "Why OTP codes should never be shared",
            category = "Banking Scams",
            readMinutes = 2,
            introduction = "One-Time Passwords (OTPs) are the final lock protecting your bank transfers and account logins.",
            explanation = "Scammers often read your card number or initiate a login on their device, then call pretending to be customer support asking for the OTP 'to cancel a fraud transaction'—when the code actually approves the theft.",
            practicalExample = "\"Sir, to stop this $800 deduction, please read me the 6-digit cancellation code sent to your phone right now.\"",
            recommendations = listOf(
                "Read the entire OTP SMS carefully—check what action the code authorizes.",
                "Legitimate support agents will NEVER ask for an OTP.",
                "Hang up immediately if anyone asks for an OTP."
            ),
            quiz = listOf(
                QuizQuestion(
                    question = "When is it safe to read a banking OTP code to someone who called you?",
                    options = listOf(
                        "When they say it is to cancel fraud",
                        "Never—legitimate banks do not ask callers for OTPs",
                        "When they know your full name"
                    ),
                    correctIndex = 1,
                    explanation = "Scammers often already know your name or phone number from leaks; never share OTP codes."
                )
            )
        ),
        CybersecurityLesson(
            id = "lesson_9",
            number = 9,
            title = "How scammers impersonate relatives",
            category = "Impersonation",
            readMinutes = 3,
            introduction = "Family impersonation scams target parents and grandparents with emotional emergencies from unknown numbers.",
            explanation = "Commonly known as the 'Hi Mom/Dad, my phone broke' scam, fraudsters message from a new number claiming they lost their phone and urgently need rent or medical bills paid to a friend's account.",
            practicalExample = "\"Hi Mum, I dropped my phone in water and I'm texting from a friend's number. Can you transfer $450 right now so I can pay a bill?\"",
            recommendations = listOf(
                "Always call the relative's original saved phone number to verify.",
                "Agree on a private family safe-word for emergencies.",
                "Never rush a money transfer based solely on a text from an unknown number."
            ),
            quiz = listOf(
                QuizQuestion(
                    question = "What is the safest first step if someone texts from an unknown number claiming to be your child in trouble?",
                    options = listOf(
                        "Transfer a small amount first",
                        "Call your child on their original saved number or verify with another relative",
                        "Ask them for their bank account details"
                    ),
                    correctIndex = 1,
                    explanation = "Calling the saved phone number immediately confirms whether it is genuinely your relative."
                )
            )
        ),
        CybersecurityLesson(
            id = "lesson_10",
            number = 10,
            title = "What to do after clicking a suspicious link",
            category = "Incident Response",
            readMinutes = 4,
            introduction = "Clicking a suspicious link does not automatically doom your device—acting quickly and calmly limits harm.",
            explanation = "If you only opened the page and closed it without typing passwords or installing files, risk is usually low. If you entered credentials or banking details, take immediate protective steps.",
            practicalExample = "You entered your email password on a fake courier page before realizing the URL was wrong.",
            recommendations = listOf(
                "Close the browser tab immediately and do not download or install any APK/file.",
                "If you entered a password, change it immediately on the real website and enable 2FA.",
                "If you entered card or bank details, contact your bank right away to freeze the card.",
                "Check your device apps list and uninstall any unfamiliar apps."
            ),
            quiz = listOf(
                QuizQuestion(
                    question = "What should you do right away if you accidentally entered your password on a fake login page?",
                    options = listOf(
                        "Ignore it and hope nothing happens",
                        "Change the password immediately on the official site and enable 2FA",
                        "Turn off your screen for 10 minutes"
                    ),
                    correctIndex = 1,
                    explanation = "Changing your password immediately on the real service locks out attackers who captured the credentials."
                )
            )
        )
    )
}
