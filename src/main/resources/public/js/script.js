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
        this.chatBasis = document.getElementById("messages")
        this.userBasis = document.getElementById("user-list").querySelector('ul')
        this.webSocket.onmessage = (event) => {
            const message = JSON.parse(event.data)
            if (message.changeUser) {
                this.userName = message.message
            } else if (message.changeList) {
                this.userList = [...message.userList]
                const str = this.userList.reduce((pre, cur) => pre + `<li>${cur}</li>`, "")
                this.userBasis.innerHTML = str
            } else {
                const chatElem = `<div><span>${message.userName}: ${message.message}</span></div>`
                this.chatBasis.insertAdjacentHTML('beforeend', chatElem)
            }
        }
        this.webSocket.onclose = (e) => {
            if (e.wasClean) alert(`Clean closed connection ${e.code}, ${e.reason}`)
            else alert('Connection was forcibly terminated.')
        }
        this.webSocket.onerror = (error) => console.error(`WebSocket Error: ${error}`)
    }
    static handleMessage(msg) {
        console.log(msg)
    }
}


const ws = new Client("ws://230.130.153.247/ws", 'userName123')
const buttonElem = document.querySelector('button');
const textElem = document.getElementById('message-input')

function sendMessage() {
    if (ws.webSocket.readyState == WebSocket.OPEN) {
        const messageList = textElem.value.split(' ');
        let message;
        if (messageList.at(0) == '/n' && messageList.length == 2) {
            message = JSON.stringify(new Message(true, false, ws.userName, messageList.at(1), []));
        } else {
            message = JSON.stringify(new Message(false, false, ws.userName, textElem.value));
        }
        ws.webSocket.send(message);
        textElem.value = '';
    } else if (ws.webSocket.readyState == WebSocket.CLOSED) {
        console.log("WebSocket connection is closed.");
    }
}
buttonElem.addEventListener('click', (e) => {
    e.preventDefault();
    sendMessage();
});

textElem.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
        e.preventDefault();
        sendMessage();
    }
})