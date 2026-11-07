src/main/java/ru/youruniversity/autosalon/
│
├── core/                  // Внутренняя логика СУБД
│   ├── model/             // Сущности данных и Метаданные
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
│       └── ExecutionResult.java// Результат для GUI (успех/ошибка, список Row для отображения)
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
