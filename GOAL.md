# Goal — Natural Cam: qualidade fotográfica e resposta contínua

Data: 8 de novembro de 2026. Prioridade: crítica. Estado: correções candidatas implementadas; 77 asserções de lógica e 33 Android aprovadas; aguardando arquivos originais; qualidade fotográfica ainda sem aprovação.

## Objetivo

Corrigir o Natural Cam para entregar fotografias naturais de alta qualidade no Find X8 Ultra, Reno16 Pro e Reno14, eliminando dominantes artificiais e ruído excessivo atribuíveis ao aplicativo, preservando textura, tons de pele, sombras e originais. Fazer exposição, zoom e edição responderem durante o gesto, com filas limitadas e sem adiar todas as atualizações até o usuário parar. Manter seletores de zoom em pills na faixa preta e o marcador EV dentro da trilha.

## Critérios de conclusão

1. Reproduzir a dominante verde e o ruído em arquivos originais; identificar se surgem no JPEG da câmera, no processamento do editor, no DNG ou no revelador. Confirmar a correção com comparação da mesma cena, lente e iluminação nos aparelhos reais.
2. Comparar pele, áreas neutras, tecido, cabelo e sombras a 100%, com a câmera OPPO e o original preservado. Reprovar dominante artificial, ruído cromático excessivo, alisamento destrutivo, halos e perda silenciosa de resolução.
3. O marcador de exposição permanece inteiro na trilha, incluindo extremos e assimetria do intervalo EV; acompanha o dedo de forma contínua. Novas requisições de exposição chegam à câmera durante o arraste, respeitando os passos reais do hardware.
4. O zoom acompanha o gesto, agrupa apenas trabalho excedente e não provoca reabertura infinita. Medir transições, tempos e intervalos entre quadros nos OPPO; respeitar as câmeras físicas e registrar o recorte.
5. A prévia do editor muda durante um arraste contínuo. Meta de avaliação em aparelho: pelo menos 20 atualizações/s e latência de interação p95 até 100 ms nos ajustes comuns, sem perda de qualidade na exportação. Se o aparelho não atingir a meta, registrar a medição e continuar a otimização.
6. Todos os zooms ficam em pills com rolagem horizontal na faixa preta, sem sobrepor a fotografia ou cortar números.
7. Compilação e regressões relevantes passam; produzir relatório que separe resultados de código, Android virtual e fotografia real. Não declarar a meta concluída se qualquer critério obrigatório estiver pendente.

## Evidência disponível e pendências

- O usuário enviou quatro capturas de tela com ruído, dominante verde, edição lenta e marcador EV fora da linha. Uma tela mostra ISO 6400; esse valor não demonstra sozinho a causa de todas as falhas.
- A versão 0.2.0 forçava redução de ruído e nitidez OFF, adiava atualizações da câmera e do editor com debounce e não limitava a posição do marcador à trilha. Esses pontos exigem correção e regressão.
- O usuário confirmou falhas nos dois aparelhos (Find X8 Ultra e Reno16 Pro), em RAW e JPEG.
- Ainda necessários: JPEG original, DNG pareado e JSON de captura/diagnóstico de cada aparelho. Capturas de tela não contêm os dados RAW nem a calibração original.
- Sem acesso físico aos celulares nesta sessão; desempenho e qualidade final permanecem pendentes de validação real.

## Comando preparado

```text
/goal Corrigir o Natural Cam conforme GOAL.md, priorizando a qualidade fotográfica real. Eliminar as causas de dominante verde e ruído excessivo atribuíveis ao app; preservar textura, pele, sombras, RAW e resolução. Fazer exposição, zoom e edição responderem continuamente durante o gesto, limitar o marcador EV à trilha e colocar os zooms em pills na faixa preta. Executar regressões e medir o comportamento nos OPPO. Não declarar conclusão nem qualidade aprovada sem comparação de arquivos reais e sem cumprir os critérios de GOAL.md. Quando faltar acesso físico ou um arquivo indispensável, registrar o bloqueio e solicitar apenas essa evidência.
```

O arquivo registra o objetivo do projeto. A ativação do modo nativo `/goal` precisa ser feita na interface que exponha o comando; esta sessão não disponibiliza uma ação para ativá-lo.


## Checkpoint de implementação

- Preservados os padrões de processamento JPEG da câmera; OFF é escolha explícita no Pro. Exposição manual mantém AWB automático e o template de cor. A heurística de subexposição de altas luzes fica opt-in.
- Requisições de exposição e zoom passam a ser limitadas por frequência, sem debounce que espere o fim do gesto; lente em abertura recebe apenas o destino mais recente.
- Marcador EV contínuo e limitado à trilha; pills de zoom na faixa preta; prévias intermediárias do editor publicadas durante o gesto, com resolução interativa limitada e exportação original preservada.
- 77 asserções de lógica passaram. As 33 asserções Android passaram. Resultados e limites estão na auditoria. Cor, ruído e fluidez nos OPPO continuam pendentes de arquivos originais e validação física.


## Checkpoint 0.2.2-teste.1 (08/11/2026)

Código corrigido para recuperação finita da lente, espera AWB única de até 110 ms, disparo ao toque inicial, transição de zoom condicionada ao quadro configurado e registro de latência. Testes de host aprovados. Não declarar qualidade/latência física aprovadas: APK não compilado nesta sessão e arquivos originais pareados ainda pendentes. Evidência em `docs/harness/`.
