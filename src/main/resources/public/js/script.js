const root = document.getElementById('root');
if (root) {
    const elem = '<input type="text" name="chat"><button>send</button>';
    root.innerHTML = elem;

    const ws = new WebSocket("ws://localhost:8080/ws")
    ws.onopen = function() {
        console.log("Connect WebSocket Server")
    }

    const buttonElem = root.querySelector('button');
    const textElem = root.querySelector('input')
    buttonElem.addEventListener('click', (e) => {
        e.preventDefault()
        if (ws.readyState == WebSocket.OPEN) {
            ws.send(textElem.value)
            console.log(`send message${textElem.value}`)
        } else if (ws.readyState == WebSocket.CLOSED) {
            console.log("closed")
        }
    });
}



async function sendChat(url, data) {
   try {
       const response = await fetch(url, {
           method: "post",
           headers: {
               "Content-Type": "application/json",
           },
           body: JSON.stringify(data),
       });
       if (!response.ok) {
           throw new Error("Http error!");
       }
       return response.json();
   } catch(error) {
       throw error;
   }
}