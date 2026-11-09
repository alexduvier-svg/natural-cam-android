# Natural Cam — leitura máxima, retrato e referência ProShot

Pesquisa: 8 de novembro de 2026.

## O que os 12 MP significam

Um arquivo de aproximadamente 12,5 MP não significa que o componente tenha apenas essa resolução. A Samsung documenta no HP5 modos de 200 MP, 50 MP e 12,5 MP. O binning reúne a contribuição de pixels; depois de reduzida a imagem, ampliar o arquivo não recupera a leitura individual descartada.

O Reno16 Pro anuncia 200 MP na principal, enquanto sua teleobjetiva tem 50 MP. No Find X8 Ultra, a referência máxima anunciada pela OPPO é 50 MP JPEG Max/RAW Max. Portanto, a meta técnica é extrair a maior informação útil de cada óptica e cada formato, mantendo distintos megapixels, detalhe, ruído e resolução de exportação.

Fontes:

- https://semiconductor.samsung.com/news-events/tech-blog/inside-isocell-hp5-exploring-05-pixel-technology/
- https://www.oppo.com/en/smartphones/series-reno/reno16-pro/specs/
- https://www.oppo.com/en/newsroom/press/oppo-hasselblad-partnership-imaging-system/

## Métodos para obter mais informação

### 1. Saídas normais e de alta resolução

Consultar as dimensões oferecidas por câmera e formato, incluindo saídas de alta resolução do mapa padrão. Um telefone pode oferecer JPEG maior e RAW menor. Testar a combinação real; não usar a resolução nominal da caixa como resposta da API.

Este caminho está implementado. No modo RAW, o primeiro caminho tenta um JPEG compatível com a resolução RAW escolhida. A configuração pode ser recusada pelo driver; alternativas de segurança são registradas.

### 2. Modo máximo do sensor pelo Android

O Android possui capacidade de sensor de resolução muito alta, mapa específico e `SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION`. O modo máximo geralmente evita o binning usado no modo padrão. Saídas e combinações dependem do dispositivo.

O botão Máxima já tenta esse caminho, tanto para JPEG/retrato quanto para RAW quando exposto. Ainda não foi comprovada uma captura de 200 MP no Reno16 Pro nem de RAW Max OPPO equivalente no X8 Ultra.

### 3. Remosaico e reprocessamento

O Android também documenta `REMOSAIC_REPROCESSING`, que exige uma sessão e entradas/saídas próprias. A Samsung descreve remosaico de hardware em seus componentes. Isso precisa estar integrado ao firmware e disponibilizado pela API; não basta chamar uma função de nitidez ou ampliar a imagem.

A implementação atual identifica a capacidade no diagnóstico, mas não inclui uma sessão própria de reprocessamento remosaico. O RAW+JPEG simultâneo usa as garantias e os formatos anunciados pelo Android; não deve tratar qualquer mosaico agrupado como Bayer convencional sem confirmação.

Fontes dos métodos 1–3:

- https://developer.android.com/reference/android/hardware/camera2/CameraCharacteristics
- https://developer.android.com/reference/android/hardware/camera2/CameraMetadata
- https://developer.android.com/reference/android/hardware/camera2/CameraDevice

### 4. Integração oficial ou captura original OPPO

CameraUnit é o caminho oficial a investigar para SAT/HDR/retrato/noturno em aparelhos suportados. Sua demonstração pública não comprova 200 MP ou suporte aos modelos atuais. Não está integrado nesta entrega.

Enquanto uma função permanecer exclusiva da câmera OPPO, seu arquivo original pode ser aberto no editor Natural Cam. Isso aproveita uma captura que o fabricante eventualmente consiga produzir melhor; não desbloqueia o sensor pelo editor.

https://github.com/oppo/CameraUnit

### 5. Empilhamento e super-resolução de vários quadros

Alinhar vários quadros pode melhorar relação sinal/ruído e, com deslocamentos adequados, recuperar parte da informação espacial. Precisa rejeitar movimento, oclusões e quadros sem confiança para não duplicar rostos, cabelo ou mãos. Não equivale a obter 200 milhões de amostras nativas de uma tele de 50 MP.

Esta continua sendo uma etapa pendente, assim como a fusão de três câmeras. Deve ser comparada ao melhor quadro individual e preservar os originais. Interpolação ou ampliação por IA, se oferecidas no futuro, precisarão ser identificadas como tal.

## Retrato em 3×, 6×, 7× e 8×

O retrato próprio pode ser aplicado após a captura máxima que a lente realmente oferecer. A profundidade é estimada numa guia reduzida; a fotografia final é processada em blocos na resolução original ou na área recortada, sem adotar a resolução da guia como saída.

No Reno, os seguintes números são apenas limites geométricos ilustrativos, supondo principal de 200 MP, tele de 50 MP e fator óptico nominal de 3,5× disponíveis integralmente. Não são capturas verificadas nem medição de detalhe:

| Enquadramento | Área aproximada aproveitada |
|---|---|
| 3× pela principal | 22,2 MP, a partir de 200 MP |
| 6× pela tele 3,5× | 17,0 MP, a partir de 50 MP |
| 7× pela tele 3,5× | 12,5 MP, a partir de 50 MP |
| 8× pela tele 3,5× | 9,6 MP, a partir de 50 MP |

Cálculo: resolução de origem dividida pelo quadrado do fator de recorte. Se a API entregar apenas 12,5 MP antes de recortar, a área útil será menor. As focais efetivamente informadas, correções geométricas e bordas do sensor também alteram esses valores.

No X8 Ultra, supondo 50 MP acessíveis nas teles nominais de 3× e 6×:

| Enquadramento | Área aproximada aproveitada |
|---|---|
| 3× ou 6× na óptica correspondente | até 50 MP |
| 7× pela tele 6× | 36,7 MP |
| 8× pela tele 6× | 28,1 MP |

Esses cálculos próprios orientam a escolha da óptica; não garantem qualidade equivalente a outra câmera. Pele, foco, movimento, luz, ruído e capacidade óptica precisam ser comparados nos arquivos reais. O bokeh virtual não é aplicado aos pixels RAW.

## O que o ProShot acrescenta à investigação

O guia oficial do ProShot confirma que fabricantes podem restringir resoluções e lentes a aplicativos externos. Não foi encontrada uma garantia de que ele desbloqueie 200 MP no Reno16 Pro.

As referências úteis são diagnóstico por capacidade, grupos de câmeras, foco/medição por toque, peaking, controles separados de redução de ruído/nitidez e extensões condicionais. O guia também documenta limites de RAW, zoom e controles nas extensões. Seus ajustes experimentais não constituem uma solução universal estável.

O ProShot Evaluator é uma ferramenta externa de avaliação disponível pela própria Rise Up Games. Pode servir como comparação independente ao diagnóstico do Natural Cam. Não foi executado nos aparelhos do usuário e nenhum binário ou código proprietário ProShot foi incorporado.

- https://www.riseupgames.com/proshot/android
- https://www.riseupgames.com/proshot
- https://play.google.com/store/apps/details?id=com.riseupgames.proshotevaluator

## Próxima evidência necessária

Em Info, exportar o diagnóstico do Natural Cam no Reno e no X8 Ultra. Conferir resoluções normal/máxima de JPEG e RAW, IDs físicos, binning e remosaico anunciado. Depois, capturar com Máxima e verificar dimensões salvas e área útil, comparando à câmera OPPO. Esse resultado permite escolher o próximo caminho sem presumir um desbloqueio inexistente.
