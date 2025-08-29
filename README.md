# HistoryHelper for DBeaver
**HistoryHelper** - плагин для [DBeaver](https://dbeaver.io/), который генерирует SQL для создания history-таблицы и также триггеры (INSERT/UPDATE/DELETE), помогает быстро включать аудит изменений и при желании сразу применять скрипт к выбранной таблице.

---

## Возможности
- Выбор колонок, которые нужно логировать.
- Генерация триггеров **ON INSERT / ON UPDATE / ON DELETE**.
- Два режима хранения истории: "полная" и "только предыдущее состояние" (В разработке)
- Просмотр сгенерированного SQL и **применение** его прямо из DBeaver
- Фильтрация системных колонок Postgres (При генерации возможно выбрать только пользовательские колонки)
- Удобное диалоговое окно + иконка команды в контекстном меню таблицы.

---

## Совместимость
- **DBeaver:** 25.x
- **Java Runtime (JRE):** 17+
- **СУБД:** PostgreSQL (совместимость для других СУБД - в разработке)

---

## Установка
1. Скачайте последний JAR из раздела **Releases** этого репозитория.
2. Поместите файл в папку 'plugins' или 'dropins' установленного DBeaver:
   - **Windows:** '<DBeaver>\plugins\' или '<DBeaver>\dropins\'
   - **Linux/macOS:** '<DBeaver>/plugins/' или '<DBeaver>/dropins/'
3. Перезапустите DBeaver с очисткой кэша (один из вариантов: "dbeaver.exe -clean -clearPersistedState")
4. Проверьте установку:
   - Для Русскоязычной версии DBeaver: **Справка -> Информация об установке -> Плагины** -> должен быть 'HistoryHelper'
   - Для англоязычной версии DBeaver: **Help -> Installation Details -> Plug-ins** -> должен быть 'HistoryHelper'.


---

## Использование
1. В **Database Navigator** щёлкните правой кнопкой мыши по таблице.
2. Выберите команду **Generate History Table**.
3. В диалоговом окне:
   - отметьте нужные колонки;
   - выберите триггеры (INSERT/UPDATE/DELETE)
   - при необходимости переключите режим хранения (TBA)
4. Нажмите **ОК** -> откроется сгенерированный SQL с history-таблицей и триггерами.
Дальше можно:
- **Применить** скрипт к БД. !После применение скрипт также скопируется в буфер обмена!
- **Скопировать** его в буфер обмена. (Удобня функция при использовании Liquibase на проекте)

---

## Сборка из исходников (Eclipse PDE)
1. Импортируйте проект как **Existing Plug-ins** / обычный Java-плагин.
2. Убедитесь, что **Project -> Properties -> Java Compiler = 21**.
3. Экспорт: **File -> Export -> Deployable plug-ins and fragments** -> получите 'HistoryHelper_<version>.jar'.

---

## Roadmap
- [] Доп. опция оптимизированного хранения (только последнее состояние записи).
- [] Поддержка других СУБД (MySQL, Oracle).
- [] Локализация UI (RU/EN)
- [] Добавление условий для выпадающего меню. Команда **Generate History Table** доступна только при нажатии правой кнопкой мыши на таблицы
- [] Добавление предупреждений, при исключительных сценариях (попытки создания history-таблиц для уже существующих history-таблиц и т.д.)

---

## Баги и фичи
- [Сообщить об ошибке (Bug report)](https://github.com/spersics/historyhelper/issues/new?template=bug_report.md)
- [Предложить улучшение (Feature request)](https://github.com/spersics/historyhelper/issues/new?template=feature_request.md)

---

## Лицензия
Проект распространяется по кастомной лицензии - см. [LICENSE](LICENSE).

---
## Автор

**Artem Belousov** 
[@spersics](https://github.com/spersics)
[E-mail](timbelousovv@gmail.com)

Ссылка на DBeaver: [DBeaver](https://dbeaver.io/)

---
