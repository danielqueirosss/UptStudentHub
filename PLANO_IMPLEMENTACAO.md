# Mar e praias

Daniel Neto · 51602 · Variante 3.

Implementação em 12 etapas. 
 1  Identificação do aluno, applicationId.
 2  Modelos imutáveis, praias, definições, ExplorerApi e ExplorerParser
 3  ExplorerViewModel, rede em fundo, estados, cancelamento e proteção contra resultados antigos
 4  Botão inicial, ExplorerActivity, ListView, ItemAdapter com ViewHolder, ícones e seleção da praia
 5  DetailActivity, segundo pedido, correspondência da hora, favoritos e devolução de resultados
 6  SettingsActivity, validação das quatro definições e recarregamento por resultado
 7  CompareActivity, pool de três threads, comparação lado a lado e falhas parciais
 8  Dois painéis em paisagem sem Fragments; seleção, posição e estado preservados na rotação
 9  NetworkCallback, faixa offline, recuperação, Handler periódico e receiver de bateria
 10  Revisão de menus e Up, três animações XML, recursos, Javadoc e logs de ciclo de vida
 11  Único bónus: tema escuro completo com values-night, incluindo módulos existentes
 12  Validação final, correções, mapa RF/RT, relatório, guião do vídeo, APK e preparação do ZIP

## Etapa 2 — modelos e dados

- `Beach`: Matosinhos, Póvoa de Varzim e Nazaré, com coordenadas do enunciado.
- `ExplorerSettings`: limiar 0,5–5,0 m, intervalo de 1/3 h, previsão de 1/2 dias. Atualização entre 1 e 60 minutos (decisão de desenho, pois o enunciado não fixa limites). Valores iniciais: 1,5 m, 1 h, 2 dias e 5 minutos.
- `MarineHour` e `WeatherHour`: campos finais; medições indisponíveis representadas por `Double` nulo. Favoritos identificados por praia + data/hora.
- `ExplorerApi`: URLs com Uri.Builder para lista, detalhe, comparação atual e erro HTTP de debug. WeatherApi delega nesta classe para manter o módulo 2 e centralizar os URLs.
- `ExplorerParser`: org.json, arrays paralelos validados, nulos com isNull, lista vazia distinta de JSON inválido, detalhe por correspondência exata da hora e verificação de interrupção. `ExplorerTime` valida o calendário sem exigir API 26.
- Removida a dependência direta activity-ktx; a aplicação continua em Java e usa as dependências AndroidX necessárias por appcompat.
- Consulta manual à API em 06/11/2026: as três praias devolveram 48 horas e condições atuais; sem nulos nos quatro campos marítimos. Não foi necessário ajustar coordenadas. Esta consulta verifica o serviço, não substitui a execução do parser na aplicação.
- Referência da API: https://open-meteo.com/en/docs/marine-weather-api (condições atuais disponíveis através de current).
- Ainda sem novos ecrãs; ligação ao ViewModel na etapa 3 e à lista na etapa 4. Sem acrescentar o bónus de testes automáticos.
- Validação: assembleDebug e lintDebug concluídos com sucesso; 0 erros, 12 avisos e 0 HardcodedText. Execução do parser na aplicação e testes em emulador pendentes da integração dos ecrãs.

