# Validação Harness — Natural Cam 0.2.2-teste.1

Data: 08/11/2026. Estado da base: ZIP `Natural_Cam_0.2.1_Correcoes_Em_Validacao_Fonte.zip` / APK `Natural_Cam_0.2.1_Correcao.apk` preservados. Testes apenas no host Linux/JDK; **não** em OPPO.

## Evidência executada

Comando: `cd <raiz-da-fonte> && python3 tests/run_harness_smoke.py`.

Resultado: **16/16 contratos estruturais aprovados**, **14/14 verificações Java da política de atraso e recuperação**, **8/8 verificações Java de geometria de captura**. Conteúdo original do modelo de profundidade e políticas AWB/JPEG/RAW presentes.

Alterações observadas: limite da espera adicional da AWB em um intervalo de 110 ms (era até 5 x 160 ms); obturador reage ao toque inicial com `performClick()` apenas uma vez; zoom não é ampliado artificialmente na lente anterior; a transição aguarda a prévia configurada; erros/desconexão/timeouts de abertura anulam a solicitação pendente e tentam uma única restauração ao último estado funcional; Máxima e indicação de zoom também retornam no fallback; JSON registra tempos de entrada→job, entrada→envio e entrada→salvamento. Cálculos de latência são medições a serem observadas, sem metas fictícias de ganho garantido.

**Autorrevisão**: não houve segundo agente independente nem dispositivo físico.

## Impedimentos reais

- Não há Android SDK plataforma 36, Android build-tools 36, empacotador D8/apksigner nem instrumentação Android nesta sessão. A recompilação integral do APK não foi executada.
- O código tem dependências Android/Java que não podem ser verificadas pelo `javac` puro sem as bibliotecas de Android. Não declarar compilação Android aprovada.
- Não há fotos JPEG/DNG originais pareadas dos OPPO nem logs instrumentados de toque/quadros. Qualidade fotográfica e latência real continuam sem aprovação.
- A assinatura privada está preservada em arquivo separado e **não foi incluída** na fonte, nem compartilhada.

## Próximos testes com Android instalado

1. Build com Android SDK 36 e assinatura original; verificar `applicationId=br.com.duvier.camera.lab`, `versionCode=4` e integridade da assinatura antes de atualizar por cima.
2. Executar testes Android existentes, emulador e aparelhos reais; guardar logs de captura/latência no JSON.
3. Repetir zoom 1→3→6→8 e retorno; simular falha e desconexão; confirmar atualização do marcador, visor sem saltos de lente e recuperação sem loops.
4. Medir tempo do toque ao envio do disparo, do toque ao arquivo salvo, p50/p95, variando ISO/RAW/JPEG/luze baixa; comparar com versão 0.2.1.
5. Comparar JPEG/DNG pareados e câmera OPPO nas mesmas condições; reprovar dominante verde, ruído, perda de textura e halos.
