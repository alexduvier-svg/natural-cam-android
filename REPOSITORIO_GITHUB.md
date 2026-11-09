# Repositório separado: Natural Cam Android

**Nome recomendado:** `natural-cam-android`  
**Conta:** `alexduvier-svg`  
**Visibilidade:** Privado (até concluir testes de qualidade fotográfica e licenças).

## Estado

Esta pasta é um repositório local preparado, **não é um repositório remoto criado na conta GitHub**. O conector GitHub do ChatGPT atual permite escrever arquivos em repositórios existentes, mas não disponibiliza criar um novo repositório nem adicionar segredos.

## Para criar no GitHub pelo celular

1. Abra https://github.com/new logado na conta `alexduvier-svg`.
2. Informe o nome `natural-cam-android`, visibilidade **Private**. Não inicialize com README, licença ou `.gitignore`, pois estão neste pacote.
3. Confirme **Create repository**.
4. Envie este projeto ao repositório com Git (ou Codespaces; não use o upload comum de arquivos grandes no navegador: o modelo `.tflite` tem quase 99 MB, acima do limite por arquivo da interface web).
5. Configure no repositório GitHub → Settings → Secrets and variables → Actions:
   - `NATURAL_CAM_KEYSTORE_B64`: **base64 do conteúdo do arquivo .jks original** (não salvar no Git).
   - `NATURAL_CAM_KEYSTORE_PASSWORD`: senha da assinatura original.
   Não exponha ou envie esses valores no chat.
6. Abra Actions → Natural Cam — APK de teste assinado → Run workflow.
7. Após sucesso, abra a execução e baixe o artefato `Natural-Cam-0.2.2-teste-1-APK`.

O build falha deliberadamente se não houver os segredos, para evitar produzir APK com certificado diferente e impedir atualização por cima.

## Comandos Git para publicação (quando o repositório existir)

```bash
git init
git add .
git commit -m "feat(camera): Natural Cam 0.2.2 testes e CI"
git branch -M main
git remote add origin https://github.com/alexduvier-svg/natural-cam-android.git
git push -u origin main
```

**Importante:** o modelo de profundidade é grande (~99 MB). Um `git push` normal pode ser aceito pois está abaixo do limite por arquivo de 100 MB do GitHub, mas se o push falhar, considere Git LFS e ajuste o checkout da CI para LFS. Não inclua a assinatura privada nem os arquivos de saída.

## Proteções e limites

- O Agro Gestão não recebe nenhum arquivo deste projeto.
- Este pacote preserva o código da versão 0.2.2 corrigida e adiciona o build seguro.
- A automação de build **não comprova** cor, nitidez, ruído ou fluidez real dos OPPO.
- Assinatura exige o mesmo certificado usado no APK instalado.
- Não habilitar gatilhos de execução com segredos em pull requests externos.
- O APK final ainda não existe; será produzido quando workflow e segredos estiverem configurados.
