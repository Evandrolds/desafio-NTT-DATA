# 🏦 Find My Agency API

API de geolocalização e cadastro de agências bancárias.  
Permite localizar as agências mais próximas com base na latitude e longitude informadas pelo usuário, integrando dados externos da **Overpass API** e armazenando no banco local.

---

## 📘 Sumário

- [🚀 Visão Geral](#-visão-geral)
- [🧩 Tecnologias Utilizadas](#-tecnologias-utilizadas)
- [📦 Estrutura do Projeto](#-estrutura-do-projeto)
- [⚙️ Configurações Principais](#️-configurações-principais)
- [🗺️ Endpoints da API](#️-endpoints-da-api)
- [📄 Modelos de Dados (DTOs)](#-modelos-de-dados-dtos)
- [💾 Entidade](#-entidade)
- [🌍 Integração com API Externa](#-integração-com-api-externa)
- [🧱 Tratamento de Erros](#-tratamento-de-erros)
- [📜 Licença](#-licença)
- [📫 Contato](#-contato)

---

## 🚀 Visão Geral

A **Find My Agency API** é um serviço RESTful desenvolvido em **Java 21 + Spring Boot 3** que permite:

- Buscar agências bancárias mais próximas com base na geolocalização.
- Cadastrar novas agências manualmente.
- Consultar e armazenar dados de agências da **Overpass API**.
- Aplicar paginação e cache em buscas.
- Utilizar resiliência em chamadas externas com **Resilience4j**.

---

## 🧩 Tecnologias Utilizadas

| Tecnologia | Versão / Descrição |
|-------------|--------------------|
| ☕ Java | 21 |
| 🧱 Spring Boot | 3.x |
| 🗄️ Spring Data JPA | Persistência de dados |
| 🧭 OpenAPI / Swagger | Documentação automática |
| 🔄 Resilience4j | Retry, CircuitBreaker |
| 🌍 Overpass API | Dados de agências bancárias (OSM) |
| 🧮 Micrometer | Métricas |
| 🧰 Lombok (opcional) | Redução de boilerplate |
| 🧠 H2 / PostgreSQL | Banco de dados local (ajustável) |

---

## 📦 Estrutura do Projeto

src/main/java/com/evandro/ntt_data/desafio/
│
├── configuration/
│ ├── ApiExternaConfiguration.java
│ ├── BeansConfig.java
│ └── ConfigOpenAPI.java
│
├── controller/
│ └── LocalizaAgenciaController.java
│
├── entity/
│ └── Agencia.java
│
├── dto/
│ ├── AgenciaRequest.java
│ ├── AgenciaResponse.java
│ └── PageResponse.java
│
├── repository/
│ └── LocalizaAgenciaRepository.java
│
├── service/
│ ├── LocalizaAgenciaService.java
│ ├── LocalizaAgenciaServiceImpl.java
│ ├── AgenciaExternaService.java
│ └── AgenciaMapper.java
│
└── handler/
└── ApiExceptionHandler.java


---

## ⚙️ Configurações Principais

### 🔧 `ApiExternaConfiguration`
Responsável por gerar a query utilizada para buscar dados na Overpass API:

```java
public String getReturnQuery(double raio, double lat, double lon) {
    return String.format(Locale.US,
        "[out:json];node[\"amenity\"=\"bank\"](around:%d,%.6f,%.6f);out;",
        (int)(raio * 1000), lat, lon);
}
````

🧱 BeansConfig

Define beans básicos:

ObjectMapper → conversão JSON

RestTemplate → consumo da API externa (com cabeçalho User-Agent padrão)

📘 ConfigOpenAPI

Configuração da documentação OpenAPI/Swagger.

```` Java
@OpenAPIDefinition(
    info = @Info(
        title = "Find My Agency API",
        version = "1.0.0",
        description = "API de geolocalização e cadastro de agências bancárias.",
        contact = @Contact(
            name = "✔ Contato: Evandro",
            email = "evandro.lima@empresa.com",
            url = "https://linkedin.com/in/evandrolds"
        ),
        license = @License(
            name = "MIT License",
            url = "https://opensource.org/licenses/MIT"
        )
    )
)
````
🗺️ Endpoints da API
POST /api/agencia/cadastrar

Descrição: Cadastra uma nova agência manualmente.

Request Body
```` java
{
  "name": "Banco XPTO",
  "latitude": -23.55052,
  "longitude": -46.633308,
  "address": "Av. Paulista, 1000 - São Paulo/SP"
}
````
Resposta

```` java
{
  "id": 1,
  "name": "Banco XPTO",
  "positionX": -23.55052,
  "positionY": -46.633308,
  "address": "Av. Paulista, 1000 - São Paulo/SP",
  "distanceKm": null,
  "externalId": null
}
````
GET /api/agencia/closest

Descrição: Busca as agências mais próximas da localização informada (com paginação).

Parâmetros:

Nome	Tipo	Obrigatório	Default	Descrição
latitude	double	✅	-	Latitude atual
longitude	double	✅	-	Longitude atual
maxDistanceKm	double	❌	50	Raio máximo (km)
page	int	❌	0	Página atual
size	int	❌	10	Tamanho da página

Exemplo de Requisição
```` GET /api/agencia/closest?latitude=-23.55&longitude=-46.63&maxDistanceKm=10&page=0&size=5 ````
Response 200
```` java
{
  "content": [
    {
      "id": 1,
      "name": "Banco do Brasil",
      "positionX": -23.551,
      "positionY": -46.633,
      "address": "Av. Paulista, 1500 - São Paulo/SP",
      "distanceKm": 0.25,
      "externalId": "123456789"
    }
  ],
  "pageSize": 5,
  "totalElements": 20,
  "totalPages": 4,
  "last": false
}
````
📄 Modelos de Dados (DTOs)
AgenciaRequest
Campo	Tipo	Validação	Descrição
name	String	@Size(min=2,max=200)	Nome da agência
latitude	Double	@DecimalMin(-90) / @DecimalMax(90)	Latitude
longitude	Double	@DecimalMin(-180) / @DecimalMax(180)	Longitude
address	String	-	Endereço
distancia	Double	-	Distância (opcional)

💾 Entidade
Agencia

Tabela: tb_agencia

Coluna	Tipo	Descrição
id	Long (PK)	Identificador
name	String	Nome
latitude	Double	Latitude
longitude	Double	Longitude
address	String	Endereço
externalId	String (único)	ID da Overpass API
distance	Double	Distância calculada

🌍 Integração com API Externa

A classe AgenciaExternaService faz chamadas para a Overpass API com resiliência:

Retry → Tenta novamente em caso de falha temporária.

CircuitBreaker → Evita sobrecarregar a API em falhas consecutivas.

Fallback → Retorna agências locais do banco caso a Overpass falhe.

Exemplo de Query Gerada:
```` [out:json];node["amenity"="bank"](around:10000,-23.55052,-46.633308);out;````

Exemplo de Fallback:
⚠️ Fallback acionado: Overpass API falhou (TimeoutException)
→ Retornando até 10 agências armazenadas localmente.

🧱 Tratamento de Erros

Classe: ApiExceptionHandler

Exceção	Código HTTP	Descrição
MethodArgumentNotValidException	400	Erro de validação nos campos
Exception	500	Erro interno inesperado

Caso a Overpass esteja indisponível:
```` json
[
  "latitude: must not be null",
  "longitude: must not be null"
]
````
📜 Licença:
Licença MIT — veja o arquivo LICENSE

📫 Contato:
**Evandro Lima**  
💼 [LinkedIn](https://www.linkedin.com/in/seu-perfil-linkedin)  
📧 [E-mail](mailto:seuemail@exemplo.com)
📂 [Repositório GitHub](https://github.com/Evandrolds/desafio-NTT-DATARepositório)

💡 Dica:
A documentação completa da API (Swagger UI) pode ser acessada em:
👉 http://localhost:8080/swagger-ui/index.html

