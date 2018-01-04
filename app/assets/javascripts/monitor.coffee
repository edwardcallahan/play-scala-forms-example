$ ->
  ws = new WebSocket $("body").data("ws-url")
  ws.onmessage = (event) ->
    console.log(event.data)