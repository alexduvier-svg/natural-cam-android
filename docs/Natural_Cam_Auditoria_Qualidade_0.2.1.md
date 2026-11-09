# Natural Cam — auditoria de qualidade e correções 0.2.1

Data: 8 de novembro de 2026. Estado: correções candidatas implementadas; aprovação fotográfica pendente. O APK de laboratório 0.2.0 foi reprovado pelo usuário em teste físico.

## Evidência do usuário

Duvier confirmou os problemas no Find X8 Ultra e no Reno16 Pro, tanto em RAW quanto em JPEG. As quatro imagens enviadas são capturas de tela de 698 × 1536, sem EXIF fotográfico original. Mostram ruído forte, dominante verde, atraso na edição e marcador EV fora da trilha. Uma tela registra ISO 6400; esse dado não explica sozinho todos os defeitos.

As capturas permitem identificar os sintomas e erros de interface. Não permitem medir a calibração do sensor nem verificar os pixels, os níveis de preto, as matrizes e o balanço de branco do DNG original. Nenhuma correção global de canal verde foi aplicada para esconder o problema.

## Falhas concretas encontradas no código

- O app impunha redução de ruído e nitidez OFF ao JPEG, inclusive em alta sensibilidade. Essa escolha comprometia o equilíbrio entre ruído e textura e não havia sido validada nos aparelhos.
- A exposição manual desligava AWB e reaplicava ganhos/matriz congelados da prévia. Isso foi removido; sua participação na dominante real ainda precisa ser comprovada.
- As atualizações de exposição eram adiadas enquanto novos comandos chegavam; o zoom tinha espera semelhante. Esse debounce poderia postergar o efeito até o fim do gesto.
- O marcador EV usava o índice de compensação como deslocamento em pixels, sem limitar o círculo à trilha.
- O editor esperava uma pausa de 110 ms e descartava quadros concluídos quando o usuário já havia movido o controle novamente. Assim, podia não mostrar nenhuma etapa intermediária.
- As tabelas de transferência de cor eram reconstruídas a cada processamento. Miniaturas e prévias também disputavam a mesma fila sem prioridade para a interação.

## Mudanças candidatas

O JPEG agora preserva os padrões de processamento do template da câmera. O Pro permite solicitar OFF explicitamente. A exposição manual mantém AWB automático e a configuração de cor do template, respeitando a trava de branco escolhida pelo fotógrafo. A heurística não calibrada de subexposição para altas luzes fica opt-in; o padrão usa a exposição automática da câmera. RAW e calibração original não recebem filtros próprios.

Exposição e zoom usam atualizações limitadas por frequência, sem esperar o gesto terminar. Uma lente em abertura recebe apenas o destino de zoom mais recente. O zoom dentro da mesma óptica não precisa reabrir a câmera. A transição entre ópticas físicas ainda depende de medição nos aparelhos.

O marcador acompanha uma coordenada contínua limitada à trilha; a câmera continua respeitando seus passos reais de compensação. Os zooms foram colocados em pills na faixa preta, fora da fotografia, com rolagem horizontal disponível.

O editor publica quadros intermediários durante o gesto, prioriza a prévia sobre miniaturas e reutiliza tabelas de cor. Durante o arraste, usa uma prévia de até 640 pixels; ao soltar, retorna à prévia maior. A exportação continua a partir do original, em resolução integral ou na área de recorte escolhida. Alterar o número f virtual reutiliza a profundidade já estimada quando o plano em foco não muda.

## Verificações concluídas

- Compilação, assinatura v3 e alinhamento APK de 16 KB aprovados, com a assinatura preservada.
- 77 asserções de lógica: 69 de qualidade/RAW/óptica/interação e oito de geometria. Incluem trilha EV nos extremos e em intervalos assimétricos, movimento contínuo abaixo de um passo do hardware e neutralidade RGB ao ajustar exposição.
- 17 asserções principais no Android virtual: orientação, exportação JNI, resolução/recorte, EXIF de lente/focal/zoom, gravação/reabertura, receita, editor e largura do visor. O erro médio por canal no fixture JPEG versus referência foi 0,465 em escala 0–255.
- 16 asserções de interação no Android virtual: preservação da política de ruído/detalhe do template, AWB automático na exposição manual, requisições EV/zoom antes de terminar o gesto, ausência de reabertura no zoom dentro da mesma óptica, pills fora da foto, trilha EV dentro da área e prévias do editor antes de soltar o controle. A prévia maior foi restaurada após o gesto.

A primeira execução do teste de edição não passou. O teste passou a aguardar o layout efetivo do controle e registrar tamanho, estado e progresso durante o gesto. Na rodada final, observou atualização antes de ACTION_UP. Os registros foram preservados; isso não é aprovação de latência física.

O último intervalo de prévia registrado no emulador foi 5.987 ms, aproximadamente seis segundos. O ambiente usa emulação de CPU sem aceleração e apresentou ANRs externos do Quickstep. Esse resultado não aprova a meta de fluidez. Não foram medidos 20 quadros/s ou latência p95 de 100 ms nos OPPO. Os testes virtuais comprovam funcionamento e ausência da espera exclusiva pelo fim do gesto; não comprovam qualidade fotográfica nem desempenho no telefone.

A rodada Android principal antecedeu apenas um ajuste final de largura/espaçamento das pills. Esse ajuste foi recompilado, instalado e inspecionado separadamente. A auditoria distingue as evidências de código das de fotografia real.

## O que falta para liberar

A dominante verde e o ruído nos arquivos reais ainda não foram confirmados como corrigidos. Não houve acesso físico aos celulares nesta sessão. Não se comprovou resolução nativa de 200 MP, superioridade à câmera OPPO, fluidez entre lentes ou qualidade profissional de retrato.

Próxima evidência mínima: um JPEG original e o DNG correspondente, do mesmo disparo no X8 Ultra, com o JSON da captura se disponível. Em seguida, repetir no Reno16 Pro. Enviar como arquivos ou ZIP, sem captura de tela ou recompressão. Uma fotografia da mesma cena feita pela câmera original da OPPO ajuda a comparar a cor.

Arquivos do app: `DCIM/NaturalCam`; relatórios: `Download/NaturalCam`. A análise deverá comparar o JPEG da câmera, o JPEG derivado e o DNG, verificando lente real, perfil de cor, CFA, níveis de preto/branco, matrizes e neutralidade. Não copiar calibrações genéricas entre sensores.

O objetivo permanece aberto em `GOAL.md` e a regra de liberação está em `AGENTS.md`. O candidato continua como build interno de teste; não é distribuído como solução fotográfica validada. O comando `/goal` foi preparado no documento, mas o modo nativo não foi ativado por esta sessão.
