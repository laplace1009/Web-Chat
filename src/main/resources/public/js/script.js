class Client {
    constructor(userName, addr) {
        this.userName = userName
        this.webSocket = new WebSocket(addr)

        this.webSocket.onmessage = (event) => {
            const message = JSON.parse(event.data)
            if (this.isChangedUserName(message)) {
                this.userName = message.userName
            }

            Client.handleMessage(`${message.userName}: ${message.message}`)
        }
        this.webSocket.onclose = (e) => {
            if (e.wasClean) alert(`Clean closed connection ${e.code}, ${e.reason}`)
            else alert('Connection was forcibly terminated.')
        }
        this.webSocket.onerror = (error) => console.error(`WebSocket Error: ${error}`)
    }

    isChangedUserName(data) {
        if (data.message.startsWith("/n ")) return true
        return false
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

class Message {
    constructor(userName, message) {
        this.userName = userName
        this.message = message
    }
}

const root = document.getElementById('root');
if (root) {
    const divs = `<div id="basis"><div id="chat-div"></div><div id="user-div"></div></div>`
    const elem = '<input type="text" name="chat"><button type="submit">send</button>';
    root.innerHTML = divs + elem;

    const ws = new Client('userName123', "ws://localhost:8080/ws")
    const buttonElem = root.querySelector('button');
    const textElem = root.querySelector('input')
    buttonElem.addEventListener('click', (e) => {
        e.preventDefault()
        if (ws.webSocket.readyState == WebSocket.OPEN) {
            const message = JSON.stringify(new Message(ws.userName, textElem.value))
            console.log(`send message: ${message}`)
            ws.webSocket.send(message)
        } else if (ws.webSocket.readyState == WebSocket.CLOSED) {
            console.log("closed")
        }
    });
}
