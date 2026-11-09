# Natural Cam — sensores e integração do Reno16 Pro

Consulta: 8 de novembro de 2026. Esta nota separa especificações da OPPO, características dos componentes e indícios de identificação. O código comercial do telefone não é prova do sensor instalado em cada lote.

## O que a OPPO confirma

A ficha global identifica o Reno16 Pro 5G como CPH2863. A versão chinesa aparece como PMK110 e possui diferenças de configuração; não devem receber cegamente o mesmo perfil.

O conjunto traseiro anunciado tem principal de 200 MP f/1,8 com OIS, ultrawide de 50 MP f/2 com autofoco e teleobjetiva de 50 MP f/2,8 com OIS. A fabricante anuncia 3,5× óptico e apresenta 7× entre os enquadramentos. Não documenta uma segunda teleobjetiva óptica de 7×.

A ficha também lista sensor de temperatura de cor. Isso não demonstra acesso público às suas leituras ou à calibração usada pelo aplicativo original.

Fontes primárias:

- https://www.oppo.com/en/smartphones/series-reno/reno16-pro/specs/
- https://www.oppo.com/en/smartphones/series-reno/reno16-pro/
- https://www.oppo.com/cn/smartphones/series-reno/reno16-pro/specs/

## Quais chips foram identificados

As referências externas consultadas apontam Samsung ISOCELL HP5 para a principal, Samsung ISOCELL JN5 para tele e frontal, e GalaxyCore GC50F6 para a ultrawide. Portanto, a investigação não deve ficar restrita a Sony e Samsung.

Esse é um mapa de candidatos, ainda sem confirmação do componente do aparelho de Duvier pela OPPO/firmware. A ficha Kimovil recuperada deixa o campo do sensor como desconhecido; não serve para fixar HP5, JN5 ou GC50F6. Resultados indexados do GSMArena identificam esses componentes, mas a abertura das páginas de notícia/câmera retornou HTTP 402. Não se deve apresentar a leitura integral dessas páginas como concluída.

Referências indicadas pelo usuário:

- https://www.kimovil.com/en/where-to-buy-oppo-reno16-pro
- https://www.gsmarena.com/oppo_reno16_pro-review-2973.php
- https://www.gsmarena.com/weekly_poll_would_you_buy_the_oppo_reno16_what_about_the_oppo_reno16_pro-news-73462.php

## O que a Samsung documenta sobre o HP5

A Samsung descreve o HP5 como 200 MP, formato óptico 1/1,56 e pixels de 0,5 µm. Documenta modos de 200 MP, 50 MP e 12,5 MP, além de remosaico e zoom no sensor. Também distingue remosaico 2× por hardware de 4× por software.

Isso explica como um componente de 200 MP pode entregar 12,5 MP por binning. Não demonstra que a OPPO exponha todos esses modos em RAW/Camera2. O remosaico do fabricante também não é equivalente a simplesmente ampliar um JPEG.

- https://semiconductor.samsung.com/image-sensor/mobile-image-sensor/isocell-hp5/
- https://semiconductor.samsung.com/news-events/tech-blog/inside-isocell-hp5-exploring-05-pixel-technology/

## O que a Samsung documenta sobre o JN5

O JN5 tem 50 MP, pixels de 0,64 µm e formato óptico de aproximadamente 1/2,76. A documentação descreve remosaico de hardware para zoom 2×, foco Super QPD e HDR com Dual Slope Gain.

Se esse for o componente da tele de 3,5× e o firmware expuser o caminho apropriado, o remosaico 2× será uma referência relevante para investigar 7×. Essa é uma hipótese de integração, não confirmação de que o Natural Cam já acione o remosaico proprietário.

- https://semiconductor.samsung.com/image-sensor/mobile-image-sensor/isocell-jn5/
- https://news.samsung.com/br/samsung-revela-sensores-de-imagem-versateis-para-fotografia-de-qualidade-superior-em-smartphone

## Caminho oficial da OPPO: CameraUnit

O SDK oficial CameraUnit e seu demonstrador público incluem SAT Zoom, HDR fotográfico, retrato e captura noturna, sujeitos ao modelo. O módulo examinado depende de `com.oplus.ocs:camera:1.0.7`. A lista pública do demonstrador cobre gerações antigas e não confirma Reno16 Pro, Reno14 ou Find X8 Ultra.

A página de configuração depende de JavaScript e não forneceu nesta consulta o texto integral de acesso atual. O SDK não foi embutido ou ativado no APK. É uma investigação prioritária para uma próxima integração: confirmar disponibilidade por região/ColorOS, requisitos atuais, licença dos binários e suporte de captura por formato. Não presumir que acesso a JPEG processado inclua RAW de 200 MP.

- https://github.com/oppo/CameraUnit
- https://github.com/oppo/CameraUnit/blob/main/camerax/build.gradle
- https://developers-eu.oppomobile.com/newservice/capability?pagename=cameraunit
- https://open.oppomobile.com/new/developmentDoc/info?id=10723

## Aplicação ao Natural Cam desta entrega

O motor usa capacidades reais por lente para selecionar RAW/JPEG, resolução e estabilização. No primeiro caminho RAW, tenta JPEG até a resolução do RAW escolhido; se a combinação for recusada, usa a sequência de segurança e registra o ocorrido. Máxima tenta as saídas máximas expostas pelo sistema.

O diagnóstico recebeu referência do Reno por código e inventário dos sensores de luz/cor anunciados ao Android. Leituras de unidades desconhecidas não são convertidas em Kelvin. A referência não altera ganhos RAW, níveis de preto, matrizes ou IDs de lente.

Para 7×, o caminho atual continua sendo a óptica acessível mais adequada com recorte explícito, preservando o RAW completo. Não é apresentado como remosaico de hardware ou segunda lente óptica de 7×. A comparação prioritária é entre esse caminho, a captura OPPO e os arquivos reais do telefone.
