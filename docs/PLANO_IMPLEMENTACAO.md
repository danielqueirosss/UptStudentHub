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

ETAPA 4
Interface do Explorer: botão no ecrã inicial com validação do nome, cumprimento ao estudante e lista de previsões reais com ItemAdapter e ViewHolder. Cada linha mostra data/hora, altura, período e direção da onda, temperatura da água e ícone conforme o limiar de surf.
Inclui carregamento automático, estados visíveis, Atualizar/Cancelar, menu de escolha exclusiva da praia, Sobre, navegação Up e animação de entrada da lista. Os dados anteriores continuam identificados após cancelamento ou erro. Foram corrigidas as margens, a seleção do menu e a ActionBar noturna necessária à navegação.

ETAPA 5 
Detalhe e favoritos: tocar numa linha abre DetailActivity com os dados marítimos em extras. DetailViewModel faz o segundo pedido em fundo e seleciona vento, direção do vento e temperatura do ar da hora exata. O cartão tem animação de entrada, mensagens de erro e botão para repetir o pedido. DetailContract trata a passagem dos dados e ExplorerFormat mantém a apresentação consistente com a lista.
Marcar/desmarcar favorito prepara um resultado com a chave praia + data/hora, devolvido por startActivityForResult/setResult ao regressar por Up ou Back. A lista atualiza a estrela correta. Remover exige AlertDialog e as alterações dão feedback por Toast. Estado do favorito e confirmação pendente sobrevivem à rotação; o ViewModel conserva o pedido e os dados. O ciclo de vida do detalhe é registado no Logcat.

ETAPA 6
Definições: SettingsActivity permite escolher limiar da onda (0,5–5,0 m), intervalo da lista (1 ou 3 horas), previsão de 1 ou 2 dias e atualização periódica (1–60 minutos). Usa EditText, RadioGroup e CheckBox. Valida os campos, aceita vírgula ou ponto decimal e preserva o formulário em rotação. Guardar devolve as escolhas ao Explorer por resultado e recarrega a lista; Cancelar, Back e Up descartam as alterações. As definições ficam em memória; o ciclo periódico será ligado na etapa 9.

ETAPA 7
Comparação: CompareActivity mostra Matosinhos, Póvoa de Varzim e Nazaré lado a lado. CompareViewModel executa três pedidos em paralelo com um pool de três threads e atualiza cada coluna independentemente. Inclui Atualizar, Cancelar, erros por praia e identificação dos últimos dados disponíveis. A opção de debug simula uma falha HTTP apenas em Nazaré; as outras praias continuam a funcionar. A rotação conserva os resultados e Up regressa à lista existente.

ETAPA 8
Paisagem: layout próprio com lista e detalhe lado a lado, sem Fragments. Tocar numa linha atualiza o painel e destaca a seleção; em retrato continua a abrir DetailActivity. O painel reutiliza o layout do detalhe e DetailViewModel, permite atualizar a meteorologia e gerir favoritos com confirmação e Toast. Mudar de hora invalida respostas antigas; mudar de praia ou excluir a hora nas definições limpa a seleção. A seleção, os resultados e a posição exata da lista sobrevivem à rotação sem repetir pedidos.

ETAPA 9
Eventos do dispositivo: ExplorerDeviceEvents regista NetworkCallback e o receiver de bateria em onStart e remove-os em onStop. Mostra faixas de falta de Internet e bateria fraca em retrato e paisagem. Quando a rede regressa após um erro, a lista recupera automaticamente, sem duplicar um pedido em curso. O Handler atualiza a lista pelo intervalo das definições enquanto o Explorer está visível, com rede e sem bateria fraca. ACTION_BATTERY_LOW suspende o ciclo e ACTION_BATTERY_OKAY retoma-o; a consulta inicial da bateria cobre alterações ocorridas fora do ecrã. Os pedidos continuam no ViewModel e os eventos de rede passam pela main thread.

ETAPA 10
Revisão de menus, navegação e documentação: confirmadas as ações do Explorer, a escolha exclusiva da praia, a identificação no Sobre e os regressos por Up. Adicionadas transições XML de entrada e saída aos quatro ecrãs do módulo, através de um tema comum que herda o tema da aplicação. A entrada das linhas, o cartão de detalhe e as transições cobrem as três situações de animação pedidas. ExplorerActivity regista agora todos os eventos de ciclo de vida com a tag ExplorerLifecycle; DetailActivity mantém DetailLifecycle. Completados os Javadoc em falta nas classes e métodos públicos e revistos os recursos.

ETAPA 11
Tema escuro: implementado o único bónus, com cores em values-night e um tema DayNight comum. A aplicação acompanha o tema do Android no ecrã inicial, horário, meteorologia do campus e todos os ecrãs do Explorer. Adaptados fundos, textos, cartões, ícones, favoritos, menus, diálogos e faixas de aviso. As barras do sistema usam recursos compatíveis com a versão do Android; a meteorologia tem cores próprias para manter o contraste em ambos os temas e orientações.

ETAPA 12
Validação e entrega: conferidos RF1–RF19 e RT1–RT12, preparados mapa de requisitos, matriz de testes e evidências. Removidos testes de exemplo e dependências sem uso; versões de Lifecycle centralizadas no catálogo Gradle. A compilação e o Lint passaram (0 erros, 12 avisos e 0 HardcodedText). O APK instalou e os percursos principais e de falha foram verificados num emulador API 34. O projeto extraído do ZIP compilou numa pasta nova.
