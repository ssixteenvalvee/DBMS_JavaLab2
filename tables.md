car_models                car_instances
┌──────────────────┐      ┌──────────────────────┐
│ id           PK  │──┐   │ id              PK   │
│ brand            │  └──<│ model_id        FK   │
│ model            │      │ vin                  │
│ release_date     │      │ color                │
│ base_price       │      │ mileage              │
└──────────────────┘      │ price                │
▲                         │ is_available         │
│                         └──────────────────────┘
│                         ▲
│ idx_car_models_brand    │ idx_car_instances_price
│ idx_car_models_model    │ idx_car_instances_model_id