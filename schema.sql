-- ====================================================
-- ApexDrive Car Rental System - MySQL Database Schema
-- ====================================================

CREATE DATABASE IF NOT EXISTS apexdrive_db;
USE apexdrive_db;

-- Table: customers
CREATE TABLE IF NOT EXISTS customers (
    customer_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    license_number VARCHAR(50) NOT NULL UNIQUE,
    phone_number VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table: cars
CREATE TABLE IF NOT EXISTS cars (
    car_id INT AUTO_INCREMENT PRIMARY KEY,
    model_name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    rate_per_day DECIMAL(10, 2) NOT NULL,
    transmission VARCHAR(30) NOT NULL,
    seats INT NOT NULL,
    fuel_type VARCHAR(50) NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    is_available BOOLEAN DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table: rentals
CREATE TABLE IF NOT EXISTS rentals (
    rental_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL,
    car_id INT NOT NULL,
    rental_days INT NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    rental_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE,
    FOREIGN KEY (car_id) REFERENCES cars(car_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Insert Initial Fleet Data
INSERT INTO cars (model_name, category, rate_per_day, transmission, seats, fuel_type, image_url) VALUES
('BMW M8 Gran Coupe', 'Luxury', 240.00, 'Automatic', 4, 'Gasoline', 'https://images.unsplash.com/photo-1555215695-3004980ad54e?auto=format&fit=crop&w=800&q=80'),
('Mercedes-Benz S-Class', 'Luxury', 280.00, 'Automatic', 5, 'Hybrid', 'https://images.unsplash.com/photo-1618843479319-5624704b2383?auto=format&fit=crop&w=800&q=80'),
('Audi e-tron GT', 'Electric', 260.00, 'Automatic', 4, 'Electric', 'https://images.unsplash.com/photo-1606152421802-db97b9c7a11b?auto=format&fit=crop&w=800&q=80'),
('Tesla Model S Plaid', 'Electric', 250.00, 'Automatic', 5, 'Electric', 'https://images.unsplash.com/photo-1617788138017-80ad40651399?auto=format&fit=crop&w=800&q=80'),
('Ford Mustang GT', 'Sports', 180.00, 'Manual', 4, 'Gasoline', 'https://images.unsplash.com/photo-1584345604476-8ac5e3cedf77?auto=format&fit=crop&w=800&q=80'),
('Porsche 911 Carrera', 'Sports', 350.00, 'Automatic', 2, 'Gasoline', 'https://images.unsplash.com/photo-1503376780353-7e6692767b70?auto=format&fit=crop&w=800&q=80'),
('Range Rover Velar', 'SUV', 220.00, 'Automatic', 7, 'Diesel', 'https://images.unsplash.com/photo-1563720223185-11003d516935?auto=format&fit=crop&w=800&q=80'),
('Honda Civic Type R', 'Sports', 140.00, 'Manual', 4, 'Gasoline', 'https://images.unsplash.com/photo-1609521263047-f8f205293f24?auto=format&fit=crop&w=800&q=80');
