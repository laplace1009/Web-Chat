class Client {
    constructor(userName, addr) {
        this.userName = userName
        this.webSocket = new WebSocket(addr)

        this.webSocket.onmessage = (event) => Client.handleMessage(`To server msg: ${event.data}`)
        this.webSocket.onerror = (error) => console.error(`WebSocket Error: ${error}`)
    }

    static handleMessage(msg) {
        console.log(msg)
    }

    Send(message) {
        this.webSocket.send(message)
    }

    Disconnect() {
        this.webSocket.close();
        Client.handleMessage(`${this.userName} disconnected!`)
    }

}

const root = document.getElementById('root');
if (root) {
    const elem = '<input type="text" name="chat"><button type="submit">send</button>';
    root.innerHTML = elem;

    const ws = new Client('userName123', "ws://localhost:8080/ws")
    const buttonElem = root.querySelector('button');
    const textElem = root.querySelector('input')
    buttonElem.addEventListener('click', (e) => {
        e.preventDefault()
        if (ws.webSocket.readyState == WebSocket.OPEN) {
            ws.Send(textElem.value)
            console.log(`send message: ${textElem.value}`)
        } else if (ws.webSocket.readyState == WebSocket.CLOSED) {
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

