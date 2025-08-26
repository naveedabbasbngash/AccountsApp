📊 Mehfooz Accounts App

A modern Android finance & accounts management app built with Kotlin, Jetpack Compose, Material 3, and Room Database.
The app is designed for small businesses and individuals who need clear tracking of transactions, balances, and analytics in multiple currencies.

⸻

🚀 Features

🔹 Transaction Management
	•	Record and view debits (Banam) and credits (Jama).
	•	Powerful search functionality by name.
	•	Filter chips to quickly switch between All, Debits, Credits, or Balance.
	•	Date picker (single day / range) to filter transactions.

🔹 Balance Insights

	•	Auto-calculates credit, debit, and balance per account.
	•	Supports multi-currency summaries.
	•	Dedicated Balance view showing totals with charts.

🔹 Analytics & Charts

	•	Daily, monthly, and currency-wise breakdowns.
	•	Integrated Pie Charts (MPAndroidChart) for easy visualization.
	•	Dashboard with clear financial overview.

🔹 Modern Android Stack
	•	Kotlin & Coroutines Flow for reactive data handling.
	•	Jetpack Compose for declarative UI with Material 3 styling.
	•	Room Database (DAO, Flow) for offline persistence.
	•	MVVM architecture with lifecycle-aware components.

⸻

📱 Screens / UI

	•	Dashboard → Monthly totals, charts & breakdowns.
	•	Transactions → Search, filter, and view transaction history.
	•	Balance → Credit/Debit per currency with visualization.
	•	Profile → (Future scope: user profile, settings, sync).

⸻

🛠️ Tech Stack

	•	Language: Kotlin
	•	UI: Jetpack Compose + Material 3
	•	Architecture: MVVM (ViewModel + StateFlow + Compose UI)
	•	Database: Room (with DAOs, queries, joins, and reactive Flow APIs)
	•	Charts: MPAndroidChart for pie chart visualizations
	•	Build System: Gradle (KTS)
