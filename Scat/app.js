console.log("loaded b!tch");

const origin = window.location.origin;

const textPlaceholder = document.getElementsByClassName("text-placeholder")[0];
const textfield = document.getElementsByClassName("text-field")[0];
const sendSound = new Audio("/audio/happy-pop-2.mp3");
const sendButton = document.getElementsByClassName("send-button")[0];
const sendErrorSound = new Audio("/audio/error1.mp3");
const messageContainer = document.getElementsByClassName("message-container")[0];
const userSelect = document.getElementById("username");
const userpfp = document.getElementById("userpfp");
const pfpselect = document.getElementById("pfpselect");

let canSend = false;

textfield.addEventListener("input",()=>{

	if(textfield.innerHTML != null && textfield.innerHTML != "<br>"){
		textPlaceholder.style.color = "transparent";
		sendButton.style.backgroundColor = "var(--ready-message-color)"
		canSend = true;
	} else {
	textPlaceholder.style.color = "var(--text-placeholder-color)";
	sendButton.style.backgroundColor = "var(--not-ready-message-color)"
	canSend = false;
	}
});
textfield.addEventListener("keydown", (event)=>{
	if(event.key === 'Enter'){
		event.preventDefault();
		sendMessage();
	}
});

function sendMessage(){
	
	if(getCookie("name") === null){
		alert("you must select a username");
		return;
	}

	if(canSend){
		sendSound.cloneNode().play();
		const message = textfield.innerHTML;
		const username = getCookie("name");
		textfield.innerHTML = '<br>';
		canSend = false;
		sendButton.style.backgroundColor = "var(--not-ready-message-color)";
		
		fetch(origin+"/api/chat",{
		method: 'PUT',
		body: JSON.stringify({message,username})
		});
		
	} else{
		let node = sendErrorSound.cloneNode();
		node.volume = 0.3;
		node.play();
	}
}
function getCookie(name){
	let cookie = document.cookie.split(";");
	console.log("cookie",cookie);
	
	for(let i = 0; i<cookie.length;i++){
	
		let arr = cookie[i].split("=",2);
		console.log('lol',arr[0]);
		if(arr[0] === name){
			return arr[1].trim();
		}
	}
	return null;
}

fetch(origin+"/api/chatroom",{
 METHOD: 'GET',
 HEADERS: {
 	"Accept-Encoding": "chunked"
 }
})
.then(async (res) => {

	const reader = res.body.getReader();
	
	console.log('get here');
	
	let full = "";
	
	reader.read().then(function doSmth({done,value}) {
		if(done){
			console.log("full",full);
			return;
		}
		let readable = new TextDecoder().decode(value);
		//console.log(readable);
		
		const json = JSON.parse(readable);
		console.log(json);
		
		let scroll = messageContainer.scrollHeight - messageContainer.scrollTop -  messageContainer.clientHeight< 10;
			console.log(messageContainer.scrollHeight - messageContainer.scrollTop -  messageContainer.clientHeight);
		
		messageContainer.insertAdjacentHTML('beforeend',`<div class="message">
				<img src="/pfp/${json.username}.png" class="pfp">
				<span class="username">${json.username}<span>${json.date}</span> </span>
				<p class="message-content">${json.content}</p>
			</div>`);
			
			
			if(scroll){
				messageContainer.scrollTop = messageContainer.scrollHeight;
			}
		
		
		return reader.read().then(doSmth);
	});

	//console.log( await res.text()); 
});
let currentMessageNumber = 0;
fetch(origin+"/api/chat?start=0&depth=20")
.then(async (res)=>{
	let list = await res.json();
	list = list.list;
	console.log(list);
	currentMessageNumber += list.length;
	for(let i = 0; i<list.length; i++){
		let json = list[i];
		messageContainer.insertAdjacentHTML('beforeend',`<div class="message">
				<img src="/pfp/${json.username}.png" class="pfp">
				<span class="username">${json.username}<span>${json.date}</span> </span>
				<p class="message-content">${json.content}</p>
			</div>`);		
	}
	messageContainer.scrollTop = messageContainer.scrollHeight;
	
}).then(()=>{setTimeout(1000,startScrollResponsive());});

if(getCookie("name") != null) {
	const name = getCookie("name");
	userSelect.value = name;
	userpfp.src = `/pfp/${name}.png`
}

function setCookie(name,value,days) {
    var expires = "";
    if (days) {
        var date = new Date();
        date.setTime(date.getTime() + (days*24*60*60*1000));
        expires = "; expires=" + date.toUTCString();
    }
    document.cookie = name + "=" + (value || "")  + expires + "; path=/";
}

userSelect.addEventListener("keydown",(event) => {
console.log("yep");
	if(event.key === 'Enter'){
		const username = userSelect.value;
		setCookie("name",username,30);
		alert('You username is now '+username);
	}
});
function changepfp() {
	const username = getCookie("name");
	console.log(username);
	let file = pfpselect.files[0];
	console.log(file);
	const reader = new FileReader();
	reader.onload = ()=>{
		let byteArr = new Uint8Array(reader.result);
		console.log(byteArr);
		
		fetch("/pfp/"+username+"?name="+username,{
			method: "PUT",
			body: byteArr
		})
	}
		reader.readAsArrayBuffer(file);
}
let topChannel = false;
function startScrollResponsive(){
messageContainer.addEventListener("scroll", (event)=>{
	let loadMore =  messageContainer.scrollTop < 1;
	//console.log( messageContainer.scrollTop);
	if(loadMore && !topChannel){
		fetch(`${origin}/api/chat?start=${currentMessageNumber}&depth=20`)
		.then(async (res)=>{
		let list = await res.json();
		list = list.list;
		if(list.length === 0){
			topChannel = true;
		}
		console.log(list);
		currentMessageNumber += list.length;
		for(let i = 0; i<list.length; i++){
			let json = list[i];
			messageContainer.insertAdjacentHTML('afterbegin',`<div class="message">
					<img src="/pfp/${json.username}.png" class="pfp">
					<span class="username">${json.username}<span>${json.date}</span> </span>
					<p class="message-content">${json.content}</p>
				</div>`);		
		}
	
		});
	}
});
}