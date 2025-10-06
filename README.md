API de Localização de Agências Bancárias.

📋 Visão Geral:

API REST para localização de agências bancárias com capacidade de buscar tanto em banco de dados local quanto em API externa (Overpass), ordenando resultados por proximidade geográfica.
🚀 Endpoints
1. Cadastrar Agência:

POST /api/agencia/cadastrar

Cadastra uma nova agência bancária no banco de dados.
Request Body:

  json
  
  {
    "name": "string (2-200 caracteres)",
    "latitude": "number (-90.0 a 90.0)",
    "longitude": "number (-180.0 a 180.0)",
    "address": "string",
    "distancia": "number"
  }

Response (201 Created):
json

{
  "id": "long",
  "name": "string",
  "positionX": "number",
  "positionY": "number",
  "address": "string",
  "distacia": "number"
}

2. Buscar Agências Próximas:

GET /api/agencia/agencias/closest

Busca agências bancárias próximas a uma localização, ordenadas por proximidade.
Parâmetros de Query:
Parâmetro	Tipo	Obrigatório	Default	Descrição
latitude	double	✅	-	Latitude do ponto de referência
longitude	double	✅	-	Longitude do ponto de referência
maxDistanceKm	double	❌	50	Raio máximo de busca em km
page	int	❌	0	Número da página para paginação
size	int	❌	10	Tamanho da página para paginação
Response (200 OK):
json

{
  "content": [
    {
      "id": "long",
      "name": "string",
      "positionX": "number",
      "positionY": "number",
      "address": "string",
      "distanceKm": "number",
      "externalId": "string"
    }
  ],
  "pageNumber": "int",
  "pageSize": "int",
  "totalElements": "long",
  "totalPages": "int",
  "last": "boolean"
}

🔄 Fluxo de Busca

A API implementa uma estratégia de busca em duas camadas:
1. Busca Local (Banco de Dados)

    Consulta agências no banco de dados local

    Filtra por proximidade usando cálculo de distância Haversine

    Retorna resultados ordenados por distância

2. Busca Externa (Overpass API)

    Se não encontrar resultados locais, consulta a API Overpass

    Busca por nodes com amenity=bank no raio especificado

    Processa e formata os dados da resposta

    Armazena no banco local para futuras consultas

🛠️ Tecnologias Utilizadas

    Java 21

    Spring Boot 3.5.6

    Spring Data JPA - Persistência de dados

    H2 Database - Banco em memória para testes

    SpringDoc OpenAPI 2.8.13 - Documentação da API

    Spring HATEOAS - Paginação e hypermedia

    Spring Cache - Cache de consultas

    Spring Actuator + Micrometer - Métricas e monitoramento

    Lombok - Redução de código boilerplate

    Mockito - Testes unitários

📐 Cálculo de Distância

Utiliza a fórmula de Haversine para calcular distâncias geográficas:
java

// Raio da Terra em km
private static final double raioDaTerraEmKm = 6371.0088;

💾 Estrutura de Dados
Tabela: tb_agencia
Campo	Tipo	Descrição
id	BIGINT	ID único (auto-increment)
name	VARCHAR	Nome da agência
latitude	DOUBLE	Coordenada latitude
longitude	DOUBLE	Coordenada longitude
address	VARCHAR	Endereço completo
externalId	VARCHAR	ID externo da Overpass API
distance	DOUBLE	Distância calculada
⚡ Funcionalidades Avançadas
Cache

    Consultas de proximidade são cacheadas usando @Cacheable

    Chave do cache: {latitude, longitude, maxDistanceKm, page, size}

Paginação

    Suporte a paginação para grandes conjuntos de resultados

    Parâmetros page e size controlam a paginação

Validação

    Validação de coordenadas geográficas (-90 a 90 para latitude, -180 a 180 para longitude)

    Validação de tamanho do nome (2-200 caracteres)

Logs

    Logs detalhados usando SLF4J

    Rastreamento de consultas locais e externas

🔍 Exemplo de Uso
Buscar agências próximas:
bash

GET /api/agencia/agencias/closest?latitude=-23.5505&longitude=-46.6333&maxDistanceKm=10&page=0&size=5

Cadastrar nova agência:
bash

POST /api/agencia/cadastrar
Content-Type: application/json

{
  "name": "Agência Centro",
  "latitude": -23.5505,
  "longitude": -46.6333,
  "address": "Rua XV de Novembro, 100 - Centro, São Paulo/SP",
  "distancia": 0.0
}

🚦 Considerações

    A API prioriza dados locais sobre consultas externas

    Dados da Overpass API são armazenados localmente após a primeira consulta

    O cálculo de distância é aproximado usando fórmula esférica

    Recomenda-se uso de índices espaciais no banco para melhor performance
