import { Injectable } from '@angular/core';
import { Client, Message } from '@stomp/stompjs';
import { BehaviorSubject } from 'rxjs';
import SockJS from 'sockjs-client';

@Injectable({
  providedIn: 'root'
})
export class WebsocketService {
  stompClient: Client | null = null;

  // Subject for incoming messages
  private messageSubject = new BehaviorSubject<any>(null);
  public messages$ = this.messageSubject.asObservable();

  // Subject for connection status
  private connectionSubject = new BehaviorSubject<boolean>(false);
  public connectionStatus$ = this.connectionSubject.asObservable();

  // Subject for online users
  private onlineUsersSubject = new BehaviorSubject<any[]>([]);
  public onlineUsers$ = this.onlineUsersSubject.asObservable();

  connect(username: string) {
    console.log('=== CONNECTING ===');
    console.log('Username:', username);

    const socket = new SockJS(`http://localhost:8088/api/v1/ws?username=${username}`);

    this.stompClient = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000,
      debug: (str) => console.log(str),
      connectHeaders: {
        username: username
      }
    });

    this.stompClient.onConnect = (frame) => {
      console.log('=== CONNECTED ===');
      console.log('Frame:', frame);
      this.connectionSubject.next(true);

      // Subscribe to public messages
      this.stompClient?.subscribe('/topic/public', (message: Message) => {
        console.log('=== PUBLIC MESSAGE RECEIVED ===');
        console.log('Message:', message.body);
        this.messageSubject.next(JSON.parse(message.body));
      });

      // Subscribe to private messages
      const privateDestination = '/user/queue/messages';
      console.log('Subscribing to:', privateDestination);

      this.stompClient?.subscribe(privateDestination, (message: Message) => {
        console.log('=== PRIVATE MESSAGE RECEIVED ===');
        console.log('Message:', message.body);
        this.messageSubject.next(JSON.parse(message.body));
      });

      // Subscribe to online users updates
      this.stompClient?.subscribe('/topic/users', (message: Message) => {
        console.log('=== ONLINE USERS UPDATE ===');
        console.log('Users:', message.body);
        const users = JSON.parse(message.body);
        this.onlineUsersSubject.next(users);
      });

      // Send JOIN message
      const joinMessage = { sender: username, type: 'JOIN' };
      console.log('Sending JOIN:', joinMessage);
      this.stompClient?.publish({
        destination: '/app/chat.addUser',
        body: JSON.stringify(joinMessage)
      });
    };

    this.stompClient.onStompError = (frame) => {
      console.error('=== STOMP ERROR ===');
      console.error('Error:', frame);
    };

    this.stompClient?.activate();
  }

  sendMessage(username: string, content: string) {
    if (this.stompClient && this.stompClient.connected) {
      const chatMessage = {
        sender: username,
        content: content,
        type: 'CHAT',
        timestamp: new Date().toISOString()
      };

      console.log('=== SENDING PUBLIC MESSAGE ===');
      console.log('Message:', chatMessage);

      this.stompClient.publish({
        destination: '/app/chat.sendMessage',
        body: JSON.stringify(chatMessage)
      });
    }
  }

  sendPrivateMessage(username: string, content: string, recipient: string) {
    if (this.stompClient && this.stompClient.connected) {
      const privateMessage = {
        sender: username,
        content: content,
        type: 'CHAT',
        room: recipient,
        recipient: recipient,
        timestamp: new Date().toISOString()
      };

      console.log('=== SENDING PRIVATE MESSAGE ===');
      console.log('From:', username);
      console.log('To:', recipient);
      console.log('Message:', privateMessage);

      this.stompClient.publish({
        destination: '/app/chat.sendPrivateMessage',
        body: JSON.stringify(privateMessage)
      });
    }
  }

  disconnect() {
    if (this.stompClient) {
      this.stompClient.deactivate();
    }
  }
}
