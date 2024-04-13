class Message {
    constructor(changeUser, changeList, userName, message, userList = []) {
        this.changeUser = changeUser
        this.changeList = changeList
        this.userName = userName
        this.message = message
        this.userList = userList
    }
}

class Client {
    constructor(addr, userName, userList = []) {
        this.userName = userName
        this.webSocket = new WebSocket(addr)
        this.chatBasis = document.getElementById("chat-div")
        this.userBasis = document.getElementById("user-div")
        this.webSocket.onmessage = (event) => {
            const message = JSON.parse(event.data)
            console.log(message)
            if (message.changeUser) {
                this.userName = message.message
            } else if (message.changeList) {
                this.userList = [...message.userList]
                const str = this.userList.reduce((pre, cur) => pre + `<div><span>${cur}</span></div>`, "")
                this.userBasis.innerHTML = str
            } else {
                const chatElem = `<div><span id="#chat-span">${message.userName}: ${message.message}</span></div>`
                this.chatBasis.insertAdjacentHTML('beforeend', chatElem)
            }
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


const root = document.getElementById('root');
if (root) {
    const divs = `<h1 id="title">WebChatting!!</h1><div id="basis"><div id="chat-div"></div><div id="user-div"></div></div>`
    const elem = '<input type="text" name="chat"><button type="submit">send</button>';
    root.innerHTML = divs + elem;

    const ws = new Client("ws://localhost:8080/ws", 'userName123')
    const buttonElem = root.querySelector('button');
    const textElem = root.querySelector('input')
    buttonElem.addEventListener('click', (e) => {
        e.preventDefault()
        if (ws.webSocket.readyState == WebSocket.OPEN) {
            const messageList = textElem.value.split(' ')
            if (messageList.at(0) == '/n' && messageList.length == 2) {
                const message = JSON.stringify(new Message(true, false, ws.userName, messageList.at(1), []))
                ws.webSocket.send(message)
                console.log(`send message: ${message}`)
            } else {
                const message = JSON.stringify(new Message(false, false, ws.userName, textElem.value))
                ws.webSocket.send(message)
                console.log(`send message: ${message}`)
            }
            textElem.value = ''

        } else if (ws.webSocket.readyState == WebSocket.CLOSED) {
            console.log("closed")
        }
    });
}
