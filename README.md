# 📊 Mehfooz Accounts App

A modern Android finance & accounts management app built with **Kotlin, Jetpack Compose, Material 3, and Room Database**.  
Designed for small businesses and individuals who need **clear tracking of transactions, balances, and analytics in multiple currencies**.

---

## 🚀 Features

### 🔹 Transaction Management
- Record and view **Debits (Banam)** and **Credits (Jama)**.  
- Powerful **type-ahead search with live suggestions**.  
- Dropdown suggestion list with single-click selection (caret fixed to end).  
- Animated **transaction detail panel** when an item is selected.  
- **Filter chips** to quickly switch between **All, Debits, Credits, Currency filter, or Balance**.  
- **Date picker** (single day / range) to filter transactions by time.

### 🔹 Balance Insights
- Auto-calculates **credit, debit, and net balance** per account.  
- **Currency chip** shows all available currencies for a person with dropdown selection.  
- Supports **multi-currency summaries**.  
- Dedicated **Balance view** showing totals with mini-pie charts and animated panels.

### 🔹 Analytics & Charts
- **Daily, monthly, and currency-wise breakdowns**.  
- Integrated **Pie Charts (MPAndroidChart)** for visualization.  
- Dashboard with **clear financial overview**: daily series, monthly totals, and per-currency breakdown.

### 🔹 Modern Android Stack
- **Kotlin & Coroutines Flow** for reactive data handling.  
- **Jetpack Compose** with Material 3 for declarative, modern UI.  
- **Room Database** (DAO, queries, joins, and reactive Flow APIs).  
- **MVVM architecture** with lifecycle-aware components.  
- Clean separation of concerns with `ViewModel`, `DAO`, and `UI`.

---

## 📱 Screens / UI

- **Dashboard** → Monthly totals, line & pie charts, per-currency breakdown.  
- **Transactions** → Search with live suggestions, filters, detail panel.  
- **Balance** → Credit/Debit per currency with visual balance insights.  
- **Profile (Future Scope)** → User profile, settings, cloud sync.

---

## 🛠️ Tech Stack

- **Language**: Kotlin  
- **UI**: Jetpack Compose + Material 3  
- **Architecture**: MVVM (ViewModel + StateFlow + Compose UI)  
- **Database**: Room (Flow-based DAOs with joins and aggregates)  
- **Charts**: MPAndroidChart (Pie + Line charts)  
- **Build System**: Gradle (KTS)

---

## 📂 Project Highlights
- Suggestion dropdown implemented with `AnimatedVisibility` under the search box.  
- Single-click selection inserts into search box with correct caret position.  
- Currency dropdown chip allows **per-currency filtering** inside Transactions.  
- Balance chip & panel for quick account balance overview.  
- Clean commit history for easy collaboration.  

---

## 🖥️ Development

