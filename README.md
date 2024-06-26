## Запуск docker контейнера

```docker-compose -f docker-compose.yml up```

---


## Пакеты
- ```elastic``` - 8.8.0
- ```kibana``` - 8.8.0
- ```log4j``` - 2.13.0
- ```jackson``` - 2.17.0
- ```rabbitmq``` - 5.2.0
- ```apache``` - 4.5.13
- ```jsoup``` - 1.15.3
- ```logstash``` - 8.8.0
- ```filebeat``` - 8.8.0

## Принцип работы

- Сначала подключается Elastic и создает/проверяет наличие индекса
- Запускаются два потока с NewsParser, обрабатывающие очереди в RabbitMQ и в случае нахождения в очереди ссылок обрабатывают их до начала работы Main Page Linker. Работают до выхода из программы
- В бесконечном цикле пользователь опрашивается о необходимости парсинга главной страницы (Main page linker). В случае отказа, все потоки, кроме основного завершаются и предлагается выполнить блок запросов с агрегацией

## Архитектура проекта

- ```utils```
  -  RequestUtils - класс для работы с запросами
  - Settings - класс с настройками и константами
- ```runnables``` -  запускаемые в потоках
  - ElasticWorker - работа с elastic
  - MainPageLinkFetcher - парсинг главной страницы и упаковка в очередь ссылок
  - NewsParser - работа с очередью ссылок, спаршенных новостей, хэшами и отправка в elastic 
- ```dataClasses``` 
  - NewsData - датакласс для работы со спаршенной новостью
  - UrlData - датакласс для работы с ссылками
- ```resources``` - настройка логгера

## Пример работы

#### Fetcher

![news](./images/news.png "logs")

![news_dashboard](./images/news_dashboard.png "logs")

#### Logstash + filebeat

![logs](./images/logs.png "Logstash")

![logs_dashboard](./images/logs_dashboard.png "Logstash")

#### RabbitMQ
**_producerLinks_**
![producerLinks](./images/producerLinks.png "RabbitMQ")

**_parsedData_**
![parsedData](./images/parsedData.png "RabbitMQ")
