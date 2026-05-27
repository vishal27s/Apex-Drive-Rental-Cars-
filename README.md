APEXDRIVE - ALL-IN-ONE OFFLINE CAR RENTAL SYSTEM (JAVA OOP + MYSQL + HTML/CSS)
================================================================================

This package contains everything you need to run the Car Rental System entirely offline on your local machine. It connects the HTML/CSS frontend directly to your MySQL database using Java's built-in HttpServer and JDBC.

--------------------------------------------------------------------------------
STEP 1: DATABASE SETUP (MySQL)
--------------------------------------------------------------------------------
1. Open your MySQL client (e.g. MySQL Workbench, DBeaver, or command line).
2. Ensure your local MySQL server is running on localhost:3306.
3. Open `schema.sql` (or copy the SQL block at the top of `OfflineCarRentalSystem.java`) and execute it.
   This will create the `apexdrive_db` database, the `customers`, `cars`, and `rentals` tables, and insert sample vehicles.

--------------------------------------------------------------------------------
STEP 2: COMPILE & RUN THE JAVA SERVER
--------------------------------------------------------------------------------
Open your terminal/command prompt in the directory containing `OfflineCarRentalSystem.java` and run:

    javac OfflineCarRentalSystem.java
    java OfflineCarRentalSystem

Note: Make sure you have the MySQL JDBC Driver (`mysql-connector-j.jar`) in your classpath if it's not globally configured:
    javac OfflineCarRentalSystem.java
    java -cp .:mysql-connector-j-8.0.33.jar OfflineCarRentalSystem   (Linux/Mac)
    java -cp .;mysql-connector-j-8.0.33.jar OfflineCarRentalSystem   (Windows)

--------------------------------------------------------------------------------
STEP 3: ACCESS THE WEBSITE
--------------------------------------------------------------------------------
Once the server starts, open your web browser and navigate to:

    http://localhost:8080

You will see the fully functional web application! 
- Browse the live fleet pulled directly from MySQL.
- Submit a booking to see it instantly committed to the `rentals` table.
- Use the Admin Console at the bottom to insert new cars into the `cars` table in real-time.

--------------------------------------------------------------------------------
STEP 4: WEBSITES OVERVIEW
--------------------------------------------------------------------------------
🏠 Home Section
<img width="1918" height="856" alt="apex 1" src="https://github.com/user-attachments/assets/1dd2c552-0238-4d59-a938-396edabe5338" />
The Home page introduces users to the premium car rental experience with:

Elegant hero section
Luxury typography and modern UI
Featured premium vehicle showcase
Call-to-action buttons for booking
Company statistics section
Years of excellence
Premium cars available
Happy clients count

The landing page is designed to immediately attract users with a high-end automotive aesthetic.
----------------------------------------------------------------------------------

ℹ️ About Us Section
<img width="1920" height="877" alt="apex 2" src="https://github.com/user-attachments/assets/2f29e198-b6dc-4865-a5ca-76cd35db2745" />


The About section explains the vision and quality standards of ApexDrive.

Includes:
Brand introduction
Premium service presentation
Feature cards showcasing:
Curated luxury vehicles
Peace of mind & insurance support
Concierge customer service

This section emphasizes trust, exclusivity, and customer satisfaction.
----------------------------------------------------------------------------------

🚗 Booking Section
<img width="1918" height="841" alt="apex 3" src="https://github.com/user-attachments/assets/7b1ef06d-9a04-455a-b9d7-fbefcf0353e5" />
The Booking section allows users to explore and reserve luxury vehicles.

Features:
Premium vehicle cards
Car pricing per day
Vehicle specifications
Transmission type
Seating capacity
Fuel type
Interactive booking form
Categorized luxury fleet

The design ensures a clean and smooth reservation experience for users.
----------------------------------------------------------------------------------
📞 Contact Section
<img width="1895" height="855" alt="apex 4" src="https://github.com/user-attachments/assets/b6768c74-e3be-46ab-9370-3e108032a094" />

The Contact section enables customers to easily connect with the company.

Includes:
Showroom location
Contact number
Email information
Customer inquiry form
Professional support layout

This section provides a premium communication experience with a polished interface.
