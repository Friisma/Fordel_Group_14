# Rapport - Fordel 14
### Indholdsfortegnelse
- [Report contribution](#Report-contribution)
- [Use of AI](#Use-of-AI)
- [Statement on code contribution score](#Statement-on-code-contribution-score)
- [Indledning](#Indledning)
- [Kravanalyse](#Kravanalyse)
- [Planlægning og design](#Planlægning-og-design)
- [Implementering](#Implementering)
- [Validering](#Validering)
- [Diskussion](#Diskussion)

## Report contribution
Tabel her

## Use of AI

## Statement on code contribution score

## Indledning

> _Hvem, Hvorfor, og Hvordan skal man læse rapporten?_

Kort sagt, har indledningen formålet at sætte kontekst og forventningerne til resten af rapporten.
En dårlig indledning giver derfor ofte et dårlig indtryk; som I så skal kæmpe jer tilbage fra i resten af rapporten.

Ting der er gode at få med:

- [ ] Sæt rapporten in kontekst. Hvad forventer I at jeres læser kan forstå?
- [ ] En kort beskrivelse af hvad I har lavet og er specielt stolte af, gerne med referencer til resten af rapporten.





## Kravanalyse
> _Hvad er problemet?_

UTD har i længere tid oplevet at håndteringen af projekter og studielinjer, samt tildelingen af vejledere ofte en kompleks proces, 
der involverer mange forskellige typer af brugere, det alt fra den studerende til administratorer og studieledere. Uden et system
til håndtering af ovenstående kan overblikket over invitationer, projektstatus og godkendelser let gå tabt. Kunden har i denne forbindelse
præsenteret os for en en række krav i form af user stories. Kravene vil i dette afsnit blive præsenteret, analyseret og gennemgået, for at danne
overblik over problemet og den tilhørende applikation. Dette vil sikre at udvikling af produktet starter i den rigtige retning.

Tabellen viser en liste over alle user stories, som kunden har fremlagt.

| ID        | User Story                                                                                                                                                                          |
|:----------|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **US-1**  | **Login:** **As** any user of the site **I want to** be able to authenticate myself **such that** I can get a personalized view and options                                         |
| **US-2**  | **Create A Study Line:** **As** the administrator **I want to** create study lines **such that** I can assign students and study leaders                                            | 
| **US-3**  | **Create Advisors:** **As** the administrator **I want to** assign users as advisors **such that** they can advise students                                                         | 
| **US-4**  | **Assign Study Leaders:** **As** the administrator **I want to** assign a study leader to a study line **such that** they can administrate the students of that line                | 
| **US-5**  | **Assign Students:** **As** a study leader **I want to** assign students to a study line **such that** I can monitor and help them complete their projects                          |
| **US-8**  | **Advisor Project Choice:** **As** an advisor **I want to** see a list of potential projects **such that** I can choose some to advise                                              |
| **US-9**  | **Advisor Project Dashboard:** **As** an advisor **I want to** see a list of projects I have accepted to advise **such that** I can have an overview of the projects I'm advising   |
| **US-10** | **Project Creation:** **As** a student **I want to** to create a project **such that** it can be chosen by an advisor                                                               |
| **US-11** | **Project Draft Creation:** **As** a student **I want to** save the project as a draft **such that** I do not have to publish an unfinished project                                 |
| **US-12** | **Multistudent Project:** **As** a student **I want to** to add other students to my project **such that** I can work with them on the project                                      |
| **US-13** | **Project Collaborator Invitation:** **As** a student **I want to** accept or reject an invitation to be part of a project **such that** I can choose which project to work on      |
| **US-14** | **Project Advisor Invitation:** **As** a student **I want to** see a list of potential advisors **such that** I can send a request to join the project                              |
| **US-16** | **Advisor Workload:** **As** an advisor **I want to** set my acceptable workload **such that** students do not contact me when I am full                                            |
| **US-19** | **Advisor Project Creation:** **As** an advisor **I want to** publish a potential project **such that** students can choose it                                                      |
| **US-20** | **Have a Cookie Notice:** **As** any user **I want to** to be informed about the use of cookies **such that** my rights are upheld                                                  |
| **US-21** | **Delete Users:** **As** any user **I want to** to be able to have my account deleted **such that** my rights are upheld                                                            |
| **US-22** | **Change name:** **As** any user **I want to** to be able change my name and information **such that** be represented as I prefer                                                   |
| **US-23** | **Add student progress:** **As** a student **I want to** to be able set my start date **such that** the system can calculate approximate start dates for internship and the project |
| **US-25** | **Invite co-advisors:** **As** an advisor **I want to** to be able to invite students and other advisors to the project **such that** I can get a co-advisors                       |
| **US-26** | **Update of project:** **As** an advisor **I want to** to be able update the name and abstract of the project after start **such that** the description match the project done      |
| **US-27** | **A Change of Language:** **As** a user **I want to** to be able to see the webpage in both Danish and English **such that** the page is easy to understand for me                  |
| **US-28** | **Expire Passwords:** **As** a system administrator **I want to** require passwords to expire after 3 months **such that** to harden the security of the system                     |

### User story analyse


---
For at danne et overblik over produktet og hvad de forskellige ord i user stories betyder, har vi valgt at lave en analyse af user stories 
med udgangspunkt i de mest centrale begreber. I analysen har vi kigget på hvilke navneord, tillægsord og udsagnsord der optræder i de forskellige
user stories. Det fortæller nemlig ofte hvilke entities, attributes og relations, produktet skal bestå af. Dette kan senere i udviklingsprocessen 
bruges at få et hurtigt overblik over hvad databasen skal bestå af, samt hvordan de forskellige roller og entities intetagerer med hinanden.

| Ord           | Ordklasse  | Domain model type |   | Ord          | Ordklasse  | Domain model type |         
|:--------------|:-----------|:------------------|:-:|:-------------|:-----------|:------------------|
| Administrator | Navneord   | User role         |   | Invitation   | Navneord   | Entity            |
| Advisor       | Navneord   | User role         |   | Advise       | Udsagnsord | Relation          |
| Student       | Navneord   | User role         |   | Invite       | Udsagnsord | Relation          |
| Study leader  | Navneord   | User role         |   | Join         | Udsagnsord | Atribute          |
| Project       | Navneord   | Entity            |   | Reject       | Udsagnsord | Atribute          |
| Study line    | Navneord   | Entity            |   | Send         | Udsagnsord | Relation          |
| Description   | Navneord   | Atribute          |   | Administrate | Udsagnsord | Relation          |

På baggrund af vores user story analyse har oversat ovenstående tabel til en domain model, som har til formål at visualiserer systemet arkitektur, og
samtidig skabe en sammenhængen mellem de forskellige attributes, entities og relations. Pilene forklarer med en kort forklarende tekst forholdet.
**"1"** og **"*"** fortæller os om kardinaliteten i forholdet, hvis vi for eksempel kigger forholdet mellem **administrator** og **study line** er
"one-to-many", hvilket fortæller os at _en administrator_ kan lave flere _study lines_, men at _en study line_ kun kan blive lavet af _en administrator_. 
Kigger vi i stedet på forholdet mellem **advisor** og **project**, som er "many-to-many", kan det beskrives som at _mange advisors_ kan oprette og vejlede
_mange projekter_, og at et/mange projekter godt kan have flere advisors. 

![sequence diagram](rapport-items/domain-model.png)

### System roller

---
Kigger man de forskellige user stories igennem, bliver det hurtigt klart, at kunden forventer et system, 
hvor brugerne kan tildeles forskellige roller med dertilhørende rettigheder. Det har derfor været essentielt 
at få skabt et overblik over, hvilke roller der findes i applikationen, og hvad de hver især har tilladelse til.
På baggrund af de givet user stories har vi derfor lavet et overblik over de forskellige roller og hvilket rettigheder
vi forventer de skal, så vi senere i udviklingen hurtigt kan få et overblik og rollerne. 

**Any user**
- Login
- Ændre navn
- Ændre sprog
- Slette egen bruger

**Administrator** 
- Oprette studielinjer 
- Tildele roller


**Study leader**
- Tilføje students til sin linje
- Råde over deres tildelte linje
- Kontakte students

**Advisor**
- Vejlede studerende
- Se en oversigt over projekter som de kan vejlede
- Se en oversigt over projekter de vejleder
- Sætte et workload så de ikke får mange projekter de ikke har tid til
- Oprette advisor projekter
- Opdaterer projekt navne eller beskrivelser

**Student**
- Oprette projekter
- Sætte en status på projektet
- Tilføje/invitere andre students til et projekt
- Accepterer eller afslå invitation til andre projekter
- Tilføje/invitere advisors til et projekt
- Tilslutte sig advisor projekter
- Sætte en studie start dato

### Prioritering af user stories (MoSCoW model) 

---
Vi har valgt at lave en MoSCoW til nemt at få et overblik over hvordan vi skulle prioriterer de forskellige user stories.
Da vi arbejder med en kunde er det meget vigtigt at få forventningsafstemt deres forventninger med vores. For så vidt
muligt at imødekomme kunden krav og behov. Vi kommer mere ind på, [senere i afsnittet](#Forventningsafstemning-af-krav-med-kunden),
hvordan de forskellige modeller i dette afsnit har ændret sig i forbindelse med de snakke vi har haft med kunden
undervejs.
### Must Have
*Kritiske user stories, der SKAL være med i vores Minimum Viable Product (MVP). Uden disse fungerer systemet ikke.*

| ID        | User Story                                                                                                                                                                     |
|:----------|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **US-1**  | **Login:** **As** any user of the site **I want to** be able to authenticate myself **such that** I can get a personalized view and options                                    |
| **US-2**  | **Create A Study Line:** **As** the administrator **I want to** create study lines **such that** I can assign students and study leaders                                       |
| **US-3**  | **Create Advisors:** **As** the administrator **I want to** assign users as advisors **such that** they can advise students                                                    |
| **US-4**  | **Assign Study Leaders:** **As** the administrator **I want to** assign a study leader to a study line **such that** they can administrate the students of that line           |
| **US-5**  | **Assign Students:** **As** a study leader **I want to** assign students to a study line **such that** I can monitor and help them complete their projects                     |
| **US-8**  | **Advisor Project Choice:** **As** an advisor **I want to** see a list of potential projects **such that** I can choose some to advise                                         |
| **US-10** | **Project Creation:** **As** a student **I want to** to create a project **such that** it can be chosen by an advisor                                                          |
| **US-12** | **Multistudent Project:** **As** a student **I want to** to add other students to my project **such that** I can work with them on the project                                 |
| **US-13** | **Project Collaborator Invitation:** **As** a student **I want to** accept or reject an invitation to be part of a project **such that** I can choose which project to work on |
| **US-14** | **Project Advisor Invitation:** **As** a student **I want to** see a list of potential advisors **such that** I can send a request to join the project                         |

---

### Should Have
*Vigtige funktioner, der skaber stor værdi for brugeren, men som kan udskydes, hvis tiden bliver for knap.*

| ID        | User Story                                                                                                                                                                          |
|:----------|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **US-9**  | **Advisor Project Dashboard:** **As** an advisor **I want to** see a list of projects I have accepted to advise **such that** I can have an overview of the projects I'm advising   |
| **US-11** | **Project Draft Creation:** **As** a student **I want to** save the project as a draft **such that** I do not have to publish an unfinished project                                 |
| **US-16** | **Advisor Workload:** **As** an advisor **I want to** set my acceptable workload **such that** students do not contact me when I am full                                            |
| **US-19** | **Advisor Project Creation:** **As** an advisor **I want to** publish a potential project **such that** students can choose it                                                      |
| **US-23** | **Add student progress:** **As** a student **I want to** to be able set my start date **such that** the system can calculate approximate start dates for internship and the project |
| **US-25** | **Invite co-advisors:** **As** an advisor **I want to** to be able to invite students and other advisors to the project **such that** I can get a co-advisors                       |
| **US-28** | **Expire Passwords:** **As** a system administrator **I want to** require passwords to expire after 3 months **such that** to harden the security of the system                     |

---

### Could Have
*Nice-to-have features. Disse kigger vi kun på, hvis vi er foran tidsplanen.*

| ID        | User Story                                                                                                                                                                     |
|:----------|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **US-20** | **Have a Cookie Notice:** **As** any user **I want to** to be informed about the use of cookies **such that** my rights are upheld                                             |
| **US-21** | **Delete Users:** **As** any user **I want to** to be able to have my account deleted **such that** my rights are upheld                                                       |
| **US-22** | **Change name:** **As** any user **I want to** to be able change my name and information **such that** be represented as I prefer                                              |
| **US-26** | **Update of project:** **As** an advisor **I want to** to be able update the name and abstract of the project after start **such that** the description match the project done |

---

### Won't Have
*Funktioner vi aktivt har valgt fra i denne omgang for at holde fokus.*

| ID        | User Story                                                                                                                                                         |
|:----------|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **US-27** | **A Change of Language:** **As** a user **I want to** to be able to see the webpage in both Danish and English **such that** the page is easy to understand for me |
 

### Forventningsafstemning af krav med kunden

---
I dette projekt er kravene til produktet blevet præsenteret i user stories. Selvom user stories fortæller meget om 
hvad kunden forventer er det stadig ikke fuldstændig tydeligt, derfor er det vigtigt at have en aktiv dialog med 
kunden undervejs i udviklingen af produktet for at tydelig gøre kravene kunden har. Vi har undervejs været til to 
møder med kunden, hvor vi haft mulighed for at få et bedre indblik i hvad kunden forventer applikationen skal kunne. 
Dette har gjort det nemmere at lave MoSCowW-prioritering af de forskellige user stories, da det gennem dialogen blev 
tydeligt, hvilke user stories der reelt skabte den største værdi. Allerede under det første møde blev det for eksempel 
tydeligt, at kundens behov for at en bruger skulle kunne oprette projekter (US-10). Dette var ikke krav vi havde vurderet
til at være must have, men samtalen gjorde at det blev prioriteret. Samtidig blev vi også klogere på domainet. Gennem
dialogen med kunden blev det meget tydeligt hvilke relationer der var i mellem brugere, projekter, study lines osv... 

### Planlægning og Design1

> _Hvordan kan vi løse det?_

Der er to dele til denne del, en beskrivelse af det system som I vil udvikle, men også en prioritering sådan at ting bliver udviklet i den rigtige orden.

Ting der er gode at få med:

- [x] Brudt User Stories ned i delsystemer.
  - Et delsystem er noget som er mere koblet end resten.
  - Bevægelse / Fængsel / Prøv Lykken / .. osv.
- [x] En beskrivelse af planlægningen.
  - Hvilke user stories afhænger af hvilke og er der tid nok til at nå de forskellige user stories.
  - Måske bruge et Gantt' Diagram.
- [x] En beskrivelse af hvordan planen har ændret sig over tid.
- [x] En beskrivelse af arkitekturen (pakke niveau).
  - Hvordan har I brudt jeres kode op i pakker sådan at I får høj sammenhæng og lav coupling.
  - Har I designet delsystemer sådan at I kan arbejde på dem parallelt?
  - Har I gjort jer nogle ideer omkring test.
- [ ] En beskrivelse af de delsystemer I vil udvikle.
  - Her kan I bruge aktivitet og sekvens diagrammer til at beskrive opførsel hvis den er anderledes
    end AS-IS.

## Planlægning og design
I dette afsnit vil vi gennemgå gruppens planlægning igennem projektets forløb, og i sammenhæng kigge på de designmæssige
tiltag der er blevet taget for at sikre at gruppen kunne efterleve denne planlægning. I vores diskussion vil vi vende tilbage til
planlægningen, hvor der reflekteres over hvad vi som 
gruppe vil gøre anderledes i fremtidige projekter. 

### Tilgang til User Stories 
Ved projektets start blev der sat tid af til, at hvert gruppemedlem kunne sætte sig ned og danne sig et overblik over projektet, 
dets user stories og den tilhørende kodebase. Dette blev gjort for at udarbejde en handleplan, 
der afspejlede den eksisterende kodebase, samt for at finde ud af, 
hvordan vi i praksis kunne implementere de givne user stories. Inden vores første møde ("M1") med kunden blev der derfor kun implementeret en meget begrænset mængde kode.

Igennem projektet blev det klart for os at vi kunne implementere forskellige User stories på samme tid, uden implementationerne overlappede hinanden.
Altså kunne vi dele det overordnede system op i del-opgaver der i starten kunne implementeres selvstændigt og virke som sit eget system. På den måde kunne vi 
imellem M1 og M2 arbejde sideløbende på flere forskellige systemer, der var med til at løse hver deres User stories.  

Det handlede derfor for os, som en start, at udvælge nogle 
user stories hvor de tilhørende implementationer kunne fungere uafhængigt af hinanden. Et eksempel på dette kunne være US10 (Project Creation) & US2 (Create A Study Line), 
der i første omgang godt kunne implementeres uden funktionelt overlap. Når begge user stories blev implementeret selvstændigt, 
kunne der herefter arbejdes på at skabe sammenhængende funktioner, som at give et projekt in tilhørende studielinje, eller på at vise alle projekter der tilhører en studielinje.

### Planlægning

For at have den ovenstående tilgang til vores user stories, krævede det at vi som gruppe kunne uddelegere de forskellige del-opgaver. 
Derfor aftalte vi i vores gruppe kontrakt, at der som udgangspunkt skulle mødes 2-3 gange om ugen. Dette bidragede til at skabe overblik over vores fremskridt 
med user stories, og uddelegere user stories og del opgaver der afhænger af nogle af de delsystemer der allerede er blevet eller ville blive
implementeret. 

For at give et indtryk af vores fremgangsmåde har vi, efter vores sidste møde ("F1"), lavet nedenstående Gantt-diagram for at vise 
hvordan vi har grebet de forskellige user stories an. Gantt diagrammet viser blandt andet hvilke arbejdsopgaver der kunne løses uden nogle foregående
funktionaliteter, og hvilke user stories der direkte afhænger af løsningen på andre del-opgaver. For at gøre diagrammet mere overskueligt 
fungere de forskellige møder med kunden som en "nulstilling" for at gøre diagrammet mere overskueligt.



![Gantt-diagram](rapport-items/Gantt_diagram.png)

Som det ses i Gantt diagrammet har vi arbejdet ud fra nogle møder med vores kunde, der har fungeret som deadlines af udvalgte user stories.
I den anledning kunne møderne også bruges til at undersøge hvad der ligges mest vægt på fra kundens side, og hvad der skal tilrettes
af det der allerede var blevet udviklet.

Inden vores andet møde med kunden ("M2"), aftalte vi at have produceret et "minimal viable product" (MVP), ud fra de tilgængelige user stories, 
og de indtryk vi havde fået fra kundens prioriteringer. Dette viste sig at fungere som en god milepæl for vores projekt, da de udvalgte user stories lagde et godt fundament
for de user stories vi blev præsenteret for efter mødet. 

op til mødet var vi også tvunget til at tage nogle beslutninger om hvilke user stories vi i første omgang ikke havde tid
til at færdiggøre, hvilket også blev gjort ud fra de indtryk vi havde fået fra kunden. Vi valgte blandt andet at fokusere mest på at etablere 
noget funktionelt i forhold til at få oprettet projekter, og etablere en kommunikation, hvilket var et af de punkter kunden lagde vægt på. 
Her valgte vi også at fravælge udvalgte user stories som US23 (add student progress), hvilket vi også har defineret som et "should have" i 
stedet for et must have i vores MoSCoW analyse.

Efter vores andet møde ("M2") og frem mod den endelige præsentation af vores produkt ("F1"), kunne vi fokusere på at få udviklet nogle af de user stories vi ikke nåede 
frem til mødet, samt de nye user stories der allerede byggede på noget af den funktionalitet vi havde implementeret. Gruppen planlagde at der, de sidste 2 dage op til mødet 
ikke skulle udvikles nye funktionaliteter, men fokuseres på JUnit test, samt at raffinere vores produkt og gøre det klar til at blive præsenteret for kunden. 

Dette resulterede i at vi nåede alle user stories undtagen US27 (A change of language), som vi også har fravalgt os i vores MoSCoW analyse, for at holde fokus på de 
egentlige funktionaliteter. Det bør også noteres at et andet sprog ville kunne tilføjes uden at skulle modificere noget af den i forvejen implementerede logik. 


##### Ændringer efter M1
Inden det første møde med kunden havde vi lagt en plan for den handlingsplan vi gerne ville præsentere kunden. Dette viste sig
at være overflødigt, da kunden havde nogle andre forventninger end dem vi indledningsvis forventede. Derfor måtte vi
omlægge vores plan til at bedre kunne imødekomme kunden forventninger. Til vores fordel havde vi valgt ikke at begynde 
at producere noget kode før M1.

##### Ændringer efter M2
Under mødet med kunden blev vores tolkning af rolle systemet udfordret. Det resulterede i at vi var nødt til at sætte tid af til at omskrive 
vores rolle system, og tilhørende systemer. Vi valgte her at fortsætte med at implementere nye funktionaliteter mens rollesystemet blev omskrevet. 
Konsekvenserne af dette vil vi reflektere yderligere over i vores diskussion. 

### Arkitektur
Systemet er opbygget efter en "layered" arkitektur, hvor de forskellige lag har forskellige funktionaliteter og formål.
Denne opdeling praktiserer "separation of concerns", hvilket gør systemet mere skalerbar, testbar og nemmere at vedligeholde **[1]**. 


Den overordnede filstruktur i projektet afspejler den her "layered" arkitektur, og er organiseret i følgende hovedområder: 
- **domain** indeholder domænemodeller og entiteter, der repræsenterer systemets og dets forretningsobjekter. Klasserne i domain afspejler den data der arbejdes med i systemet på tværs af lagene.  
- **repository** står alene for at kommunikere med databasen. Her implementeres der SQL-operationer, der anvendes til oprettelse, sletning og hentning af data. 
Ved at samle database adgangen i ét lag, fremmer vi også **"high cohesion"**, da laget har et meget veldefineret ansvar.  
- **services** indeholder projektets forretningslogik, og klasser som "ProjectManager" fungerer som et bindeled mellem view-laget og repository-laget. Service-laget koordinerer kald til 
repository laget, håndterer domæne logik og gør det muligt at genbruge kode på tværs af systemet. Dette er med til at fremme **"low coupling"** da service-laget abstraherer repository-laget
væk fra view laget, så de enkelte lag kan ændres uafhængigt af hinanden.
- **view** håndterer præsentationen af data fra repository laget over for brugeren, og gør brug af Thymeleaf og Spring boot til at vise og hente data. 
- **resources** indeholder database migrations, HTML-skabeloner og statiske filer som CSS og JavaScript.

Ved at bruge denne arkitektur, har vi en klar opdeling af ansvar, hvor ændringer i ét lag, har minimal påvirkning på de øvrige lag. Det har givet os fleksibilitet i udviklingen
og gjort det nemmere for os at arbejde parallelt på de forskellige dele af systemet.

### Delsystemer



### Implementering

> _Hvad fik vi lavet?_

Denne sektion er dedikeret til at beskrive hvad I har lavet.

Ting der er gode at få med:

- [ ] En forklaring af hvilke user stories I har nået, og på hvilke tidspunkter.
- [ ] En liste over de værktøjer, biblioteker, og ekstern kode I har brugt med versioner.
  - Java version, GUI_Library, IDE version, osv.
- [ ] En beskrivelse af hvordan man kører jeres kode.
- [ ] Et overblik over koden.
  - Her kan I bruge et klasse diagram til at beskrive hvordan det ser ud.
- [ ] En gennemgang af de patterns I har gjort brug af.
  - Dependency Inversion/Injection, MVC, Observer Pattern, GoF, osv...
  - Her kan I bruge klasse diagrammer og sekvens diagrammer.
  
- [ ] En forklaring af specielet interessant eller vigtig kode.
  - Her kan I bruge sekvens diagrammer, tilstands diagrammer, eller kode eksempler.

![sequence diagram](rapport-items/create_project_sequence.png)

Husk at bruge de rigtige pile i klasse-diagrammet (By Yanpas - Own work, [CC BY-SA 4.0](https://commons.wikimedia.org/w/index.php?curid=48015014)):

<img src="graphics/uml_classes_en.svg" width="250pt" />

### Validering

> _Virker det?_

I denne sektion skal I forklare hvorfor I tænker at koden I har afleveret virker.
I kan beskrive hvilke ekstra steps I har taget for at få accepttest til at kører nemt.

Ting der er gode at få med:

- [ ] En beskrivelse af hvilke systemer som koden er blevet testet på.
  - Linux, MacOSX, Windows?
  - Versioner af Java?
- [ ] En gennem gang af de accepttest der lykkedes og ikke lykkedes.
- [ ] En forklaring af brug af unit-test og code coverage.
  - Screen shots fra jeres IDE med code coverage.

### Diskussion

> _Hvad har vi lært?_

I denne sektion er en refleksion over hvad I mangler, hvordan gruppearbejdet er gået og hvad I gerne vil have lavet, hvis I har haft mere tid.
Dette er et godt sted at rede en karakter, hvis man har lavet nogle fejl :).

Ting der er gode at få med:

- [ ] Hvad gik godt, hvad gik dårligt?
- [ ] Næste gang, hvad ville I gøre anderledes?
- [ ] Hvad har I lært?

**HUSK AT SKRIV OM VORES MÆRKELIGE ROLLE SYSTEM**
**REFLEKTER OVER HVAD VI KUNNE HAVE GJORT ANDERLEDES I FORHOLD TIL PLANLÆGNINGEN**


## Kilder

- [1]: GeeksForGeeks, "Separation of Concerns (SoC)", 2024. https://www.geeksforgeeks.org/software-engineering/separation-of-concerns-soc/