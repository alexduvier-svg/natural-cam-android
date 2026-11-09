# Natural Cam — 0.2.1-candidate.1: validação de correções

Qualidade fotográfica ainda não aprovada. A versão 0.2.0 foi reprovada pelo teste físico do usuário nos dois aparelhos, em RAW e JPEG.

- Compilação, assinatura e alinhamento APK: aprovados. Mesmo certificado da primeira versão.
- Regressões de lógica: 69 de qualidade/RAW/óptica/interação mais oito de geometria de captura (77 no total).
- Android virtual API 35 x86_64: 17 asserções principais e 16 de interação aprovadas (33 no total).
- O teste de interação confirma requisições de exposição e zoom durante o gesto, preservação dos padrões de processamento da câmera e AWB automático na exposição manual; confirma também pills fora da fotografia e prévias do editor antes de soltar o controle.
- Os testes virtuais não validam cor/ruído dos OPPO, frequência de quadros, latência p95 de 100 ms ou troca física de lentes. O último intervalo de prévia medido no emulador foi 5.987 ms; esse resultado não aprova fluidez e não foi convertido em promessa de desempenho nos celulares.
- O emulador apresentou avisos ANR do Quickstep. As imagens foram usadas como inspeção parcial de layout, sem alegação de estabilidade física.

Evidências e análise: `docs/Natural_Cam_Auditoria_Qualidade_0.2.1.md`. Critérios de liberação: `GOAL.md` e `AGENTS.md`. O histórico da 0.2.0 está em `docs/qa/VALIDACAO_0.2.0_historico.md`.

O APK candidato é intermediário de teste interno; não está liberado como correção fotográfica validada. Ainda são necessários JPEG, DNG e JSON originais do mesmo disparo, primeiro no Find X8 Ultra e depois no Reno16 Pro.
