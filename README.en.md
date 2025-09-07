# HistoryHelper for DBeaver
**Avaliable in**: [Русский](https://github.com/spersics/historyhelper/blob/main/README.md) | [English](https://github.com/spersics/historyhelper/blob/main/README.en.md)
---
**HistoryHelper** - a plugin for [DBeaver](https://dbeaver.io/) that generates SQL for creating history tables and triggers (INSERT/UPDATE/DELETE). It helps quickly enable auditing of changes and, if necessary, immediately apply a script to the selected table.

---

## Features
- Select columns to be logged.
- UI localization (RU/EN).
- Added context menu condition (Generate History Table available only for tables).
- Generate triggers for **ON INSERT / ON UPDATE / ON DELETE**.
- Two history modes: full and only previous state (in development)
- View the generated SQL and apply it directly from DBeaver.
- Filter PostgreSQL system columns (only user-defined columns are included).
- Convenient dialog window + context menu icon for tables.

---

## Compatibility
- **DBeaver:** 25.x
- **Java Runtime (JRE):** 17+
- **Database:** PostgreSQL (support for other DBMS is planned).

---

## Installation
1. Download the latest JAR file from the **Releases** section
2. Place the file into the plugins or dropins folder of your DBeaver installation:
    - **Windows:** '<DBeaver>\plugins\' or '<DBeaver>\dropins\'
    - **Linux/macOS:** '<DBeaver>/plugins/' or '<DBeaver>/dropins/'
3. Restart DBeaver (optionally clear the cache with "dbeaver.exe -clean -clearPersistedState")
4. Verify installation:
    - In the Russian version of DBeaver: **Справка -> Информация об установке -> Плагины** -> should list 'HistoryHelper'
    - In the English version of DBeaver: **Help -> Installation Details -> Plug-ins** -> should list 'HistoryHelper'.

---

## Usage
1. In **Database Navigator** right-click on a table.
2. Select **Generate History Table**.
3. In the dialog window:
    - choose columns to log;
    - select triggers (INSERT/UPDATE/DELETE)
    - optionally switch history storage mode (TBA)
4. Click **ОК** -> a SQL script for history tables and triggers will be generated..
   You can then:
- **Execute** SQL script. !The script is also copied to the clipboard!
- **Copy** to the clipboard. (use it with Liquibase or other migration tools).

---

## Build from source (Eclipse PDE)
1. Import the project as **Existing Plug-ins** / regular Java plugin.
2. Make sure **Project -> Properties -> Java Compiler = 21**.
3. Export: **File -> Export -> Deployable plug-ins and fragments** -> get 'HistoryHelper_<version>.jar'.

---

## Roadmap
- [] Additional optimized history mode (store only the last state of the record).
- [] Support for other DBMS (MySQL, Oracle).
- [] Add warnings for edge cases (e.g., trying to generate history for and existing history table).

---

## Issues and Features
- [Report a bug](https://github.com/spersics/historyhelper/issues/new?template=bug_report.md)
- [Suggest a feature](https://github.com/spersics/historyhelper/issues/new?template=feature_request.md)

---

## License
This project is distributed under a custom license - see [LICENSE](LICENSE).

---
## Author

**Artem Belousov**
[@spersics](https://github.com/spersics)
[E-mail](timbelousovv@gmail.com)

Built for [DBeaver](https://dbeaver.io/)

---