# Restaurant Manager

**Restaurant Manager** è un sistema software per la gestione di una piccola attività di ristorazione.  
Il progetto è stato sviluppato nell’ambito del corso **ISPW – 2026** e fornisce strumenti per la gestione degli ordini, del menu e dei ruoli operativi all’interno di un ristorante.

## Panoramica

L’applicazione consente la sincronizzazione in tempo reale delle operazioni tra:
- sala (waiter),
- cucina (kitchen staff),
- cassa (cash desk),
- gestione amministrativa (manager).

Il sistema mantiene traccia degli ordini, del loro stato, del contenuto e del costo al momento dell’inserimento, garantendo una chiara separazione delle responsabilità tra i diversi ruoli.

---

## Funzionalità principali

- Gestione completa degli ordini e del loro stato
- Comunicazione tra ruoli (waiter, kitchen, cashier, manager)
- Visualizzazione ordini per tavolo
- Calcolo automatico del conto
- Personalizzazione degli ordini (note e varianti)
- Gestione del menu (inserimento, modifica, eliminazione prodotti)
- Report finanziari e storico ordini
- Gestione utenti e ruoli

---

## Ruoli supportati

- **Manager**: gestione menu, utenti, report finanziari
- **Waiter**: inserimento e modifica ordini
- **Kitchen staff**: visualizzazione e aggiornamento stato ordini
- **Cash desk**: visualizzazione ordini e chiusura conto

---

## Architettura

Il progetto adotta una **architettura multilayer** con separazione delle responsabilità:

- Presentation Layer
- Application Layer
- Business Layer
- Data Access Layer (DAO)
- Infrastructure Layer

### Pattern utilizzati
- DAO (Data Access Object)
- Singleton
- Factory Method
- Dependency Injection (via costruttore)
- Use Case Interfaces

Sono disponibili due implementazioni della persistenza:
- **PostgreSQL (JDBC)**
- **File System (CSV)**

---

## Tecnologie utilizzate

- **Java 21**
- **JDBC**
- **PostgreSQL**
- **Docker**
- **JUnit 5**
- **Mockito**
- **SonarCloud**

---

## Requisiti

### Software
- Java 21 o superiore
- Maven
- Docker (per l’ambiente di sviluppo con PostgreSQL)

### Hardware
- Qualsiasi dispositivo in grado di eseguire file JAR
- Connessione di rete per il database

---

## Database

Il sistema utilizza un database relazionale **PostgreSQL**.  
Durante lo sviluppo, il DB è eseguito tramite **Docker** per garantire isolamento e riproducibilità.

Tutte le operazioni sul database sono incapsulate nel livello DAO; nessun accesso diretto è consentito al di fuori di tale livello.

---

## Testing

Il progetto include test automatici per:
- Controller
- DAO
- Servizi di configurazione
- Use case principali (creazione ordine, flusso cucina)

Sono presenti sia **test unitari** sia **test di integrazione del flusso applicativo**.

Framework utilizzati:
- JUnit 5
- Mockito

---

## Qualità del codice

L’analisi statica è stata eseguita tramite **SonarCloud**:  
https://sonarcloud.io/project/overview?id=BrugnoliDavide_Restaurant-Managaer

---

## Demo

Una dimostrazione funzionante del sistema è disponibile al seguente link:  
https://youtu.be/bJbB3tiGvWw

---

## Autore

**Davide Brugnoli**  
Corso di Ingegneria Informatica  
Progetto ISPW – 2026

