# Введение
Упрощённая система управления базами данных на Java. Поддерживает SQL-подобный язык запросов:
(INSERT, SELECT, UPDATE, DELETE, CREATE TABLE, DROP TABLE, CREATE INDEX). А так же сериализацию в JSON, вторичные индексы на `TreeMap`, поиск по id и GUI на Swing.
**Команда: Бухвалов Никита, Артём Дубинин, Арсений Филатов. (25КНТ-4)**

## Требования
JDK 21+
## Запуск
Скомпилировать ui/Main.java и запустить
Либо Run Main.java в IDE

Автоматически создается папка data/
Если она пуста, можно сразу добавить таблицы:
```sql
CREATE TABLE car_models (
    id INT PRIMARY KEY,
    brand STRING,
    model STRING,
    release_date DATE,
    base_price FLOAT
);

CREATE TABLE car_instances (
    id INT PRIMARY KEY,
    model_id INT,
    vin STRING,
    color STRING,
    mileage INT,
    price FLOAT,
    is_available BOOLEAN
);
```
# Архитектура
## Слои
```
┌─────────────────────────────────────────┐
│ ui (Swing GUI)                          │   JFrame, JTable, JTextArea
│   Main, Frame, Controller               │
└──────────────────┬──────────────────────┘
                   │ SQL-строка
                   ▼
┌─────────────────────────────────────────┐
│ ui.Controller                           │   координатор: parser → engine → save → gui
└─────┬───────────────────────┬───────────┘
      │                       │
      ▼                       ▼
┌──────────────┐         ┌───────────────────┐
│ parser       │         │ core.service      │
│  Lexer       │         │  DatabaseEngine   │   исполнение Command
│  QueryParser │         │  QResult          │
│  cmd.*       │         │                   │
└──────────────┘         └─────────┬─────────┘
                                   │
                                   ▼
┌─────────────────────────────────────────┐
│ core.logic                              │   модель данных
│   Database, Table, Row, Column,         │
│   DataType                              │
├─────────────────────────────────────────┤
│ core.index                              │   индексирование
│   Index, TreeIndex                      │
└─────────────────────────────────────────┘
                   ▲
                   │
┌─────────────────────────────────────────┐
│ storage                                 │   сериализация
│   StorageManager (interface)            │
│   JsonStorageManager                    │
└─────────────────────────────────────────┘
```
## Диаграмма
```
Database ──1..*── Table
                    │
                    ├─1..*── Column  (colName, DataType, isPK)
                    ├─0..*── Row     (LinkedHashMap<String,Object>)
                    └─0..*── Index ──implements── TreeIndex
                                                       │
                                                       └─ TreeMap<Object, List<Row>>

Command (interface)
   ├── InsertCmd   (tableName, columns, Row values)
   ├── SelectCmd   (tableName, columns, whereCol, whereOp, whereVal)
   ├── UpdateCmd   (tableName, setValues, whereCol, whereOp, whereVal)
   ├── DeleteCmd   (tableName, whereCol, whereOp, whereVal)
   ├── CreateTableCmd (tableName, scheme)
   ├── DropTableCmd   (tableName)
   └── CreateIndexCmd (indexName, tableName, column)

QResult (success, msg, rows, scheme)

DatabaseEngine ──uses── Database
               ──uses── StorageManager
               ──uses── Command
               ──returns── QResult

Controller ──uses── QueryParser
               ──uses── DatabaseEngine
               ──uses── JsonStorageManager
               ──uses── Frame

StorageManager (interface)
   └── JsonStorageManager   (save/load *.json)
```
## Последовательности
Пример `INSERT INTO ... VALUES (...)`:
```
MainController ── parse ──► QueryParser ── InsertCmd ──► DatabaseEngine.insert()
                                                            │
                                                            ├─ db.getTable()
                                                            ├─ проверка колонок и типов
                                                            ├─ Row r = new Row(...)
                                                            └─ t.addRow(r)
                                                                  ├─ нормализация (null для отсутствующих)
                                                                  ├─ PK: автоинкремент / уникальность
                                                                  └─ обновление всех индексов
MainController ── storage.save(db)   (один раз после пачки команд)
```
 Пример `SELECT * FROM car_models WHERE brand = 'Toyota'`:
 ```
 Пользователь                MainController         QueryParser        DatabaseEngine        Table
     │                          │                      │                     │                 │
     │ Ctrl+Enter               │                      │                     │                 │
     ├─────────────────────────►│                      │                     │                 │
     │                          ├─ parse(sql) ────────►│                     │                 │
     │                          │◄─ List<SelectCmd> ───┤                     │                 │
     │                          ├─ execute(cmd) ─────────────────────────────►│                 │
     │                          │                      │                     ├─ getTable() ───►│
     │                          │                      │                     │◄── Table ───────┤
     │                          │                      │                     ├─ resolveWhere()─┤
     │                          │                      │                     │  (= + index?)   │
     │                          │                      │                     │◄── List<Row> ───┤
     │                          │                      │                     ├─ project() ─────┤
     │                          │                      │                     │◄── List<Row> ───┤
     │                          │◄─ QResult ──────────────────────────────── │                 │
     │                          ├─ showTable(QResult) ──►│ (JTable)          │                 │
     │◄─────────────────────────┤                      │                     │                 │
 ```
# Инструкция
Окно:
``` 
┌──────────────────────────────────────────────────────────────┐
│ SELECT * FROM car_models;                                    │  ← SQL-редактор
│                                                              │
│                                          [Clear]  [Run]      │
├──────────────────────────────────────────────────────────────┤
│  id  │ brand  │ model  │ release_date │ base_price           │  ← JTable
│  1   │ Toyota │ Camry  │ 2023-01-15   │ 25000.0              │
│  2   │ BMW    │ X5     │ 2022-11-10   │ 55000.0              │
├──────────────────────────────────────────────────────────────┤
│  2 row(s)                                                    │  ← статус-бар
└──────────────────────────────────────────────────────────────┘
```
Поддерживаются подобные запросы:
```sql
-- CREATE TABLE (обязательно колонка id INT PRIMARY KEY)
CREATE TABLE car_models (
    id INT PRIMARY KEY,
    brand STRING,
    model STRING,
    release_date DATE,
    base_price FLOAT
);

-- DROP TABLE
DROP TABLE car_models;

-- CREATE INDEX
CREATE INDEX idx_car_models_brand ON car_models (brand);

-- INSERT (все колонки позиционно)
INSERT INTO car_models VALUES (1, 'Toyota', 'Camry', '2023-01-15', 25000.0);

-- INSERT (частичный — только указанные колонки; остальные → null)
INSERT INTO car_models (id, brand, model) VALUES (2, 'BMW', 'X5');

-- INSERT нескольких строк
INSERT INTO car_models VALUES
    (3, 'Audi', 'A4', '2024-03-01', 45000.0),
    (4, 'Kia',  'Rio','2023-07-20', 18000.0);

-- SELECT
SELECT * FROM car_models;
SELECT brand, base_price FROM car_models WHERE base_price > 30000;
SELECT * FROM car_models WHERE brand = 'Toyota';

-- UPDATE
UPDATE car_models SET base_price = 26000 WHERE id = 1;
UPDATE car_models SET base_price = 27000, model = 'Camry XLE' WHERE brand = 'Toyota';

-- DELETE
DELETE FROM car_models WHERE brand = 'Toyota';
DELETE FROM car_models;   -- удалить все
```
## Ограничения
- PK всегда `<"id", INT>`, immutable. Нельзя CREATE TABLE не указывая PK.
- WHERE Содержит одно условие. Без AND/OR/NOT
- Operators: = / != / < / > / >= / <=
- Дата  в ISO-Формате: `'YYYY-MM-DD'`, в одинарных кавычках.
- Двойные кавычки трактуются как строка. Имена таблиц/колонок указываются без кавычек.
- No JOIN. Связь по ключу через две команды SELECT
- CREATE TABLE cars - Error. Нужно указать все колонки, которые в ней будут.
- После выполнения каждой команды автоматически вызывается сохранение.
Пример:
```sql
CREATE TABLE car_models (
    id INT PRIMARY KEY, brand STRING, model STRING,
    release_date DATE, base_price FLOAT
);
INSERT INTO car_models VALUES (1, 'Toyota', 'Camry', '2023-01-15', 25000.0);
INSERT INTO car_models VALUES (2, 'BMW',    'X5',   '2022-11-10', 55000.0);

CREATE TABLE car_instances (
    id INT PRIMARY KEY, model_id INT, vin STRING, color STRING,
    mileage INT, price FLOAT, is_available BOOLEAN
);
INSERT INTO car_instances VALUES (10, 1, 'VIN001', 'White', 15000, 24000.0, TRUE);
INSERT INTO car_instances VALUES (11, 1, 'VIN002', 'Black', 20000, 23000.0, TRUE);
INSERT INTO car_instances VALUES (12, 2, 'VIN003', 'Red',   5000,  54000.0, FALSE);

SELECT id FROM car_models WHERE brand = 'Toyota';    -- → 1
SELECT * FROM car_instances WHERE model_id = 1;      -- → VIN001, VIN002
```
# Вклад участников:
## Бухвалов Никита
Выполнено:
- Пакет Core
- Пакет ui
- Пакет Controller
## Дубинин Артём
Выполнено:
- Пакет parser
- Правки в Core (Индексирование по ID)
- Тестирование
## Филатов Арсений
Выполнено:
- Интерфейс StorageManager
- class JsonStorageManger