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

ETAPA 1
Identificação do projeto: colocado o teu nome e número de aluno, alterado o identificador da aplicação e traduzidos os textos para português.

ETAPA 2
Preparação dos dados: criadas as classes das praias, previsões e definições; preparados os pedidos à Open-Meteo e a leitura das respostas, incluindo dados em falta.

ETAPA 3
Gestão do carregamento: implementados os pedidos em segundo plano, os estados de carregamento/erro/cancelamento e a proteção contra respostas antigas. Preparados também favoritos e definições em memória.