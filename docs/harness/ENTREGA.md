# Entrega Harness — Natural Cam 0.2.2-teste.1

**Estado:** código-fonte corrigido e verificações limitadas ao host aprovadas; **APK NÃO GERADO**, **QUALIDADE FOTOGRÁFICA AINDA PENDENTE**.

Entregáveis: ZIP da fonte integral com assets JNI/Depth Anything; patch textual 0.2.1 → 0.2.2; testes e relatórios dentro de `docs/harness`.

Caminho de atualização (quando puder compilar): `python3 build.py --sdk ... --keystore ... --password-file ...`. A assinatura existente e o mesmo pacote Android devem ser preservados; o versionCode sobe de 3 para 4, sem desinstalação nem limpeza de dados. Manter os APKs anteriores disponíveis para rollback ou comparação, sem presumir que um downgrade direto seja aceito pelo Android.

Não há commit GitHub nesta entrega porque não foi identificado um repositório de código Natural Cam; o repositório consultado anteriormente pertence ao aplicativo Agro Gestão, que não deve receber o código da câmera por engano.
