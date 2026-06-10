# to-do & user story analysis

## Project overview 
- View: UI / brugerinput
- Services: use cases og forretningslogik
- Domain: hvad systemet handler om (User, Email osv.)
- Repository: databaseadgang
- H2Repository: konkret implementation mod H2-databasen

### US2 Create A Study Line: As the administrator I want to create study lines so that I can assign students and study leaders
- **"As the administrator"**
  - meaning there must be an administrator role. This could be implemented in the model - maybe extending the user?
  - Should also reflect the state of the database 
- **"i want to create study lines"**
  - we need to implement the study line - also in the model? 
  - Should also reflect the state of the database 
    - a student can only be part of one study line, while a study line can have multiple students
    - a student must have a study line. 
    - although a study line can hold 0 students. 
- **"so that i can assign students and study leaders"**
  - student and study leaders must also be a user role. 
  - Should each role be its own class? 
    - Should also reflect the state of the database
  - Different roles → different functionalities and different access. 
