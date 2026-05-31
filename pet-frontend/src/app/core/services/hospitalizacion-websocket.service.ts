import { Injectable } from '@angular/core';
import { Client, Message } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { environment } from '../../../environments/environment';
import { BehaviorSubject, Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class HospitalizacionWebsocketService {
  private stompClient: Client;
  private alertSubject = new BehaviorSubject<string | null>(null);

  constructor() {
    this.stompClient = new Client({
      webSocketFactory: () => new SockJS(environment.apiUrl.replace('/api', '/ws')),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000
    });

    this.stompClient.onConnect = (frame) => {
      console.log('Connected to WebSocket: ' + frame);
      this.stompClient.subscribe('/topic/alertas-hospitalizacion', (message: Message) => {
        if (message.body) {
          this.alertSubject.next(message.body);
        }
      });
    };

    this.stompClient.onStompError = (frame) => {
      console.error('Broker reported error: ' + frame.headers['message']);
      console.error('Additional details: ' + frame.body);
    };
  }

  connect() {
    this.stompClient.activate();
  }

  disconnect() {
    this.stompClient.deactivate();
  }

  getAlerts(): Observable<string | null> {
    return this.alertSubject.asObservable();
  }
}


