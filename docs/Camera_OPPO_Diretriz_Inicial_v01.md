# Natural Cam — Diretriz de desenvolvimento

Versão: 0.6 — correções candidatas, validação física pendente; 8 de novembro de 2026.

Atualização: zoom e leitura do sensor de 200 MP; retrato; empilhamento; fusão multicâmera; contraluz; tons ao vivo; paletas; editor e combinação seletiva de projetos.

Estado: segunda implementação nativa de laboratório, denominada Natural Cam, versão 0.2.0-lab.2. A seção 12.10 documenta historicamente a primeira versão; a seção 12.11 atualiza o realizado e o pendente. O usuário testou a versão 0.1 no Find X8 Ultra e forneceu capturas e observações. Não houve acesso físico aos aparelhos nesta implementação; cor, resolução e fluidez ainda exigem validação por modelo.

## 1. Objetivo

Desenvolver uma câmera Android para uso de Duvier nos OPPO Find X8 Ultra, Reno16 Pro e Reno14, com captura RAW em DNG quando disponibilizada pelo aparelho, HDR, alta resolução, retratos naturais e estilos de cor e preto e branco inspirados nas referências Leica, Kodak, Fujifilm e Hasselblad.

“Extrair o máximo do sensor” significa aproveitar a informação real que o sistema permite capturar, preservar o original e escolher o processamento adequado à cena. Aumentar o número de pixels por interpolação não equivale a obter mais detalhe óptico.

## 2. Decisões confirmadas pelo usuário

- Aparelhos: Find X8 Ultra, Reno16 Pro e Reno14 padrão.
- Operação: Automático + Pro, com ISO, velocidade, foco e balanço de branco acessíveis.
- Saída principal: foto pronta com o estilo escolhido e DNG original, quando disponível.
- Prioridades do pedido: qualidade de retrato, saída de alta qualidade, HDR, RAW/DNG e aproveitamento do sensor.

## 3. Premissas propostas

- Primeira etapa dedicada à fotografia. Vídeo exige especificação e validação próprias.
- Processamento local e funcionamento offline como base.
- Sem root ou modificações do sistema.
- Nome comercial ainda em aberto; “Câmera OPPO” identifica este projeto, sem afiliação com a fabricante.
- Interface escura inspirada nas referências fornecidas: visor amplo, obturador destacado, acesso próximo às lentes e controles contextuais.
- Não copiar os rótulos cortados observados nas capturas. As imagens não permitem confirmar se isso decorre de animação, tradução, rolagem ou layout.

## 4. O que a documentação confirma e o que permanece aberto

O Android oferece Camera2 e DngCreator para gravar os dados RAW disponibilizados pela câmera em DNG. DNG é um formato que pode armazenar RAW; não representa um segundo sensor ou um nível de qualidade independente.

A OPPO anuncia JPEG Max e RAW Max de 50 MP no Find X8 Ultra. Isso descreve o recurso do fabricante e não confirma que as mesmas resoluções, processamento ou combinações sejam oferecidos a um aplicativo independente.

A resolução disponível deve ser consultada por câmera, formato e modo do sensor. O Android possui configurações específicas para sensores que expõem um modo de resolução máxima. A presença de um sensor de muitos megapixels na ficha comercial não substitui essa consulta e o teste de captura.

HDR e bokeh podem ser oferecidos por extensões do fabricante. A disponibilidade varia entre aparelhos e câmeras. Não se presume que RAW, HDR, bokeh e resolução máxima funcionem simultaneamente.

O HDR do modo de cena Camera2 não produz saída RAW. Um processamento HDR próprio a partir de várias exposições é um caminho separado e exige alinhamento, tratamento de movimento e validação de qualidade.

Até aqui, não estão verificados nos três aparelhos: câmeras acessíveis, resoluções RAW/JPEG, extensões disponíveis, captura simultânea RAW/JPEG, estabilização controlável e tempo de processamento.

## 5. Primeira etapa técnica: diagnóstico e captura de referência

A primeira versão instalável deverá consultar o aparelho e testar as capacidades. Uma lista de recursos anunciados é insuficiente: o relatório deve separar “informado pela API”, “captura bem-sucedida” e “não verificado/falhou”.

O diagnóstico deverá registrar:

- Modelo técnico, versão de Android e ColorOS quando disponível, sem IMEI ou número de série.
- IDs de câmeras acessíveis, câmeras físicas associadas e distâncias focais declaradas.
- Formatos e dimensões disponíveis em RAW, JPEG e YUV; HEIF/Ultra HDR somente quando suportados pela implementação.
- Configurações de resolução máxima, quando expostas.
- Faixas de ISO, exposição, compensação, foco e controles manuais.
- Suporte informado a HDR, bokeh, noite e outras extensões relevantes.
- Resultado real de abertura da sessão e captura por combinação de lente, formato e resolução.
- Duração da captura, gravação, processamento, tamanho dos arquivos e falhas encontradas.

O relatório deverá poder ser exportado manualmente. Não haverá envio automático de fotografias ou dados do aparelho.

Esta etapa depende da execução nos celulares de Duvier. Nenhuma inspeção de documentação substitui esse teste.

## 6. Organização do aplicativo

### Foto

Captura automática com foco e exposição por toque; compensação de exposição; timer; grade; nível; escolha das câmeras acessíveis; seleção de formato e HDR quando compatíveis.

O modo de qualidade máxima deverá mostrar a resolução efetiva e o impacto de processamento. A escolha automática não deve forçar a maior contagem de pixels em cenas onde isso prejudique movimento, ruído ou alcance dinâmico.

### Retrato

Priorizar olhos em foco, pele natural, textura e transições suaves. Desfoque ajustável, preservando cabelo, orelhas, óculos e contornos da roupa. Sem alterar a geometria do rosto por padrão.

Avaliar primeiro os recursos de retrato disponibilizados pelo fabricante. Se forem insuficientes ou indisponíveis, um processamento próprio exigirá segmentação ou estimativa de profundidade e testes específicos. Não presumir paridade com o retrato da câmera original.

### Pro

ISO, tempo de exposição, balanço de branco, compensação e foco conforme o suporte real. Controles indisponíveis devem ser explicados. Histograma e avisos de exposição precisam representar o fluxo que estiver sendo medido, distinguindo prévia e captura.

### Revelar

Edição não destrutiva e comparação com o original. A importação de DNG capturado na câmera original da OPPO será uma rota complementar a avaliar, especialmente se o aplicativo independente não tiver acesso ao RAW Max. Compatibilidade de leitura e fidelidade de cor precisam ser verificadas com arquivos reais de cada modelo.

## 7. RAW e fotografia final

- Preservar o DNG original sem gravar o filtro sobre os dados do sensor.
- Salvar os ajustes separadamente e gerar a fotografia final em resolução compatível com o original.
- Não gerar o arquivo final a partir de uma miniatura ou captura do visor.
- Não regravar JPEG repetidamente durante a edição.
- Preservar orientação, dimensões, identificação de lente e metadados fotográficos pertinentes.
- JPEG de alta qualidade como saída de ampla compatibilidade. HEIF, Ultra HDR e um mestre de maior profundidade de cor são opções condicionadas ao suporte e à implementação.
- Ultra HDR de saída e HDR por fusão de exposições são recursos diferentes. Um filtro deve preservar ou recalcular corretamente a informação HDR; se a exportação for SDR, identificá-la como tal.
- Não confundir um contêiner de 16 bits com 16 bits de informação efetivamente capturada pelo sensor.

## 8. Estilos de imagem

Criar perfis próprios de cor e preto e branco inspirados nas referências solicitadas, sem apresentá-los como filtros oficiais ou reprodução exata de ciência de cor proprietária.

- Referência Leica: explorar contraste, separação tonal e preto e branco expressivo.
- Referência Kodak: escolher depois quais filmes orientarão os perfis; retrato suave e cor intensa exigem tratamentos diferentes.
- Referência Fujifilm: explorar paletas e resposta tonal de filmes específicos a definir.
- Referência Hasselblad: explorar tons de pele, gradações e cor natural.

Essas descrições orientam o projeto; não definem todas as câmeras ou filmes de cada marca. Cada perfil deverá ter intensidade ajustável e ser calibrado com fotografias de teste. Granulação, vinheta e nitidez serão controles separados.

A visualização em tempo real poderá usar resolução reduzida para manter fluidez, mas a aplicação final deverá respeitar a resolução de saída e o gerenciamento de cor.

## 9. Desempenho e integridade

- Aplicativo Android nativo, com Camera2 como base de controle e avaliação de extensões por aparelho.
- Separar captura, gravação e processamento para manter a interface responsiva.
- Limitar imagens em memória, processar por blocos quando necessário e fechar buffers de câmera corretamente.
- Fila de processamento com limite e indicação clara de gravação concluída.
- Não descartar o original quando o processamento do estilo falhar.
- Gravação segura, tratamento de pouco espaço e recuperação após interrupção.
- Prévia com latência baixa; velocidade e consumo da captura máxima serão medidos nos aparelhos.
- HDR com movimento deverá evitar duplicação de pessoas e contornos. A captura de referência deverá ser preservada quando a fusão falhar.

## 10. Critérios para aceitar a qualidade

1. RAW legítimo: arquivo DNG legível em editor compatível, com dados e metadados coerentes com a captura; não apenas JPEG convertido ou renomeado.
2. Resolução: dimensões verificadas no arquivo e correspondentes à configuração utilizada. Ampliação computacional deve ser identificada separadamente.
3. Preservação: original intacto depois da aplicação e troca de estilos.
4. Cor: balanço de branco, tons de pele e altas luzes avaliados em cenas controladas e situações reais.
5. Retrato: inspeção a 100% de olhos, pele, cabelo, óculos, orelhas e roupas, além de avaliação no celular.
6. HDR: teste de contraluz, sombras, luzes noturnas e pessoas em movimento, sem halos ou fantasmas excessivos.
7. Arquivos: abertura fora do aplicativo, orientação correta, EXIF coerente e ausência de corrupção.
8. Desempenho: medir abertura, foco, captura, gravação, processamento, memória e aquecimento em cada aparelho.
9. Comparação: fotografar cenas equivalentes na câmera original da OPPO e no aplicativo, com enquadramento e iluminação comparáveis. Julgar detalhe, textura, cor, ruído e alcance dinâmico.
10. Comunicação: não declarar superioridade à câmera original ou “máximo do sensor” antes dos testes.

## 11. Marcos de entrega

1. Diagnóstico instalável e captura de referência: verificar o acesso real ao hardware nos três aparelhos.
2. Captura Automático + Pro: RAW/DNG e fotografia pronta, nas combinações validadas.
3. Revelação e estilos: calibração de cor e preto e branco, preservando o original.
4. HDR e retrato: validar extensões e implementar processamento adicional onde houver benefício demonstrável.
5. Refinamento: qualidade visual, estabilidade, consumo e testes comparativos por modelo.

O primeiro APK terá finalidade de diagnóstico; não deverá ser apresentado como entrega concluída de todos os recursos.

## 12. Requisitos complementares e arquitetura integrada

### 12.1. Um único motor de imagem

O usuário autorizou aproveitar o melhor dos projetos pesquisados e combinar os recursos no aplicativo. A arquitetura deverá ter uma representação comum de imagem, metadados, cor e receitas de edição. A integração deve ocorrer por componentes selecionados; não pela inclusão de aplicativos completos em paralelo.

Fluxo previsto: diagnóstico do hardware; captura; seleção e alinhamento de quadros; eventual fusão; revelação RAW; tratamento de retrato e tom; estilos de cor; edição; exportação e paleta.

- Photon Camera de bjzhou: candidato inicial para captura, edição, importação de LUTs e organização da experiência. Seu uso como base depende da avaliação técnica e da origem dos componentes.
- PhotonCamera de eszdman: referência para alinhamento, fusão RAW, redução de ruído e perfis por sensor.
- GCam/AGC/LMC: referência para calibração por aparelho e lente. Configurações e APKs não equivalem ao código completo do motor Google Camera.
- Open Camera: referência de controles manuais, diagnóstico, histograma, zebra e auxílio de foco.
- MotionCam: referência para fluxo RAW e monitoramento; vídeo permanece sujeito a uma especificação própria.

Antes de incorporar código, registrar origem, versão e licença de cada componente, inclusive modelos e materiais auxiliares. Compatibilidade de formato, espaço de cor, precisão, metadados e orçamento de memória deve ser verificada na fronteira entre os componentes.

O estudo não autoriza presumir que a licença principal de um repositório cubra todos os seus materiais. Nenhum código dos projetos foi incorporado até esta revisão.

### 12.2. Sensor de 200 MP e zoom no Reno16 Pro

Prioridade expressa pelo usuário: extrair a maior qualidade real do sensor principal de 200 MP e dos enquadramentos em torno de 3×/3,5× e 7×. Segundo seu relato, o aplicativo original entrega aproximadamente 12 MP e não apresenta opção de saída em 200 MP. Esse comportamento deve ser tratado como observação do aparelho do usuário, ainda não reproduzida pela equipe.

A documentação oficial do Reno16 Pro consultado descreve câmera principal de 200 MP, ultrawide de 50 MP e uma teleobjetiva de 50 MP com zoom óptico de 3,5×. A página também mostra o enquadramento 7×. Não foram documentadas duas teleobjetivas físicas, de 3,5× e 7×.

Os 200 MP pertencem à câmera principal. A teleobjetiva possui seu próprio sensor e sua própria óptica. Não presumir que os 200 MP do sensor principal possam ser lidos através da lente teleobjetiva.

O diagnóstico deve consultar e testar separadamente:

- Resoluções padrão e de alta resolução em cada formato, câmera e modo do sensor.
- Mapa de resolução máxima e `SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION`, se expostos.
- Formatos RAW, padrão de filtros de cor, metadados de binning e necessidade de remosaico.
- Dimensões reais dos arquivos após cada captura; comparar solicitação e resultado.
- Identificação da câmera física utilizada em 3×, 3,5× e 7×; região de recorte e eventual troca automática em pouca luz.
- Combinações efetivamente suportadas de prévia, RAW e fotografia processada.

A possibilidade de saída nativa em aproximadamente 200 MP depende do firmware e dos modos disponibilizados. A configuração de alta resolução JPEG não comprova disponibilidade RAW na mesma resolução. Se houver somente saída com binning, informar a limitação medida; não apresentar uma ampliação dessa saída como leitura nativa de 200 MP.

O enquadramento 3× não deve ser automaticamente rotulado como a teleobjetiva óptica de 3,5×. Preservar a focal correta e o campo de visão efetivo.

Como exemplo geométrico, um recorte de 2× em cada dimensão utiliza um quarto dos pixels: 50 MP passam a aproximadamente 12,5 MP. Isso ilustra por que o enquadramento 7× de uma tele de 3,5× não oferece automaticamente os mesmos 50 MP de amostragem; o funcionamento real do OPPO precisa ser medido.

Manter separados os conceitos: leitura de alta resolução do sensor, recorte de zoom, empilhamento, super-resolução e tamanho do arquivo exportado.

### 12.3. Empilhamento adaptativo e fusão das câmeras

O empilhamento passa a integrar o escopo do projeto. Deve ter seleção Automático, Desligado e Qualidade máxima, respeitando o suporte e a capacidade do aparelho.

Avaliar três caminhos distintos:

1. Vários quadros da mesma câmera: redução de ruído e eventual super-resolução, com detecção de movimento.
2. Exposições diferentes: HDR para preservar informação em sombras e altas luzes, incluindo retrato contra o sol.
3. Várias câmeras: experimento de fusão entre principal, teleobjetiva e ultrawide, quando as combinações de captura e a cena permitirem.

A quantidade de quadros deve ser adaptativa. Priorizar quadro-base nítido, rejeitar capturas tremidas ou desalinhadas e reduzir a fusão em áreas móveis. Conservar uma captura original utilizável quando a fusão falhar.

A fusão multicâmera exige compatibilidade de exposição, balanço de branco, sincronização, geometria, distorção, foco, ruído e resolução. Os campos de visão e os pontos de observação são diferentes; profundidade e oclusões tornam a sobreposição incompleta.

A API Android não garante que três câmeras sejam capturáveis simultaneamente em resolução máxima. Testar os grupos físicos e as combinações suportadas. Se apenas captura sequencial for viável, restringir o experimento a cenas adequadas e explicar a condição.

A contribuição das câmeras auxiliares deve ser limitada às regiões de sobreposição e confiança suficientes. Não somar megapixels de sensores diferentes para declarar a resolução da imagem final. Não impor o uso das três câmeras quando isso reduzir a qualidade.

O modo experimental “Ultra High Image” deverá escolher a melhor combinação validada para a cena. A aceitação exige ganho observável de detalhe real ou redução de ruído/maior faixa dinâmica, sem erros de paralaxe, textura falsa, halos ou contornos duplicados.

Preservar o DNG original de referência. Uma eventual saída RAW fundida deve ser identificada como processada, distinta do original de uma única captura.

### 12.4. Retrato, pele, textura e nitidez

Prioridade máxima: fidelidade de tons de pele, textura natural e foco. O processamento não deve clarear sistematicamente a pele, uniformizar pessoas diferentes ou alterar traços e geometria do rosto.

- Aplicar redução de ruído e nitidez de forma seletiva.
- Preservar poros, fios de cabelo e detalhes reais de olhos, cílios, barba, roupa e acessórios.
- Evitar pele plastificada, contraste excessivo nos poros, halos e contornos artificiais.
- Tratar pele e fundo de forma independente quando a máscara tiver qualidade suficiente.
- Avaliar transições de desfoque em cabelos, óculos, orelhas, mãos e roupas.
- Nas sequências de retrato, priorizar exposição curta e quadro-base nítido quando houver movimento.
- Não substituir cílios, poros ou detalhes faciais por conteúdo inventado para aparentar nitidez.

O Find X9 Ultra foi definido pelo usuário como referência desejada de qualidade, com ambição de igualar ou superar seus resultados. Esse é um objetivo de avaliação; não constitui resultado comprovado nem garantia.

Comparar cenas e enquadramentos equivalentes em retrato, contraluz, teleobjetiva, movimento, pouca luz e iluminação mista. Comparar arquivos em dimensões de visualização equivalentes e também a 100%. Os resultados dependem de sensor, óptica, acesso ao hardware e processamento. Até aqui, não houve comparação física com um Find X9 Ultra.

### 12.5. Contraluz, principalmente com sol

Criar um tratamento específico que preserve o rosto e as altas luzes, mantendo contraste natural.

- Avaliar exposição com atenção à pele e às áreas claras.
- Usar HDR de múltiplas exposições quando melhorar a cena.
- Corrigir alinhamento e movimento em rostos, cabelo, mãos e vegetação.
- Permitir ajuste separado de altas luzes, sombras e contraste local.
- Evitar céu artificialmente escurecido, rosto acinzentado ou halos no contorno.
- Não confundir recuperação de altas luzes com reconstrução de informação totalmente saturada em todos os quadros.
- Reflexos e flare óptico devem ser avaliados separadamente; não prometer remoção perfeita por software.

Testes obrigatórios incluem sol atrás da pessoa, luz lateral dura, cabelo contra céu claro e pessoa em movimento. Os valores criativos devem permanecer reversíveis.

### 12.6. Tom e cor ao vivo

Interpretar o pedido de “tons aí vivo” como ajustes de tom e cor ao vivo, antes do disparo, inspirados na experiência dos Estilos Fotográficos do iPhone e das ferramentas de cor da linha X da OPPO. A implementação será própria.

Prever controle simples de Tom e Cor, intensidade do estilo, temperatura e matiz, com ajustes de sombras e altas luzes disponíveis em detalhe. Manter proteção de tons de pele e permitir redefinir ou salvar uma receita pessoal.

A calibração técnica por sensor é distinta do ajuste criativo: uma estabelece a base de cor; o outro expressa a intenção do fotógrafo.

A prévia deve usar o mesmo gerenciamento de cor da exportação, dentro das diferenças de resolução e processamento disponíveis. Empilhamento e HDR podem produzir diferenças após a captura; não prometer equivalência perfeita antes do teste.

### 12.7. Editor integrado

Prever edição não destrutiva com fluxo inspirado em ferramentas fotográficas como Adobe e Luminar, sem presumir acesso a seus algoritmos ou serviços proprietários.

Escopo inicial: exposição, balanço de branco, temperatura/matiz, altas luzes, sombras, brancos/pretos, curvas, HSL, contraste local, redução de ruído, nitidez e recorte/alinhamento.

Evolução condicionada a desempenho e testes: máscaras para pessoa, pele, céu e fundo; ajustes locais; correção de aberração cromática e distorção quando houver perfil confiável.

Manter histórico, antes/depois e receitas editáveis. Recursos automáticos devem ter intensidade controlável e opção de desfazer. A referência a esses editores não implica reproduzir integralmente seus recursos.

### 12.8. Paleta de cores exportável

O usuário escolheu ambas as opções: uma versão com faixa de cores na imagem e uma paleta em arquivo separado.

- Preservar a fotografia limpa e o DNG original.
- Permitir uma exportação derivada com faixa abaixo da fotografia, ampliando a tela da composição sem reduzir silenciosamente os pixels da área fotográfica.
- Exportar também uma paleta visual independente e os respectivos códigos de cor.
- Extrair cores da imagem final tratada, evitando usar apenas a miniatura.
- Declarar o espaço de cor; converter adequadamente para sRGB antes de apresentar códigos HEX/RGB destinados a esse espaço.
- Permitir copiar códigos e exportar dados em JSON como opção complementar.
- Regenerar a paleta quando o tratamento da fotografia mudar.
- Exportar apenas a paleta ou a imagem com faixa quando o usuário selecionar essas opções.

### 12.9. Critérios adicionais de aceitação

- Cada zoom identifica a câmera física e a resolução efetivamente obtida.
- A saída nativa de aproximadamente 200 MP só aparece como validada após captura, inspeção do arquivo e verificação da origem.
- O relatório distingue resolução nativa, recorte e ampliação computacional.
- Empilhamento deve mostrar benefício em comparação com o quadro de referência, sem degradar o rosto.
- A fusão multicâmera precisa superar a melhor captura individual na finalidade proposta e na região analisada.
- Tons de pele devem ser avaliados em pessoas e iluminações diversas, com referência controlada quando possível.
- Contraluz deve ser avaliado em movimento e em cena estática.
- Ajustes ao vivo, edição e exportação devem manter receitas e cor coerentes.
- A exportação de paleta não deve substituir a fotografia original.
- Nenhuma alegação de superioridade ao Find X9 Ultra será feita sem comparação verificável.

### 12.10. Modo Natural e primeira implementação

Requisito acrescentado pelo usuário: as fotografias devem representar o ambiente e evitar dominantes artificiais quentes ou frias. O modo padrão passa a se chamar Natural.

Uma cena realmente iluminada por luz quente ou fria pode manter essa característica; o objetivo não é neutralizar toda iluminação para uma temperatura fixa. A fidelidade deve considerar luz, objetos neutros e tons de pele.

O retrato final deverá ter seletor de profundidade de campo, com f/5,6 como referência padrão de simulação, e transições graduais entre primeiro plano, pessoa em foco e segundo/terceiro planos. A abertura real da lente deve permanecer nos metadados ópticos.

O seletor final deverá cobrir, inicialmente, f/1,4, f/2, f/2,8, f/4, f/5,6, f/8, f/11 e f/16. O plano em foco é definido pela pessoa selecionada ou pelo toque do fotógrafo; o desfoque aumenta gradualmente com a distância em relação a esse plano. O segundo e o terceiro planos não devem receber a mesma máscara de desfoque uniforme. Objetos à frente também exigem tratamento coerente com a profundidade e com as oclusões.

A implementação profissional deve usar profundidade contínua, preservar bordas e detalhes delicados e evitar halos em cabelo, óculos e contornos contra o sol. A relação entre número f e desfoque precisa considerar a referência de focal, o tamanho do sensor e a distância de foco disponível. O ajuste digital não recupera detalhes já desfocados pela óptica. Até haver calibração verificável, a interface deve identificar o controle como simulação.

Na versão instalável 0.1.0-lab.1:

- Núcleo próprio Camera2, implementado para diagnóstico e captura real. Nenhum código ou modelo dos projetos pesquisados foi incorporado nesta primeira etapa.
- Natural usa o balanço de branco automático do aparelho, sem Kelvin fixo ou filtro de aquecimento/resfriamento.
- JPEG original, RAW/DNG quando disponível, tentativa de resolução máxima exposta, metadados por captura e diagnóstico exportável.
- Controles de ISO/exposição onde suportados, compensação EV, foco por toque e trava de balanço de branco.
- HDR e retrato usam extensões disponíveis do fabricante. A disponibilidade informada pela API é distinguida de captura bem-sucedida.
- O seletor f/5,6 só controla intensidade quando a extensão expõe esse parâmetro. O mapeamento é experimental, não uma calibração física de abertura ou dos planos de profundidade. Caso contrário, o app informa profundidade automática do fabricante.
- Paleta separada em PNG e códigos JSON da última fotografia.
- Sem permissão de internet ou transmissão automática.
- RAW limitado pelo orçamento de memória do laboratório: até aproximadamente 50 MP em Máxima e 16 MP no modo comum. Essa é uma condição da implementação atual, não uma afirmação sobre o limite do sensor.

Ainda pendentes: calibração própria de cor/pele, revelação RAW própria, empilhamento, fusão multicâmera, retrato com profundidade própria entre planos, estilos de filme, editor avançado e faixa de paleta incorporada à fotografia.

A versão de laboratório serve para verificar quais recursos a OPPO libera e produzir fotografias de referência. Não comprova acesso a 200 MP, ausência de dominantes ou equivalência ao Find X9 Ultra.

## 13. Fontes técnicas consultadas

- Android — DngCreator: https://developer.android.com/reference/android/hardware/camera2/DngCreator
- Android — capacidades RAW e resolução máxima: https://developer.android.com/reference/android/hardware/camera2/CameraMetadata
- Android — configurações de câmera: https://developer.android.com/reference/android/hardware/camera2/CameraCharacteristics
- Android — extensões de câmera: https://developer.android.com/media/camera/camera-extensions
- Android — Ultra HDR: https://developer.android.com/media/platform/hdr-image-format
- OPPO — Find X8 Ultra e RAW Max: https://www.oppo.com/en/newsroom/press/oppo-hasselblad-partnership-imaging-system/
- OPPO — Reno16 Pro: https://www.oppo.com/ph/smartphones/series-reno/reno16-pro/specs/

- Android — modos de pixel e resolução máxima: https://developer.android.com/reference/android/hardware/camera2/CaptureResult
- Android — API multicâmera: https://developer.android.com/media/camera/camera2/multi-camera
- OPPO — câmeras do Reno16 Pro global: https://www.oppo.com/en/smartphones/series-reno/reno16-pro/specs/
- OPPO — enquadramentos 3,5× e 7×: https://www.oppo.com/en/smartphones/series-reno/reno16-pro/
- OPPO — referência Find X9 Ultra: https://www.oppo.com/en/smartphones/series-find-x/find-x9-ultra/specs/
- Apple — Estilos Fotográficos: https://support.apple.com/pt-br/guide/iphone/iph629d2cd37/ios
- Adobe — mesclagem HDR: https://helpx.adobe.com/br/lightroom-classic/desktop/process-and-develop-photos/hdr-photo-merge.html
- Google — exemplo de fusão e zoom computacional: https://blog.google/products-and-platforms/devices/pixel/super-res-zoom-google-pixel/
- Pesquisa complementar do projeto: Camera_OPPO_Pesquisa_GitHub_v01.md.

As fontes documentam as APIs e os recursos anunciados. Não comprovam a compatibilidade desta futura implementação com os aparelhos físicos.


### 12.11. Natural Cam — revisão após o teste no Find X8 Ultra

**Nome aprovado:** Natural Cam. Manter o identificador `br.com.duvier.camera.lab` e a assinatura privada da primeira versão para atualização direta. Os nomes antigos em caminhos de código e na chave não alteram o nome visível do produto. Preservar o histórico dos APKs e fontes anteriores.

#### Visor e operação

Remover o cabeçalho grande e o botão Lentes. O visor ocupa a largura disponível; controles menores e seletores horizontais seguem as referências OPPO. Máxima fica à esquerda do obturador e Edição à direita; Pro fica embaixo. HDR fica junto de Info no topo, somente quando a extensão é disponibilizada pela câmera. Não mostrar um seletor de HDR sem implementação funcional.

Adicionar foco e medição por toque, compensação EV junto ao ponto, trava AE/AF por pressão longa/cadeado conforme suporte, foco manual e peaking opcional. O peaking é uma ajuda visual de bordas da prévia, não uma medição óptica calibrada e não deve aparecer na fotografia salva.

Agrupar solicitações rápidas de zoom, preferir o último destino e executar a abertura de câmeras/edição fora da interface. O alinhamento visual da troca usa os novos quadros disponíveis. A fluidez e os limites de cada driver precisam de teste físico; não prometer ausência absoluta de travamentos em todo Android.

#### Óptica, RAW e megapixels reais

A captura escolhe a óptica física acessível de acordo com as distâncias focais anunciadas. Nos fluxos físicos de uma câmera lógica, usar saídas identificadas e requisições apropriadas à câmera física. Parear RAW, JPEG e metadados por timestamp. Se o RAW não puder ser confirmado, preservar o JPEG e informar o problema.

O motor deixa de impor um teto fixo de 16 MP ao RAW normal. Seleciona a maior resolução disponibilizada dentro de um orçamento de memória. Máxima tenta também o modo de resolução máxima quando disponível; combinações de segurança são tentadas se a sessão for recusada. A confirmação depende de uma captura bem-sucedida, não apenas do catálogo de capacidades.

No X8, 3× e 6× devem usar as ópticas correspondentes quando acessíveis. 7×/8× podem ser recortes da tele de 6×. No Reno16 Pro, a óptica de 3,5× e o enquadramento de 7× devem ser distinguidos. O pedido de 3× não pode produzir o campo mais amplo usando uma tele de 3,5× sem informação externa; deve usar outra óptica compatível e registrar o recorte.

Nos aumentos digitais, recortar o JPEG e escrever `DefaultCropOrigin`/`DefaultCropSize` no DNG, preservando todos os blocos RAW originais. Leitores que ignorem esses campos poderão mostrar toda a área capturada pela óptica. Não converter um JPEG ampliado em suposto RAW nativo nem afirmar 200 MP extraídos sem evidência. Recortar 2× um RAW de 50 MP usa aproximadamente 12,5 MP da área original.

Registrar ID da lente, focal óptica, equivalente de 35 mm calculado, fator digital, dimensões capturadas e área utilizada. Manter a calibração do DNG. A resolução máxima da principal não representa automaticamente a resolução das teleobjetivas.

#### Retrato, cor e texturas

Integração local de Depth Anything V2 Small para profundidade relativa e desfoque por planos. Seletor virtual f/1,4–f/16, inicial f/5,6, com escolha do plano em foco. A abertura virtual não substitui a abertura física no EXIF e não representa profundidade de campo óptica calibrada. Manter o original em falhas; avaliar cabelo, óculos, bordas, contraluz e movimento nos aparelhos.

Natural usa o balanço de branco automático anunciado pela câmera, sem impor temperatura por horário. Não aplicar estilo criativo, alisamento de pele ou clareamento global de sombras por padrão. Solicitar redução de ruído e nitidez OFF quando suportadas e registrar os modos observados; isso não remove o processamento obrigatório do fabricante no JPEG.

A política de cena tem coletor, ação limitada e auditor. A proteção de altas luzes é limitada, estabilizada e subordinada ao fotógrafo. Não foi treinado um modelo de cena com imagens OPPO. O modelo pré-treinado de profundidade é um componente separado.

#### Editor e originais

O editor abre o original da última captura ou uma imagem importada compatível com o decodificador Android. O DNG ainda depende de revelador externo ou do JPEG pareado; não declarar revelação RAW própria.

São 40 receitas próprias, dez por família Leica, Fuji, Kodak e Hasselblad, incluindo cor e preto e branco. Não são LUTs oficiais nem reproduções exatas da ciência de cor das marcas. Disponibilizar intensidade, exposição, contraste, realces, sombras, temperatura, tonalidade, matiz, saturação, HSL em oito faixas, curvas RGB/canais, granulado, vinheta e nitidez positiva/negativa.

Rosto e Máscara usam ajustes locais manuais com pincel/borracha. Não aplicar alteração facial ou suavização automática. Comparação com o original, desfazer e receita JSON devem preservar o controle do fotógrafo.

Cada exportação recomeça na fonte original, processada por blocos sem reduzir o arquivo à resolução da prévia. JPEG de saída sRGB SDR, qualidade 100 e amostragem de cor 4:4:4. Isso não constitui mestre RAW de 16 bits ou exportação Ultra HDR com gain map. Não sobrescrever o original.

A paleta de seis cores é exportada separadamente em PNG/JSON, a partir da imagem final. A faixa de paleta incorporada à fotografia permanece pendente.

#### Pesquisa anexada e limites

Anexar `Natural_Cam_Pesquisa_Integracoes_v02.md`, com a revisão de GCam, configuração AGC para o X8, código Photon examinado, bancos Cube++ e HDR+, contratos/licenças do modelo de profundidade e referências de cor/fotografia. O arquivo AGC específico retornou HTTP 403; não foi importado. Não incorporar binários proprietários da GCam nem apresentar configurações comunitárias como código do HDR+ atual.

As anotações de referência Cube++ ajudam a desenhar avaliações por ambiente e iluminação; não copiar ganhos Canon para OPPO. O horário é contexto e não substitui a luz medida. Não houve treinamento de comportamento por horário. O teste com DNG real da base HDR+ verifica integridade de recorte, não qualidade do sensor OPPO.

Continuam pendentes: empilhamento/fusão RAW própria, fusão de três câmeras, revelação DNG própria, faixa de paleta na fotografia, calibração física por aparelho e demonstração de qualidade superior a outras câmeras. A experiência de câmera dedicada não modifica firmware nem desbloqueia recursos privados.

#### Aceitação desta versão

Consultar `VALIDACAO.md` para resultados de compilação, assinatura, testes de lógica, integridade DNG e execução Android. Separar claramente os testes virtuais da validação fotográfica real. Priorizar no campo: X8 RAW 3×/6×/7×/8×; Reno16 Pro 3×/3,5×/7× e Máxima; depois Reno14 e outros Android de 64 bits. Avaliar pares originais, pele, tecido, madeira, sombras, sol e luz artificial sem recompressão.


### 12.12. Pesquisa dos fabricantes dos sensores — Reno16 Pro

Consultar a nota `Natural_Cam_Reno16Pro_Sensores_Integracao_v01.md`. Distinguir CPH2863 global e PMK110 chinês. HP5/JN5/GC50F6 são candidatos de identificação externa; não copiar calibrações de fichas comerciais. Samsung documenta modos e remosaico de seus componentes, mas a exposição dessas funções no telefone precisa ser demonstrada. CameraUnit oficial oferece SAT/HDR/retrato/noturno em modelos suportados; integração e suporte ao Reno16 Pro ainda não confirmados. O APK registra referências e sensores de luz/cor no diagnóstico, sem interpretar unidades desconhecidas como Kelvin. No caminho RAW inicial, tenta JPEG com resolução até a do RAW selecionado; as alternativas de segurança continuam explícitas.


### 12.13. Retrato máximo e investigação ProShot

A nota `Natural_Cam_Alta_Resolucao_ProShot_v01.md` distingue leitura máxima, remosaico, empilhamento, ampliação e aproveitamento da câmera OPPO. Retrato pós-processado deve manter a resolução da fotografia/recorte, não a do mapa de profundidade. A tele de 50 MP não passa a ter 200 MP nativos; registrar a área efetiva em cada zoom. O diagnóstico informa capacidades de resolução máxima e remosaico, fator de binning e matrizes de pixels quando anunciadas. A sessão própria de remosaico ainda não está implementada. O guia ProShot confirma limites impostos pelo fabricante; seu Evaluator é referência independente, sem teste nos aparelhos do usuário ou promessa de desbloqueio.


### 12.14. Reprovação de qualidade e objetivo permanente

O usuário reprovou a saída fotográfica da versão 0.2.0: ruído forte, dominante verde, marcador EV fora da trilha e atraso nos ajustes. A qualidade da imagem é condição obrigatória de aceitação; testes funcionais e emulador não comprovam o resultado fotográfico. Suspender a indicação da versão como solução fotográfica validada.

Prioridades: identificar a origem da dominante em JPEG/DNG/edição, rever a solicitação indiscriminada de redução de ruído e nitidez OFF, manter textura e cor natural, corrigir o marcador EV e responder durante o gesto de exposição/zoom/edição. Zoom em pills na faixa preta. A troca física de lentes precisa ser medida no aparelho. Não converter metas de fluidez em alegação de ausência universal de engasgos.

O objetivo, os critérios mensuráveis e as pendências estão em Natural_Cam_GOAL_Qualidade.md; as regras do código constam em AGENTS.md. O modo nativo /goal não foi ativado por esta sessão, que não expõe essa ação; o comando está preparado no documento. A meta continua aberta até haver validação de arquivos e comportamento reais nos OPPO.


### 12.15. Correções candidatas 0.2.1 — resposta aos dois aparelhos

O usuário confirmou a ocorrência de ruído e dominante verde nos dois aparelhos (Find X8 Ultra e Reno16 Pro), em RAW e JPEG. A confirmação do modelo e formato não substitui a análise do JPEG/DNG original com o relatório da mesma captura.

O código candidato deixa de impor redução de ruído/nitidez OFF no JPEG: preserva os padrões do template da câmera, e OFF fica como opção explícita no Pro. A exposição manual mantém AWB automático e não injeta ganhos e matriz de cor congelados da prévia. A heurística de subexposição para altas luzes passa a ser opt-in; o padrão usa a exposição automática da câmera. Essas mudanças tratam causas plausíveis e removem intervenções não calibradas; não demonstram que a dominante verde real tenha sido corrigida.

Atualizações de exposição e zoom são limitadas por frequência sem adiar todo o trabalho até o fim do gesto. Durante a abertura de uma lente, permanece apenas a intenção de zoom mais recente. O marcador EV usa geometria contínua limitada à trilha, independentemente dos passos discretos da câmera. O zoom passa a pills na faixa preta, fora da fotografia.

O editor prioriza a prévia, reutiliza as tabelas de transferência de cor e publica imagens intermediárias durante o arraste. Usa até 640 pixels na prévia interativa e retorna à prévia maior ao soltar. A exportação continua partindo da fonte original. Alterar o número f virtual reutiliza a profundidade já estimada quando o plano em foco não muda.

Resultados e limites constam em `Natural_Cam_Auditoria_Qualidade_0.2.1.md`. O build é interno e não recebe aprovação fotográfica com base em testes virtuais. A meta continua aberta para confirmar cor/ruído e medir fluidez no hardware real; não liberar como qualidade corrigida sem essa evidência.
