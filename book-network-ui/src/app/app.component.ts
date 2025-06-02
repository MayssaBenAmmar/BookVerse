import { Component, OnInit } from '@angular/core';
import { KeycloakService } from './services/keycloak/keycloak.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {
  title = 'book-network-ui';

  constructor(private keycloakService: KeycloakService) {}

  ngOnInit(): void {
    const token = this.keycloakService.token;
    console.log('Access Token:', token);
  }
}
