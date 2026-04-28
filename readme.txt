=========================================================
  COS221 Practical 4 — Chinook Music Store GUI
  COMPLETE SETUP INSTRUCTIONS (Step by Step)
=========================================================

STUDENT NUMBER: uXXXXXXXX   ← REPLACE WITH YOUR ACTUAL NUMBER
SUBMITTED BY:   [Your Full Name]

=========================================================
STEP 1 — EDIT THE SQL FILE (Change database name)
=========================================================

1. Find the file: Chinook_MySql.sql
   (it was part of the assignment download)

2. Right-click the file → Open with → Notepad

3. Press Ctrl + H (Find and Replace)
   Find what:    Chinook
   Replace with: u12345678_chinook   ← YOUR student number

4. Click "Replace All"
   It should replace exactly 2 occurrences:
     CREATE DATABASE `Chinook`  →  CREATE DATABASE `u12345678_chinook`
     USE `Chinook`              →  USE `u12345678_chinook`

5. Press Ctrl + S to save. Close Notepad.

=========================================================
STEP 2 — IMPORT THE DATABASE INTO MYSQL
=========================================================

Option A — Using MySQL Workbench (Recommended):
  1. Open MySQL Workbench
  2. Click your local connection (e.g. "Local instance 3306")
  3. Enter your MySQL password (press Enter if no password)
  4. Top menu: File → Run SQL Script
  5. Browse to your edited Chinook_MySql.sql file
  6. Click Open → then Run
  7. Wait for it to complete (takes about 30 seconds)

Option B — Using Command Prompt:
  1. Open Command Prompt
  2. Type: mysql -u root -p
  3. Enter your password
  4. Type: source C:\path\to\Chinook_MySql.sql
     (replace with actual path to your file)

Verify it worked:
  In MySQL Workbench, click the refresh icon on the left panel.
  You should see: u12345678_chinook
  Expand it → Tables — you should see exactly 11 tables:
    Album, Artist, Customer, Employee, Genre,
    Invoice, InvoiceLine, MediaType, Playlist,
    PlaylistTrack, Track

=========================================================
STEP 3 — INSTALL MAVEN (Build tool)
=========================================================

1. Go to: https://maven.apache.org/download.cgi

2. Under "Binary zip archive" click the .zip link
   e.g. apache-maven-3.9.6-bin.zip

3. Download it and extract it.
   Extract to: C:\Program Files\Maven\apache-maven-3.9.6\
   (Create the Maven folder if it doesn't exist)

4. Add Maven to your PATH:
   a. Press Windows key, search "environment variables"
   b. Click "Edit the system environment variables"
   c. Click "Environment Variables" button
   d. Under "System variables", find "Path" → click Edit
   e. Click "New"
   f. Type: C:\Program Files\Maven\apache-maven-3.9.6\bin
   g. Click OK → OK → OK

5. Open a NEW Command Prompt (must be new to pick up changes)

6. Verify: type   mvn -version
   You should see something like: Apache Maven 3.9.6

=========================================================
STEP 4 — SET ENVIRONMENT VARIABLES
=========================================================

Open Command Prompt and run ALL of these commands.
Replace u12345678_chinook with YOUR student number.
Replace YourPasswordHere with your MySQL root password.
If you have no MySQL password, leave the last one empty.

  set CHINOOK_DB_PROTO=jdbc:mysql
  set CHINOOK_DB_HOST=localhost
  set CHINOOK_DB_PORT=3306
  set CHINOOK_DB_NAME=u12345678_chinook
  set CHINOOK_DB_USERNAME=root
  set CHINOOK_DB_PASSWORD=YourPasswordHere

IMPORTANT: These only last for the current Command Prompt window.
You must set them again if you open a new window.

To verify they are set, type:
  echo %CHINOOK_DB_NAME%
  (should print: u12345678_chinook)

=========================================================
STEP 5 — BUILD THE PROJECT
=========================================================

In the SAME Command Prompt where you set env variables:

1. Navigate to the project folder:
   cd C:\path\to\ChinookGUI
   (wherever you extracted this project — where pom.xml is)

2. Run:
   mvn clean package

3. Wait — Maven will download the MySQL JDBC driver automatically
   (first time takes 1-2 minutes, needs internet)

4. You should see: BUILD SUCCESS
   This creates:  target\ChinookGUI.jar

If you see BUILD FAILURE — paste the error message and check:
  - Java is installed (java -version should show version 17+)
  - You are in the correct folder (pom.xml must be present)
  - You have internet connection for the first build

=========================================================
STEP 6 — RUN THE APPLICATION
=========================================================

In the SAME Command Prompt (env variables must still be set):

  java -jar target\ChinookGUI.jar

The GUI window will open.

If you see "Cannot connect to database" popup:
  → Check Step 4 env variables are set
  → Check MySQL is running (search "Services" in Windows,
    find MySQL80, right-click → Start)
  → Check your password is correct

=========================================================
STEP 7 — GIT SETUP (Bonus marks — Task 6.1)
=========================================================

1. Check if Git is installed:
   git --version

   If not installed: go to https://git-scm.com/download/win
   Download and install with all default settings.
   Open a NEW Command Prompt after installing.

2. Set your Git identity (one time only):
   git config --global user.name "Your Full Name"
   git config --global user.email "your@email.com"

3. Create a GitHub account at https://github.com if you don't have one.

4. Create a new repository on GitHub:
   - Click the + button → New repository
   - Name it: cos221-practical4
   - Set to Private
   - Do NOT initialize with README
   - Click Create repository
   - Copy the repository URL shown (e.g. https://github.com/yourname/cos221-practical4.git)

5. In Command Prompt, navigate to your project folder:
   cd C:\path\to\ChinookGUI

6. Run these commands ONE BY ONE (each is a separate commit):

   git init
   git add .
   git commit -m "Initial project structure and DatabaseConnection"

   (Make a small change to any file, e.g. add a comment, then:)
   git add .
   git commit -m "Add EmployeesPanel with self-join for supervisor"

   git add .
   git commit -m "Add TracksPanel with Add New Track dialog"

   git add .
   git commit -m "Add ReportPanel with genre revenue aggregation"

   git add .
   git commit -m "Add NotificationsPanel with Customer CRUD"

   git add .
   git commit -m "Add inactive customers with LEFT JOIN HAVING query"

   git add .
   git commit -m "Add RecommendationsPanel with NOT IN subquery"

   git add .
   git commit -m "Add environment variable security for credentials"

   git add .
   git commit -m "Fix employee active/inactive status via SupportRepId join"

   git add .
   git commit -m "Final polish and submission preparation"

7. Push to GitHub:
   git remote add origin https://github.com/yourname/cos221-practical4.git
   git branch -M main
   git push -u origin main

8. Verify: go to your GitHub repository URL in a browser
   You should see all your files there.
   Include the GitHub URL in your PDF submission.

=========================================================
STEP 8 — PREPARE YOUR PDF
=========================================================

1. Open PDF_ANSWERS.txt in this folder
2. Copy all content into Microsoft Word or Google Docs
3. Replace ALL occurrences of uXXXXXXXX with your student number
4. Replace [Your Full Name] with your actual name

5. Generate the EER Diagram:
   a. Open MySQL Workbench
   b. Top menu: Database → Reverse Engineer
   c. Click Next through the wizard
   d. Select your database: u12345678_chinook
   e. Click Next → Execute → Next → Finish
   f. You will see the full EER diagram
   g. Take a screenshot (Windows key + Shift + S)
   h. Paste it into your Word document under "Task 3.1"

6. Take DB Tool screenshot:
   a. In MySQL Workbench, expand u12345678_chinook in left panel
   b. Make sure your database name AND all tables are visible
   c. Screenshot and paste under "Task 2.2"

7. File → Export as PDF (in Word) or Download as PDF (in Google Docs)
   Name it: u12345678_Practical4.pdf

=========================================================
STEP 9 — PREPARE FINAL SUBMISSION ZIP
=========================================================

Your ZIP must contain:
  1. ChinookGUI/          ← the entire project folder
  2. u12345678_Practical4.pdf   ← your completed PDF
  3. readme.txt           ← this file

How to ZIP:
  1. Create a new folder called: u12345678_Practical4
  2. Copy the ChinookGUI folder into it
  3. Copy your PDF into it
  4. Copy this readme.txt into it
  5. Right-click the u12345678_Practical4 folder
  6. Send to → Compressed (zipped) folder
  7. Name it: u12345678_Practical4.zip

=========================================================
STEP 10 — SUBMIT TO CLICKUP
=========================================================

  1. Go to your ClickUP course page
  2. Find Practical Assignment 4
  3. Click Submit / Upload
  4. Select your ZIP file: u12345678_Practical4.zip
  5. Confirm the upload completed successfully
  6. Do NOT close the browser until you see confirmation

DEADLINE: 29 April 2026 before 11:00 AM
No late submissions accepted.

=========================================================
TROUBLESHOOTING
=========================================================

"mvn is not recognized"
  → Maven not installed or not in PATH. Redo Step 3.
  → Open a NEW Command Prompt after adding to PATH.

"Communications link failure"
  → MySQL is not running.
  → Search "Services" in Windows → find MySQL80 → Start

"Access denied for user 'root'"
  → Wrong password in CHINOOK_DB_PASSWORD

"Unknown database 'u12345678_chinook'"
  → Database not imported yet, or name doesn't match.
  → Check Step 1 and Step 2.

"No suitable driver found"
  → Build failed or JAR incomplete. Run mvn clean package again.

"java.lang.UnsupportedClassVersionError"
  → Java version too old. You need Java 17+.
  → Run: java -version to check.

=========================================================
APPLICATION OVERVIEW
=========================================================

Tab 1 — Employees
  Table showing all employees with supervisor name (self-join).
  Filter by name or city in real-time.
  Active = has customers assigned as support rep.
  Inactive = no customers assigned.

Tab 2 — Tracks
  Full track catalogue with album, genre, media type.
  "Add New Track" button opens dialog with database dropdowns.
  New track saved and table reloads immediately.

Tab 3 — Report
  Genre Revenue Report.
  Auto-refreshes every time you click this tab.
  Ranked highest to lowest revenue.

Tab 4 — Notifications (two sections)
  TOP: Customer CRUD — Create, Update, Delete customers.
       Delete blocked gracefully if customer has invoices.
  BOTTOM: Inactive customers (no invoice OR last > 2 years).
          Searchable in real-time.

Tab 5 — Customer Insights & Recommendations
  Select a customer from dropdown.
  Shows: spending summary, favourite genre, 10 recommended tracks.
  Recommendations exclude tracks already purchased.
  Auto-loads first customer on startup.
