# TODO

## Consultar lista de páginas com ligação para uma página específica. 
É possível saber, para cada página, todas as ligações conhecidas que apontem para essa página. Esta funcionalidade pode estar associada à funcionalidade de pesquisa (por exemplo, uma opção associada a cada resultado).

## HackerNews e OpenAI
6 Este projeto deverá ser integrado com o Hacker News e com a OpenAI, ou qualquer alternativa equivalente que utilize REST. As documentações da APIs estão disponíveis em
https://github.com/HackerNews/API e https://platform.openai.com/docs/api-reference/
chat permitindo construir as duas funcionalidades pretendidas através de REST, designadamente:
• Indexar URLs das top stories que contenham os termos da pesquisa. Na sequência de uma pesquisa do Googol um utilizador deve poder solicitar a indexação
dos URLs das “top stories” do Hacker News que contenham (no texto) os termos
da pesquisa efetuada. Em vez das “top stories”, aceita-se como alternativa que um
utilizador com conta no Hacker News peça ao Googol para ir buscar todas as suas
“stories” e indexar todos os seus URLs do Hacker News.
• Gerar com a API da OpenAI uma análise contextualizada. Usando a API de inteligência artificial generativa da OpenAI (serviço de “chat completions”) deverá
ser acrescentada à página de resultados do Googol uma análise textual baseada
nos termos da pesquisa e/ou nas citações curtas dos resultados da pesquisa. Poderão igualmente usar uma qualquer alternativa à OpenAI, desde que seja usada
uma API REST

## Relatório