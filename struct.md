src/
│
├── core/                  // Внутренняя логика СУБД
│   ├── logic/             // Сущности данных и Метаданные
│   │   ├── Database.java  // Хранит таблицы (cars и car_details)
│   │   ├── Table.java     // Хранит строки, схему колонок и индексы
│   │   ├── Column.java    // Метаданные (name, DataType, isPrimaryKey)
│   │   ├── Row.java       // Запись таблицы: Map<String, Object> (поля и их значения)
│   │   └── DataType.java  // Enum (INT, FLOAT, STRING, DATE, BOOLEAN)
│   │
│   ├── index/             // Индексирование для повышенной оценки
│   │   ├── Index.java     // Интерфейс индекса
│   │   └── TreeMapIndex.java // Индекс на TreeMap для быстрого поиска (например, по Price или VIN)
│   │
│   └── service/           // Движок выполнения операций
│       ├── DatabaseEngine.java // Принимает команды, проверяет PK/FK, меняет состояние таблиц
│       └── QResult.java// Результат для GUI (успех/ошибка, список Row для отображения)
│
├── parser/                // Парсер SQL-запросов (Паттерн Команда)
│   ├── ast/               // Абстрактное синтаксическое дерево запросов
│   │   ├── Command.java   // Базовый интерфейс
│   │   ├── SelectCommand.java
│   │   ├── InsertCommand.java
│   │   ├── UpdateCommand.java
│   │   └── DeleteCommand.java
│   └── QueryParser.java   // Превращает строку в объект Command с помощью Regex/Токенов
│
├── storage/               // Сериализация в JSON
│   ├── StorageManager.java// Интерфейс (save/load)
│   └── JsonStorageManager.java // Сохраняет таблицы в cars.json и car_details.json
│
└── ui/                    // Графический интерфейс
    ├── App.java           // Главный класс запуска программы
    ├── MainController.java// Контроллер: берет текст из UI -> в парсер -> в движок -> обновляет UI
    └── MainView.java      // Окно с текстовым полем для SQL и JTable/TableView для вывода

=============================================

[ Запрос пользователя ]
│
▼
[QueryParser] ──(создает команду)──> [Database]
                                            │
                                (находит нужную таблицу)
                                            │
                                            ▼
                                        ┌───────────┐
                                        │   Table   │
                                        └─────┬─────┘
                                                │
                    ┌──────────────────────────┼──────────────────────────┐
                    ▼                          ▼                          ▼
                    [ Схема: Column ]           [ Данные: Row ]           [ Индексы: Index ]
                    Проверяет типы данных.      Ищет совпадения по ID.     Обновляет/ищет строки
                    Превращает текст в Java-     Добавляет/удаляет строки   в TreeMap за O(log n).
                    объекты (INT, DATE...).     из внутреннего списка.

===============================================
Row:      LinkedHashMap<String, Object>          // поле → значение
Table.indexes: LinkedHashMap<String, Index>      // имя колонки → индекс
Index (TreeIndex): TreeMap<Object, ArrayList<Row>>  // значение поля → строки с этим значением

поле row и имя колонки должны быть одинаковыми:
scheme: { "id" → Column, "brand" → Column, "price" → Column }
row:    { "id" → 1,      "brand" → "Toyota", "price" → 25000.0 }
↑ одно и то же имя

================================================
Парсер возвращает ArrayList<InsertCmd>, а контроллер в цикле зовёт engine.insert(...).
С остальными Cmd такого нет. Это обусловлено тем, что в Insert должен поддерживать множественную вставку Row,
но движок вставляет по одной строке. В случае для UpdateCmd/DeleteCmd/SelectCmd у них есть условие WHERE, а
оно в свою очередь раскрывается в resolveWhere.

Всё сохраняется после вставки, не в самом Database Engine, а в контроллере: прошёл цикл/все операции
выполнены -> StorageManager.save(db).

