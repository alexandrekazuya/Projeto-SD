# TODO

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

ideia para o hacker news: 
Após uma pesquisa normal no Googol, os resultados são apresentados ao utilizador. Caso este não esteja satisfeito, pode carregar no botão “Não estou satisfeito – procurar também no Hacker News”.
Ao fazê-lo, o sistema consulta a API do Hacker News para obter as top stories, filtra aquelas cujo título ou texto contém os termos da pesquisa e solicita a sua indexação através do método putNew.
Inicialmente, podem ser apresentados apenas os resultados do Hacker News já indexados; posteriormente, após o processo de crawling e indexação, novas pesquisas passam a incluir também os conteúdos do Hacker News relevantes para a pesquisa efetuada.

ideia para o open ai


## Relatório