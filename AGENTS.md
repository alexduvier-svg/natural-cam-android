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
