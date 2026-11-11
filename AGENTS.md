# Natural Cam — regras obrigatórias do projeto

O usuário Duvier definiu qualidade fotográfica real como critério obrigatório de aceitação. Fotografias com dominante verde artificial, ruído excessivo, pele falsa, textura destruída, halos ou resolução reduzida silenciosamente reprovam a entrega. Manter essa regra nas próximas sessões.

- Tratar a 0.2.0-lab.2 como reprovada pelo teste físico do usuário. Testes funcionais em emulador não demonstram qualidade profissional.
- Corrigir e investigar a origem das falhas antes de declarar a imagem corrigida. Distinguir JPEG do sensor, JPEG editado e DNG/revelador.
- Preservar originais, calibração RAW e metadados reais por lente. Não inventar detalhe, cores, megapixels ou resultados de testes.
- Não desligar indiscriminadamente o processamento de ruído/cor do JPEG para tentar preservar textura. O processamento escolhido precisa ser avaliado; RAW original e decisões criativas do fotógrafo permanecem preservados.
- Exposição: marcador limitado à trilha, movimento contínuo e solicitações Camera2 durante o arraste, sem debounce que espere o gesto terminar.
- Zoom: resposta contínua, fila limitada à intenção mais recente, sem reinicializar repetidamente uma lente que já está abrindo. Não prometer transições físicas sem interrupções antes de medir no aparelho.
- Zooms em pills na faixa preta, fora da fotografia. Editor responde durante o gesto; a prévia interativa pode ser reduzida, mas a exportação deve partir da resolução original.
- Não considerar a meta concluída por compilar, instalar ou passar testes sintéticos. A conclusão exige comparação fotográfica real nos OPPO e verificação de fluidez/qualidade. Sem acesso ou arquivos suficientes, registrar a pendência e não declarar qualidade aprovada.
- Não distribuir uma nova versão como solução fotográfica validada enquanto a dominante verde e o ruído não forem confirmados como corrigidos. Builds intermediários servem para testes internos.

Objetivo e critérios mensuráveis: `GOAL.md`. Preservar assinatura, pacote e versões anteriores.

## Como trabalhar (regras de prompt, 11/11/2026)

Para quem e o que não pode falhar: quem usa e julga é o Duvier, fotógrafo mobile premiado, nos OPPO, comparando com a câmera original a 100 %. As regras de qualidade acima valem acima de qualquer teste passando. Pacote `br.com.duvier.camera.lab`; o app irmão The Natural Cam (repositório `the-natural-cam`, pacote `br.com.duvier.thenaturalcam`) é outro projeto, sem misturar código nem pacotes.

Estado do trabalho: ler `docs/harness/PLANO.md`, `PROGRESSO.md` e `VALIDACAO.md` antes de começar e atualizá-los ao terminar. Regressões no PC: `python3 tests/run_harness_smoke.py` e `python3 tests/run_host_checks.py --json-jar <org.json.jar>`.

Dono do código: este projeto é seu, você é quem vai mantê-lo a longo prazo e o desenho dele é decisão sua. Se for preciso reorganizar para fazer bem, reorganize; prefiro o código mais simples no final, não remendo ao lado. Ao mexer num trecho de linhas compactadas muito longas, deixe-o legível.

Pedir OK antes de: disparar o workflow de build assinado ou usar a assinatura original; mudar pacote, assinatura ou baixar o `versionCode`; instalar no aparelho, desinstalar, limpar dados ou comandar a tela por adb; distribuir um APK como correção fotográfica validada; fazer push no `main` ou push forçado. Não precisam de OK: rodar regressões no PC, criar testes, refatorar, atualizar `docs/harness/` e subir o `versionCode`. Aprovação vale quando é específica ("sim, dispare o build assinado"); "ok" solto não vale para essas ações.

Terminar o que foi pedido: não parar enquanto faltar parte do pedido. Se aparecer erro, descobrir a causa e resolver. Seguir com tudo o que não depende do usuário e perguntar só as decisões que de fato estão em aberto. Obstáculos comuns são diagnosticados e contornados; bloqueios deliberados (lista acima, falta do aparelho ou dos arquivos originais) são registrados como pendência, seguindo com o resto. Para mudança grande no pipeline de imagem, antes de escrever código, entrevistar o usuário, uma pergunta por vez, começando pelas que mudariam a forma de construir.

Prova antes de "pronto": só dizer que algo funciona depois de ver funcionando, separando três níveis (regressão no PC; Android virtual ou real; foto real pareada JPEG + DNG + JSON comparada com a câmera OPPO). Qualidade fotográfica só é aprovada no último nível. Ao fim de cada etapa grande, fazer revisão em sessão nova (outro agente ou nova conversa) só com o pedido, o diff, `GOAL.md` e este arquivo; sem isso, rotular como autorrevisão.

Relatório final: começar pelo resultado, em frases curtas: o que ficou pronto, em qual nível foi conferido, o que ficou de fora e por quê, para quem não viu os comandos nem os arquivos. Raciocínio: esforço médio para ajustes pequenos e alto para correção de cor, ruído ou fluxo da câmera, definido na configuração da ferramenta e não por frases como "pense menos" ou limites de tokens no texto.
