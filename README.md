🏦 Find My Agency API - Documentação
📋 Sobre a API

API de geolocalização e cadastro de agências bancárias que permite encontrar as agências mais próximas com base na localização do usuário, utilizando cálculos de distância por coordenadas geográficas.
🚀 Funcionalidades

    📍 Geolocalização Inteligente - Encontra agências mais próximas usando coordenadas GPS

    📊 Cadastro de Agências - Gerencia informações completas das agências

    🔍 Busca com Filtros - Filtra por distância máxima e paginação

    ⚡ Performance Otimizada - Cache e métricas para melhor performance

    📈 Monitoramento - Métricas e logging detalhado

🛠️ Tecnologias Utilizadas

    Java 17+ - Linguagem de programação

    Spring Boot 3.x - Framework principal

    H2 Database - Banco de dados em memória

    Spring Data JPA - Persistência de dados

    Micrometer - Métricas e monitoramento

    Spring Cache - Cache de resultados

    OpenAPI 3 - Documentação da API

📚 Endpoints da API
1. 🆕 Cadastrar Agência

POST /api/agencia/cadastrar

Cadastra uma nova agência no sistema.

Request Body:
json

{
"name": "string",
"address": "string",
"latitude": -23.5505,
"longitude": -46.6333
}

Response:
json

{
"id": 1,
"name": "Agência Centro",
"address": "Rua Principal, 123",
"latitude": -23.5505,
"longitude": -46.6333
}

2. 📍 Buscar Agências Próximas

GET /api/agencia/distancia

Encontra agências mais próximas ordenadas por distância.

Parâmetros:
Parâmetro	Tipo	Obrigatório	Default	Descrição
latitude	double	✅	-	Latitude do ponto de busca
longitude	double	✅	-	Longitude do ponto de busca
maxDistanceKm	Double	❌	50.0	Distância máxima em km (opcional)
page	int	❌	0	Página para paginação
size	int	❌	20	Tamanho da página (max: 100)

Exemplo de Request:
text

GET /api/agencia/distancia?latitude=-23.5505&longitude=-46.6333&maxDistanceKm=10&page=0&size=10

Response:
json

{
"content": [
{
"id": 1,
"name": "Agência Centro",
"address": "Rua Principal, 123",
"latitude": -23.5505,
"longitude": -46.6333,
"distanceKm": 0.5
}
],
"page": 0,
"size": 10,
"totalElements": 1,
"totalPages": 1
}

🔧 Configuração e Instalação
Pré-requisitos

    Java 17+

    Maven 3.6+

    Spring Boot 3.x

Configuração do Banco de Dados

A aplicação utiliza H2 Database em memória:
properties

spring.datasource.url=jdbc:h2:mem:agenciadb
spring.datasource.username=seu_usuario
spring.datasource.password=sua_senha
spring.h2.console.enabled=true

Console H2: http://localhost:8080/h2-console
Métricas e Monitoramento

A API expõe métricas via Spring Actuator:

    Health: /actuator/health

    Metrics: /actuator/metrics

    Prometheus: /actuator/prometheus

🎯 Algoritmo de Busca
Cálculo de Distância

Utiliza a Fórmula de Haversine para calcular distâncias entre coordenadas:
java

public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
// Implementação do algoritmo Haversine
// Retorna distância em quilômetros
}

Otimizações Implementadas

    Bounding Box Filter - Filtro inicial por área aproximada

    Cache de Resultados - Cache com @Cacheable para consultas repetidas

    Paginação - Controle de número de resultados

    Métricas - Timer para monitorar performance

📊 Estrutura do Projeto
text

src/
├── main/
│   ├── java/
│   │   └── com/evandro/ntt_data/desafio/
│   │       ├── controller/     # Controladores REST
│   │       ├── service/        # Lógica de negócio
│   │       ├── repository/     # Camada de dados
│   │       ├── domain/         # Entidades JPA
│   │       ├── dto/           # Objetos de transferência
│   │       └── util/          # Utilitários
│   └── resources/
│       └── application.properties

🔮 Exemplos de Uso
Exemplo 1: Buscar agências próximas ao centro de São Paulo
bash

curl "http://localhost:8080/api/agencia/distancia?latitude=-23.5505&longitude=-46.6333&maxDistanceKm=5&size=5"

Exemplo 2: Cadastrar nova agência
bash

curl -X POST http://localhost:8080/api/agencia/cadastrar \
-H "Content-Type: application/json" \
-d '{
"name": "Agência Paulista",
"address": "Av. Paulista, 1000",
"latitude": -23.5631,
"longitude": -46.6542
}'

👤 Contato

Evandro Lima
📧 evandro.lima@empresa.com
🔗 LinkedIn
🏢 Desenvolvedor Backend
📄 Licença

MIT License - Veja o arquivo LICENSE para detalhes.
🔗 Links Úteis

    Documentação Swagger: http://localhost:8080/swagger-ui.html

    OpenAPI Spec: http://localhost:8080/v3/api-docs

    H2 Console: http://localhost:8080/h2-console

    Health Check: http://localhost:8080/actuator/health

🐛 Troubleshooting
Logs

Para debug, configure o logging:
properties

logging.level.com.evandro.ntt_data.desafio=DEBUG

Cache

O cache é automaticamente gerenciado pelo Spring Cache. Para limpar:

    Reinicie a aplicação ou

    Configure TTL no cache

⭐ Se este projeto foi útil, deixe uma estrela no repositório!