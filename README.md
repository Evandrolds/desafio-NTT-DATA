API de Localização de Agências Bancárias
📋 Visão Geral

API REST para localização de agências bancárias desenvolvida em Spring Boot 3.5.6 com Java 21. A API busca agências em banco de dados local e, caso não encontre, consulta a API externa Overpass, retornando resultados ordenados por proximidade geográfica.
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

🚀 Endpoints
1. Cadastrar Agência

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

2. Buscar Agências Próximas

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
1. Busca Local (Banco H2)

    Consulta agências no banco H2 local

    Filtra por proximidade usando cálculo de distância Haversine

    Retorna resultados ordenados por distância

2. Busca Externa (Overpass API)

    Se não encontrar resultados locais, consulta a API Overpass

    Busca por nodes com amenity=bank no raio especificado

    Processa e formata os dados da resposta

    Armazena no banco local para futuras consultas

📐 Cálculo de Distância

Utiliza a fórmula de Haversine para calcular distâncias geográficas:
java

private static final double raioDaTerraEmKm = 6371.0088;

public static double distanceKm(double positionX, double positionY, double latitude, double longitude) {
    double latRad1 = Math.toRadians(positionX);
    double latRad2 = Math.toRadians(positionY);
    double deltaLat = Math.toRadians(latitude - positionX);
    double deltaLon = Math.toRadians(longitude - positionY);
    
    double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
            + Math.cos(latRad1) * Math.cos(latRad2)
            * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
    
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return raioDaTerraEmKm * c;
}

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
⚡ Funcionalidades
Cache

    Consultas cacheadas com @Cacheable

    Chave: {latitude, longitude, maxDistanceKm, page, size}

Paginação

    Implementada com Spring HATEOAS

    Parâmetros page e size para controle

Documentação

    Swagger UI disponível em /swagger-ui.html

    OpenAPI 3.0 em /v3/api-docs

Monitoramento

    Spring Actuator para health checks

    Micrometer para métricas de performance

Validação

    Validação de coordenadas geográficas

    Tamanho do nome: 2-200 caracteres

🔍 Exemplos de Uso
Buscar agências próximas:
bash

GET /api/agencia/agencias/closest?latitude=-23.5505&longitude=-46.6333&maxDistanceKm=10&page=0&size=5

Cadastrar agência:
bash

POST /api/agencia/cadastrar
Content-Type: application/json

{
  "name": "Agência Centro",
  "latitude": -23.5505,
  "longitude": -46.6333,
  "address": "Rua XV de Novembro, 100 - Centro, São Paulo/SP"
}

🚀 Execução
bash

# Compilar e executar
mvn spring-boot:run

# Acessar documentação
http://localhost:8080/swagger-ui.html

# Acessar banco H2 (se configurado)
http://localhost:8080/h2-console

📊 Características Técnicas

    Cache: Spring Cache com estratégia simples

    Paginação: Spring HATEOAS com PageResponse customizado

    Logging: SLF4J com logs detalhados

    Testes: Mockito para testes unitários

    Validação: Bean Validation com mensagens customizadas
