# Câmera OPPO — Pesquisa de projetos e recomendações

Consulta: 7 de novembro de 2026, horário de Brasília.

Escopo: pesquisa de repositórios, documentação, arquivos de configuração, trechos de código e lançamentos. Não houve instalação, compilação ou teste de câmera nos aparelhos. Esta pesquisa complementa a diretriz inicial e mantém as decisões confirmadas: Find X8 Ultra, Reno16 Pro e Reno14; Automático + Pro; foto pronta e DNG original quando disponível.

## Conclusão

Vale agregar uma arquitetura de processamento já demonstrada em projetos públicos. O principal candidato de base para avaliação passa a ser **bjzhou/PhotonCamera**, por reunir captura, edição por LUTs, síntese de múltiplos quadros e retrato, além de configuração específica para o Find X8 Ultra. A recomendação é preliminar: qualidade, estabilidade e compatibilidade dependem de testes físicos e da seleção dos componentes com origem e licença adequadas.

O maior ganho potencial é combinar **calibração por lente, processamento RAW e tratamento de movimento**. A biblioteca de estilos deve ser construída sobre essa base de imagem.

## Como foi avaliado o “sucesso”

Foram considerados sinais públicos de adoção, lançamentos, continuidade e disponibilidade de código. Estrelas, forks e downloads ajudam a localizar projetos relevantes, mas não demonstram superioridade fotográfica, usuários ativos ou compatibilidade com os três OPPO.

- Open Camera: a página oficial na Google Play informa mais de 100 milhões de downloads.
- eszdman/PhotonCamera: aproximadamente 1,1 mil estrelas e 110 forks; um APK da versão v0.8817884 registrava aproximadamente 20 mil downloads.
- bjzhou/PhotonCamera: aproximadamente 630 estrelas e 63 forks; os três APKs da versão 1.28.5 somavam aproximadamente 14 mil downloads.
- MotionCam: há produto atual e ecossistema de ferramentas, mas o código público antigo consultado não representa automaticamente o MotionCam Pro atual.

As contagens são um retrato da consulta e podem mudar.

## 1. Photon Camera de bjzhou — principal candidato de base

Repositório: https://github.com/bjzhou/PhotonCamera

Este repositório é distinto de eszdman/PhotonCamera; os projetos de nomes semelhantes não devem ser confundidos.

O README descreve LUTs importáveis, ajustes de cor, síntese de vários quadros, bokeh por profundidade e operação com a câmera do sistema. O histórico de lançamentos inclui Ultra HDR e motores de simulação de filme. Essas são capacidades declaradas pelo projeto, ainda não verificadas nos celulares de Duvier.

A licença principal declarada é Apache-2.0, com exceções explícitas para componentes, modelos e materiais de terceiros. A seleção de código exige conferir também suas dependências e recursos; a licença da raiz não permite presumir a mesma condição para todo arquivo incluído.

### Evidência específica do Find X8 Ultra

O arquivo `app/src/main/assets/device_configurations/oppo_x8_ultra.json` lista os modelos PKJ110 e PKU110 e define ajustes de câmera principal, quantidade de quadros e níveis de preto RAW. Isso demonstra adaptação específica no projeto, mas não comprova a qualidade final ou a liberação de todas as resoluções.

Não se deve copiar IDs de câmera ou níveis de preto sem validar o firmware e os metadados dos aparelhos. Nos Reno16 Pro e Reno14, não confirmei adaptação específica equivalente na pesquisa realizada.

### Retrato: conferir a implementação atual

O README cita MiDaS e otimização Qualcomm. Entretanto, o `DepthEstimator.kt` atual usa inferência local CPU/XNNPACK e comenta a desativação do acelerador GPU por problemas em determinados drivers. A decisão deve considerar o código e os testes atuais, além do resumo do README.

A OPPO informa MediaTek Dimensity 8550 SUPER para o Reno16 Pro global consultado e Dimensity 8350 para o Reno14 brasileiro. Portanto, não se deve projetar o processamento supondo que os três aparelhos tenham a mesma GPU ou NPU. Confirmar também a variante física.

### Operação com a câmera original

O `PhantomService.kt` observa novos arquivos de câmera por MediaStore e encaminha fotografias elegíveis para processamento; há lógica para pares JPEG/RAW e espera pela conclusão da gravação. É um caminho concreto para fotografar na câmera original e aplicar o tratamento depois.

Isso não significa que o aplicativo obtenha acesso interno irrestrito aos algoritmos da OPPO. A integração depende das permissões, do arquivo gravado, do formato e do comportamento do sistema. Como rota complementar, merece ser testada.

**Decisão proposta:** avaliar primeiro como base de uma versão de teste enxuta, preservando captura, RAW, estilos e retrato; incorporar outros recursos somente quando beneficiarem o objetivo.

## 2. PhotonCamera de eszdman — referência de processamento RAW

Repositório: https://github.com/eszdman/PhotonCamera

O código consultado contém tratamento RAW, alinhamento, fusão, modelos de ruído e configurações específicas por sensor. `HdrxProcessor.java` organiza os quadros e o processamento; `NoiseModeler.java` considera sensibilidade e parâmetros de ruído; `SensorSpecifics.java` carrega ajustes específicos.

Isso oferece uma referência técnica para calibrar o comportamento por lente e reduzir ruído preservando textura. A existência do código não comprova desempenho ou ganho de detalhe nos OPPO.

A licença principal é GPL-3.0. O reaproveitamento deve manter as condições aplicáveis; a pesquisa também pode orientar uma implementação própria.

**Decisão proposta:** usar como referência e candidato a experimento de processamento. Não defini-lo automaticamente como base completa do produto.

## 3. GCam / AGC / LMC — referência de adaptação e calibração

Projetos de configuração no GitHub, como `WonITKorea/BSG_MGC_Configs`, ilustram a distribuição de ajustes por aparelho. Esses repositórios não equivalem ao código completo do processamento Google Camera.

Foi localizada ainda uma configuração comunitária **GR7 AGC 9.2 OPPO Find X8 Ultra V5**, publicada pelo autor GajosR7 no AGC ToolKit. A descrição recomenda ajustar modelos de ruído e transformação de cor por lente. É evidência de trabalho comunitário específico para o X8 Ultra, não garantia de resultado superior.

**O que agregar:** perfis independentes por aparelho e lente, com correção de cor, ruído, exposição e nitidez. Um mesmo filtro estético deve receber uma base de cor consistente nas diferentes câmeras.

**Decisão proposta:** usar uma versão compatível de GCam como comparação fotográfica e estudar a lógica de calibração. Não escolher um repositório de XML ou APKs como se fornecesse todo o motor da câmera.

## 4. Open Camera — referência de controles e operação

Projeto oficial: https://opencamera.org.uk/

Embora existam cópias e derivações no GitHub, a fonte oficial de Mark Harman está no SourceForge. Foi feita essa distinção para evitar escolher um espelho antigo ou outro aplicativo de nome semelhante.

A documentação oficial oferece referências para RAW/DNG, controles manuais, histograma, zebra, auxílio de foco, bracketing e HDR com alinhamento e tratamento de fantasmas. Algumas funções dependem do aparelho.

**O que agregar:** histograma, aviso de áreas estouradas e realce de foco, mantendo clara a diferença entre o que a prévia mede e o que foi capturado. A interface do nosso app continuará baseada nas referências de Duvier.

**Decisão proposta:** referência de operação, diagnóstico e testes. Código sob GPL-3.0 ou posterior, conforme documentação oficial.

## 5. MotionCam — referência de captura e processamento

Código público antigo consultado: https://github.com/f0enix/motioncam

O README descreve redução de ruído com múltiplas imagens RAW, recuperação de altas luzes e prévia de processamento. O repositório consultado registra envio de alterações em 2021; não deve ser tratado como código atual do MotionCam Pro.

O produto atual destaca captura RAW, controles e monitoramento; o site informa que o suporte Android depende do acesso ao fluxo RAW Camera2. O decodificador MCRAW público também não corresponde ao aplicativo completo.

**Decisão proposta:** referência conceitual e comparação; prioridade maior em uma futura etapa de vídeo, caso esse escopo seja solicitado.

## 6. Simulações Fujifilm e materiais de cor

Foram localizados `abpy/FujifilmCameraProfiles` e `TingfengLuo/Camera-Profile-for-Fujifilm-Film-Simulation`. O segundo relaciona módulos do Find X8 Ultra e descreve uma base de calibração específica combinada a perfis criativos.

O material de abpy inclui LUTs e descreve a necessidade de uma entrada com contraste linear para determinados perfis. Portanto, aplicar uma LUT qualquer diretamente sobre um JPEG já tratado pode produzir uma transformação diferente da pretendida.

O repositório abpy declara CC-BY-NC-SA-4.0 para os perfis. No repositório de TingfengLuo não foi identificado um arquivo LICENSE na raiz; a origem de seus materiais precisa ser considerada individualmente. Não foi aprovado copiar esses pacotes para distribuição no aplicativo.

**O que agregar:** suporte a LUTs importadas pelo usuário e perfis próprios, com entrada/saída de cor declaradas, intensidade ajustável e original preservado. As referências Leica, Kodak, Fujifilm e Hasselblad continuam sendo direções estéticas, sem promessa de equivalência oficial.

## Prioridades de agregação

### P1 — primeira versão fotográfica

- Calibração versionada por aparelho e por lente: cor, níveis RAW, ruído e nitidez.
- DNG original e fotografia final em arquivos separados; distinguir RAW de quadro único e eventual RAW processado por fusão.
- Motor de estilos com LUT 3D, curvas e controle de intensidade, com renderização final em resolução adequada.
- Histograma e avisos de exposição e foco, opcionais no visor.
- Perfis de captura “Rápido”, “Qualidade” e “Máxima resolução”, com funcionamento explicado e medido.

### P2 — após as capturas de referência

- Fusão RAW adaptativa: selecionar quantidade de quadros conforme cena, movimento e recursos disponíveis.
- Tratamento de movimento para evitar duplicação de rostos, mãos e roupas.
- Retrato local com avaliação de cabelo, óculos, bordas e preservação da pele.
- Importação assistida de arquivos da câmera original, preservando o original.
- Ultra HDR com informação coerente de brilho e exportação compatível.

### Experimentos posteriores

- Super-resolução multiquadro somente quando houver ganho real demonstrável; identificar ampliação computacional.
- Modelos maiores, simulações mais complexas e vídeo RAW exigem avaliação própria de memória, consumo e latência.

## Impacto esperado no desempenho

Perfis de calibração e LUTs podem ter custo relativamente limitado; já fusão de imagens, revelação RAW e profundidade exigem mais processamento. O impacto real ainda não foi medido.

Exemplo de dimensionamento, não medição: uma imagem RAW16 de 50 milhões de pixels representa aproximadamente 100 MB de dados de pixels; oito quadros representam 800 MB antes de estruturas adicionais, imagens intermediárias e memória da GPU. Isso justifica processamento com orçamento de memória, reutilização de buffers e número adaptativo de quadros.

Não presumir que RAW, HDR, retrato e resolução máxima possam operar simultaneamente em todas as lentes. A seleção deve ser feita a partir de capturas bem-sucedidas e testes de qualidade.

## Próxima decisão técnica recomendada

Avaliar uma adaptação enxuta do Photon Camera de bjzhou em uma versão separada de teste, com identidade própria e componentes selecionados. Comparar as capturas com a câmera original da OPPO e, quando compatível, GCam/AGC.

O primeiro experimento deve verificar abertura, lentes, arquivos DNG/JPEG, resolução, cor, textura de pele, contraluz, movimento e tempo de processamento. A escolha definitiva da base depende dos resultados e da origem dos componentes reutilizados.

Nenhuma alteração de código, instalação ou publicação foi realizada nesta pesquisa.

## Fontes e evidências

- Photon Camera de bjzhou: https://github.com/bjzhou/PhotonCamera
- Lançamentos: https://github.com/bjzhou/PhotonCamera/releases
- Snapshot de referência de `main`: `a57a1877bf6cecaae9222d321065a51a583bdb74`.
- Configuração X8 Ultra: https://github.com/bjzhou/PhotonCamera/blob/a57a1877bf6cecaae9222d321065a51a583bdb74/app/src/main/assets/device_configurations/oppo_x8_ultra.json
- Profundidade: https://github.com/bjzhou/PhotonCamera/blob/a57a1877bf6cecaae9222d321065a51a583bdb74/app/src/main/java/com/hinnka/mycamera/ml/DepthEstimator.kt
- Integração com arquivos da câmera: https://github.com/bjzhou/PhotonCamera/blob/a57a1877bf6cecaae9222d321065a51a583bdb74/app/src/main/java/com/hinnka/mycamera/phantom/PhantomService.kt
- Licença principal: https://github.com/bjzhou/PhotonCamera/blob/main/LICENSE
- PhotonCamera de eszdman: https://github.com/eszdman/PhotonCamera
- Snapshot de referência de `dev`: `b2eff821a858033515eb2700e7292befecc95225`.
- Processamento HDRX: https://github.com/eszdman/PhotonCamera/blob/b2eff821a858033515eb2700e7292befecc95225/app/src/main/java/com/particlesdevs/photoncamera/processing/processor/HdrxProcessor.java
- Modelos de ruído: https://github.com/eszdman/PhotonCamera/blob/b2eff821a858033515eb2700e7292befecc95225/app/src/main/java/com/particlesdevs/photoncamera/processing/render/NoiseModeler.java
- Lançamentos eszdman: https://github.com/eszdman/PhotonCamera/releases
- Configurações BSG: https://github.com/WonITKorea/BSG_MGC_Configs
- Configuração AGC para X8 Ultra: https://config.agccam.com/450
- Open Camera oficial: https://opencamera.org.uk/
- Open Camera, documentação: https://www.opencamera.org.uk/help.html
- Open Camera, página oficial Google Play: https://play.google.com/store/apps/details?id=net.sourceforge.opencamera
- MotionCam, código antigo: https://github.com/f0enix/motioncam
- MotionCam atual: https://www.motioncamapp.com/
- Perfis Fujifilm abpy: https://github.com/abpy/FujifilmCameraProfiles
- Perfis de câmera TingfengLuo: https://github.com/TingfengLuo/Camera-Profile-for-Fujifilm-Film-Simulation
- Reno16 Pro global, especificações: https://www.oppo.com/ph/smartphones/series-reno/reno16-pro/specs/
- Reno14 brasileiro, especificações: https://www.oppo.com/br/smartphones/series-reno/reno14/specs/

Limite da conclusão: inspeção estática de fontes públicas e código, sem benchmark, auditoria completa de licenças ou validação fotográfica nos aparelhos.
