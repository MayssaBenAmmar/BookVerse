import {Component, OnInit, OnDestroy} from '@angular/core';
import {WebsocketService} from "../../../../../services/WebsocketService";
import { UserProfile } from 'src/app/services/keycloak/user-profile';
import { KeycloakService } from 'src/app/services/keycloak/keycloak.service';

@Component({
  selector: 'app-form-chat',
  templateUrl: './form-chat.component.html',
  styleUrls: ['./form-chat.component.scss']
})
export class FormChatComponent implements OnInit, OnDestroy {
  title = 'frontend';
  username: string = '';
  message: string = '';
  messages: any[] = [];
  isConnected = false;
  connectingMessage = 'Connecting...';
  userProfile: UserProfile | undefined;

  // Enhanced features properties
  onlineUsers: any[] = [];
  selectedUser: any = null;
  privateMessages: { [key: string]: any[] } = {};
  showSettings = false;
  showInfo = false;
  showSearch = false;
  searchQuery = '';
  currentRoom = 'public';
  chatBoxVisible = false;

  // New property to track session start time
  sessionStartTime: Date = new Date();
  filteredMessages: any[] = [];

  // NEW: Properties for friend message highlighting
  friendsList: string[] = []; // Will be populated with other online users
  newMessages: Set<string> = new Set(); // Track new messages
  userNewMessageCounts: { [username: string]: number } = {}; // Track message count per user

  // Settings
  settings = {
    notifications: true,
    soundEnabled: true,
    theme: 'light'
  };

  // Auto-save interval
  private autoSaveInterval: any;

  constructor(
    private websocketService: WebsocketService,
    private keycloakService: KeycloakService
  ) {
    console.log('AppComponent constructor called');
  }

  ngOnInit(): void {
    console.log('AppComponent ngOnInit called');

    // Initialize theme on body
    document.body.classList.add('light-theme');

    this.userProfile = this.keycloakService.profile;
    console.log(this.userProfile);
    this.username = this.userProfile?.firstName || '';

    // Load saved settings
    this.loadSettings();

    // Apply theme
    this.applyTheme();

    // Request notification permission if enabled
    if (this.settings.notifications) {
      this.requestNotificationPermission();
    }

    // Load saved messages from localStorage
    this.loadMessages();

    this.connect();

    // Subscribe to messages observable
    this.websocketService.messages$.subscribe((message: any) => {
      if (message) {
        this.handleIncomingMessage(message);
      }
    });

    // Subscribe to online users
    this.websocketService.onlineUsers$.subscribe((users: any[]) => {
      this.onlineUsers = users.filter((user: any) => user.username !== this.username);
    });

    // Subscribe to connection status
    this.websocketService.connectionStatus$.subscribe((connected: boolean) => {
      this.isConnected = connected;
      if (connected) {
        this.connectingMessage = '';
        console.log('WebSocket connection established');
      }
    });

    // Set up auto-save interval (save every 10 seconds)
    this.autoSaveInterval = setInterval(() => {
      this.saveMessages();
    }, 10000);
  }

  ngOnDestroy(): void {
    // Clear the auto-save interval when component is destroyed
    if (this.autoSaveInterval) {
      clearInterval(this.autoSaveInterval);
    }

    // Save messages when user logs out
    this.saveMessages();
  }

  connect() {
    console.log('Attempting to connect to WebSocket with username:', this.username);
    this.websocketService.connect(this.username);
  }

  // NEW: Method to check if user has new messages
  hasNewMessageFromUser(username: string): boolean {
    return (this.userNewMessageCounts[username] || 0) > 0;
  }

  // NEW: Method to get new message count for user
  getNewMessageCount(username: string): number {
    return this.userNewMessageCounts[username] || 0;
  }

  // NEW: Method to check if message is from a friend
  isMessageFromFriend(sender: string): boolean {
    return this.onlineUsers.some((user: any) => user.username === sender) && sender !== this.username;
  }

  // NEW: Method to check if it's a new message from friend
  isNewMessageFromFriend(message: any): boolean {
    return this.isMessageFromFriend(message.sender) &&
      this.newMessages.has(message.sender + '_' + message.timestamp);
  }

  handleIncomingMessage(message: any) {
    console.log(`Message received from ${message.sender}: ${message.content}`);

    if (message.type === 'CHAT') {
      // Check if it's a message from someone else
      if (message.sender !== this.username) {
        // NEW: Mark as new message from friend and increment count
        if (this.isMessageFromFriend(message.sender)) {
          const messageKey = message.sender + '_' + message.timestamp;
          this.newMessages.add(messageKey);

          // Increment message count for this user
          if (!this.userNewMessageCounts[message.sender]) {
            this.userNewMessageCounts[message.sender] = 0;
          }
          this.userNewMessageCounts[message.sender]++;

          // Auto-remove "new" indicator after 5 seconds
          setTimeout(() => {
            this.newMessages.delete(messageKey);
          }, 5000);
        }

        // Play notification sound
        this.playNotificationSound();

        // Show browser notification if the window is not focused
        if (document.hidden && this.settings.notifications) {
          this.showBrowserNotification(
            `New message from ${message.sender}`,
            message.content
          );
        }
      }

      if (message.room === 'public' || !message.room) {
        this.messages.push(message);
        // Save messages after receiving a new one
        this.saveMessages();
      } else if (message.room === this.username || message.sender === this.selectedUser?.username) {
        // Private message
        const chatKey = message.sender === this.username ? message.room : message.sender;
        if (!this.privateMessages[chatKey]) {
          this.privateMessages[chatKey] = [];
        }
        this.privateMessages[chatKey].push(message);
        // Save messages after receiving a new one
        this.saveMessages();
      }
    } else if (message.type === 'JOIN' || message.type === 'LEAVE') {
      this.messages.push(message);
      // Save messages after receiving a system message
      this.saveMessages();
    }
  }

  sendMessage() {
    if (this.message.trim()) {
      if (this.currentRoom === 'public') {
        this.websocketService.sendMessage(this.username, this.message);
      } else if (this.selectedUser) {
        this.websocketService.sendPrivateMessage(
          this.username,
          this.message,
          this.selectedUser.username
        );

        // Add message to local private messages
        if (!this.privateMessages[this.selectedUser.username]) {
          this.privateMessages[this.selectedUser.username] = [];
        }
        this.privateMessages[this.selectedUser.username].push({
          sender: this.username,
          content: this.message,
          type: 'CHAT',
          timestamp: new Date().toISOString()
        });

        // Save messages after sending a new one
        this.saveMessages();
      }
      this.message = '';
    }
  }

  // Message persistence methods
  saveMessages() {
    try {
      // Create an object to store all messages
      const messageStorage = {
        publicMessages: this.messages,
        privateMessages: this.privateMessages,
        timestamp: new Date().toISOString()
      };

      // Save to localStorage with username as part of the key for user-specific storage
      localStorage.setItem(`chatMessages_${this.username}`, JSON.stringify(messageStorage));
      console.log('Messages saved to localStorage');
    } catch (error) {
      console.error('Error saving messages to localStorage:', error);
      // If localStorage is full, we could implement a cleanup strategy here
      this.handleStorageError();
    }
  }

  loadMessages() {
    try {
      // Get saved messages from localStorage
      const savedMessages = localStorage.getItem(`chatMessages_${this.username}`);

      if (savedMessages) {
        const messageStorage = JSON.parse(savedMessages);

        // Check if the saved messages are not too old (e.g., older than 7 days)
        const savedTime = new Date(messageStorage.timestamp);
        const now = new Date();
        const maxAge = 7 * 24 * 60 * 60 * 1000; // 7 days in milliseconds

        if (now.getTime() - savedTime.getTime() < maxAge) {
          // Restore messages
          this.messages = messageStorage.publicMessages || [];
          this.privateMessages = messageStorage.privateMessages || {};

          console.log('Loaded saved messages from localStorage');
        } else {
          console.log('Saved messages are too old, not loading');
          // Clear old messages
          localStorage.removeItem(`chatMessages_${this.username}`);
        }
      }
    } catch (error) {
      console.error('Error loading messages from localStorage:', error);
    }
  }

  handleStorageError() {
    // This method handles localStorage errors, like when it's full

    // Strategy 1: Remove old messages to free up space
    try {
      // Keep only the last 100 messages for each conversation
      if (this.messages.length > 100) {
        this.messages = this.messages.slice(-100);
      }

      // Do the same for private messages
      Object.keys(this.privateMessages).forEach((key: string) => {
        if (this.privateMessages[key].length > 100) {
          this.privateMessages[key] = this.privateMessages[key].slice(-100);
        }
      });

      // Try saving again
      this.saveMessages();
    } catch (error) {
      console.error('Could not recover from storage error:', error);

      // Strategy 2: Clear all messages as a last resort
      alert('Storage is full. Some message history will be cleared.');
      this.clearOldMessages();
    }
  }

  clearOldMessages() {
    // Clear messages older than 3 days
    const thresholdDate = new Date();
    thresholdDate.setDate(thresholdDate.getDate() - 3);

    // Filter public messages
    this.messages = this.messages.filter((msg: any) =>
      new Date(msg.timestamp) > thresholdDate
    );

    // Filter private messages
    Object.keys(this.privateMessages).forEach((key: string) => {
      this.privateMessages[key] = this.privateMessages[key].filter((msg: any) =>
        new Date(msg.timestamp) > thresholdDate
      );
    });

    // Try saving again
    this.saveMessages();
  }

  selectUser(user: any): void {
    this.selectedUser = user;
    this.currentRoom = user ? user.username : 'public';

    // NEW: Clear new message count when user is selected
    if (user && this.userNewMessageCounts[user.username]) {
      this.userNewMessageCounts[user.username] = 0;
    }
  }

  goToPublicChat(): void {
    this.selectedUser = null;
    this.currentRoom = 'public';
  }

  getCurrentMessages() {
    if (this.searchQuery) {
      return this.filteredMessages;
    }

    if (this.currentRoom === 'public') {
      return this.messages;
    } else if (this.selectedUser) {
      return this.privateMessages[this.selectedUser.username] || [];
    }
    return [];
  }

  getAvatarColor(sender: string): string {
    const colors = ['#2196F3', '#32c787', '#00BCD4', '#ff5652', '#ffc107', '#ff85af', '#FF9800', '#39bbb0'];
    let hash = 0;
    for (let i = 0; i < sender.length; i++) {
      hash = 31 * hash + sender.charCodeAt(i);
    }
    return colors[Math.abs(hash % colors.length)];
  }

  toggleSettings() {
    this.showSettings = !this.showSettings;
    this.showInfo = false;
    this.showSearch = false;
  }

  toggleInfo() {
    this.showInfo = !this.showInfo;
    this.showSettings = false;
    this.showSearch = false;
  }

  toggleSearch() {
    this.showSearch = !this.showSearch;
    this.showSettings = false;
    this.showInfo = false;

    if (!this.showSearch) {
      this.clearSearch();
    }
  }

  search() {
    if (!this.searchQuery.trim()) {
      this.clearSearch();
      return;
    }

    console.log('Searching for:', this.searchQuery);

    // Search in current room messages
    let messagesToSearch = [];
    if (this.currentRoom === 'public') {
      messagesToSearch = this.messages;
    } else if (this.selectedUser) {
      messagesToSearch = this.privateMessages[this.selectedUser.username] || [];
    }

    // Filter messages that contain the search query
    this.filteredMessages = messagesToSearch.filter((msg: any) =>
      msg.type === 'CHAT' &&
      msg.content.toLowerCase().includes(this.searchQuery.toLowerCase())
    );

    console.log('Found', this.filteredMessages.length, 'messages');
  }

  clearSearch() {
    this.searchQuery = '';
    this.filteredMessages = [];
  }

  highlightSearchResults(text: string): string {
    if (!this.searchQuery || !text) {
      return text;
    }

    // Create a safe regex pattern for the search query (escaping special characters)
    const safeQuery = this.searchQuery.replace(/[.*+?^${}()|[\]\\]/g, '\\    // Subscribe to messages observable  this.websocketService.');
    const regex = new RegExp(`(${safeQuery})`, 'gi');

    // Replace occurrences with highlighted version
    return text.replace(regex, '<span class="highlight">$1</span>');
  }

  // Settings related methods
  loadSettings() {
    const savedSettings = localStorage.getItem('chatSettings');
    if (savedSettings) {
      this.settings = JSON.parse(savedSettings);
    }
  }

  saveSettings() {
    localStorage.setItem('chatSettings', JSON.stringify(this.settings));
  }

  applyTheme() {
    const body = document.body;
    if (this.settings.theme === 'dark') {
      body.classList.remove('light-theme');
      body.classList.add('dark-theme');
    } else {
      body.classList.remove('dark-theme');
      body.classList.add('light-theme');
    }
  }

  updateSettings() {
    // Save settings to localStorage
    this.saveSettings();

    // Apply theme immediately
    this.applyTheme();

    // Request notification permission if just enabled
    if (this.settings.notifications) {
      this.requestNotificationPermission();
    }

    console.log('Settings updated:', this.settings);
    this.showSettings = false;
  }

  // Sound notification function
  playNotificationSound(): void {
    if (this.settings.soundEnabled) {
      const audio = new Audio('assets/sounds/notification.mp3');
      audio.play().catch((err: any) => console.log('Could not play sound:', err));
    }
  }

  // Browser notification function
  showBrowserNotification(title: string, body: string) {
    if (this.settings.notifications && 'Notification' in window && Notification.permission === 'granted') {
      new Notification(title, {
        body,
        icon: 'assets/icons/chat-icon.png' // Optional: add an icon for notifications
      });
    }
  }

  // Request notification permission
  requestNotificationPermission(): void {
    if ('Notification' in window && this.settings.notifications) {
      Notification.requestPermission().then((permission: NotificationPermission) => {
        console.log('Notification permission:', permission);
      });
    }
  }

  toggleChatBox() {
    this.chatBoxVisible = !this.chatBoxVisible;
  }

  // Methods for the Info Panel
  getTotalMessages(): number {
    let total = this.messages.length;

    // Add private messages
    Object.keys(this.privateMessages).forEach((key: string) => {
      total += this.privateMessages[key].length;
    });

    return total;
  }

  getSessionDuration(): string {
    const now = new Date();
    const diff = now.getTime() - this.sessionStartTime.getTime();

    // Format as hours:minutes:seconds
    const hours = Math.floor(diff / (1000 * 60 * 60));
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
    const seconds = Math.floor((diff % (1000 * 60)) / 1000);

    return `${this.padZero(hours)}:${this.padZero(minutes)}:${this.padZero(seconds)}`;
  }

  private padZero(num: number): string {
    return num < 10 ? `0${num}` : `${num}`;
  }

  testSound(): void {
    const audio = new Audio('assets/sounds/notification.mp3');
    audio.play().catch((err: any) => console.log('Could not play sound:', err));
  }

  // New method to provide option for user to clear message history
  clearMessageHistory() {
    if (confirm('Are you sure you want to clear all message history?')) {
      this.messages = [];
      this.privateMessages = {};
      localStorage.removeItem(`chatMessages_${this.username}`);
      console.log('Message history cleared');
    }
  }
}
