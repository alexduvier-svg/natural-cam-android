# Plano Harness — Natural Cam 0.2.2-teste.1

Estado: base verificável `0.2.1-candidate.1` extraída do ZIP preservado da Biblioteca; sem repositório Git de câmera confirmado.

Objetivos autorizados:
1. Recuperar a última lente válida após erro, desconexão ou timeout de abertura, sem loop e sem deixar zoom antigo pendente.
2. Evitar animação de enquadramento na óptica anterior quando uma troca de lente física está em curso.
3. Reduzir o intervalo entre o toque do obturador e o envio da captura, sem desligar AWB, redução de ruído ou diminuir resolução.
4. Preservar JPEG, RAW e exportação; não declarar qualidade fotográfica sem fotos reais pareadas.

Estratégia: correção mínima e reversível em `CameraEngine` e `MainActivity`; teste de estado de recuperação e política de pré-disparo, host regression + diffs; novo pacote fonte separado da base.

Critérios de aceite: AWB espera máxima controlada, captura encaminhada na primeira oportunidade, acessibilidade mantida; transições apenas após evidência de quadro novo; recuperação sem loops, geração antiga inválida; sinalização de erro e rollback no zoom; não alterar assinatura, packageName ou API. Gate de APK somente com SDK, dependências e keystore confirmados; caso contrário, fonte validada sem APK.

Rollback: restaurar arquivos da versão 0.2.1 preservada.
