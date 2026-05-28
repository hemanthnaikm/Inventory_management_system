# Inventory Management System

## Introduction
This project is a desktop-based inventory dashboard developed in Java using the Swing framework. It establishes a direct connection to a local MySQL database via JDBC to manage product data in real time. The application is designed to give users a clean interface to track items, monitor quantities, and handle stock registry workflows efficiently.

<img width="1483" height="928" alt="image" src="https://github.com/user-attachments/assets/e94f8c1f-ad6e-4231-a85b-57e6a1a3c232" />

## How to Setup

Follow these steps to configure your local environment and run the desktop application:

1. **Verify Prerequisites**: Ensure you have Java JDK 11 or higher installed on your system. You will also need a local instance of the MySQL Server database engine running and an IDE such as Eclipse.
2. **Setup the Database Credentials**: Open the project source file `InventoryApp.java` inside your workspace. Navigate to the database configuration parameters at the top of the file and ensure that the `USERNAME` and `PASSWORD` fields match your local MySQL server setup (the default configured value is user "root" with password "1122").
3. **Add Dependencies**: This application requires the official MySQL Connector/J driver to establish connectivity between the Java environment and the database backend. Download the `mysql-connector-j-x.x.x.jar` file and add it to your project's Build Path or external library references in Eclipse.
4. **Compile and Build**: Refresh the project tree inside your development environment to resolve dependencies. Compile the code to ensure that all class structures, swing layouts, and SQL driver imports resolve correctly.
5. **Run the Initialization**: Locate and run the `InventoryApp.java` file as a Java application. On its initial launch cycle, the engine will safely connect using universal parameters to discover whether the target database exists. If missing, it automatically creates the database schema `inventory_management_db` and builds the storage tables.
6. **Manage Inventory Records**: Use the input dashboard panel on the left to add your product data. Fill in the SKU, Name, Quantity, Price, and Status fields, then submit entries to watch the persistent inventory tracking sheets and real-time visualization graphs refresh instantly.

## How It Works
When the application is launched, it attempts to connect to your local MySQL server using specified host, port, and password credentials. It automatically creates the target database and the required inventory table if they do not already exist. 

Once running, users can input item details such as SKU, product name, quantity, and price. When a product is added, the data is sent directly to the SQL database, and the local table views update instantly. The application divides products into two distinct visual tabs based on their status: available items are shown in the stock registry, while completed sales move to the sold registry. A custom-drawn graphics panel reads these data structures to paint live quantity bar charts on the screen.

<img width="1918" height="1008" alt="image" src="https://github.com/user-attachments/assets/0430d903-0341-48c6-a8f2-a1e8b8212d24" />

## Key Points
* Automated Database Initialization: The software automatically generates the database schema and table structures upon its first execution.
* Direct SQL Connectivity: Uses a robust JDBC URL configuration to handle authentication and prevent SSL handshake errors during local connections.
* Dual Tab Integration: Automatically separates active products from completed transactions to keep the workspace organized.
* Custom Metrics Visualization: Features a dedicated graphics canvas that calculates and renders a dynamic bar chart tracking item metrics.

<img width="1050" height="762" alt="image" src="https://github.com/user-attachments/assets/16b73c95-1419-4d07-a55a-3bdde19fa9c2" />

### Advantages
* Seamless Setup: Eliminates the need for manual SQL script execution since the application configures its own database tables.
* Real-Time Updates: Any modification, addition, or deletion instantly synchronizes between the interface and the database server.
* Lightweight Design: Operates with minimal system resources and does not require complex external web servers to run.
* Offline Reliability: Works entirely on a local machine, making it ideal for standalone workstations or local environments.

<img width="1448" height="536" alt="image" src="https://github.com/user-attachments/assets/5dd4eb66-916c-4644-a124-59184ca03357" />

### Disadvantages
* Hardcoded Credentials: The database connection details are written directly into the source code, which requires manual updates if network settings or passwords change.
* Scalability Constraints: Designed as a single-user desktop system rather than a distributed, multi-user web application.
* Local Dependency: Requires a MySQL server instance to be actively running on the host machine to execute properly.

## Conclusion
This Inventory Management System serves as a practical desktop solution for handling local stock tracking and business records. By combining Java Swing for the user interface with a structured MySQL backend, the application ensures reliable data persistence, automated setup, and real-time visual reporting for everyday operational workflows.

<img width="1920" height="1020" alt="image" src="https://github.com/user-attachments/assets/8a669184-c8ac-46fc-97ac-79b7edcee16b" />
