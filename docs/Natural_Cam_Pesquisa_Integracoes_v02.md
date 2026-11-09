# Natural Cam — pesquisa incorporada à versão 0.2

Consulta: 8 de novembro de 2026. Aparelhos prioritários: OPPO Find X8 Ultra, Reno16 Pro e Reno14. Outros Android recebem adaptação por capacidades, sem presumir acesso às mesmas lentes ou resoluções.

## O que entrou no aplicativo

A versão 0.2 mantém a base própria e incorpora seleção de lentes por distância focal, recorte RAW identificado, controles por toque, editor não destrutivo, 40 receitas próprias de estilo e profundidade relativa local. A política de imagem coleta métricas, executa ações limitadas e audita a origem da captura. O relógio aparece como contexto; não impõe temperatura de cor.

O aplicativo usa APIs públicas. A experiência busca controles de câmera dedicada; não substitui firmware, não altera o sistema e não libera recursos que a fabricante mantenha privados.

## GCam recente: qual referência faz mais sentido

A lista de versões estáveis consultada apresenta BSG MGC 9.4.103 V41, de 18/10/2026, e AGC 9.2.14 V14.0. A própria lista explica que compatibilidade frequente não significa melhor qualidade em todos os aparelhos.

Para o Find X8 Ultra, a referência mais específica encontrada foi GR7 AGC 9.2 OPPO Find X8 Ultra V5, com arquivo identificado como `GR7_AGC92_OFX8U_V5_20260602.agc`. A publicação recomenda ajustar ruído e transformação de cor por lente. É uma candidata melhor direcionada para comparação de campo que uma classificação genérica de “melhor GCam”.

A página foi examinada. O endereço de obtenção do arquivo retornou HTTP 403 nesta sessão; o conteúdo completo do `.agc` não foi importado. Não foi feita comparação de fotografias dessas versões nos OPPO.

A contribuição aplicada ao Natural Cam é o princípio de separar óptica, ruído, cor e resolução por câmera, e registrar os resultados reais. Não foram copiados APKs, bibliotecas proprietárias ou o motor fechado do Google Camera. Configurações comunitárias não equivalem ao código do HDR+ atual.

Fontes:

- https://www.celsoazevedo.com/files/android/google-camera/dev-suggested/
- https://config.agccam.com/450
- https://config.agccam.com/372

## Código aberto efetivamente examinado

Photon Camera de bjzhou: revisão `a57a1877bf6cecaae9222d321065a51a583bdb74`. Foram examinados o seletor de modelo de profundidade, o contrato de tensores, o processamento por planos e a configuração do Find X8 Ultra. A release 1.28.5 descreve novidades de renderização, foco e seleção horizontal de LUTs.

O projeto licencia seu código principal como Apache-2.0 e ressalva componentes de terceiros. O Natural Cam utiliza uma implementação Java própria, com atribuição à referência do contrato de profundidade e à preferência de câmera principal do X8. Os valores de níveis de preto, perfis Adobe DCP e LUTs do repositório não foram transplantados indiscriminadamente para OPPO ou Reno.

A identificação preferencial da principal do X8 só é considerada nos modelos técnicos correspondentes e quando a câmera existe. Reno16 Pro, Reno14 e aparelhos genéricos são examinados por características e distâncias focais; não recebem os mesmos IDs fixos.

Fontes:

- https://github.com/bjzhou/PhotonCamera
- https://github.com/bjzhou/PhotonCamera/releases
- https://github.com/bjzhou/PhotonCamera/blob/a57a1877bf6cecaae9222d321065a51a583bdb74/app/src/main/assets/device_configurations/oppo_x8_ultra.json

## RAW e zoom: correção da origem do arquivo

O Android distingue dispositivos lógicos e câmeras físicas. Solicitar zoom no JPEG não garante, por si só, o mesmo enquadramento no RAW. O motor agora tenta escolher a óptica física acessível adequada e parear seus metadados com o RAW/JPEG por timestamp.

Para aumentos digitais sobre a óptica escolhida, o JPEG é recortado e o DNG recebe `DefaultCropOrigin` e `DefaultCropSize`. Os pixels RAW de todo o sensor são preservados. Editores precisam respeitar esses campos para abrir o enquadramento digital inicialmente; visualizadores que os ignorem podem mostrar a área completa da óptica selecionada.

Exemplos de comportamento planejado e testado em lógica: X8 em 7× pode usar a óptica de 6× com recorte; Reno16 Pro em 7× pode usar a tele de 3,5× com recorte de 2×. O resultado real depende das câmeras expostas pelo telefone. A captura não declara 200 MP nativos sem validação física.

O EXIF do JPEG registra focal óptica, identificação da lente, equivalente em 35 mm calculado e fator de recorte. O DNG mantém os dados ópticos e a calibração do disparo. O relatório distingue o solicitado, o observado e o não confirmado.

Fontes:

- https://developer.android.com/media/camera/camera2/multi-camera
- https://developer.android.com/reference/android/hardware/camera2/DngCreator
- https://developer.android.com/reference/android/hardware/camera2/params/OutputConfiguration

## Bancos de imagens para comportamento por ambiente

### Cube++

Base de estimativa de iluminação com 4.890 imagens RAW, objeto de calibração e anotações de ambiente. Licença declarada: CC BY 4.0. É útil para estudar iluminação natural, artificial, mista, sombras e condições diurnas/noturnas.

Foram examinadas oito anotações reais, com exemplos externos diurnos, internos artificiais e externo noturno. Seus campos de referência foram anexados ao projeto, com atribuição. Nenhuma fotografia do banco foi embutida no APK. Os sensores Canon da base não autorizam copiar seus ganhos de branco para os OPPO.

https://github.com/Visillect/CubePlusPlus

### HDR+ Burst Photography Dataset

Base de 3.640 sequências RAW de celulares Android, com resultados e metadados. A licença declarada é CC BY-SA. É útil para validar integridade de DNG, alinhamento, fusão e comportamento em contraste/baixa luz.

Um DNG real da base foi utilizado no teste do recorte. A verificação independente confirmou que o conteúdo dos blocos RAW permaneceu idêntico após atualizar o enquadramento padrão. Isso valida a integridade dessa operação; não comprova qualidade fotográfica nos OPPO nem implementa o HDR+ proprietário.

https://www.hdrplusdata.org/dataset.html

### Como isso influencia o motor agora

Coletor: luminância, sombras, recorte de altas luzes, energia de bordas, médias de cor, horário local e metadados da câmera. Métricas do visor são identificadas como tal.

Ação: escolha da óptica acessível, orçamento de resolução/memória, foco/medição por toque, preservação do RAW, solicitações de nitidez/redução de ruído desligadas quando suportadas e proteção limitada de altas luzes. Controles do fotógrafo têm prioridade; não há clareamento automático geral de sombras.

Auditor: lente solicitada e observada, focal, recorte, dimensões reais, correspondência de timestamps, tempos de processamento e falhas. A política de cena é determinística e ainda não foi treinada ou calibrada para cada OPPO. O modelo de profundidade é um componente diferente.

## Profundidade local

Foi integrado o checkpoint Small de Depth Anything V2, sob Apache-2.0, convertido para TFLite pelo Qualcomm AI Hub. O arquivo tem SHA-256 `69302c7b94a2ebe6f31d5caa78cb72d7e15f5ba72804d0423e699787368a255f` e é executado localmente, por CPU/XNNPACK.

O mapa representa profundidade relativa. O seletor f/1,4–f/16, com f/5,6 inicial, é virtual e não mede uma abertura física equivalente. A simulação aplica transições graduais e preserva o plano em foco, mas sua qualidade em cabelos, óculos, contraluz e oclusões ainda precisa de avaliação nos aparelhos. Falhas mantêm o original.

- https://github.com/DepthAnything/Depth-Anything-V2
- https://aihub.qualcomm.com/models/depth_anything_v2

## Referências de cor e fotografia

As 40 receitas são interpretações próprias: dez Leica, dez Fuji, dez Kodak e dez Hasselblad, incluindo cor e preto e branco. A documentação das marcas orienta contraste, separação de tons, saturação e transições. Nenhuma receita é apresentada como LUT oficial ou reprodução exata de ciência de cor proprietária.

Referências de leitura:

- Leica Looks: https://leica-camera.com/en-int/photography/leica-looks
- Fujifilm Film Simulation: https://www.fujifilm-x.com/pt-br/products/film-simulation/
- Kodak Professional: https://www.kodak.com/en/still-film/products/professional/
- Hasselblad Natural Colour Solution: https://www.hasselblad.com/learn/hasselblad-natural-colour-solution/

A pesquisa dos fotógrafos orienta decisões, sem aplicar um suposto “filtro do fotógrafo”: Salgado como referência de gradação em preto e branco; McCurry de relações de cor e retrato; Webb de luz, sombras densas e planos simultâneos.

Foram consultadas páginas da Leica e a transcrição do vídeo de análise de Graeme Williams sobre uma fotografia de Alex Webb. A análise reforça que sombras profundas e relações entre cores participam da construção da imagem; não devem ser neutralizadas automaticamente. Não houve extração de LUTs ou uso das fotografias dos artistas como material de treinamento.

- https://leica-camera.com/en-int/photography/a-century-of-vision
- https://leica-camera.com/en-US/blog/photography/steve-mccurry-sl3-p-colours-of-life
- https://www.youtube.com/watch?v=MLkIIdJ3MXM

Os exemplos de Snapseed, Facetune, Lightroom Mobile e VSCO fornecidos pelo usuário orientam a navegação horizontal e o acesso a ajustes locais. Não há integração com seus algoritmos ou serviços.

## Limites desta etapa

Permanecem pendentes a revelação DNG própria, a fusão RAW de vários quadros, a fusão das três câmeras, a exportação com faixa de paleta e a calibração física de cor, profundidade e desempenho por OPPO. A liberação das lentes e dos 200 MP depende do firmware/API. A experiência não garante paridade com câmeras mirrorless ou com o Find X9 Ultra.

Instalação, controles, exportação e testes virtuais devem ser distinguidos da avaliação física. Ausência absoluta de travamentos em qualquer Android não é uma promessa verificável; a versão reduz trabalho na interface, agrupa comandos, limita filas e trata falhas sem substituir silenciosamente arquivos originais.


## Complemento: fabricante dos sensores do Reno16 Pro

A nota `Natural_Cam_Reno16Pro_Sensores_Integracao_v01.md` cruza as fichas OPPO, Samsung HP5/JN5 e as referências Kimovil/GSMArena. Identificação externa de chip não muda automaticamente a calibração RAW. O demonstrador CameraUnit da OPPO é uma alternativa oficial a investigar para SAT/HDR, com suporte nos modelos atuais ainda não comprovado.
